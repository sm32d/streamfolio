import Foundation
import Testing
@testable import StreamFolioCore

/// End to end with canned HTTP: curated feeds → parse → group → store, plus the publisher-terms
/// behaviour of link-only sources (source list, 30-minute floor, withdrawal).
@Suite("NewsRepository", .serialized)
struct RepositoryTests {
    static let sourceListUrl = SourceConfig.url.absoluteString
    static let enabledList = #"{"version":1,"sources":{"joho-todai-en":{"enabled":true}}}"#
    static let withdrawnList = #"{"version":1,"sources":{}}"#

    struct Harness {
        let http = StubHTTP()
        let clock = TestClock()
        let database: AppDatabase
        let store: ArticleStore
        let state: LinkOnlyStateStore
        let preferences: AppPreferences
        let repository: NewsRepository
        let downloader: FeedDownloader

        init() throws {
            database = try AppDatabase.inMemory()
            store = ArticleStore(database)
            state = LinkOnlyStateStore(suiteName: "test-\(UUID().uuidString)")
            preferences = AppPreferences(defaults: isolatedDefaults())
            let clock = self.clock
            downloader = FeedDownloader(http: http, state: state, store: store, appVersion: "9.9", now: { clock.now })
            repository = NewsRepository(store: store, downloader: downloader, preferences: preferences)
        }
    }

    static let johoXml = """
        <rss version="2.0" xmlns:content="http://purl.org/rss/1.0/modules/content/"><channel><title>Joho</title><ttl>30</ttl>
        <item><title>Link only headline</title><link>https://joho-todai.com/en/a/</link>
        <description>Short</description><content:encoded><![CDATA[<p>FULL</p>]]></content:encoded></item></channel></rss>
        """

    @Test("Refreshing a category stores parsed, grouped articles from every reachable feed")
    func refreshStoresArticles() async throws {
        let h = try Harness()
        h.http.on("https://", body: try RepoFiles.string("sample_xml/cbssports.xml"))   // every feed
        h.http.on(Self.sourceListUrl, body: Self.withdrawnList)
        h.preferences[Prefs.region] = "US"

        let outcome = await h.repository.refreshDefaultFeeds(category: "Sports")
        #expect(outcome.feedsRequested == 2)       // US Sports: CBS + Yahoo
        #expect(outcome.feedsFetched == 2)
        let stored = try await h.store.recent(category: "Sports", limit: 500)
        #expect(!stored.isEmpty)
        #expect(stored.allSatisfy { $0.groupId != nil }, "every article gets a group (possibly of one)")
        #expect(Set(stored.map(\.link)).count == stored.count)
    }

    @Test("A failing feed does not fail the refresh")
    func failingFeedIsSkipped() async throws {
        let h = try Harness()
        h.http.on(Self.sourceListUrl, body: Self.withdrawnList)
        h.http.on("https://www.cbssports.com", body: try RepoFiles.string("sample_xml/cbssports.xml"))
        h.http.on("https://sports.yahoo.com") { _ in .init(error: URLError(.timedOut)) }
        let outcome = await h.repository.refreshDefaultFeeds(category: "Sports")
        #expect(outcome.feedsFetched == 1)
        // 36 items in the file, one link appears twice → 35 distinct articles
        #expect(try await h.store.count() == 35)
    }

    @Test("Curated feeds switched off means no requests")
    func curatedOff() async throws {
        let h = try Harness()
        h.preferences[Prefs.defaultFeedsEnabled] = false
        let outcome = await h.repository.refreshDefaultFeeds(category: "Sports")
        #expect(outcome == .init())
        #expect(h.http.requests.filter { $0.url!.absoluteString != Self.sourceListUrl }.isEmpty)
    }

    @Test("Re-refreshing keeps user state and does not duplicate")
    func refreshIsIdempotent() async throws {
        let h = try Harness()
        h.http.on("https://", body: try RepoFiles.string("sample_xml/cbssports.xml"))
        h.http.on(Self.sourceListUrl, body: Self.withdrawnList)
        await h.repository.refreshDefaultFeeds(category: "Sports")
        let first = try await h.store.recent(category: "Sports", limit: 500)
        try await h.store.setRead(true, link: first[0].link)
        await h.repository.refreshDefaultFeeds(category: "Sports")
        let second = try await h.store.recent(category: "Sports", limit: 500)
        #expect(second.count == first.count)
        #expect(second.first { $0.link == first[0].link }?.isRead == true)
    }

