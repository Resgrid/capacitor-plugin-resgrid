import Swift
import SwiftUI
import UIKit
import Foundation

class SelfSizingHostingController<Content>: UIHostingController<Content> where Content: View {
    private var heightConstraint: NSLayoutConstraint?

    override func viewDidLoad() {
        heightConstraint = view.heightAnchor.constraint(equalToConstant: 0)
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        //view.invalidateIntrinsicContentSize()
        view.sizeToFit()
       // heightConstraint?.constant = view.bounds.height
       // heightConstraint?.isActive = true
        view.invalidateIntrinsicContentSize()
    }
}
