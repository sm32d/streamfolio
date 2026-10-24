import Foundation
import os

/// Fetches an article's page and stores what the reader needs: the body text and the lead image.
///
/// Link-only publishers (publisher terms) are never fetched here — no page, no body, no image. That
/// check is the first line of every method that touches the network, so no caller has to remember it.
public struct ArticleContentService: Sendable {

    /// What the reader can show for an article.
    public enum Body: Sendable, Equatable {
        /// Readable article text (paragraphs separated by a blank line).
        case text(String)
        /// The page loaded but had no extractable text (paywall, script-rendered). Offer Web View.
        case unparseable
        /// The page could not be fetched.
        case failed
        /// Link-only publisher: the original page is the only way to read it.
        case notAllowed
    }

    /// Stored in `thumbnailUrl` when resolution found nothing, so it is not retried forever.
    public static let thumbnailFailed = "failed"

    private let store: ArticleStore
    private let http: any HTTPFetching
    private let log = Logger(subsystem: "uk.sume.streamfolio", category: "ArticleContent")

    public init(store: ArticleStore, http: any HTTPFetching) {
        self.store = store
        self.http = http
    }

    // MARK: Body

    /// The cached body if there is one, otherwise fetched, extracted and cached.
    public func body(for article: Article) async -> Body {
        guard ArticleCapabilities(for: article).canShowReader else { return .notAllowed }
        if let cached = article.fullText, !cached.isEmpty { return .text(cached) }

        guard let page = await fetchPage(article.link) else { return .failed }
        guard let text = ArticleExtractor.body(fromHTML: page.html, baseURL: page.url) else { return .unparseable }
        try? await store.setFullText(text, link: article.link)
        return .text(text)
    }

    /// Fetches bodies in the background so the reader opens instantly; never overwrites a cached one.
    public func prefetchBodies(_ articles: [Article], limit: Int = 10) async {
        let candidates = articles
            .filter { ArticleCapabilities(for: $0).canPrefetchBody && ($0.fullText ?? "").isEmpty }
            .prefix(limit)
        await Concurrency.forEach(Array(candidates), limit: 4) { _ = await self.body(for: $0) }
    }

    // MARK: Thumbnail

    /// Finds the article's lead image on its page and stores it (or `thumbnailFailed`).
    public func resolveThumbnail(for article: Article) async {
        guard ArticleCapabilities(for: article).showsThumbnail else { return }
        let found: String?
        if let page = await fetchPage(article.link) {
            found = ArticleExtractor.thumbnail(fromHTML: page.html, baseURL: page.url)
        } else {
            found = nil
        }
        try? await store.setThumbnail(found ?? Self.thumbnailFailed, link: article.link)
    }

    /// Resolves images for articles whose feed had none (up to `limit` per refresh).
    public func resolveThumbnails(_ articles: [Article], limit: Int = 40) async {
        let candidates = articles
            .filter { ($0.thumbnailUrl ?? "").isEmpty && ArticleCapabilities(for: $0).showsThumbnail }
            .prefix(limit)
        await Concurrency.forEach(Array(candidates), limit: 6) { await self.resolveThumbnail(for: $0) }
    }

    // MARK: Fetching

    private struct Page { let html: String; let url: String }

    private func fetchPage(_ link: String) async -> Page? {
        guard let safe = UrlSecurityValidator.normalizeToHttps(link), let url = URL(string: safe) else { return nil }
        var request = URLRequest(url: url)
        request.setValue(FeedDownloader.browserUserAgent, forHTTPHeaderField: "User-Agent")
        request.setValue("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8", forHTTPHeaderField: "Accept")
        request.setValue("en-US,en;q=0.9", forHTTPHeaderField: "Accept-Language")
        do {
            let result = try await http.fetch(request)
            guard result.isSuccessful else { return nil }
            return Page(html: Self.decode(result.data, response: result.response), url: result.response.url?.absoluteString ?? safe)
        } catch {
            log.error("Page fetch failed for \(url.host() ?? "[invalid]", privacy: .public)")
            return nil
        }
    }

    /// Decodes with the charset the server declared, falling back to UTF-8 then Latin-1.
    static func decode(_ data: Data, response: HTTPURLResponse) -> String {
        if let name = response.textEncodingName {
            let cf = CFStringConvertIANACharSetNameToEncoding(name as CFString)
            if cf != kCFStringEncodingInvalidId,
               let text = String(data: data, encoding: String.Encoding(rawValue: CFStringConvertEncodingToNSStringEncoding(cf))) {
                return text
            }
        }
        return String(data: data, encoding: .utf8) ?? String(data: data, encoding: .isoLatin1) ?? ""
    }
}

/// Small structured-concurrency helper: run `work` over `items` with at most `limit` in flight.
enum Concurrency {
    static func forEach<Element: Sendable>(
        _ items: [Element], limit: Int, _ work: @escaping @Sendable (Element) async -> Void
    ) async {
        await withTaskGroup(of: Void.self) { group in
            var iterator = items.makeIterator()
            var running = 0
            while running < limit, let next = iterator.next() {
                group.addTask { await work(next) }
                running += 1
            }
            while await group.next() != nil {
                if let next = iterator.next() { group.addTask { await work(next) } }
            }
        }
    }
}
