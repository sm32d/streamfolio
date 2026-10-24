import Foundation

/// What can be done to an article, in one place. Swipe actions, context menus, the Detail toolbar and
/// (later) widget intents all call these, so behaviour — and the confirmation the user sees — is the
/// same everywhere. Android wires bookmark/share/read separately per screen.
public struct ArticleActions: Sendable {
    public enum Outcome: Sendable, Equatable {
        case saved(link: String)
        case unsaved(link: String)
        case markedRead(link: String)
        case markedUnread(link: String)
        case shared
        case none
    }

    private let articles: ArticleStore
    private let bookmarks: BookmarkStore

    public init(articles: ArticleStore, bookmarks: BookmarkStore) {
        self.articles = articles
        self.bookmarks = bookmarks
    }

    @discardableResult
    public func toggleSaved(_ article: Article) async -> Outcome {
        let save = !article.isBookmarked
        do {
            try await bookmarks.setSaved(save, link: article.link)
            return save ? .saved(link: article.link) : .unsaved(link: article.link)
        } catch {
            return .none
        }
    }

    @discardableResult
    public func toggleRead(_ article: Article) async -> Outcome {
        let read = !article.isRead
        do {
            try await articles.setRead(read, link: article.link)
            return read ? .markedRead(link: article.link) : .markedUnread(link: article.link)
        } catch {
            return .none
        }
    }

    public func markRead(_ article: Article) async {
        guard !article.isRead else { return }
        try? await articles.setRead(true, link: article.link)
    }

    /// Share text and URL for the system share sheet.
    public func shareItem(for article: Article) -> (text: String, url: URL?) {
        (ShareLinks.shareText(title: article.title, articleLink: article.link), ShareLinks.shareURL(for: article.link))
    }

    /// Runs a configured swipe action (`swipe_left_action` / `swipe_right_action`). Sharing is
    /// presentation, so it is reported back for the view to present.
    @discardableResult
    public func perform(swipe action: String, on article: Article) async -> Outcome {
        switch action {
        case "bookmark": await toggleSaved(article)
        case "read": await toggleRead(article)
        case "share": .shared
        default: .none
        }
    }
}
