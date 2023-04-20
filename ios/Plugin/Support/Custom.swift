import SwiftUI
import SwiftUI
import LiveKitClient
import SFSafeSymbols
import WebRTC
import AVKit
import CoreBluetooth

struct LazyView<Content: View>: View {
    let build: () -> Content
    init(_ build: @autoclosure @escaping () -> Content) {
        self.build = build
    }
    var body: Content {
        build()
    }
}

// Default button style for this example
struct LKButton: View {

    let title: String
    let action: () -> Void

    var body: some View {

        Button(action: action,
               label: {
                Text(title.uppercased())
                    .fontWeight(.bold)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 10)
               }
        )
        .cornerRadius(8)
    }
}

#if os(iOS)
extension LKTextField.`Type` {
    func toiOSType() -> UIKeyboardType {
        switch self {
        case .default: return .default
        case .URL: return .URL
        case .ascii: return .asciiCapable
        }
    }
}
#endif

#if os(macOS)
// Avoid showing focus border around textfield for macOS
extension NSTextField {
    open override var focusRingType: NSFocusRingType {
        get { .none }
        set { }
    }
}
#endif

struct LKTextField: View {

    enum `Type` {
        case `default`
        case URL
        case ascii
    }

    let title: String
    @Binding var text: String
    var type: Type = .default

    var body: some View {
        VStack(alignment: .leading, spacing: 10.0) {
            Text(title)
                .fontWeight(.bold)

            TextField("", text: $text)
                .textFieldStyle(PlainTextFieldStyle())
                .disableAutocorrection(true)
                // TODO: add iOS unique view modifiers
                // #if os(iOS)
                // .autocapitalization(.none)
                // .keyboardType(type.toiOSType())
                // #endif
                .padding()
                .overlay(RoundedRectangle(cornerRadius: 10.0)
                            .strokeBorder(Color.white.opacity(0.3),
                                          style: StrokeStyle(lineWidth: 1.0)))

        }.frame(maxWidth: .infinity)
    }
}

extension CIImage {
    // helper to create a `CIImage` for both platforms
    convenience init(named name: String) {
        self.init(cgImage: UIImage(named: name)!.cgImage!)
    }
}

extension RTCIODevice: Identifiable {

    public var id: String {
        deviceId
    }
}


extension GeometryProxy {

    public var isTall: Bool {
        size.height > size.width
    }

    var isWide: Bool {
        size.width > size.height
    }
}

struct AVRoutePicker: UIViewRepresentable {
    func makeUIView(context: Context) -> AVRoutePickerView {
        let v = AVRoutePickerView()
        v.activeTintColor = .orange
        return v
    }

    func updateUIView(_ uiView: AVRoutePickerView, context: Context) {

    }

    typealias UIViewType = AVRoutePickerView
}

extension Decimal {
    mutating func round(_ scale: Int, _ roundingMode: NSDecimalNumber.RoundingMode) {
        var localCopy = self
        NSDecimalRound(&self, &localCopy, scale, roundingMode)
    }

    func rounded(_ scale: Int, _ roundingMode: NSDecimalNumber.RoundingMode) -> Decimal {
        var result = Decimal()
        var localCopy = self
        NSDecimalRound(&result, &localCopy, scale, roundingMode)
        return result
    }

    func remainder(of divisor: Decimal) -> Decimal {
        let s = self as NSDecimalNumber
        let d = divisor as NSDecimalNumber
        let b = NSDecimalNumberHandler(roundingMode: .down,
                scale: 0,
                raiseOnExactness: false,
                raiseOnOverflow: false,
                raiseOnUnderflow: false,
                raiseOnDivideByZero: false)
        let quotient = s.dividing(by: d, withBehavior: b)

        let subtractAmount = quotient.multiplying(by: d)
        return s.subtracting(subtractAmount) as Decimal
    }
}

func descriptorValueToString(_ value: Any) -> String {
    if let str = value as? String {
        return str
    }
    if let data = value as? Data {
        return dataToString(data)
    }
    if let uuid = value as? CBUUID {
        return uuid.uuidString
    }
    return ""
}

func dataToString(_ data: Data) -> String {
    var valueString = ""
    for byte in data {
        valueString += String(format: "%02hhx ", byte)
    }
    return valueString
}

func stringToData(_ dataString: String) -> Data {
    let hexValues = dataString.split(separator: " ")
    var data = Data(capacity: hexValues.count)
    for hex in hexValues {
        data.append(UInt8(hex, radix: 16)!)
    }
    return data
}

func cbuuidToString(_ uuid: CBUUID) -> String {
    // declare as optional because of https://github.com/capacitor-community/bluetooth-le/issues/170
    let uuidString: String? = uuid.uuidString
    var str = uuidString!.lowercased()
    if str.count == 4 {
        str = "0000\(str)-0000-1000-8000-00805f9b34fb"
    } else if str.count == 8 {
        str = "\(str)-0000-1000-8000-00805f9b34fb"
    }
    return str
}

func cbuuidToStringUppercase(_ uuid: CBUUID) -> String {
    let str = cbuuidToString(uuid)
    return str.uppercased()
}

struct GrowingButton: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        if #available(iOS 15.0, *) {
            configuration.label
                    .padding()
                    .frame(minWidth: 0,
                            maxWidth: 250,
                            minHeight: 0,
                            maxHeight: 80)
                    .background(configuration.isPressed ? Color.red : Color.blue)
                    .foregroundColor(.white)
                    .clipShape(Capsule())
                    .scaleEffect(configuration.isPressed ? 1.3 : 1)
                    .animation(.easeOut(duration: 0.2), value: configuration.isPressed)
        }
    }
}
