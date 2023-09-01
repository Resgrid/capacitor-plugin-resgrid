import Foundation
import Capacitor
import AVFoundation
import CoreBluetooth

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */
@available(iOS 15.0, *)
@objc(ResgridPlugin)
public class ResgridPlugin: CAPPlugin {
    private var configModel: ConfigModel?
    private var roomViewController: MainModalView?

    @objc func start(_ call: CAPPluginCall) {
        let token = call.getString("token") ?? ""
        let url = call.getString("url") ?? ""
        let type = call.getInt("type") ?? 0
        let title = call.getString("title") ?? ""
        let defaultMic = call.getString("defaultMic") ?? ""
        let defaultSpeaker = call.getString("defaultSpeaker") ?? ""
        let roomsJS = call.getArray("rooms") ?? []
        let apiUrl = call.getString("apiUrl") ?? ""
        let canConnectToVoiceApiToken = call.getString("canConnectToVoiceApiToken") ?? ""

        var rooms: [RoomInfoModel] = []
        if (roomsJS != nil && roomsJS.count > 0) {
            for roomJS in roomsJS {
                if let roomObject = roomJS as? JSObject {
                    rooms.append(RoomInfoModel(name: roomObject["name"] as! String,
                                                id: roomObject["id"] as! String,
                                                token: roomObject["token"] as! String));
                }
            }
        }

        self.configModel = ConfigModel(url: url, token: token, type: type, rooms: rooms,
                title: title, defaultMic: defaultMic, defaultSpeaker: defaultSpeaker, apiUrl: apiUrl,
                canConnectToVoiceApiToken: canConnectToVoiceApiToken);

        call.resolve()
    }

    @objc override public func checkPermissions(_ call: CAPPluginCall) {
        var result: [String: Any] = [:]
        for permission in MicrophonePermissionType.allCases {
            let state: String
            switch permission {
            case .microphone:
                state = String(AVCaptureDevice.authorizationStatus(for: .audio).rawValue)
            }
            result[permission.rawValue] = state
        }
        call.resolve(result)
    }


    @objc override public func requestPermissions(_ call: CAPPluginCall) {
        let permissions: [MicrophonePermissionType] = MicrophonePermissionType.allCases

        let group = DispatchGroup()
        for permission in permissions {
            switch permission {
            case .microphone:
                group.enter()
                AVCaptureDevice.requestAccess(for: .audio) { _ in
                    group.leave()
                }
            }
        }
        group.notify(queue: DispatchQueue.main) { [weak self] in
            self?.checkPermissions(call)
        }
    }

    @objc func showModal(_ call: CAPPluginCall) {
        DispatchQueue.main.async {

            guard let bridge = self.bridge else {
                return
            }

            if let rootViewController = UIApplication.shared.keyWindow?.rootViewController {
                if self.roomViewController == nil {
                    self.roomViewController = MainModalView()
                    self.roomViewController?.viewModel = self.configModel
                    
                    if (self.configModel != nil && self.configModel?.type == 1) {
                        self.roomViewController?.modalPresentationStyle = .fullScreen
                    } else {
                        if let sheet = self.roomViewController?.sheetPresentationController {
                            sheet.detents = [.medium()]
                        }
                        self.roomViewController?.modalPresentationStyle = .pageSheet
                    }
                    
                    self.roomViewController?.view.frame = rootViewController.view.bounds
                    self.roomViewController?.view.tag = 325973359 // rand
                    self.roomViewController?.view.backgroundColor = .black
                    
                    bridge.viewController?.present(self.roomViewController!, animated: true, completion: nil)
                } else {
                    if (self.configModel != nil && self.configModel?.type == 1) {
                        self.roomViewController?.modalPresentationStyle = .fullScreen
                    } else {
                        if let sheet = self.roomViewController?.sheetPresentationController {
                            sheet.detents = [.medium()]
                        }
                        self.roomViewController?.modalPresentationStyle = .pageSheet
                    }

                    self.roomViewController?.view.frame = rootViewController.view.bounds
                    self.roomViewController?.view.tag = 325973359 // rand
                    self.roomViewController?.view.backgroundColor = .black

                    bridge.viewController?.present(self.roomViewController!, animated: true, completion: nil)
                }
            }
        }
        call.resolve()
    }
}
