import Foundation
import Testing
@testable import StreamFolioCore

@Suite("ArticleExtractor")
struct ArticleExtractorTests {
    static let longParagraph = "The transport minister said on Saturday that the programme would be expanded to cover more roles across the rail network."

    @Test("Reads paragraphs of the article container and drops promos, captions and short UI text")
    func bodyFromArticle() throws {
        let html = """
        <html><body>
          <nav><p>This navigation paragraph is long enough but sits outside the article container</p></nav>
          <article>
            <p>\(Self.longParagraph)</p>
            <p>Subscribe to our newsletter to get the latest stories delivered to your inbox every morning.</p>
            <p>Share</p>
            <p>Photo: A worker inspects a train at the depot on Saturday afternoon in the west.</p>
            <p>Officials added that new training modules will be rolled out over the next two years for engineers.</p>
          </article>
        </body></html>
        """
        let body = try #require(ArticleExtractor.body(fromHTML: html, baseURL: "https://example.com/a"))
        let paragraphs = body.components(separatedBy: "\n\n")
        #expect(paragraphs.count == 2)
        #expect(paragraphs[0] == Self.longParagraph)
        #expect(paragraphs[1].hasPrefix("Officials added"))
        #expect(!body.contains("navigation"))
    }

    @Test("Falls back to every paragraph when there is no article container")
    func bodyFallback() throws {
        let html = "<html><body><div><p>\(Self.longParagraph)</p></div></body></html>"
        #expect(ArticleExtractor.body(fromHTML: html, baseURL: "https://example.com") == Self.longParagraph)
    }

    @Test("Nothing readable gives nil")
    func bodyNil() {
        #expect(ArticleExtractor.body(fromHTML: "<html><body><script>app()</script><p>Short</p></body></html>", baseURL: "https://e.com") == nil)
        #expect(ArticleExtractor.body(fromHTML: "", baseURL: "https://e.com") == nil)
    }

    @Test("Entities are decoded and inline markup flattened")
    func bodyText() throws {
        let html = "<article><p>Fish &amp; chips <b>sales</b> rose sharply in the quarter, the statistics office said on Friday.</p></article>"
        let body = try #require(ArticleExtractor.body(fromHTML: html, baseURL: "https://e.com"))
        #expect(body.hasPrefix("Fish & chips sales rose"))
    }

    @Test("Promo detection")
    func promo() {
        #expect(ArticleExtractor.isPromo("Subscribe now for unlimited access to every story we publish"))
        #expect(ArticleExtractor.isPromo("Too short"))
        #expect(!ArticleExtractor.isPromo(Self.longParagraph))
        // Credits and datelines (iOS additions)
        #expect(ArticleExtractor.isPromo("PHOTOS: SCREENGRAB FROM GRACE FU/FACEBOOK"))
        #expect(ArticleExtractor.isPromo("VIDEO: Reuters footage of the evacuation in the port"))
        #expect(ArticleExtractor.isPromo("Published Oct 24, 2026, 07:15 PM"))
        #expect(ArticleExtractor.isPromo("Updated 24 October 2026 at 7.15pm Singapore time"))
        #expect(!ArticleExtractor.isPromo("Published reports suggest that the 2026 budget will include new rail subsidies."))
    }

    @Test("Thumbnail precedence: og:image, twitter:image, JSON-LD, first article image")
    func thumbnails() {
        let base = "https://example.com/story"
        func head(_ meta: String, body: String = "") -> String { "<html><head>\(meta)</head><body>\(body)</body></html>" }
        #expect(ArticleExtractor.thumbnail(fromHTML: head(#"<meta property="og:image" content="https://cdn.example.com/og.jpg"><meta name="twitter:image" content="https://cdn.example.com/tw.jpg">"#), baseURL: base) == "https://cdn.example.com/og.jpg")
        #expect(ArticleExtractor.thumbnail(fromHTML: head(#"<meta name="twitter:image" content="https://cdn.example.com/tw.jpg">"#), baseURL: base) == "https://cdn.example.com/tw.jpg")
        #expect(ArticleExtractor.thumbnail(fromHTML: head(#"<script type="application/ld+json">{"image": {"@type":"ImageObject","url":"https:\/\/cdn.example.com\/ld.jpg"}}</script>"#), baseURL: base) == "https://cdn.example.com/ld.jpg")
        #expect(ArticleExtractor.thumbnail(fromHTML: head("", body: #"<article><img src="/photos/lead.jpg"></article>"#), baseURL: base) == "https://example.com/photos/lead.jpg")
    }

