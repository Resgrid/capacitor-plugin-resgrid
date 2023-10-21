import SwiftUI
import KeychainAccess
import LiveKitClient
import SFSafeSymbols
import WebRTC
import AVKit

@available(iOS 15.0, *)
struct AudioRoomView: View {

    @EnvironmentObject var appCtx: AppContext
    @EnvironmentObject var roomCtx: RoomContext
    @EnvironmentObject var room: CustomObservableRoom

    @State private var screenPickerPresented = false
    @State private var showConnectionTime = true
    @State var isLongPressing = false
    @State var isDeviceConnected = false
    @State var deviceId = "";

    @State private var deviceManager: DeviceManager?

    @State private var timer: Timer! = nil

    let CONNECTION_TIMEOUT: Double = 10
    let DEFAULT_TIMEOUT: Double = 5

    var body: some View {

        NavigationView {
            GeometryReader { geometry in
                content(geometry: geometry)
            }.toolbar {
                        ToolbarItemGroup(placement: .navigationBarLeading) {
                            Text("(\(room.room.remoteParticipants.count)) ")

                            Divider()

                            // Select Headset
                            Menu {
                                Section {
                                    Button(action: {
                                        disconnectHeadset()
                                    }) {
                                        Label("Disconnect", systemImage: "minus")
                                    }
                                    Button(action: {
                                        deviceManager = DeviceManager({ (success, message) -> Void in
                                            if success {
                                                connectToHeadset()
                                            } else {
                                                isDeviceConnected = false
                                            }
                                        })

                                    }) {
                                        Label("Connect", systemImage: "plus")
                                    }
                                }
                            } label: {
                                Image(systemSymbol: .phoneFill)
                                    .renderingMode(isDeviceConnected ? .original : .template)
                            }

                            Divider()

                            Group {
                                // Toggle microphone enabled
                                Button(action: {
                                    self.toggleTransmitting()
                                },
                                        label: {
                                            Image(systemSymbol: .micFill)
                                                    .renderingMode((room.room.localParticipant?.isMicrophoneEnabled() ?? false) ? .original : .template)
                                        })
                                        // disable while publishing/un-publishing
                                        .disabled(room.microphoneTrackState.isBusy)

                            }

                            Divider()

                            AVRoutePicker()
                            Divider()

                            // Disconnect
                            Button(action: {
                                Task {
                                    try await roomCtx.disconnect()
                                }
                            },
                                    label: {
                                        Image(systemSymbol: .xmarkCircleFill)
                                                .renderingMode(.original)
                                    })
                        }
                    }
                    .onAppear {

                        Timer.scheduledTimer(withTimeInterval: 3, repeats: false) { _ in
                            DispatchQueue.main.async {
                                withAnimation {
                                    self.showConnectionTime = false
                                }
                            }
                        }
                    }
        }
    }
    
    func disconnectHeadset() {
        guard let device = deviceManager!.getDevice(deviceId) else {
            return
        }
        
        deviceManager!.disconnect(device, CONNECTION_TIMEOUT, bleDisconnectComplete)
        
    }
    
    func bleDisconnectComplete(_ success: Bool, _ message: String) -> Void {
        if success {
            log("Disconnected from device")
            isDeviceConnected = false
        }
    }
    
    func connectToHeadset() {
        deviceManager!.startScanning(
                [HeadsetPeripheral.AINA_HEADSET_SERVICE],
                nil,
                nil,
                false,
                false,
                30,
                bleScanComplete,
                { (_, _, _) -> Void in }
        )
    }

    func bleScanComplete(_ success: Bool, _ message: String) -> Void {
        // selected a device
        if success {
            guard let device = deviceManager!.getDevice(message) else {
                return
            }
            log("Scanning complete, attempting to connect.")
            deviceId = message
            
            device.setOnConnected(CONNECTION_TIMEOUT, { (success2, message2) -> Void in
                if success2 {
                    log("Connected to device")
                    isDeviceConnected = true
                    
                    device.setNotifications(
                            HeadsetPeripheral.AINA_HEADSET_SERVICE,
                            HeadsetPeripheral.AINA_HEADSET_SERVICE_PROP,
                            true,
                            processBluetoothEvents,
                            CONNECTION_TIMEOUT,
                            { (success, value) -> Void in
                                if success {
                                    //call.resolve()
                                } else {
                                    //call.reject(value)
                                }
                            })

                    // only resolve after service discovery
                    //call.resolve()
                } else {
                    //call.reject(message)
                    isDeviceConnected = false
                }
            })
            self.deviceManager?.setOnDisconnected(device, { (_, _) -> Void in
                //let key = "disconnected|\(device.getId())"
                //self.notifyListeners(key, data: nil)
            })
            self.deviceManager?.connect(device, CONNECTION_TIMEOUT, { (success3, message3) -> Void in
                if success3 {
                    log("Connected to peripheral. Waiting for service discovery.")
                } else {
                    //call.reject(message)
                }
            })

            //self.deviceMap[device.getId()] = device
            //let bleDevice: BleDevice = self.getBleDevice(device)
            //call.resolve(bleDevice)
        } else {
            isDeviceConnected = false
        }
    }

