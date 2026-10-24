import Foundation
import Testing
@testable import StreamFolioCore

@Suite("Search matching")
struct SearchMatchingTests {
    @Test("Long queries match anywhere, case-insensitively; quotes are ignored")
    func longQueries() {
        let article = makeArticle("https://e.com/1", title: "Haze returns to Singapore")
        #expect(SearchMatching.matches(article, query: "singapore", includingSource: false))
        #expect(SearchMatching.matches(article, query: "\"HAZE returns\"", includingSource: false))
        #expect(!SearchMatching.matches(article, query: "malaysia", includingSource: false))
        #expect(!SearchMatching.matches(article, query: "   ", includingSource: false))
    }

    @Test("Queries of three characters or fewer must match whole words")
    func shortQueries() {
        let hainan = makeArticle("https://e.com/1", title: "Hainan said it would reopen")
        let ai = makeArticle("https://e.com/2", title: "How AI is changing newsrooms")
        #expect(!SearchMatching.matches(hainan, query: "AI", includingSource: false), "not inside Hainan or said")
        #expect(SearchMatching.matches(ai, query: "ai", includingSource: false))
    }

    @Test("Online search also matches the publisher; the local search does not")
    func sourceMatching() {
        var article = makeArticle("https://e.com/1", title: "Unrelated headline")
        article.sourceName = "Channel NewsAsia"
        #expect(SearchMatching.matches(article, query: "newsasia", includingSource: true))
        #expect(!SearchMatching.matches(article, query: "newsasia", includingSource: false))
    }

    @Test("LIKE wildcards in the query are literal")
    func likeEscaping() {
        #expect(SearchMatching.likePattern("50%_off") == "%50\\%\\_off%")
    }
}

@Suite("Trending topics")
struct TrendingTopicsTests {
    func article(_ title: String, tags: String? = nil, category: String = "World", feed: String = "https://feed.example/rss") -> Article {
        var a = makeArticle("https://e.com/\(title.hashValue)", title: title, category: category, sourceUrl: feed)
        a.tags = tags
        return a
    }

    @Test("Defaults when nothing is active or nothing matches")
    func defaults() {
        #expect(TrendingTopics.compute(from: [], activeFeedUrls: []) == TrendingTopics.defaults)
        #expect(TrendingTopics.compute(from: [article("Something")], activeFeedUrls: ["https://other.example"]) == TrendingTopics.defaults)
    }

    @Test("Tags outrank categories, which outrank headline words; stopwords and SEARCH are dropped")
    func ranking() {
        let list = [
            article("Singapore haze worsens as winds shift", tags: "Haze, Weather"),
            article("Haze alert issued for Singapore", tags: "Haze"),
            article("The latest news about the economy", category: "SEARCH"),
        ]
        let topics = TrendingTopics.compute(from: list, activeFeedUrls: ["https://feed.example/rss"])
        #expect(topics.first == "Haze")                      // two tags (20) + two words (2)
        #expect(topics.contains("Singapore") && topics.contains("World"))
        #expect(!topics.contains("The") && !topics.contains("SEARCH") && !topics.contains("news"))
        #expect(topics.count <= 8)
    }
}

@Suite("SearchService", .serialized)
struct SearchServiceTests {
    static let rss = """
        <rss><channel><title>Feed</title>
        <item><title>Haze returns to Singapore</title><link>https://news.example.com/haze</link></item>
        <item><title>Football results</title><link>https://news.example.com/football</link></item>
        </channel></rss>
        """

    func make() async throws -> (SearchService, ArticleStore, StubHTTP, AppPreferences) {
        let database = try AppDatabase.inMemory()
        let store = ArticleStore(database)
        let http = StubHTTP()
        let preferences = AppPreferences(defaults: isolatedDefaults())
        let downloader = FeedDownloader(http: http, state: LinkOnlyStateStore(suiteName: "t-\(UUID())"), store: store, appVersion: "1")
        return (SearchService(store: store, downloader: downloader, preferences: preferences), store, http, preferences)
    }

    @Test("Fetches the active feeds, caches only matches under SEARCH, and replaces the previous search")
    func searchAndReplace() async throws {
        let (service, store, http, prefs) = try await make()
        prefs[Prefs.region] = "GB"
        prefs[Prefs.selectedCategories] = ["Top Stories"]
        http.on("https://", body: Self.rss)

        #expect(await service.searchOnline("haze") == 1 * service.feedsToSearch().count)
        let first = try await store.recent(category: SearchService.category, limit: 20)
        #expect(first.map(\.link) == ["https://news.example.com/haze"])

        _ = await service.searchOnline("football")
        let second = try await store.recent(category: SearchService.category, limit: 20)
        #expect(second.map(\.link) == ["https://news.example.com/football"], "previous results are replaced")
    }

