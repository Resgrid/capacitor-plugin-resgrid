import UIKit
import SwiftUI

@available(iOS 15.0, *)
class MainModalView: UIViewController {
    var hostview: UIHostingController<RoomContextView>!
    var viewModel: ConfigModel!

    override func viewDidLoad() {
        super.viewDidLoad()

        hostview = UIHostingController<RoomContextView>(rootView: RoomContextView(viewModel: viewModel))

        addChild(hostview)
        view.addSubview(hostview.view)
        hostview.didMove(toParent: self)
        //hostview.sizingOptions = [.preferredContentSize]
        hostview.view.frame = self.view.bounds
    }
}