    @Test("Refreshing also fetches lead images for articles without one and the newest bodies")
    func enrichment() async throws {
        let h = try Harness()
        let content = ArticleContentService(store: h.store, http: h.http)
        let repository = NewsRepository(store: h.store, downloader: h.downloader, preferences: h.preferences, content: content, enrichment: .inline)
        let feed = """
            <rss><channel><title>Daily</title>
            <item><title>With image</title><link>https://news.example.com/1</link><media:thumbnail url="https://cdn.example.com/1.jpg" xmlns:media="http://search.yahoo.com/mrss/"/></item>
            <item><title>No image</title><link>https://news.example.com/2</link></item>
            </channel></rss>
            """
        h.http.on(Self.sourceListUrl, body: Self.withdrawnList)
        h.http.on("https://feeds.", body: feed)
        h.http.on("https://news.example.com", body: ArticleContentServiceTests.page)
        h.preferences[Prefs.region] = "GB"

        await repository.refreshDefaultFeeds(category: "Sports")   // GB sport feed
        let one = try #require(try await h.store.article(link: "https://news.example.com/1"))
        let two = try #require(try await h.store.article(link: "https://news.example.com/2"))
        #expect(one.thumbnailUrl == "https://cdn.example.com/1.jpg", "feed image kept, page not needed for it")
        #expect(two.thumbnailUrl == "https://cdn.example.com/lead.jpg", "resolved from the article page")
        #expect(one.fullText?.hasPrefix("The transport minister") == true)
        #expect(two.fullText?.hasPrefix("The transport minister") == true)
        #expect(h.http.requestCount(prefix: "https://news.example.com/1") == 1, "one page fetch serves body only")
    }

    // MARK: Link-only

    @Test("Link-only feed is fetched only after a validated source list, and keeps only headline data")
    func linkOnlyFetchedWhenEnabled() async throws {
        let h = try Harness()
        h.http.on(Self.sourceListUrl, body: Self.enabledList)
        h.http.on(LinkOnlySources.johoTodaiEnFeed, body: Self.johoXml, headers: ["ETag": "\"v1\""])
        h.http.on("https://techcrunch.com", body: "<rss><channel></channel></rss>")
        h.http.on("https://www.theverge.com", body: "<rss><channel></channel></rss>")

        await h.repository.refreshDefaultFeeds(category: "Technology")
        let stored = try await h.store.recent(category: "Technology")
        let joho = try #require(stored.first { $0.link == "https://joho-todai.com/en/a/" })
        #expect(joho.thumbnailUrl == nil)
        #expect(!joho.description.contains("FULL"))

        let request = try #require(h.http.requests.first { $0.url!.absoluteString == LinkOnlySources.johoTodaiEnFeed })
        #expect(request.value(forHTTPHeaderField: "User-Agent") == "StreamFolio/9.9 (iOS feed reader; headline-and-link only)")
    }

    @Test("Never fetched when the source list is unreachable or does not enable it")
    func linkOnlySuspended() async throws {
        let unreachable = try Harness()   // no handler for the list → request throws
        unreachable.http.on(LinkOnlySources.johoTodaiEnFeed, body: Self.johoXml)
        await unreachable.repository.refreshDefaultFeeds(category: "Technology")
        #expect(unreachable.http.requestCount(prefix: LinkOnlySources.johoTodaiEnFeed) == 0)

        let disabled = try Harness()
        disabled.http.on(Self.sourceListUrl, body: Self.withdrawnList)
        disabled.http.on(LinkOnlySources.johoTodaiEnFeed, body: Self.johoXml)
        await disabled.repository.refreshDefaultFeeds(category: "Technology")
        #expect(disabled.http.requestCount(prefix: LinkOnlySources.johoTodaiEnFeed) == 0)

        let broken = try Harness()
        broken.http.on(Self.sourceListUrl, body: "not json")
        broken.http.on(LinkOnlySources.johoTodaiEnFeed, body: Self.johoXml)
        await broken.repository.refreshDefaultFeeds(category: "Technology")
        #expect(broken.http.requestCount(prefix: LinkOnlySources.johoTodaiEnFeed) == 0)
    }

    @Test("At most one request per 30 minutes, then fetched again with the stored validators")
    func thirtyMinuteThrottle() async throws {
        let h = try Harness()
        h.http.on(Self.sourceListUrl, body: Self.enabledList)
        h.http.on(LinkOnlySources.johoTodaiEnFeed, body: Self.johoXml, headers: ["ETag": "\"v1\"", "Last-Modified": "Fri, 23 Oct 2026 11:10:46 GMT"])
        let feed = LinkOnlySources.johoTodaiEnFeed

        await h.repository.refreshDefaultFeeds(category: "Technology")
        #expect(h.http.requestCount(prefix: feed) == 1)

        h.clock.advance(minutes: 5)
        await h.repository.refreshDefaultFeeds(category: "Technology")
        h.clock.advance(minutes: 20)
        await h.repository.refreshDefaultFeeds(category: "Technology")
        #expect(h.http.requestCount(prefix: feed) == 1, "inside the 30-minute window")

        h.clock.advance(minutes: 6)
        h.http.on(feed, status: 304)
        await h.repository.refreshDefaultFeeds(category: "Technology")
        let conditional = try #require(h.http.requests.last { $0.url!.absoluteString == feed })
        #expect(h.http.requestCount(prefix: feed) == 2)
        #expect(conditional.value(forHTTPHeaderField: "If-None-Match") == "\"v1\"")
        #expect(conditional.value(forHTTPHeaderField: "If-Modified-Since") == "Fri, 23 Oct 2026 11:10:46 GMT")
        #expect(try await h.store.article(link: "https://joho-todai.com/en/a/") != nil, "304 keeps cached articles")
    }

