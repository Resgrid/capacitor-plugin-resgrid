import SwiftUI
import LiveKitClient
import WebRTC
import Combine

extension ObservableObject where Self.ObjectWillChangePublisher == ObservableObjectPublisher {
    func notify() {
        DispatchQueue.main.async { self.objectWillChange.send() }
    }
}

// This class contains the logic to control behavior of the whole app.
final class AppContext: ObservableObject {

    private let store: ValueStore<Preferences>

    @Published var videoViewVisible: Bool = true {
        didSet { store.value.videoViewVisible = videoViewVisible }
    }

    @Published var showInformationOverlay: Bool = false {
        didSet { store.value.showInformationOverlay = showInformationOverlay }
    }

    @Published var preferMetal: Bool = true {
        didSet { store.value.preferMetal = preferMetal }
    }

    @Published var videoViewMode: VideoView.LayoutMode = .fit {
        didSet { store.value.videoViewMode = videoViewMode }
    }

    @Published var videoViewMirrored: Bool = false {
        didSet { store.value.videoViewMirrored = videoViewMirrored }
    }

    @Published var selectedChannel: RoomInfoModel? = nil {
        didSet { store.value.selectedChannel = selectedChannel }
    }

    @Published var connectionHistory: Set<ConnectionHistory> = [] {
        didSet { store.value.connectionHistory = connectionHistory }
    }

    @Published var headsetDeviceId: String = "" {
        didSet { store.value.headsetDeviceId = headsetDeviceId }
    }

    @Published var outputDevice: RTCAudioDevice = RTCAudioDevice.defaultDevice(with: .output) {
        didSet {
            print("didSet outputDevice: \(String(describing: outputDevice))")

            if !Room.audioDeviceModule.setOutputDevice(outputDevice) {
                print("failed to set value")
            }
        }
    }

    @Published var inputDevice: RTCAudioDevice = RTCAudioDevice.defaultDevice(with: .input) {
        didSet {
            print("didSet inputDevice: \(String(describing: inputDevice))")

            if !Room.audioDeviceModule.setInputDevice(inputDevice) {
                print("failed to set value")
            }
        }
    }

    @Published var preferSpeakerOutput: Bool = true {
        didSet { AudioManager.shared.preferSpeakerOutput = preferSpeakerOutput }
    }

    public init(store: ValueStore<Preferences>) {
        self.store = store

        self.videoViewVisible = store.value.videoViewVisible
        self.showInformationOverlay = store.value.showInformationOverlay
        self.preferMetal = store.value.preferMetal
        self.videoViewMode = store.value.videoViewMode
        self.videoViewMirrored = store.value.videoViewMirrored
        self.connectionHistory = store.value.connectionHistory
        self.headsetDeviceId = store.value.headsetDeviceId

        // Setting selectedChannel to nil for now on init
        //self.selectedChannel = store.value.selectedChannel
        self.selectedChannel = nil;

        Room.audioDeviceModule.setDevicesUpdatedHandler {
            print("devices did update")
            // force UI update for outputDevice / inputDevice
            DispatchQueue.main.async {

                // set to default device if selected device is removed
                if !Room.audioDeviceModule.outputDevices.contains(where: { self.outputDevice == $0 }) {
                    self.outputDevice = RTCAudioDevice.defaultDevice(with: .output)
                }

                // set to default device if selected device is removed
                if !Room.audioDeviceModule.inputDevices.contains(where: { self.inputDevice == $0 }) {
                    self.inputDevice = RTCAudioDevice.defaultDevice(with: .input)
                }

                self.objectWillChange.send()
            }
        }
    }
}
