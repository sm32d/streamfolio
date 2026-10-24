import Foundation
import Testing
@testable import StreamFolioCore

@Suite("Custom feeds", .serialized)
struct CustomFeedTests {
    @Test("Only safe HTTPS feeds with a title and tab label are stored")
    func validation() async throws {
        let store = CustomFeedStore(try AppDatabase.inMemory())
        #expect(try await store.add(title: "  Wired  ", url: " https://www.wired.com/feed/rss ", category: " Tech ") != nil)
        #expect(try await store.add(title: "Plain", url: "http://example.com/rss", category: "Tech") == nil, "HTTPS only")
        #expect(try await store.add(title: "Local", url: "https://192.168.1.5/rss", category: "Tech") == nil, "private hosts refused")
        #expect(try await store.add(title: "Meta", url: "https://169.254.169.254/latest", category: "Tech") == nil)
        #expect(try await store.add(title: "", url: "https://example.com/rss", category: "Tech") == nil)
        #expect(try await store.add(title: "No tab", url: "https://example.com/rss", category: "  ") == nil)
        let all = try await store.all()
        #expect(all.count == 1)
        #expect(all[0].title == "Wired" && all[0].category == "Tech")
        #expect(all[0].id != nil)
    }

    @Test("Editing rewrites the row; an unsafe edit changes nothing")
    func editAndDelete() async throws {
        let store = CustomFeedStore(try AppDatabase.inMemory())
        let feed = try #require(try await store.add(title: "A", url: "https://a.example.com/rss", category: "News"))
        #expect(try await store.update(feed, title: "B", url: "http://b.example.com/rss", category: "News") == nil)
        #expect(try await store.all().first?.title == "A")
        let updated = try #require(try await store.update(feed, title: "B", url: "https://b.example.com/rss", category: "World News"))
        #expect(updated.id == feed.id)
        #expect(try await store.all().map(\.title) == ["B"])
        try await store.delete(updated)
        #expect(try await store.all().isEmpty)
    }

    @Test("OPML import: ticked folders only, renamed, safe and new URLs only")
    func importFeeds() async throws {
        let store = CustomFeedStore(try AppDatabase.inMemory())
        try await store.add(title: "Existing", url: "https://known.example.com/rss", category: "Tech")
        let parsed = [
            OpmlFeed(title: "Known", xmlUrl: "https://KNOWN.example.com/rss", category: "Tech"),
            OpmlFeed(title: "New", xmlUrl: "https://new.example.com/rss", category: "Tech"),
            OpmlFeed(title: "Unsafe", xmlUrl: "http://insecure.example.com/rss", category: "Tech"),
            OpmlFeed(title: "Skipped folder", xmlUrl: "https://skipped.example.com/rss", category: "Sport"),
            OpmlFeed(title: "Blank name", xmlUrl: "https://blank.example.com/rss", category: "Misc"),
        ]
        let added = try await store.importFeeds(
            parsed, categoryMapping: ["Tech": "Technology", "Misc": "  "], selectedCategories: ["Tech", "Misc"]
        )
        #expect(added == 2)
        let all = try await store.all()
        #expect(all.first { $0.title == "New" }?.category == "Technology")
        #expect(all.first { $0.title == "Blank name" }?.category == "Misc", "a blank new name keeps the folder name")
        #expect(!all.contains { $0.title == "Skipped folder" || $0.title == "Unsafe" })
        #expect(all.count == 3)
    }
}

@Suite("OPML")
struct OpmlTests {
    static let sample = """
        <?xml version="1.0"?><opml version="2.0"><head><title>x</title></head><body>
          <outline text="Tech">
            <outline type="rss" text="Wired" xmlUrl="https://wired.example/rss"/>
            <outline text="Nested"><outline type="rss" title="Ars" xmlurl="https://ars.example/rss"/></outline>
            <outline type="rss" text="Verge" xmlUrl="https://verge.example/rss"/>
          </outline>
          <outline type="rss" text="Loose" xmlUrl="https://loose.example/rss"/>
          <outline type="rss" xmlUrl="https://untitled.example/rss"/>
        </body></opml>
        """

