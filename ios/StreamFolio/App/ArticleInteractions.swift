import Observation
import StreamFolioCore

/// What the user can do to an article from any list: the configured swipe actions, the long-press
/// menu, and the two sheets they can open (share, save to collection). One instance serves every
/// screen, and `RootView` presents the sheets once, so Home, Search and Saved behave identically.
@MainActor
@Observable
final class ArticleInteractions {
    /// Article whose share sheet is showing.
    var sharing: ShareContent?
    /// Article whose "Save to Collection" sheet is showing.
    var managingCollections: Article?

    private let actions: ArticleActions

    init(actions: ArticleActions) {
        self.actions = actions
    }

    /// Runs a configured action name (`bookmark`, `share`, `read`) on `article`.
    func perform(_ action: String, on article: Article) {
        Task {
            if await actions.perform(swipe: action, on: article) == .shared { share(article) }
        }
    }

    func share(_ article: Article) {
        sharing = ShareContent(article: article, actions: actions)
    }

    func manageCollections(of article: Article) {
        managingCollections = article
    }
}