    @Test("Thumbnails are upgraded to https, resolved against the page, and aggregator logos are rejected")
    func thumbnailSafety() {
        let base = "https://example.com/story"
        #expect(ArticleExtractor.thumbnail(fromHTML: #"<meta property="og:image" content="http://cdn.example.com/a.jpg">"#, baseURL: base) == "https://cdn.example.com/a.jpg")
        #expect(ArticleExtractor.thumbnail(fromHTML: #"<meta property="og:image" content="/img/a.jpg">"#, baseURL: base) == "https://example.com/img/a.jpg")
        #expect(ArticleExtractor.thumbnail(fromHTML: #"<meta property="og:image" content="https://lh3.googleusercontent.com/x=s0">"#, baseURL: base) == nil)
        #expect(ArticleExtractor.thumbnail(fromHTML: #"<meta property="og:image" content="http://127.0.0.1/x.jpg">"#, baseURL: base) == nil)
        #expect(ArticleExtractor.thumbnail(fromHTML: "<html></html>", baseURL: base) == nil)
    }
}

@Suite("ArticleContentService", .serialized)
struct ArticleContentServiceTests {
    static let page = """
        <html><head><meta property="og:image" content="https://cdn.example.com/lead.jpg"></head><body><article>
        <p>\(ArticleExtractorTests.longParagraph)</p><p>Officials added that new training modules will be rolled out over the next two years for engineers.</p>
        </article></body></html>
        """

    func make(articles: [Article] = []) async throws -> (ArticleContentService, ArticleStore, StubHTTP) {
        let database = try AppDatabase.inMemory()
        let store = ArticleStore(database)
        try await store.upsert(articles)
        let http = StubHTTP()
        return (ArticleContentService(store: store, http: http), store, http)
    }

    @Test("Fetches once, extracts and caches the body")
    func fetchesAndCaches() async throws {
        let (service, store, http) = try await make(articles: [makeArticle("https://example.com/a")])
        http.on("https://example.com", body: Self.page)
        let article = try #require(try await store.article(link: "https://example.com/a"))

        guard case .text(let text) = await service.body(for: article) else { Issue.record("expected text"); return }
        #expect(text.hasPrefix("The transport minister"))
        #expect(http.requestCount(prefix: "https://example.com") == 1)

        let cached = try #require(try await store.article(link: "https://example.com/a"))
        #expect(cached.fullText == text)
        _ = await service.body(for: cached)
        #expect(http.requestCount(prefix: "https://example.com") == 1, "served from the database the second time")
    }

    @Test("Outcomes: unparseable page, failed fetch")
    func failures() async throws {
        let (service, store, http) = try await make(articles: [makeArticle("https://example.com/a"), makeArticle("https://down.example.com/b")])
        http.on("https://example.com", body: "<html><body><div id=app></div></body></html>")
        http.on("https://down.example.com", status: 503)
        let a = try #require(try await store.article(link: "https://example.com/a"))
        let b = try #require(try await store.article(link: "https://down.example.com/b"))
        #expect(await service.body(for: a) == .unparseable)
        #expect(await service.body(for: b) == .failed)
        #expect(try await store.article(link: a.link)?.fullText == nil, "failures are not cached")
    }

    @Test("Link-only articles are never fetched: no body, no page, no image")
    func linkOnlyNeverFetched() async throws {
        let linkOnly = makeArticle("https://joho-todai.com/en/a/", sourceUrl: LinkOnlySources.johoTodaiEnFeed)
        let (service, store, http) = try await make(articles: [linkOnly])
        http.on("https://", body: Self.page)
        #expect(await service.body(for: linkOnly) == .notAllowed)
        await service.resolveThumbnail(for: linkOnly)
        await service.resolveThumbnails([linkOnly])
        await service.prefetchBodies([linkOnly])
        #expect(http.requests.isEmpty)
        #expect(try await store.article(link: linkOnly.link)?.thumbnailUrl == nil)
    }

