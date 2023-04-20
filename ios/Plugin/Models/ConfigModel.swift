import SwiftUI

class ConfigModel: ObservableObject {
    var url: String
    var token: String
    var type: Int
    var rooms: [RoomInfoModel] = []
    var title: String
    var defaultMic: String
    var defaultSpeaker: String
    
    init(url: String, token: String, type: Int, rooms: [RoomInfoModel], title: String, defaultMic: String, defaultSpeaker: String) {
        self.url = url
        self.token = token
        self.type = type
        self.rooms = rooms;
        self.title = title;
        self.defaultMic = defaultMic;
        self.defaultSpeaker = defaultSpeaker;
    }
}
