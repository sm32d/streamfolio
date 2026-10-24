import SwiftUI
import UIKit
import StreamFolioCore

/// What a share sheet presents for an article.
struct ShareContent: Identifiable {
    let id = UUID()
    let items: [Any]

    init(article: Article, actions: ArticleActions) {
        let item = actions.shareItem(for: article)
        items = [item.text]
    }
}

/// The system share sheet, for share actions that are triggered from a swipe or menu (a toolbar
/// button uses `ShareLink` instead).
struct ActivityView: UIViewControllerRepresentable {
    let items: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: items, applicationActivities: nil)
    }

    func updateUIViewController(_ controller: UIActivityViewController, context: Context) {}
}
