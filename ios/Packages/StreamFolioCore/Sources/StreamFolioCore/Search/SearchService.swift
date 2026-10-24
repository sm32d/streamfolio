import Foundation
import os

/// Online search: fetch every active feed, keep what matches, and cache it so the local search shows
/// it too. Port of `NewsRepository.searchNewsOnline`.
///
/// Link-only publishers are never fetched on demand — a search must not bypass their per-device fetch
/// limit — but their cached headlines are found by the local search.
public struct SearchService: Sendable {
    /// Category under which fetched matches are cached (replaced by each new search).
    public static let category = "SEARCH"

    private let store: ArticleStore
    private let downloader: FeedDownloader
    private let preferences: AppPreferences
    private let curated: CuratedFeeds
    private let parser: RssParser
    private let customFeeds: CustomFeedStore?
    private let log = Logger(subsystem: "uk.sume.streamfolio", category: "Search")

    public init(
        store: ArticleStore, downloader: FeedDownloader, preferences: AppPreferences,
        customFeeds: CustomFeedStore? = nil, curated: CuratedFeeds = .shared, parser: RssParser = RssParser()
    ) {
        self.customFeeds = customFeeds
        self.store = store
        self.downloader = downloader
        self.preferences = preferences
        self.curated = curated
        self.parser = parser
    }

    /// Feed URLs (with the category each belongs to) that online search covers.
    func feedsToSearch() -> [(url: String, category: String)] {
        guard preferences[Prefs.defaultFeedsEnabled] else { return [] }
        var seen = Set<String>()
        var result: [(String, String)] = []
        for category in preferences[Prefs.selectedCategories].sorted() {
            let urls = curated.feeds(
                region: preferences[Prefs.region], category: category,
                disabledFeedUrls: preferences[Prefs.disabledFeedUrls],
                enabledCrossRegionFeeds: preferences[Prefs.enabledCrossRegionFeeds]
            )
            for url in urls where seen.insert(url).inserted && !LinkOnlySources.isLinkOnly(url: url) {
                result.append((url, category))
            }
        }
        return result
    }

    /// Fetches, filters and caches matches for `query`. Returns how many matched.
    @discardableResult
    public func searchOnline(_ query: String) async -> Int {
        guard !SearchMatching.clean(query).isEmpty else { return 0 }
        // The user's own feeds are searched too, even with curated feeds off (like Android).
        var seen = Set<String>()
        let own = ((try? await customFeeds?.all()) ?? []).map { (url: $0.url, category: $0.category) }
        let feeds = (feedsToSearch() + own).filter { seen.insert($0.url).inserted }

        let reader = FeedReader(downloader: downloader, parser: parser)
        let collected = await withTaskGroup(of: [Article].self) { group -> [Article] in
            var iterator = feeds.makeIterator()
            var running = 0
            var all: [Article] = []
            func start() {
                guard let feed = iterator.next() else { return }
                running += 1
                group.addTask {
                    (await reader.articles(from: feed.url, category: feed.category, skipLinkOnly: true) ?? [])
                        .filter { SearchMatching.matches($0, query: query, includingSource: true) }
                }
            }
            for _ in 0..<8 { start() }
            while let batch = await group.next() {
                running -= 1
                all.append(contentsOf: batch)
                start()
            }
            return all
        }
        if Task.isCancelled { return 0 }

        // Cached matches carry the SEARCH category; the previous search's unsaved results are dropped.
        let matches = collected.map { article -> Article in
            var copy = article
            copy.category = Self.category
            return copy
        }
        do {
            try await store.replaceSearchResults(with: matches)
        } catch {
            log.error("Caching search results failed: \(String(describing: error), privacy: .public)")
        }
        return matches.count
    }
}

extension SearchService {
    /// Every feed the user has active (for trending topics): curated feeds of the selected categories.
    public func activeFeedUrls() -> Set<String> {
        guard preferences[Prefs.defaultFeedsEnabled] else { return [] }
        var urls = Set<String>()
        for category in preferences[Prefs.selectedCategories] {
            urls.formUnion(curated.feeds(
                region: preferences[Prefs.region], category: category,
                disabledFeedUrls: preferences[Prefs.disabledFeedUrls],
                enabledCrossRegionFeeds: preferences[Prefs.enabledCrossRegionFeeds]
            ))
        }
        return urls
    }
}
