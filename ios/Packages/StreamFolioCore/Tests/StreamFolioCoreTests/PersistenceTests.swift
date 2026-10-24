import Foundation
import GRDB
import Testing
@testable import StreamFolioCore

@Suite("Persistence")
struct PersistenceTests {

    @Test("Schema matches Room v9: tables and the groupId index")
    func schema() throws {
        let database = try AppDatabase.inMemory()
        try database.writer.read { db in
            for table in ["articles", "custom_feeds", "tts_playlist_state", "bookmark_lists", "article_bookmark_lists"] {
                #expect(try db.tableExists(table), "\(table)")
            }
            let columns = try db.columns(in: "articles").map(\.name)
            #expect(columns == ["link", "title", "description", "pubDate", "sourceName", "sourceUrl", "category", "thumbnailUrl",
                                "isBookmarked", "customFeedId", "fullText", "tags", "aiSummary", "translatedTitle", "translatedBody",
                                "translatedLanguage", "detectedLanguage", "isRead", "groupId"])
            let indexes = try db.indexes(on: "articles").map(\.name)
            #expect(indexes.contains("index_articles_groupId"))
        }
    }

    @Test("The default Saved list exists on a fresh database and is re-created if missing")
    func defaultList() throws {
        let database = try AppDatabase.inMemory()
        let first = try database.writer.read { try BookmarkList.fetchAll($0) }
        #expect(first.map(\.name) == ["Saved"])
        #expect(first[0].isDefault && first[0].id == BookmarkList.defaultListId)

        try database.writer.write { try $0.execute(sql: "DELETE FROM bookmark_lists") }
        try AppDatabase.ensureDefaultBookmarkList(database.writer)   // what every launch does
        #expect(try database.writer.read { try BookmarkList.fetchCount($0) } == 1)
    }

    @Test("Upsert inserts, refreshes feed fields and keeps user state")
    func upsertPreservesUserState() async throws {
        let database = try AppDatabase.inMemory()
        let store = ArticleStore(database)
        try await store.upsert([makeArticle("https://e.com/1", title: "Old title", groupId: "g1")])

        try await database.writer.write {
            try $0.execute(sql: "UPDATE articles SET isBookmarked = 1, isRead = 1, fullText = 'body', aiSummary = 'sum', translatedTitle = 'tt', detectedLanguage = 'fr' WHERE link = 'https://e.com/1'")
        }
        try await store.upsert([makeArticle("https://e.com/1", title: "New title")])   // groupId nil → keep existing

        let stored = try #require(try await store.article(link: "https://e.com/1"))
        #expect(stored.title == "New title")
        #expect(stored.isBookmarked && stored.isRead)
        #expect(stored.fullText == "body" && stored.aiSummary == "sum")
        #expect(stored.translatedTitle == "tt" && stored.detectedLanguage == "fr")
        #expect(stored.groupId == "g1")
        #expect(try await store.count() == 1)
    }

    @Test("Prune removes old un-bookmarked articles but never bookmarked or listed ones")
    func prune() async throws {
        let database = try AppDatabase.inMemory()
        let store = ArticleStore(database)
        try await store.upsert([
            makeArticle("https://e.com/old", pubDate: "2020-01-01T00:00:00.000Z"),
            makeArticle("https://e.com/old-bookmarked", pubDate: "2020-01-01T00:00:00.000Z"),
            makeArticle("https://e.com/old-listed", pubDate: "2020-01-01T00:00:00.000Z"),
            makeArticle("https://e.com/new", pubDate: "2026-10-24T00:00:00.000Z"),
        ])
        try await database.writer.write { db in
            try db.execute(sql: "UPDATE articles SET isBookmarked = 1 WHERE link = 'https://e.com/old-bookmarked'")
            try db.execute(sql: "INSERT INTO article_bookmark_lists (articleLink, listId) VALUES ('https://e.com/old-listed', 1)")
        }
        try await store.prune(olderThan: "2025-01-01T00:00:00.000Z")
        let remaining = try await database.writer.read { try Article.fetchAll($0).map(\.link).sorted() }
        #expect(remaining == ["https://e.com/new", "https://e.com/old-bookmarked", "https://e.com/old-listed"])
    }

    @Test("Observation emits the category newest-first and updates on change")
    func observe() async throws {
        let database = try AppDatabase.inMemory()
        let store = ArticleStore(database)
        try await store.upsert([makeArticle("https://e.com/a", pubDate: "2026-10-24T01:00:00.000Z"),
                                makeArticle("https://e.com/b", pubDate: "2026-10-24T02:00:00.000Z"),
                                makeArticle("https://e.com/other", category: "Sports")])
        var iterator = store.observe(category: "World").makeAsyncIterator()
        #expect(try await iterator.next()?.map(\.link) == ["https://e.com/b", "https://e.com/a"])
        try await store.upsert([makeArticle("https://e.com/c", pubDate: "2026-10-24T03:00:00.000Z")])
        #expect(try await iterator.next()?.map(\.link) == ["https://e.com/c", "https://e.com/b", "https://e.com/a"])
    }

    @Test("Withdrawal deletes matching articles and their bookmark links, bookmarked or not")
    func deleteMatching() async throws {
        let database = try AppDatabase.inMemory()
        let store = ArticleStore(database)
        try await store.upsert([makeArticle("https://joho-todai.com/en/a/", sourceUrl: LinkOnlySources.johoTodaiEnFeed), makeArticle("https://e.com/keep")])
        try await database.writer.write { db in
            try db.execute(sql: "UPDATE articles SET isBookmarked = 1 WHERE link LIKE '%joho%'")
            try db.execute(sql: "INSERT INTO article_bookmark_lists (articleLink, listId) VALUES ('https://joho-todai.com/en/a/', 1)")
        }
        try await store.deleteMatching(pattern: "%joho-todai.com%")
        #expect(try await store.count() == 1)
        #expect(try await database.writer.read { try ArticleBookmarkListCrossRef.fetchCount($0) } == 0)
    }

    @Test("Related lookup by groupId excludes the article itself")
    func related() async throws {
        let store = ArticleStore(try AppDatabase.inMemory())
        try await store.upsert([makeArticle("https://e.com/1", groupId: "g"), makeArticle("https://e.com/2", groupId: "g"), makeArticle("https://e.com/3", groupId: "h")])
        #expect(try await store.related(groupId: "g", excluding: "https://e.com/1").map(\.link) == ["https://e.com/2"])
    }
}