    @Test("Concurrent refreshes cannot both pass the 30-minute check")
    func concurrentRefreshesSendOneRequest() async throws {
        let h = try Harness()
        h.http.on(Self.sourceListUrl, body: Self.enabledList)
        h.http.on(LinkOnlySources.johoTodaiEnFeed, body: Self.johoXml)
        await withTaskGroup(of: Void.self) { group in
            for _ in 0..<8 { group.addTask { await h.repository.refreshDefaultFeeds(category: "Technology") } }
        }
        #expect(h.http.requestCount(prefix: LinkOnlySources.johoTodaiEnFeed) == 1)
    }

    @Test("Server back-off instructions are honoured in full, with no 24-hour ceiling")
    func serverBackoff() async throws {
        let h = try Harness()
        h.http.on(Self.sourceListUrl, body: Self.enabledList)
        h.http.on(LinkOnlySources.johoTodaiEnFeed, status: 429, headers: ["Retry-After": "172800"])   // 48 h
        let feed = LinkOnlySources.johoTodaiEnFeed
        await h.repository.refreshDefaultFeeds(category: "Technology")
        h.clock.advance(minutes: 47 * 60)
        await h.repository.refreshDefaultFeeds(category: "Technology")
        #expect(h.http.requestCount(prefix: feed) == 1)
        h.clock.advance(minutes: 2 * 60)
        await h.repository.refreshDefaultFeeds(category: "Technology")
        #expect(h.http.requestCount(prefix: feed) == 2)
    }

    @Test("A transport error does not throttle the next attempt")
    func transportErrorNoThrottle() async throws {
        let h = try Harness()
        h.http.on(Self.sourceListUrl, body: Self.enabledList)
        h.http.on(LinkOnlySources.johoTodaiEnFeed) { _ in .init(error: URLError(.notConnectedToInternet)) }
        await h.repository.refreshDefaultFeeds(category: "Technology")
        h.http.on(LinkOnlySources.johoTodaiEnFeed, body: Self.johoXml)
        await h.repository.refreshDefaultFeeds(category: "Technology")
        #expect(h.http.requestCount(prefix: LinkOnlySources.johoTodaiEnFeed) == 2)
        #expect(try await h.store.article(link: "https://joho-todai.com/en/a/") != nil)
    }

    @Test("Withdrawal deletes cached articles, bookmarks and throttle state on the next refresh")
    func withdrawal() async throws {
        let h = try Harness()
        h.http.on(Self.sourceListUrl, body: Self.enabledList)
        h.http.on(LinkOnlySources.johoTodaiEnFeed, body: Self.johoXml, headers: ["ETag": "\"v1\""])
        await h.repository.refreshDefaultFeeds(category: "Technology")
        try await h.database.writer.write { try $0.execute(sql: "UPDATE articles SET isBookmarked = 1 WHERE link LIKE '%joho-todai.com%'") }
        #expect(try await h.store.article(link: "https://joho-todai.com/en/a/") != nil)

        h.clock.advance(seconds: 61)   // the cached list is stale → re-downloaded
        h.http.on(Self.sourceListUrl, body: Self.withdrawnList)
        await h.repository.refreshDefaultFeeds(category: "Technology")

        #expect(try await h.store.article(link: "https://joho-todai.com/en/a/") == nil)
        #expect(h.state.etag(LinkOnlySources.johoTodaiEnFeed) == nil)
        #expect(h.state.nextFetchAt(LinkOnlySources.johoTodaiEnFeed) == 0)
        #expect(h.state.isEnabled("joho-todai-en") == false)
    }

    @Test("The source list is reused for 60 seconds, then re-validated before the next link-only fetch")
    func sourceListFreshness() async throws {
        let h = try Harness()
        h.http.on(Self.sourceListUrl, body: Self.enabledList)
        h.http.on(LinkOnlySources.johoTodaiEnFeed, body: Self.johoXml)
        await h.repository.refreshDefaultFeeds(category: "Technology")
        await h.repository.refreshDefaultFeeds(category: "Business")
        #expect(h.http.requestCount(prefix: Self.sourceListUrl) == 1)
        h.clock.advance(seconds: 61)
        await h.repository.refreshDefaultFeeds(category: "Business")
        #expect(h.http.requestCount(prefix: Self.sourceListUrl) == 2)
    }
}
