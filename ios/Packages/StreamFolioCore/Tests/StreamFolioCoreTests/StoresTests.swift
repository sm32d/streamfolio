import Foundation
import Testing
@testable import StreamFolioCore

@Suite("Bookmarks and actions")
struct BookmarkTests {
    @Test("Save adds to the default list; unsave clears every list")
    func saveAndUnsave() async throws {
        let database = try AppDatabase.inMemory()
        let articles = ArticleStore(database), bookmarks = BookmarkStore(database)
        try await articles.upsert([makeArticle("https://e.com/1")])
        let custom = try #require(try await bookmarks.createList(named: "Later"))

        try await bookmarks.setSaved(true, link: "https://e.com/1")
        #expect(try await bookmarks.listIds(forArticle: "https://e.com/1") == [BookmarkList.defaultListId])
        #expect(try await articles.article(link: "https://e.com/1")?.isBookmarked == true)

        try await bookmarks.setLists([BookmarkList.defaultListId, custom], link: "https://e.com/1")
        try await bookmarks.removeFromList(link: "https://e.com/1", listId: BookmarkList.defaultListId)
        #expect(try await articles.article(link: "https://e.com/1")?.isBookmarked == true, "still in Later")
        try await bookmarks.removeFromList(link: "https://e.com/1", listId: custom)
        #expect(try await articles.article(link: "https://e.com/1")?.isBookmarked == false)

        try await bookmarks.setSaved(true, link: "https://e.com/1")
        try await bookmarks.setSaved(false, link: "https://e.com/1")
        #expect(try await bookmarks.listIds(forArticle: "https://e.com/1").isEmpty)
    }

    @Test("List names are unique and trimmed; the default list cannot be renamed or deleted")
    func listRules() async throws {
        let bookmarks = BookmarkStore(try AppDatabase.inMemory())
        #expect(try await bookmarks.createList(named: "  ") == nil)
        let first = try await bookmarks.createList(named: " Recipes ")
        #expect(first != nil)
        #expect(try await bookmarks.createList(named: "Recipes") == nil, "duplicate name")
        try await bookmarks.renameList(id: BookmarkList.defaultListId, to: "Hacked")
        try await bookmarks.deleteList(id: BookmarkList.defaultListId)
        var iterator = bookmarks.observeLists().makeAsyncIterator()
        let lists = try #require(try await iterator.next())
        #expect(lists.map(\.name) == ["Saved", "Recipes"])
    }

    @Test("Deleting a list can move its articles to another list; orphans are un-bookmarked")
    func deleteList() async throws {
        let database = try AppDatabase.inMemory()
        let articles = ArticleStore(database), bookmarks = BookmarkStore(database)
        try await articles.upsert([makeArticle("https://e.com/a"), makeArticle("https://e.com/b")])
        let list = try #require(try await bookmarks.createList(named: "Temp"))
        try await bookmarks.setLists([list], link: "https://e.com/a")
        try await bookmarks.setLists([list], link: "https://e.com/b")

        try await bookmarks.deleteList(id: list, moveArticlesTo: BookmarkList.defaultListId)
        #expect(try await bookmarks.listIds(forArticle: "https://e.com/a") == [BookmarkList.defaultListId])
        #expect(try await articles.article(link: "https://e.com/a")?.isBookmarked == true)

        let again = try #require(try await bookmarks.createList(named: "Gone"))
        try await bookmarks.setLists([again], link: "https://e.com/b")
        try await bookmarks.deleteList(id: again)
        #expect(try await articles.article(link: "https://e.com/b")?.isBookmarked == false)
    }

    @Test("ArticleActions toggles save/read and runs configured swipes")
    func actions() async throws {
        let database = try AppDatabase.inMemory()
        let articles = ArticleStore(database), actions = ArticleActions(articles: articles, bookmarks: BookmarkStore(database))
        try await articles.upsert([makeArticle("https://e.com/1")])
        let article = try #require(try await articles.article(link: "https://e.com/1"))
        #expect(await actions.perform(swipe: "bookmark", on: article) == .saved(link: article.link))
        #expect(await actions.perform(swipe: "read", on: article) == .markedRead(link: article.link))
        #expect(await actions.perform(swipe: "share", on: article) == .shared)
        #expect(await actions.perform(swipe: "none", on: article) == .none)
        let saved = try #require(try await articles.article(link: article.link))
        #expect(saved.isBookmarked && saved.isRead)
        #expect(await actions.toggleSaved(saved) == .unsaved(link: article.link))
    }
}

