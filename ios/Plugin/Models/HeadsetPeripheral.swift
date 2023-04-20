import Foundation
import CoreBluetooth

class HeadsetPeripheral: NSObject {

    // AINA PTT Responder Headset
    public static let AINA_HEADSET = CBUUID.init(string: "D11C8116-A913-434D-A79D-97AE94A529B3")
    public static let AINA_HEADSET_SERVICE = CBUUID.init(string: "127FACE1-CB21-11E5-93D0-0002A5D5C51B")
    public static let AINA_HEADSET_SERVICE_PROP = CBUUID.init(string: "127FBEEF-CB21-11E5-93D0-0002A5D5C51B")
}