@Suite("Story grouping")
struct StoryGrouperTests {
    @Test("Titles reduce to comparable word sets")
    func words() {
        let words = StoryGrouper.words(in: "Fire at the Port, officials say - BBC News")
        #expect(words.contains("fire") && words.contains("port") && words.contains("officials"))
        #expect(!words.contains("the") && !words.contains("at") && !words.contains("bbc"))
    }

    @Test("Similar headlines share a group; unrelated ones start their own")
    func grouping() {
        let a = makeArticle("https://a.com/1", title: "Earthquake strikes northern Japan coast")
        let b = makeArticle("https://b.com/1", title: "Strong earthquake strikes Japan northern coast")
        let c = makeArticle("https://c.com/1", title: "Central bank raises interest rates again")
        let result = StoryGrouper.assignGroupIds(to: [a, b, c], existing: [])
        #expect(result[0].groupId == a.link)
        #expect(result[1].groupId == a.link)
        #expect(result[2].groupId == c.link)   // a group of one: groupId == link
    }

    @Test("A new article joins an existing group's id")
    func joinsExisting() {
        let existing = makeArticle("https://old.com/1", title: "Earthquake strikes northern Japan coast", groupId: "G-OLD")
        let fresh = makeArticle("https://new.com/1", title: "Earthquake strikes Japan northern coast today")
        #expect(StoryGrouper.assignGroupIds(to: [fresh], existing: [existing])[0].groupId == "G-OLD")
    }

    @Test("Group collapses primary and secondary coverage in order of first appearance")
    func collapse() {
        let list = [makeArticle("1", groupId: "x"), makeArticle("2", groupId: "y"), makeArticle("3", groupId: "x")]
        let groups = StoryGrouper.group(list)
        #expect(groups.map(\.primary.link) == ["1", "2"])
        #expect(groups[0].secondary.map(\.link) == ["3"])
    }

    @Test("Jaccard edge cases")
    func jaccard() {
        #expect(StoryGrouper.jaccard([], []) == 1)
        #expect(StoryGrouper.jaccard(["a"], []) == 0)
        #expect(StoryGrouper.jaccard(["a", "b"], ["b", "c"]) == 1.0 / 3.0)
    }
}

@Suite("Preferences")
struct PreferencesTests {
    @Test("Defaults match Android")
    func defaults() {
        let prefs = AppPreferences(defaults: isolatedDefaults())
        #expect(prefs[Prefs.region] == "US" && prefs[Prefs.language] == "en")
        #expect(prefs[Prefs.completedOnboarding] == false)
        #expect(prefs[Prefs.selectedCategories] == Set(NewsCategory.all))
        #expect(prefs[Prefs.categoryOrder] == NewsCategory.all)
        #expect(prefs[Prefs.cacheHistoryDays] == 36_500)
        #expect(prefs[Prefs.readerFontSize] == 16 && prefs[Prefs.readerLineSpacing] == 1.5)
        #expect(prefs[Prefs.swipeLeftAction] == "bookmark" && prefs[Prefs.swipeRightAction] == "share")
        #expect(prefs[Prefs.aiEnabled] == false && prefs[Prefs.aiTranslationEnabled] && prefs[Prefs.aiSummaryEnabled])
        #expect(prefs[Prefs.groupSimilarStories] && prefs[Prefs.bookmarkListViewIsGrid] && prefs[Prefs.defaultFeedsEnabled])
        #expect(prefs[Prefs.lastSelectedBookmarkListId] == -1)
    }

    @Test("Values round-trip and ranges are clamped")
    func clamping() {
        let prefs = AppPreferences(defaults: isolatedDefaults())
        prefs[Prefs.region] = "SG"
        prefs[Prefs.readerFontSize] = 99
        prefs[Prefs.cacheHistoryDays] = 0
        prefs[Prefs.ttsSpeechRate] = 0.1
        prefs[Prefs.swipeLeftAction] = "explode"
        prefs[Prefs.recentSearches] = (1...15).map(String.init)
        prefs[Prefs.selectedCategories] = ["World"]
        #expect(prefs[Prefs.region] == "SG")
        #expect(prefs[Prefs.readerFontSize] == 30)
        #expect(prefs[Prefs.cacheHistoryDays] == 1)
        #expect(prefs[Prefs.ttsSpeechRate] == 0.5)
        #expect(prefs[Prefs.swipeLeftAction] == "bookmark")
        #expect(prefs[Prefs.recentSearches].count == 10)
        #expect(prefs[Prefs.selectedCategories] == ["World"])
    }
}