    @Test("A saved search result survives the next search")
    func savedResultKept() async throws {
        let (service, store, http, prefs) = try await make()
        prefs[Prefs.selectedCategories] = ["Top Stories"]
        http.on("https://", body: Self.rss)
        _ = await service.searchOnline("haze")
        try await store.setBookmarkedForTest(link: "https://news.example.com/haze")
        _ = await service.searchOnline("football")
        #expect(try await store.article(link: "https://news.example.com/haze") != nil)
    }

    @Test("Link-only feeds are never fetched by search")
    func linkOnlySkipped() async throws {
        let (service, _, http, prefs) = try await make()
        prefs[Prefs.region] = "US"
        prefs[Prefs.selectedCategories] = ["Technology"]
        http.on("https://", body: Self.rss)
        #expect(!service.feedsToSearch().contains { LinkOnlySources.isLinkOnly(url: $0.url) })
        _ = await service.searchOnline("haze")
        #expect(http.requestCount(prefix: LinkOnlySources.johoTodaiEnFeed) == 0)
        #expect(service.activeFeedUrls().contains(LinkOnlySources.johoTodaiEnFeed), "still counted for trending")
    }

    @Test("Blank queries do nothing; curated feeds off means no requests")
    func blankAndOff() async throws {
        let (service, _, http, prefs) = try await make()
        #expect(await service.searchOnline("  ") == 0)
        prefs[Prefs.defaultFeedsEnabled] = false
        _ = await service.searchOnline("haze")
        #expect(http.requests.isEmpty)
    }
}

extension ArticleStore {
    /// Marks an article bookmarked directly (the test database is private to the store).
    func setBookmarkedForTest(link: String) async throws {
        try await setBookmarked(link: link)
    }
}

@Suite("SearchStore", .serialized)
@MainActor
struct SearchStoreTests {
    func make(articles: [Article] = [], http: StubHTTP = StubHTTP()) async throws -> (SearchStore, AppPreferences) {
        let database = try AppDatabase.inMemory()
        let store = ArticleStore(database)
        try await store.upsert(articles)
        let preferences = AppPreferences(defaults: isolatedDefaults())
        preferences[Prefs.defaultFeedsEnabled] = false   // no online fetching in these tests
        let downloader = FeedDownloader(http: http, state: LinkOnlyStateStore(suiteName: "t-\(UUID())"), store: store, appVersion: "1")
        let service = SearchService(store: store, downloader: downloader, preferences: preferences)
        return (SearchStore(articles: store, service: service, preferences: preferences), preferences)
    }

    func waitUntil(_ condition: () -> Bool) async {
        for _ in 0..<300 where !condition() { try? await Task.sleep(for: .milliseconds(10)) }
    }

    @Test("Phases: idle → loading/results → no results → idle")
    func phases() async throws {
        let (search, _) = try await make(articles: [makeArticle("https://e.com/1", title: "Haze returns to Singapore")])
        #expect(search.phase == .idle)
        search.query = "haze"
        #expect(search.phase == .loading)
        await waitUntil { search.phase == .results }
        #expect(search.results.map(\.link) == ["https://e.com/1"])

        search.query = "zebra crossing"
        await waitUntil { search.phase == .noResults }
        #expect(search.phase == .noResults)

        search.query = ""
        #expect(search.phase == .idle && search.results.isEmpty)
    }

    @Test("Recent searches: newest first, no duplicates, capped at 8, removable and clearable")
    func recents() async throws {
        let (search, prefs) = try await make()
        for term in ["a1", "b2", "c3", "a1"] { search.remember(term) }
        #expect(search.recentSearches == ["a1", "c3", "b2"])
        for index in 0..<10 { search.remember("term\(index)") }
        #expect(search.recentSearches.count == 8 && search.recentSearches.first == "term9")
        search.removeRecent("term9")
        #expect(!search.recentSearches.contains("term9"))
        #expect(prefs[Prefs.recentSearches] == search.recentSearches, "persisted")
        search.remember("   ")
        search.clearRecents()
        #expect(search.recentSearches.isEmpty && prefs[Prefs.recentSearches].isEmpty)
    }