    @Test("Feeds are filed under the nearest folder, or Imported")
    func parse() {
        let feeds = Opml.parse(Data(Self.sample.utf8))
        #expect(feeds.map(\.title) == ["Wired", "Ars", "Verge", "Loose", "Untitled Feed"])
        #expect(feeds.map(\.category) == ["Tech", "Nested", "Tech", "Imported", "Imported"])
        #expect(feeds[1].xmlUrl == "https://ars.example/rss", "lower-case xmlurl is read too")
    }

    @Test("Garbage and DOCTYPE payloads yield nothing and no crash")
    func hostile() {
        #expect(Opml.parse(Data("not xml".utf8)).isEmpty)
        let xxe = #"<?xml version="1.0"?><!DOCTYPE o [<!ENTITY x SYSTEM "file:///etc/passwd">]><opml><body><outline text="&x;" xmlUrl="https://e.example/rss"/></body></opml>"#
        let feeds = Opml.parse(Data(xxe.utf8))
        #expect(!feeds.contains { $0.title.contains("root:") })
    }

    @Test("Export escapes markup, keeps folder order, and round-trips")
    func export() {
        let feeds = [
            CustomFeed(id: 1, title: "A & <B>", url: "https://a.example/rss?x=1&y=2", category: "Z \"quoted\""),
            CustomFeed(id: 2, title: "C", url: "https://c.example/rss", category: "Another"),
            CustomFeed(id: 3, title: "D", url: "https://d.example/rss", category: "Z \"quoted\""),
        ]
        let xml = Opml.export(feeds)
        #expect(xml.contains("&amp;") && xml.contains("&lt;B&gt;") && xml.contains("&quot;quoted&quot;"))
        #expect(xml.range(of: "Z &quot;")!.lowerBound < xml.range(of: "Another")!.lowerBound, "first-seen folder order")
        let back = Opml.parse(Data(xml.utf8))
        #expect(back.map(\.title) == ["A & <B>", "D", "C"])
        #expect(back.map(\.xmlUrl) == ["https://a.example/rss?x=1&y=2", "https://d.example/rss", "https://c.example/rss"])
        #expect(back[0].category == "Z \"quoted\"")
    }

    @Test("The import list drops unsafe and already subscribed feeds")
    func importable() {
        let existing = [CustomFeed(id: 1, title: "x", url: "https://known.example/rss", category: "c")]
        let list = Opml.importable([
            OpmlFeed(title: "k", xmlUrl: "https://Known.example/rss", category: "c"),
            OpmlFeed(title: "h", xmlUrl: "http://plain.example/rss", category: "c"),
            OpmlFeed(title: "ok", xmlUrl: "https://ok.example/rss", category: "c"),
        ], existing: existing)
        #expect(list.map(\.title) == ["ok"])
    }
}

@Suite("Category layout")
struct CategoryLayoutTests {
    @Test("Curated categories honour selection and order; custom tabs follow; unknown go last")
    func layout() {
        let result = CategoryLayout.categories(
            defaultFeedsEnabled: true, selected: ["World", "Sports", "Health"],
            order: ["Tech Blogs", "Sports", "World"], customCategories: ["Tech Blogs", "Recipes", "Tech Blogs"]
        )
        #expect(result == ["Tech Blogs", "Sports", "World", "Health", "Recipes"])
    }

    @Test("Empty selection falls back to Top Stories; curated off leaves only custom tabs")
    func fallbacks() {
        #expect(CategoryLayout.categories(defaultFeedsEnabled: true, selected: [], order: [], customCategories: []) == ["Top Stories"])
        #expect(CategoryLayout.categories(defaultFeedsEnabled: false, selected: ["World"], order: [], customCategories: ["Mine"]) == ["Mine"])
        #expect(CategoryLayout.categories(defaultFeedsEnabled: false, selected: ["World"], order: [], customCategories: []).isEmpty)
    }