    func processBluetoothEvents(_ success: Bool, _ message: String) -> Void {
        if success {
            log("ble message: " + message)

            let trimmedMessage = message.trimmingCharacters(in: .whitespacesAndNewlines)
            
            if (trimmedMessage == "00") {
                stopTransmitting()
            } else if (trimmedMessage == "04" || trimmedMessage == "01") {
                startTransmitting()
            }
        }
    }

    func stopTransmitting() {
        if room.room.localParticipant?.isMicrophoneEnabled() == true {
            let audio = Audio()
            audio.playStopTransmit()
            room.toggleMicrophoneEnabled()
        }
    }

    func startTransmitting() {
        if room.room.localParticipant?.isMicrophoneEnabled() == false {
            let audio = Audio()
            audio.playStartTransmit()
            room.toggleMicrophoneEnabled()
        }
    }

    func toggleTransmitting() {
        if room.room.localParticipant?.isMicrophoneEnabled() == true {
            stopTransmitting()
        } else {
            startTransmitting()
        }
    }

    func sortedParticipants() -> [ObservableParticipant] {
        room.allParticipants.values.sorted { p1, p2 in
            if p1.participant is LocalParticipant {
                return true
            }
            if p2.participant is LocalParticipant {
                return false
            }
            return (p1.participant.joinedAt ?? Date()) < (p2.participant.joinedAt ?? Date())
        }
    }

    func content(geometry: GeometryProxy) -> some View {
            VStack(alignment: .center) {
                if showConnectionTime {
                    Text("Connected (\([room.room.serverRegion, "\(String(describing: room.room.connectStopwatch.total().rounded(to: 2)))s"].compactMap { $0 }.joined(separator: ", ")))")
                        .multilineTextAlignment(.center)
                        .foregroundColor(.white)
                        .padding()
                }
                
                if case .connecting = room.room.connectionState {
                    Text("Re-connecting...")
                        .multilineTextAlignment(.center)
                        .foregroundColor(.white)
                        .padding()
                }
                Text("Current Channel: \(appCtx.selectedChannel!.name)")
                    .multilineTextAlignment(.center)
                    .foregroundColor(.white)
                    .padding()
                
                ZStack {
                    RoundedRectangle(cornerRadius: 10)
                        .fill(isLongPressing ? Color.red : Color.blue)
                        .frame(width: 260, height: 80).scaleEffect(isLongPressing ? 1.3 : 1)
                        .animation(.easeOut(duration: 0.2), value: isLongPressing)
                    Text(isLongPressing ? "Transmitting" : "Push and Hold To Talk")
                        .foregroundColor(.white)
                }.simultaneousGesture(
                    DragGesture(minimumDistance: 0)
                        .onChanged({ _ in
                            if isLongPressing == false {
                                if (room.room.localParticipant != nil && !room.room.localParticipant!.isMicrophoneEnabled()) {
                                    startTransmitting()
                                }
                            }
                            
                            isLongPressing = true
                        })
                        .onEnded({ _ in
                            isLongPressing = false
                            
                            if (room.room.localParticipant != nil && room.room.localParticipant!.isMicrophoneEnabled()) {
                                stopTransmitting()
                            }
                            
                        })
                )
                Spacer()
                HorVStack(axis: geometry.isTall ? .vertical : .horizontal, spacing: 0) {
                    Group {
                        ParticipantLayout(sortedParticipants(), spacing: 0) { participant in
                            ParticipantView(participant: participant,
                                            videoViewMode: appCtx.videoViewMode) { participant in
                                room.focusParticipant = participant
                                
                            }
                        }
                    }.frame(minWidth: 0, maxWidth: 1, minHeight: 0, maxHeight: 1)
                }
        }.frame(
            minWidth: 0,
            maxWidth: .infinity,
            minHeight: 0,
            maxHeight: .infinity)
        
    }
}