    @Test("Choosing a suggestion searches it and remembers it")
    func choose() async throws {
        let (search, _) = try await make(articles: [makeArticle("https://e.com/1", title: "Climate talks resume")])
        search.choose("climate")
        #expect(search.query == "climate" && search.recentSearches == ["climate"])
        await waitUntil { search.phase == .results }
        #expect(search.results.count == 1)
    }
}

@Suite("Saved collections", .serialized)
@MainActor
struct SavedStoreTests {
    func waitUntil(_ condition: () -> Bool) async {
        for _ in 0..<300 where !condition() { try? await Task.sleep(for: .milliseconds(10)) }
    }

    @Test("Collections show counts and a preview image; All Saved lists everything saved")
    func collections() async throws {
        let database = try AppDatabase.inMemory()
        let articles = ArticleStore(database), bookmarks = BookmarkStore(database)
        var withImage = makeArticle("https://e.com/1", pubDate: "2026-10-24T09:00:00.000Z")
        withImage.thumbnailUrl = "https://cdn.example.com/1.jpg"
        try await articles.upsert([withImage, makeArticle("https://e.com/2", pubDate: "2026-10-24T10:00:00.000Z")])
        let later = try #require(try await bookmarks.createList(named: "Later"))
        try await bookmarks.setSaved(true, link: "https://e.com/1")
        try await bookmarks.setLists([later, BookmarkList.defaultListId], link: "https://e.com/2")

        let saved = SavedStore(bookmarks: bookmarks, preferences: AppPreferences(defaults: isolatedDefaults()))
        saved.start()
        await waitUntil { saved.hasLoaded && saved.allSaved.count == 2 && saved.collections.count == 2 }
        #expect(saved.collections.map(\.name) == ["Saved", "Later"], "default first")
        #expect(saved.collections[0].articleCount == 2 && saved.collections[1].articleCount == 1)
        #expect(saved.collections[0].previewThumbnail == "https://cdn.example.com/1.jpg")
        #expect(saved.collections[1].previewThumbnail == nil, "no usable image in Later")
        #expect(saved.allSavedPreview == "https://cdn.example.com/1.jpg")
        #expect(saved.name(of: .all) == "All Saved Articles" && saved.name(of: .list(later)) == "Later")
        #expect(!saved.isEmpty)
    }

    @Test("Deleting a collection can move its articles; renaming and creating respect the rules")
    func management() async throws {
        let database = try AppDatabase.inMemory()
        let articles = ArticleStore(database), bookmarks = BookmarkStore(database)
        try await articles.upsert([makeArticle("https://e.com/1")])
        let saved = SavedStore(bookmarks: bookmarks, preferences: AppPreferences(defaults: isolatedDefaults()))
        saved.start()

        let id = try #require(await saved.create(named: " Recipes "))
        await saved.rename(id, to: "Cooking")
        try await bookmarks.setLists([id], link: "https://e.com/1")
        await waitUntil { saved.collections.first { $0.id == id }?.articleCount == 1 }
        #expect(saved.collections.first { $0.id == id }?.name == "Cooking")

        await saved.delete(id, moveTo: BookmarkList.defaultListId)
        await waitUntil { saved.collections.count == 1 }
        #expect(try await bookmarks.listIds(forArticle: "https://e.com/1") == [BookmarkList.defaultListId])
    }

    @Test("Removing from a collection, or from All Saved")
    func removal() async throws {
        let database = try AppDatabase.inMemory()
        let articles = ArticleStore(database), bookmarks = BookmarkStore(database)
        try await articles.upsert([makeArticle("https://e.com/1")])
        let later = try #require(try await bookmarks.createList(named: "Later"))
        try await bookmarks.setLists([later, BookmarkList.defaultListId], link: "https://e.com/1")
        let article = try #require(try await articles.article(link: "https://e.com/1"))

        let inLater = CollectionContents(id: .list(later), bookmarks: bookmarks)
        inLater.start()
        await waitUntil { inLater.articles.count == 1 }
        await inLater.remove(article)
        await waitUntil { inLater.articles.isEmpty }
        #expect(try await bookmarks.listIds(forArticle: article.link) == [BookmarkList.defaultListId])

        let everything = CollectionContents(id: .all, bookmarks: bookmarks)
        everything.start()
        await waitUntil { everything.articles.count == 1 }
        await everything.remove(article)
        await waitUntil { everything.articles.isEmpty }
        #expect(try await bookmarks.listIds(forArticle: article.link).isEmpty)
    }
}
