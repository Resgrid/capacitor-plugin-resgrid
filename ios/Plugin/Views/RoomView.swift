import SwiftUI
import KeychainAccess
import LiveKitClient
import SFSafeSymbols
import WebRTC
import AVKit

@available(iOS 15.0, *)
struct RoomView: View {
    
    @EnvironmentObject var appCtx: AppContext
    @EnvironmentObject var roomCtx: RoomContext
    @EnvironmentObject var room: CustomObservableRoom
    
    @State private var screenPickerPresented = false
    @State private var showConnectionTime = true

    var body: some View {
        
        NavigationView {
            GeometryReader { geometry in
                content(geometry: geometry)
            }
            .toolbar {
                ToolbarItemGroup(placement: .bottomBar) {
                    
                    Text("(\(room.room.remoteParticipants.count)) ")

                    // VideoView mode switcher
                    Picker("Mode", selection: $appCtx.videoViewMode) {
                        Text("Fit").tag(VideoView.LayoutMode.fit)
                        Text("Fill").tag(VideoView.LayoutMode.fill)
                    }
                    .pickerStyle(SegmentedPickerStyle())
                    
                    Spacer()
                    
                    Group {
                        // Toggle camera enabled
                        Button(action: {
                            room.toggleCameraEnabled()
                        },
                               label: {
                            Image(systemSymbol: .videoFill)
                                .renderingMode((room.room.localParticipant?.isCameraEnabled() ?? false) ? .original : .template)
                        })
                        // disable while publishing/un-publishing
                        .disabled(room.cameraTrackState.isBusy)
                        
                        if (room.room.localParticipant?.isCameraEnabled() ?? false) && CameraCapturer.canSwitchPosition() {
                            
                            Menu {
                                Button("Switch position") {
                                    room.switchCameraPosition()
                                }
                                Button("Disable") {
                                    room.toggleCameraEnabled()
                                }
                            } label: {
                                Image(systemSymbol: .videoFill)
                                    .renderingMode(.original)
                            }
                            
                        }
                        
                        // Toggle microphone enabled
                        Button(action: {
                            room.toggleMicrophoneEnabled()
                        },
                               label: {
                            Image(systemSymbol: .micFill)
                                .renderingMode((room.room.localParticipant?.isMicrophoneEnabled() ?? false) ? .original : .template)
                        })
                        // disable while publishing/un-publishing
                        .disabled(room.microphoneTrackState.isBusy)
                        
                        Button(action: {
                            room.toggleScreenShareEnablediOS()
                        },
                               label: {
                            Image(systemSymbol: .rectangleFillOnRectangleFill)
                                .renderingMode(room.screenShareTrackState.isPublished ? .original : .template)
                        })
                        
                        // Toggle messages view (chat example)
                        Button(action: {
                            withAnimation {
                                room.showMessagesView.toggle()
                            }
                        },
                               label: {
                            Image(systemSymbol: .messageFill)
                                .renderingMode(room.showMessagesView ? .original : .template)
                        })
                        
                    }
                    
                    // Spacer()
                    
                    //#if os(iOS)
                    //SwiftUIAudioRoutePickerButton()
                    AVRoutePicker()
                    //#endif
                    
                    Menu {
                        
                        Toggle("Show info overlay", isOn: $appCtx.showInformationOverlay)
                        
                        Group {
                            Toggle("VideoView visible", isOn: $appCtx.videoViewVisible)
                            Toggle("VideoView preferMetal", isOn: $appCtx.preferMetal)
                            Toggle("VideoView flip", isOn: $appCtx.videoViewMirrored)
                            Divider()
                        }
                        
                        Divider()
                        
                        Button {
                            Task {
                                try await roomCtx.room.unpublishAll()
                            }
                        } label: {
                            Text("Unpublish all")
                        }
                        
                        Divider()
                        
                        Menu {
                            Button {
                                roomCtx.room.room.sendSimulate(scenario: .nodeFailure)
                            } label: {
                                Text("Node failure")
                            }
                            
                            Button {
                                roomCtx.room.room.sendSimulate(scenario: .serverLeave)
                            } label: {
                                Text("Server leave")
                            }
                            
                            Button {
                                roomCtx.room.room.sendSimulate(scenario: .migration)
                            } label: {
                                Text("Migration")
                            }
                            
                            Button {
                                roomCtx.room.room.sendSimulate(scenario: .speakerUpdate(seconds: 3))
                            } label: {
                                Text("Speaker update")
                            }
                            
                        } label: {
                            Text("Simulate scenario")
                        }
                        
                        Group {
                            Menu {
                                Button {
                                    roomCtx.room.room.localParticipant?.setTrackSubscriptionPermissions(allParticipantsAllowed: true)
                                } label: {
                                    Text("Allow all")
                                }
                                
                                Button {
                                    roomCtx.room.room.localParticipant?.setTrackSubscriptionPermissions(allParticipantsAllowed: false)
                                } label: {
                                    Text("Disallow all")
                                }
                            } label: {
                                Text("Track permissions")
                            }
                            
                            Toggle("Prefer speaker output", isOn: $appCtx.preferSpeakerOutput)
                        }
                        
                    } label: {
                        Image(systemSymbol: .gear)
                            .renderingMode(.original)
                    }
                    
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
    
    func messageView(_ message: RoomMessage) -> some View {

        let isMe = message.senderSid == room.room.localParticipant?.sid

        return HStack {
            if isMe {
                Spacer()
            }

            //            VStack(alignment: isMe ? .trailing : .leading) {
            //                Text(message.identity)
            Text(message.text)
                .padding(8)
                .background(isMe ? .red : .gray)
                .foregroundColor(.white)
                .cornerRadius(18)
            //            }
            if !isMe {
                Spacer()
            }
        }.padding(.vertical, 5)
        .padding(.horizontal, 10)
    }

    func scrollToBottom(_ scrollView: ScrollViewProxy) {
        guard let last = room.messages.last else { return }
        withAnimation {
            scrollView.scrollTo(last.id)
        }
    }

    func messagesView(geometry: GeometryProxy) -> some View {

        VStack(spacing: 0) {
            ScrollViewReader { scrollView in
                ScrollView(.vertical, showsIndicators: true) {
                    LazyVStack(alignment: .center, spacing: 0) {
                        ForEach(room.messages) {
                            messageView($0)
                        }
                    }
                    .padding(.vertical, 12)
                    .padding(.horizontal, 7)
                }
                .onAppear(perform: {
                    // Scroll to bottom when first showing the messages list
                    scrollToBottom(scrollView)
                })
                .onChange(of: room.messages, perform: { _ in
                    // Scroll to bottom when there is a new message
                    scrollToBottom(scrollView)
                })
                .frame(
                    minWidth: 0,
                    maxWidth: .infinity,
                    minHeight: 0,
                    maxHeight: .infinity,
                    alignment: .topLeading
                )
            }
            HStack(spacing: 0) {
                
                TextField("Enter message", text: $room.textFieldString)
                    .textFieldStyle(PlainTextFieldStyle())
                    .disableAutocorrection(true)
                // TODO: add iOS unique view modifiers
                // #if os(iOS)
                // .autocapitalization(.none)
                // .keyboardType(type.toiOSType())
                // #endif
                
                //    .overlay(RoundedRectangle(cornerRadius: 10.0)
                //        .strokeBorder(Color.white.opacity(0.3),
                //                      style: StrokeStyle(lineWidth: 1.0)))
                
                Button {
                    room.sendMessage()
                } label: {
                    Image(systemSymbol: .paperplaneFill)
                        .foregroundColor(room.textFieldString.isEmpty ? nil : .red)
                }
                .buttonStyle(.borderless)
            }
            .padding()
            .background(.black)
        }
        .background(.black)
        .cornerRadius(8)
        .frame(
            minWidth: 0,
            maxWidth: geometry.isTall ? .infinity : 320
        )
    }

    func sortedParticipants() -> [ObservableParticipant] {
        room.allParticipants.values.sorted { p1, p2 in
            if p1.participant is LocalParticipant { return true }
            if p2.participant is LocalParticipant { return false }
            return (p1.participant.joinedAt ?? Date()) < (p2.participant.joinedAt ?? Date())
        }
    }

    func content(geometry: GeometryProxy) -> some View {

        VStack {

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

            HorVStack(axis: geometry.isTall ? .vertical : .horizontal, spacing: 5) {

                Group {
                    if let focusParticipant = room.focusParticipant {
                        ZStack(alignment: .bottomTrailing) {
                            ParticipantView(participant: focusParticipant,
                                            videoViewMode: appCtx.videoViewMode) { _ in
                                room.focusParticipant = nil
                            }
                            .overlay(RoundedRectangle(cornerRadius: 5)
                                        .stroke(.red.opacity(0.7), lineWidth: 5.0))
                            Text("SELECTED")
                                .font(.system(size: 10))
                                .fontWeight(.bold)
                                .foregroundColor(.white)
                                .padding(.horizontal, 5)
                                .padding(.vertical, 2)
                                .background(.red.opacity(0.7))
                                .cornerRadius(8)
                                .padding(.vertical, 35)
                                .padding(.horizontal, 10)
                        }

                    } else {
                        // Array([room.allParticipants.values, room.allParticipants.values].joined())
                        ParticipantLayout(sortedParticipants(), spacing: 5) { participant in
                            ParticipantView(participant: participant,
                                            videoViewMode: appCtx.videoViewMode) { participant in
                                room.focusParticipant = participant

                            }
                        }
                    }
                }
                .frame(
                    minWidth: 0,
                    maxWidth: .infinity,
                    minHeight: 0,
                    maxHeight: .infinity
                )
                // Show messages view if enabled
                if room.showMessagesView {
                    messagesView(geometry: geometry)
                }
            }
        }
        .padding(5)
    }
}