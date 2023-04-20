import Foundation

struct RoomInfoModel: Identifiable, Hashable, Codable {
    var name: String
    var id: String
    var token: String

    init(name: String, id: String, token: String) {
        self.name = name
        self.id = id
        self.token = token
    }

    static func == (lhs: RoomInfoModel, rhs: RoomInfoModel) -> Bool {
        return lhs.id == rhs.id
    }

    func hash(into hasher: inout Hasher) {
        hasher.combine(id)
    }
}