    @Test("Thumbnail is stored, or the failure sentinel so it is not retried forever")
    func thumbnailResolution() async throws {
        let (service, store, http) = try await make(articles: [makeArticle("https://example.com/a"), makeArticle("https://bare.example.com/b")])
        http.on("https://example.com", body: Self.page)
        http.on("https://bare.example.com", body: "<html><body>nothing</body></html>")
        await service.resolveThumbnails([try #require(try await store.article(link: "https://example.com/a")),
                                         try #require(try await store.article(link: "https://bare.example.com/b"))])
        #expect(try await store.article(link: "https://example.com/a")?.thumbnailUrl == "https://cdn.example.com/lead.jpg")
        #expect(try await store.article(link: "https://bare.example.com/b")?.thumbnailUrl == ArticleContentService.thumbnailFailed)
    }

    @Test("Prefetch covers only the newest N articles that have no body yet")
    func prefetch() async throws {
        let list = (0..<15).map { makeArticle("https://example.com/\($0)", pubDate: "2026-10-24T\(String(format: "%02d", $0)):00:00.000Z") }
        let (service, store, http) = try await make(articles: list)
        try await store.setFullText("already cached body", link: "https://example.com/14")
        http.on("https://example.com", body: Self.page)
        let newestFirst = try await store.articles(links: list.map(\.link)).sorted { $0.pubDate > $1.pubDate }
        await service.prefetchBodies(newestFirst, limit: 10)
        #expect(http.requests.count == 10)
        #expect(!http.requests.contains { $0.url!.absoluteString == "https://example.com/14" })
    }

    @Test("Declared charsets are honoured")
    func charset() {
        let latin1 = Data([0x63, 0x61, 0x66, 0xE9])   // "café" in ISO-8859-1
        let response = HTTPURLResponse(url: URL(string: "https://e.com")!, statusCode: 200, httpVersion: nil,
                                       headerFields: ["Content-Type": "text/html; charset=iso-8859-1"])!
        #expect(ArticleContentService.decode(latin1, response: response) == "café")
    }
}

@Suite("Reader content", .serialized)
@MainActor
struct ReaderStoreTests {
    @Test("Decision table: full text, short text, unparseable, failed, link-only")
    func decisions() {
        let long = String(repeating: "word ", count: 60) + "\n\n" + "Second paragraph."
        #expect(ReaderStore.content(for: .text(long), description: "d") == .article([String(repeating: "word ", count: 60).trimmed, "Second paragraph."]))
        #expect(ReaderStore.content(for: .text("Too short."), description: "The description.") == .excerpt(["The description."]))
        #expect(ReaderStore.content(for: .unparseable, description: "The description.") == .excerpt(["The description."]))
        #expect(ReaderStore.content(for: .failed, description: "") == .webOnly)
        #expect(ReaderStore.content(for: .notAllowed, description: "x") == .webOnly)
    }

    func make(_ article: Article, http: StubHTTP) async throws -> (ReaderStore, ArticleStore) {
        let database = try AppDatabase.inMemory()
        let store = ArticleStore(database)
        try await store.upsert([article])
        let actions = ArticleActions(articles: store, bookmarks: BookmarkStore(database))
        let stored = try #require(try await store.article(link: article.link))
        return (ReaderStore(article: stored, articles: store, service: ArticleContentService(store: store, http: http), actions: actions), store)
    }

    @Test("Opening marks the article read, loads the text and keeps the article live")
    func openFlow() async throws {
        let http = StubHTTP()
        http.on("https://example.com", body: ArticleContentServiceTests.page)
        let (reader, store) = try await make(makeArticle("https://example.com/a"), http: http)
        #expect(reader.mode == .reader && reader.content == .loading)

        await reader.open()
        guard case .article(let paragraphs) = reader.content else { Issue.record("expected article, got \(reader.content)"); return }
        #expect(paragraphs.count == 2)
        #expect(try await store.article(link: "https://example.com/a")?.isRead == true)

        await reader.toggleSaved()
        for _ in 0..<100 where !reader.article.isBookmarked { try await Task.sleep(for: .milliseconds(10)) }
        #expect(reader.article.isBookmarked, "the observed article reflects the save")
    }

    @Test("Link-only articles open in Web View and make no network request")
    func linkOnly() async throws {
        let http = StubHTTP()
        let (reader, _) = try await make(makeArticle("https://joho-todai.com/en/a/", sourceUrl: LinkOnlySources.johoTodaiEnFeed), http: http)
        #expect(reader.mode == .web)
        await reader.open()
        #expect(reader.content == .webOnly)
        #expect(http.requests.isEmpty)
    }

    @Test("Reading time is 200 words per minute, minimum one")
    func readingTime() {
        var article = makeArticle("https://e.com/1")
        #expect(ReadingTime.minutes(for: article) == 1)
        article.fullText = Array(repeating: "word", count: 401).joined(separator: " ")
        #expect(ReadingTime.minutes(for: article) == 3)
        #expect(ReadingTime.label(for: article) == "3 min read")
    }
}
