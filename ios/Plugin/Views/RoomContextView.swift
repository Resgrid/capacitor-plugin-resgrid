import SwiftUI
import KeychainAccess

let sync = ValueStore<Preferences>(store: Keychain(service: "com.resgrid.plugin.livekit.1"),
        key: "preferences",
        default: Preferences())

@available(iOS 15.0, *)
struct RoomContextView: View {

    @StateObject var viewModel: ConfigModel
    @StateObject var appCtx = AppContext(store: sync)
    @StateObject var roomCtx = RoomContext(store: sync)

    var shouldShowRoomView: Bool {
        roomCtx.room.room.connectionState.isConnected || roomCtx.room.room.connectionState.isReconnecting
    }

    func computeTitle() -> String {
        if shouldShowRoomView {
            let elements = [roomCtx.room.room.name,
                            roomCtx.room.room.localParticipant?.name,
                            roomCtx.room.room.localParticipant?.identity]
            return elements.compactMap {
                        $0
                    }
                    .filter {
                        !$0.isEmpty
                    }
                    .joined(separator: " ")
        }

        return "LiveKit"
    }

    init(viewModel: ConfigModel) {
        self._viewModel = StateObject(wrappedValue: viewModel)
    }

    var body: some View {
        ZStack(alignment: .top) {
            Color.black.ignoresSafeArea()
            if shouldShowRoomView {
                if (self.viewModel.type == 1) {
                    RoomView()
                } else if (self.viewModel.type == 0) {
                    AudioRoomView()
                }
            } else {
                if roomCtx.room.room.connectionState == .disconnected() {
                    VStack(alignment: .center) {
                        Text("You are disconnected, select a channel to connect")
                                .foregroundColor(.white).padding()
                        Picker(selection: $appCtx.selectedChannel,
                                label: Text("Selected Channel"),
                                content: {
                                    Text("Select Channel").tag(nil as RoomInfoModel?)
                                    Divider()
                                    ForEach(self.viewModel.rooms) { room in
                                        Text(room.name).tag(room as RoomInfoModel?)
                                    }
                                })
                                .padding().font(.headline)
                        let action: () -> Void = {

                            roomCtx.url = viewModel.url
                            guard let selectedChannel = appCtx.selectedChannel else {
                                return
                            }
                            roomCtx.token = selectedChannel.token

                            Task {
                                let room = try await roomCtx.connect()
                                appCtx.connectionHistory.update(room: room)
                            }
                        }
                        Button(action: action, label: {
                            Text("Connect")
                        })
                                .cornerRadius(8).padding().buttonStyle(.borderedProminent)
                        Spacer()
                    }
                } else {
                    ProgressView().padding()
                }
            }
        }
                .environment(\.colorScheme, .dark)
                .foregroundColor(Color.white)
                .environmentObject(appCtx)
                .environmentObject(roomCtx)
                .environmentObject(roomCtx.room)
                .onAppear(perform: {
                    roomCtx.url = viewModel.url

                    //if (appCtx.selectedChannel != nil) {
                    //    roomCtx.token = viewModel.token
                    //} else {
                    //    roomCtx.token = viewModel.token
                    //}

                    //Task {
                    //    let room = try await roomCtx.connect()
                    //    appCtx.connectionHistory.update(room: room)
                    //}
                })
                //.onDisappear {
                //    print("\(String(describing: type(of: self))) onDisappear")
                //    Task {
                //        try await roomCtx.disconnect()
                //    }
                //}
                .onOpenURL(perform: { url in

                    guard let urlComponent = URLComponents(url: url, resolvingAgainstBaseURL: false) else {
                        return
                    }
                    guard let host = url.host else {
                        return
                    }

                    let secureValue = urlComponent.queryItems?.first(where: { $0.name == "secure" })?.value?.lowercased()
                    let secure = ["true", "1"].contains {
                        $0 == secureValue
                    }

                    let tokenValue = urlComponent.queryItems?.first(where: { $0.name == "token" })?.value ?? ""

                    var builder = URLComponents()
                    builder.scheme = secure ? "wss" : "ws"
                    builder.host = host
                    builder.port = url.port

                    guard let builtUrl = builder.url?.absoluteString else {
                        return
                    }

                    print("built URL: \(builtUrl), token: \(tokenValue)")

                    Task { @MainActor in
                        roomCtx.url = builtUrl
                        roomCtx.token = tokenValue
                        if !roomCtx.token.isEmpty {
                            let room = try await roomCtx.connect()
                            appCtx.connectionHistory.update(room: room)
                        }
                    }
                })
    }
}