    @Test("Equal rank keeps the original order (stable)")
    func stable() {
        let result = CategoryLayout.categories(defaultFeedsEnabled: false, selected: [], order: [], customCategories: ["B", "A", "C"])
        #expect(result == ["B", "A", "C"])
    }
}

@Suite("Provider selection")
struct ProviderSelectionTests {
    func make(region: String = "US") -> (ProviderSelection, AppPreferences) {
        let preferences = AppPreferences(defaults: isolatedDefaults())
        preferences[Prefs.region] = region
        return (ProviderSelection(preferences: preferences), preferences)
    }

    @Test("In the user's region providers are on until disabled; elsewhere off until enabled")
    func toggling() throws {
        let (selection, prefs) = make(region: "US")
        let providers = CuratedFeeds.shared.allProviders()
        let home = try #require(providers.first { $0.region == "US" })
        let away = try #require(providers.first { $0.region == "GB" })
        #expect(selection.isEnabled(home) && !selection.isEnabled(away))

        #expect(!selection.setEnabled(false, for: home))
        #expect(!selection.isEnabled(home))
        #expect(prefs[Prefs.disabledFeedUrls].contains(home.id))
        #expect(selection.setEnabled(true, for: home), "turning on reports it, so the caller can fetch it now")
        #expect(prefs[Prefs.disabledFeedUrls].isEmpty)

        #expect(selection.setEnabled(true, for: away))
        #expect(prefs[Prefs.enabledCrossRegionFeeds] == [away.id])
        #expect(!selection.setEnabled(true, for: away), "already on")
        #expect(!selection.setEnabled(false, for: away))
        #expect(prefs[Prefs.enabledCrossRegionFeeds].isEmpty)
    }

    @Test("What is toggled is what is fetched")
    func matchesFetching() throws {
        let (selection, prefs) = make(region: "US")
        let providers = CuratedFeeds.shared.allProviders()
        let home = try #require(providers.first { $0.region == "US" && $0.category == "Sports" })
        let away = try #require(providers.first { $0.region == "GB" && $0.category == "Sports" })
        func fetched() -> [String] {
            CuratedFeeds.shared.feeds(region: "US", category: "Sports", disabledFeedUrls: prefs[Prefs.disabledFeedUrls], enabledCrossRegionFeeds: prefs[Prefs.enabledCrossRegionFeeds])
        }
        #expect(fetched().contains(home.url) && !fetched().contains(away.url))
        selection.setEnabled(false, for: home)
        selection.setEnabled(true, for: away)
        #expect(!fetched().contains(home.url) && fetched().contains(away.url))
    }

    @Test("The user's region sorts first, the rest by country name")
    func regionOrder() {
        let sorted = ProviderSelection.sortedRegions(["US", "GB", "AU", "SG"], active: "SG")
        #expect(sorted == ["SG", "AU", "GB", "US"])
    }
}

@Suite("Backup", .serialized)
struct BackupTests {
    struct Side {
        let database: AppDatabase
        let preferences: AppPreferences
        let service: BackupService
        let articles: ArticleStore
        let feeds: CustomFeedStore
        let bookmarks: BookmarkStore

        init() throws {
            database = try AppDatabase.inMemory()
            preferences = AppPreferences(defaults: isolatedDefaults())
            service = BackupService(database: database, preferences: preferences)
            articles = ArticleStore(database)
            feeds = CustomFeedStore(database)
            bookmarks = BookmarkStore(database)
        }
    }

