import Foundation
import Observation

/// State behind the article page: the live article (save/read/image changes appear immediately), the
/// reader body, and the Reader / Web View choice. Android's `DetailScreen` + `loadArticleBody`.
@MainActor
@Observable
public final class ReaderStore {
    public enum Mode: String, Sendable, CaseIterable { case reader, web }

    public enum Content: Equatable, Sendable {
        case loading
        /// The full article text.
        case article([String])
        /// Only the feed's short description could be shown; suggest the Web View.
        case excerpt([String])
        /// Link-only publisher, or nothing readable at all.
        case webOnly
    }

    public private(set) var article: Article
    public private(set) var content: Content = .loading
    public var mode: Mode

    /// Below this many characters the extracted text is treated as a failed extraction.
    public static let minimumBodyLength = 150

    private let articles: ArticleStore
    private let service: ArticleContentService
    private let actions: ArticleActions
    @ObservationIgnored private var observation: Task<Void, Never>?

    public init(article: Article, articles: ArticleStore, service: ArticleContentService, actions: ArticleActions) {
        self.article = article
        self.articles = articles
        self.service = service
        self.actions = actions
        self.mode = ArticleCapabilities(for: article).canShowReader ? .reader : .web
    }

    public var capabilities: ArticleCapabilities { ArticleCapabilities(for: article) }
    public var readingTime: String { ReadingTime.label(for: article) }

    /// Marks the article read, keeps it live, finds a missing image and loads the body.
    public func open() async {
        observeArticle()
        await actions.markRead(article)
        guard capabilities.canShowReader else { content = .webOnly; return }

        if (article.thumbnailUrl ?? "").isEmpty || article.thumbnailUrl == ArticleContentService.thumbnailFailed {
            Task { await service.resolveThumbnail(for: article) }
        }
        content = Self.content(for: await service.body(for: article), description: article.description)
    }

    public func toggleSaved() async { await actions.toggleSaved(article) }

    /// The reader decision table (Android: body < 150 chars or a failure → show the description with a
    /// "tap Web View" hint).
    static func content(for body: ArticleContentService.Body, description: String) -> Content {
        func paragraphs(_ text: String) -> [String] {
            text.components(separatedBy: "\n\n").map(\.trimmed).filter { !$0.isEmpty }
        }
        switch body {
        case .notAllowed: return .webOnly
        case .text(let text) where text.count >= minimumBodyLength: return .article(paragraphs(text))
        case .text(let text): return .excerpt(paragraphs(description.isEmpty ? text : description))
        case .unparseable, .failed:
            return description.isEmpty ? .webOnly : .excerpt(paragraphs(description))
        }
    }

    private func observeArticle() {
        observation?.cancel()
        let link = article.link
        observation = Task { [articles] in
            do {
                for try await latest in articles.observe(link: link) {
                    guard let latest else { return }
                    self.article = latest
                }
            } catch {}
        }
    }

    deinit { observation?.cancel() }
}
