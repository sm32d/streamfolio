import Foundation
import os

/// Orchestrates feed refreshes: fetch → parse → group → store → prune. The Android equivalent is
/// `NewsRepository.fetchDefaultFeeds` / `insertAndPruneArticles`. Thumbnail resolution, body
/// pre-scraping, tagging and custom feeds are added in later steps of the iOS port.
public struct NewsRepository: Sendable {
    /// What happens to freshly saved articles besides storing them.
    public enum Enrichment: Sendable {
        /// Fetch lead images and the newest bodies after the refresh returns (the app).
        case background
        /// Same, but finish before the refresh returns (tests).
        case inline
        case off
    }

    public struct RefreshOutcome: Sendable, Equatable {
        public var feedsRequested = 0
        public var feedsFetched = 0
        public var articlesParsed = 0
    }

    private let store: ArticleStore
    private let downloader: FeedDownloader
    private let parser: RssParser
    private let curated: CuratedFeeds
    private let preferences: AppPreferences
    private let content: ArticleContentService?
    private let enrichment: Enrichment
    private let log = Logger(subsystem: "uk.sume.streamfolio", category: "NewsRepository")

    public init(
        store: ArticleStore,
        downloader: FeedDownloader,
        preferences: AppPreferences,
        content: ArticleContentService? = nil,
        enrichment: Enrichment = .background,
        parser: RssParser = RssParser(),
        curated: CuratedFeeds = .shared
    ) {
        self.content = content
        self.enrichment = enrichment
        self.store = store
        self.downloader = downloader
        self.preferences = preferences
        self.parser = parser
        self.curated = curated
    }

    /// Refreshes one category from the curated feeds of the user's region. All feeds are fetched in
    /// parallel; a feed that fails is skipped, never failing the whole refresh.
    @discardableResult
    public func refreshDefaultFeeds(category: String) async -> RefreshOutcome {
        await downloader.refreshSourceListIfDue()

        guard preferences[Prefs.defaultFeedsEnabled] else { return RefreshOutcome() }
        let feedUrls = curated.feeds(
            region: preferences[Prefs.region],
            category: category,
            disabledFeedUrls: preferences[Prefs.disabledFeedUrls],
            enabledCrossRegionFeeds: preferences[Prefs.enabledCrossRegionFeeds]
        )

        var outcome = RefreshOutcome(feedsRequested: feedUrls.count)
        var collected: [Article] = []
        await withTaskGroup(of: [Article]?.self) { group in
            for url in feedUrls {
                group.addTask { [downloader, parser] in
                    guard let data = await downloader.feedData(for: url) else { return nil }
                    return parser.parse(data, category: category, feedUrl: url)
                }
            }
            for await parsed in group {
                if let parsed {
                    outcome.feedsFetched += 1
                    collected.append(contentsOf: parsed)
                }
            }
        }
        outcome.articlesParsed = collected.count

        let saved = await save(collected, category: category)
        await enrich(saved)
        return outcome
    }

    /// Refreshes everything that feeds one Home tab: the user's custom feeds filed under `category`,
    /// then the curated feeds when `category` is a curated one (a tab can be both). Mirrors Android's
    /// `refreshCurrentFeed`.
    @discardableResult
    public func refresh(category: String, customFeeds: [CustomFeed]) async -> RefreshOutcome {
        var outcome = RefreshOutcome()
        let matching = customFeeds.filter { $0.category == category }
        if !matching.isEmpty {
            outcome = await refreshCustomFeeds(matching, category: category)
        }
        if NewsCategory.all.contains(category) {
            let curated = await refreshDefaultFeeds(category: category)
            outcome.feedsRequested += curated.feedsRequested
            outcome.feedsFetched += curated.feedsFetched
            outcome.articlesParsed += curated.articlesParsed
        }
        return outcome
    }

    /// Fetches the user's feeds for one tab in parallel. A feed that is unsafe, unreachable or
    /// unreadable is skipped. An OPML document used as a "feed" is expanded into the feeds it lists
    /// (each validated again), like Android.
    @discardableResult
    public func refreshCustomFeeds(_ feeds: [CustomFeed], category: String) async -> RefreshOutcome {
        await downloader.refreshSourceListIfDue()

        var outcome = RefreshOutcome(feedsRequested: feeds.count)
        var collected: [Article] = []
        let reader = FeedReader(downloader: downloader, parser: parser)
        await withTaskGroup(of: [Article]?.self) { group in
            for feed in feeds {
                group.addTask { await reader.articles(from: feed.url, category: feed.category, customFeed: feed) }
            }
            for await parsed in group {
                if let parsed {
                    outcome.feedsFetched += 1
                    collected.append(contentsOf: parsed)
                }
            }
        }
        outcome.articlesParsed = collected.count

        let saved = await save(collected, category: category)
        await enrich(saved)
        return outcome
    }

    /// Fetches one curated feed right after the user turns it on (`fetchSingleFeed`).
    public func refreshSingleFeed(url: String, category: String) async {
        await downloader.refreshSourceListIfDue()
        let reader = FeedReader(downloader: downloader, parser: parser)
        guard let parsed = await reader.articles(from: url, category: category), !parsed.isEmpty else { return }
        let saved = await save(parsed, category: category)
        await enrich(saved)
    }

    /// Groups, stores and prunes (`insertAndPruneArticles`). Returns the stored articles, newest first.
    @discardableResult
    func save(_ articles: [Article], category: String) async -> [Article] {
        do {
            let existing = (try? await store.recent(category: category)) ?? []
            let grouped = StoryGrouper.assignGroupIds(to: Self.uniqued(articles), existing: existing)
            try await store.upsert(grouped)

            let days = preferences[Prefs.cacheHistoryDays]
            if days < 36_500, let expiry = Calendar.current.date(byAdding: .day, value: -days, to: Date()) {
                try await store.prune(olderThan: ArticleDate.string(from: expiry))
            }
            let stored = try await store.articles(links: grouped.map(\.link))
            return stored.sorted { $0.pubDate > $1.pubDate }
        } catch {
            log.error("Saving articles failed: \(String(describing: error), privacy: .public)")
            return []
        }
    }

    /// Lead images for stories whose feed had none, and the newest bodies so the reader opens
    /// instantly (Android: `triggerThumbnailResolution` / `triggerArticleBodyPreScrape`). Link-only
    /// articles are skipped inside `ArticleContentService`.
    private func enrich(_ articles: [Article]) async {
        guard let content, enrichment != .off, !articles.isEmpty else { return }
        let work: @Sendable () async -> Void = {
            async let thumbnails: Void = content.resolveThumbnails(articles)
            async let bodies: Void = content.prefetchBodies(articles)
            _ = await (thumbnails, bodies)
        }
        switch enrichment {
        case .inline: await work()
        case .background: Task.detached(priority: .utility) { await work() }
        case .off: break
        }
    }

    /// The same article can arrive from two feeds in one refresh; the later one wins, like a REPLACE.
    private static func uniqued(_ articles: [Article]) -> [Article] {
        var byLink: [String: Article] = [:]
        var order: [String] = []
        for article in articles {
            if byLink[article.link] == nil { order.append(article.link) }
            byLink[article.link] = article
        }
        return order.compactMap { byLink[$0] }
    }
}