    @Test("Everything round-trips between two installs")
    func roundTrip() async throws {
        let a = try Side()
        a.preferences[Prefs.region] = "SG"
        a.preferences[Prefs.readerFontSize] = 22.0
        a.preferences[Prefs.swipeLeftAction] = "read"
        a.preferences[Prefs.categoryOrder] = ["World", "Top Stories"]
        a.preferences[Prefs.disabledFeedUrls] = ["US|Sports|https://x.example/rss"]
        a.preferences[Prefs.aiEnabled] = true
        a.preferences[Prefs.recentSearches] = ["haze", "cup"]
        try await a.feeds.add(title: "Wired", url: "https://wired.example/rss", category: "Tech")
        try await a.articles.upsert([makeArticle("https://e.com/saved", title: "Saved one"), makeArticle("https://e.com/plain", title: "Plain")])
        try await a.bookmarks.setSaved(true, link: "https://e.com/saved")
        let listId = try #require(try await a.bookmarks.createList(named: "Later"))
        try await a.bookmarks.setLists([listId], link: "https://e.com/saved")
        try await a.articles.setRead(true, link: "https://e.com/plain")

        let json = try await a.service.export()
        let b = try Side()
        let summary = try await b.service.restore(json)

        #expect(summary.feeds == 1 && summary.articles == 2 && summary.lists == 2)
        #expect(b.preferences[Prefs.region] == "SG")
        #expect(b.preferences[Prefs.readerFontSize] == 22.0)
        #expect(b.preferences[Prefs.swipeLeftAction] == "read")
        #expect(b.preferences[Prefs.categoryOrder] == ["World", "Top Stories"])
        #expect(b.preferences[Prefs.disabledFeedUrls] == ["US|Sports|https://x.example/rss"])
        #expect(b.preferences[Prefs.aiEnabled] && b.preferences[Prefs.recentSearches] == ["haze", "cup"])
        #expect(try await b.feeds.all().map(\.title) == ["Wired"])
        #expect(try await b.articles.article(link: "https://e.com/saved")?.isBookmarked == true)
        #expect(try await b.articles.article(link: "https://e.com/plain")?.isRead == true)
        #expect(try await b.bookmarks.listIds(forArticle: "https://e.com/saved") == [listId])
    }

    @Test("The file uses Android's names and shape")
    func format() async throws {
        let a = try Side()
        a.preferences[Prefs.defaultFeedsEnabled] = false
        let object = try #require(try JSONSerialization.jsonObject(with: try await a.service.export()) as? [String: Any])
        #expect(object["version"] as? Int == 2)
        let prefs = try #require(object["preferences"] as? [String: Any])
        #expect(prefs["is_default_feeds_enabled"] as? Bool == false)
        #expect(Set(prefs.keys) == Set(BackupPreferences.all.map(\.jsonKey)))
        #expect(prefs["reader_font_size"] is Double)
        #expect(["custom_feeds", "articles", "bookmark_lists", "article_bookmark_cross_refs"].allSatisfy { object[$0] is [Any] })
    }

    @Test("An Android-made backup restores, with bounds applied")
    func androidFile() async throws {
        let json = """
        {"version":2,
         "preferences":{"language":"fr","region":"FR","selected_categories":["World","Science"],"is_default_feeds_enabled":true,
           "cache_history_days":7,"reader_font_family":"serif","reader_font_size":18.0,"swipe_right_action":"none",
           "tts_speech_rate":1.5,"use_dynamic_colors":true,"last_selected_bookmark_list_id":-1,"has_seen_swipe_hint":true},
         "custom_feeds":[{"title":"Le Monde","url":"https://www.lemonde.fr/rss/une.xml","category":"Actu"}],
         "articles":[{"link":"https://e.com/a","title":"A","description":"d","pubDate":"2026-10-01T00:00:00.000Z","sourceName":"S","sourceUrl":"https://e.com","category":"World","thumbnailUrl":null,"isBookmarked":true,"customFeedId":null,"fullText":null,"isRead":false}],
         "bookmark_lists":[{"id":1,"name":"Saved","isDefault":true,"createdAt":1700000000000},{"id":5,"name":"Trip","isDefault":false,"createdAt":1700000001000}],
         "article_bookmark_cross_refs":[{"articleLink":"https://e.com/a","listId":5}]}
        """
        let b = try Side()
        try await b.service.restore(Data(json.utf8))
        #expect(b.preferences[Prefs.language] == "fr" && b.preferences[Prefs.region] == "FR")
        #expect(b.preferences[Prefs.selectedCategories] == ["World", "Science"])
        #expect(b.preferences[Prefs.readerFontFamily] == "serif" && b.preferences[Prefs.cacheHistoryDays] == 7)
        #expect(b.preferences[Prefs.swipeRightAction] == "none" && b.preferences[Prefs.ttsSpeechRate] == 1.5)
        #expect(b.preferences[Prefs.useDynamicColors], "kept so it round-trips, though iOS ignores it")
        #expect(try await b.bookmarks.listIds(forArticle: "https://e.com/a") == [5])
        let titles = try await b.bookmarks.observeCollections().first { _ in true }?.map(\.name)
        #expect(titles == ["Saved", "Trip"])
    }

