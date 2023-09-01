import UIKit
import SwiftUI

@available(iOS 15.0, *)
class MainModalView: UIViewController {
    var hostview: UIHostingController<RoomContextView>!
    var viewModel: ConfigModel!

    override func viewDidLoad() {
        super.viewDidLoad()

        hostview = UIHostingController<RoomContextView>(rootView: RoomContextView(viewModel: viewModel))
        
        var screenWidth: CGFloat = UIScreen.main.bounds.size.width
        var width: CGFloat = self.view.bounds.width
        
        if screenWidth > 1000 {
            width = 800
        }
        
        addChild(hostview)
        view.addSubview(hostview.view)
        hostview.didMove(toParent: self)
        //hostview.sizingOptions = [.preferredContentSize]
        //hostview.view.frame = self.view.bounds
        //hostview.view.frame = CGRect(x: 0, y: 0, width: self.view.bounds.width - (self.view.bounds.width * 0.40), height: self.view.bounds.height)
        hostview.view.frame = CGRect(x: 0, y: 0, width: width, height: self.view.bounds.height)
        
    }
}