@Suite("Share links")
struct ShareLinksTests {
    @Test("Share text carries an https article URL inside the streamfolio.sume.uk/open link")
    func buildAndParse() throws {
        let text = ShareLinks.shareText(title: "Big news", articleLink: "http://example.com/a?b=1&c=2")
        #expect(text.hasPrefix("Big news\n\nRead more: https://streamfolio.sume.uk/open?url="))
        let url = try #require(ShareLinks.shareURL(for: "http://example.com/a?b=1&c=2"))
        #expect(ShareLinks.parseArticleUrl(url) == "https://example.com/a?b=1&c=2")
    }

    @Test("Only our host and path are accepted, and the target must be safe")
    func parseRejects() {
        #expect(ShareLinks.parseArticleUrl(URL(string: "https://evil.example/open?url=https://a.com")) == nil)
        #expect(ShareLinks.parseArticleUrl(URL(string: "https://streamfolio.sume.uk/other?url=https://a.com")) == nil)
        #expect(ShareLinks.parseArticleUrl(URL(string: "http://streamfolio.sume.uk/open?url=https://a.com")) == nil)
        #expect(ShareLinks.parseArticleUrl(URL(string: "https://streamfolio.sume.uk/open?url=http://127.0.0.1/x")) == nil)
        #expect(ShareLinks.parseArticleUrl(URL(string: "https://streamfolio.sume.uk/open")) == nil)
        #expect(ShareLinks.parseArticleUrl(nil) == nil)
    }
}

@Suite("FeedStore", .serialized)
@MainActor
struct FeedStoreTests {
    func makeStore(articles: [Article] = []) async throws -> (FeedStore, ArticleStore, AppPreferences) {
        let database = try AppDatabase.inMemory()
        let store = ArticleStore(database)
        try await store.upsert(articles)
        let preferences = AppPreferences(defaults: isolatedDefaults())
        let http = StubHTTP()
        let downloader = FeedDownloader(http: http, state: LinkOnlyStateStore(suiteName: "t-\(UUID())"), store: store, appVersion: "1")
        preferences[Prefs.defaultFeedsEnabled] = false   // no network in these tests
        let repository = NewsRepository(store: store, downloader: downloader, preferences: preferences)
        return (FeedStore(repository: repository, articles: store, customFeeds: CustomFeedStore(database), preferences: preferences), store, preferences)
    }

    func waitUntil(_ condition: () -> Bool) async {
        for _ in 0..<200 where !condition() { try? await Task.sleep(for: .milliseconds(10)) }
    }

    @Test("First three stories are trending, the rest are stories, grouped by story")
    func trendingSplit() async throws {
        let list = (0..<10).map { makeArticle("https://e.com/\($0)", title: "Story \($0) unique\($0)", pubDate: "2026-10-24T0\($0):00:00.000Z", groupId: "g\($0)") }
            + [makeArticle("https://e.com/dup", title: "Story 9 dup", pubDate: "2026-10-24T00:30:00.000Z", groupId: "g9")]
        let (feed, _, _) = try await makeStore(articles: list.map { var a = $0; a.category = "Top Stories"; return a })
        feed.start()
        await waitUntil { feed.hasLoaded }
        #expect(feed.trending.count == 3)
        #expect(feed.trending[0].primary.link == "https://e.com/9")
        #expect(feed.trending[0].secondary.map(\.link) == ["https://e.com/dup"], "same groupId → one story")
        #expect(feed.stories.count == 7)
        #expect(!feed.hasMoreStories)
    }

    @Test("Categories follow the user's order and selection")
    func categoryOrder() async throws {
        let (feed, _, preferences) = try await makeStore()
        preferences[Prefs.defaultFeedsEnabled] = true
        preferences[Prefs.selectedCategories] = ["Sports", "World", "Health"]
        preferences[Prefs.categoryOrder] = ["World", "Business", "Sports"]
        #expect(feed.categories == ["World", "Sports", "Health"])
        preferences[Prefs.selectedCategories] = []
        #expect(feed.categories == ["Top Stories"], "never empty")
    }

    @Test("Switching category swaps the list")
    func switching() async throws {
        let (feed, _, _) = try await makeStore(articles: [makeArticle("https://e.com/w", category: "World"), makeArticle("https://e.com/s", category: "Sports")])
        feed.start()
        feed.select("Sports")
        await waitUntil { feed.hasLoaded && feed.category == "Sports" && !feed.allGroups.isEmpty }
        #expect(feed.allGroups.map(\.primary.link) == ["https://e.com/s"])
    }

    @Test("Turning grouping off lists every article on its own")
    func groupingOff() async throws {
        let list = [makeArticle("https://e.com/1", category: "Top Stories", groupId: "x"), makeArticle("https://e.com/2", category: "Top Stories", groupId: "x")]
        let (feed, _, preferences) = try await makeStore(articles: list)
        feed.start()
        await waitUntil { feed.hasLoaded }
        #expect(feed.allGroups.count == 1)
        preferences[Prefs.groupSimilarStories] = false
        feed.regroup()
        #expect(feed.allGroups.count == 2)
    }
}