    @Test("Out-of-range and malformed preferences are ignored, not clamped")
    func rejectsBadValues() async throws {
        let b = try Side()
        let json = #"""
        {"version":2,"custom_feeds":[],"preferences":{"reader_font_size":99,"reader_line_spacing":"wide","cache_history_days":0,
          "swipe_left_action":"explode","tts_speech_rate":9,"reader_font_family":"comic","group_similar_stories":1,"region":"JP"}}
        """#
        try await b.service.restore(Data(json.utf8))
        #expect(b.preferences[Prefs.readerFontSize] == Prefs.readerFontSize.defaultValue)
        #expect(b.preferences[Prefs.readerLineSpacing] == Prefs.readerLineSpacing.defaultValue)
        #expect(b.preferences[Prefs.cacheHistoryDays] == Prefs.cacheHistoryDays.defaultValue)
        #expect(b.preferences[Prefs.swipeLeftAction] == "bookmark")
        #expect(b.preferences[Prefs.ttsSpeechRate] == 1.0)
        #expect(b.preferences[Prefs.readerFontFamily] == "sans_serif")
        #expect(b.preferences[Prefs.groupSimilarStories], "a number is not a boolean")
        #expect(b.preferences[Prefs.region] == "JP", "valid neighbours are still applied")
    }

    @Test("Unsafe URLs are skipped")
    func unsafeUrls() async throws {
        let b = try Side()
        let json = """
        {"version":2,"preferences":{},
         "custom_feeds":[{"title":"Bad","url":"http://example.com/rss","category":"x"},{"title":"Local","url":"https://127.0.0.1/rss","category":"x"},{"title":"Good","url":"https://good.example/rss","category":"x"},{"title":"No url","category":"x"}],
         "articles":[{"link":"javascript:alert(1)","title":"x"},{"link":"https://10.0.0.1/a","title":"y"},{"link":"https://ok.example/a","title":"z","thumbnailUrl":"http://ok.example/t.jpg"}]}
        """
        let summary = try await b.service.restore(Data(json.utf8))
        #expect(try await b.feeds.all().map(\.title) == ["Good"])
        #expect(summary.articles == 1)
        let article = try #require(try await b.articles.article(link: "https://ok.example/a"))
        #expect(article.thumbnailUrl == "https://ok.example/t.jpg", "thumbnails are upgraded to HTTPS")
        #expect(article.isBookmarked, "isBookmarked defaults to true when missing, like Android")
    }

    @Test("Link-only publishers are never exported or restored")
    func linkOnly() async throws {
        let a = try Side()
        let linkOnly = makeArticle("https://joho-todai.com/en/a/", title: "Headline", sourceUrl: LinkOnlySources.johoTodaiEnFeed)
        try await a.articles.upsert([linkOnly, makeArticle("https://e.com/ok")])
        try await a.bookmarks.setSaved(true, link: linkOnly.link)
        try await a.bookmarks.setSaved(true, link: "https://e.com/ok")
        let json = try await a.service.export()
        let text = String(decoding: json, as: UTF8.self)
        #expect(!text.contains("joho-todai.com") && text.contains("e.com/ok"))

        let b = try Side()
        let hand = #"{"version":2,"preferences":{},"custom_feeds":[],"articles":[{"link":"https://joho-todai.com/en/b/","title":"t"}],"article_bookmark_cross_refs":[{"articleLink":"https://joho-todai.com/en/b/","listId":1}]}"#
        let summary = try await b.service.restore(Data(hand.utf8))
        #expect(summary.articles == 0)
        #expect(try await b.articles.article(link: "https://joho-todai.com/en/b/") == nil)
    }

    @Test("A bad file changes nothing")
    func badFile() async throws {
        let b = try Side()
        try await b.feeds.add(title: "Keep", url: "https://keep.example/rss", category: "k")
        b.preferences[Prefs.region] = "AU"
        await #expect(throws: BackupService.BackupError.notABackup) { try await b.service.restore(Data("nope".utf8)) }
        await #expect(throws: BackupService.BackupError.notABackup) { try await b.service.restore(Data("[1,2]".utf8)) }
        await #expect(throws: BackupService.BackupError.unsupportedVersion(0)) { try await b.service.restore(Data(#"{"version":0}"#.utf8)) }
        #expect(try await b.feeds.all().map(\.title) == ["Keep"])
        #expect(b.preferences[Prefs.region] == "AU")
        #expect(!BackupService.looksLikeBackup(Data("hello".utf8)))
        #expect(BackupService.looksLikeBackup(Data(#"{"preferences":{},"custom_feeds":[]}"#.utf8)))
    }

    @Test("Links to lists that do not exist are dropped")
    func danglingRefs() async throws {
        let b = try Side()
        let json = #"{"version":2,"preferences":{},"custom_feeds":[],"articles":[{"link":"https://e.com/a","title":"A"}],"bookmark_lists":[{"id":1,"name":"Saved","isDefault":true,"createdAt":1}],"article_bookmark_cross_refs":[{"articleLink":"https://e.com/a","listId":1},{"articleLink":"https://e.com/a","listId":42}]}"#
        try await b.service.restore(Data(json.utf8))
        #expect(try await b.bookmarks.listIds(forArticle: "https://e.com/a") == [1])
    }
}

@Suite("Custom feeds in refresh and search", .serialized)
struct CustomFeedRefreshTests {
    static let rss = """
        <rss><channel><title>Mine</title>
        <item><title>Haze returns</title><link>https://mine.example/haze</link><pubDate>Sat, 24 Oct 2026 09:00:00 GMT</pubDate></item>
        <item><title>Cup final</title><link>https://mine.example/cup</link><pubDate>Sat, 24 Oct 2026 08:00:00 GMT</pubDate></item>
        </channel></rss>
        """

    struct Harness {
        let http = StubHTTP()
        let database: AppDatabase
        let articles: ArticleStore
        let feeds: CustomFeedStore
        let preferences = AppPreferences(defaults: isolatedDefaults())
        let repository: NewsRepository
        let search: SearchService

        init() throws {
            database = try AppDatabase.inMemory()
            articles = ArticleStore(database)
            feeds = CustomFeedStore(database)
            let downloader = FeedDownloader(http: http, state: LinkOnlyStateStore(suiteName: "t-\(UUID())"), store: articles, appVersion: "1")
            repository = NewsRepository(store: articles, downloader: downloader, preferences: preferences, enrichment: .off)
            search = SearchService(store: articles, downloader: downloader, preferences: preferences, customFeeds: feeds)
            http.on(SourceConfig.url.absoluteString, body: #"{"version":1,"sources":{}}"#)
        }
    }

    @Test("A custom feed fills its tab, tagged with the feed's id and title")
    func refreshCustom() async throws {
        let h = try Harness()
        h.http.on("https://mine.example/feed", body: Self.rss)
        let feed = try #require(try await h.feeds.add(title: "My Feed", url: "https://mine.example/feed", category: "Mine"))
        h.preferences[Prefs.defaultFeedsEnabled] = false

        let outcome = await h.repository.refresh(category: "Mine", customFeeds: try await h.feeds.all())
        #expect(outcome.feedsFetched == 1 && outcome.articlesParsed == 2)
        let stored = try await h.articles.recent(category: "Mine")
        #expect(stored.count == 2)
        #expect(stored.allSatisfy { $0.customFeedId == feed.id })
    }

    @Test("Other tabs' feeds are not fetched; a custom tab named like a curated one gets both")
    func onlyMatchingCategory() async throws {
        let h = try Harness()
        h.http.on("https://mine.example/feed", body: Self.rss)
        h.http.on("https://other.example/feed", body: Self.rss)
        try await h.feeds.add(title: "Mine", url: "https://mine.example/feed", category: "Mine")
        try await h.feeds.add(title: "Other", url: "https://other.example/feed", category: "Other")
        h.preferences[Prefs.defaultFeedsEnabled] = false
        await h.repository.refresh(category: "Mine", customFeeds: try await h.feeds.all())
        #expect(h.http.requestCount(prefix: "https://other.example") == 0)

        h.http.on("https://", body: Self.rss)
        h.preferences[Prefs.defaultFeedsEnabled] = true
        h.preferences[Prefs.region] = "US"
        try await h.feeds.add(title: "Mine World", url: "https://mine-world.example/feed", category: "World")
        let outcome = await h.repository.refresh(category: "World", customFeeds: try await h.feeds.all())
        #expect(outcome.feedsRequested > 1, "the custom feed plus the curated World feeds")
        #expect(h.http.requestCount(prefix: "https://mine-world.example") == 1)
    }

    @Test("An OPML document used as a feed is expanded, each child re-validated")
    func opmlFeed() async throws {
        let h = try Harness()
        let opml = #"<opml version="2.0"><body><outline text="A" xmlUrl="https://child.example/a"/><outline text="Bad" xmlUrl="http://insecure.example/b"/><outline text="Local" xmlUrl="https://10.0.0.1/c"/></body></opml>"#
        h.http.on("https://list.example/opml", body: opml)
        h.http.on("https://child.example/a", body: Self.rss)
        try await h.feeds.add(title: "List", url: "https://list.example/opml", category: "Bundle")
        h.preferences[Prefs.defaultFeedsEnabled] = false
        await h.repository.refresh(category: "Bundle", customFeeds: try await h.feeds.all())
        #expect(try await h.articles.recent(category: "Bundle").count == 2)
        #expect(h.http.requestCount(prefix: "http://insecure.example") == 0 && h.http.requestCount(prefix: "https://10.0.0.1") == 0)
    }

    @Test("A broken custom feed is skipped, not fatal")
    func brokenFeed() async throws {
        let h = try Harness()
        h.http.on("https://down.example", status: 500)
        h.http.on("https://mine.example/feed", body: Self.rss)
        try await h.feeds.add(title: "Down", url: "https://down.example/feed", category: "Mixed")
        try await h.feeds.add(title: "Up", url: "https://mine.example/feed", category: "Mixed")
        h.preferences[Prefs.defaultFeedsEnabled] = false
        let outcome = await h.repository.refresh(category: "Mixed", customFeeds: try await h.feeds.all())
        #expect(outcome.feedsRequested == 2 && outcome.feedsFetched == 1)
    }

    @Test("Online search covers custom feeds, even with curated feeds off")
    func searchCustom() async throws {
        let h = try Harness()
        h.http.on("https://mine.example/feed", body: Self.rss)
        try await h.feeds.add(title: "Mine", url: "https://mine.example/feed", category: "Mine")
        h.preferences[Prefs.defaultFeedsEnabled] = false
        #expect(await h.search.searchOnline("haze") == 1)
        #expect(try await h.articles.recent(category: SearchService.category).map(\.link) == ["https://mine.example/haze"])
    }

    @Test("Turning a provider on fetches just that feed")
    func singleFeed() async throws {
        let h = try Harness()
        h.http.on("https://one.example/feed", body: Self.rss)
        await h.repository.refreshSingleFeed(url: "https://one.example/feed", category: "World")
        #expect(try await h.articles.recent(category: "World").count == 2)
        await h.repository.refreshSingleFeed(url: "http://insecure.example/feed", category: "World")
        #expect(h.http.requestCount(prefix: "http://insecure.example") == 0)
    }
}
