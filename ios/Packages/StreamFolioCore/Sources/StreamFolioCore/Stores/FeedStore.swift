import Foundation
import Observation

/// State behind the Home feed: the selected category, its articles grouped by story, and refreshing.
/// Mirrors the Home part of Android's `NewsViewModel` (category selection with a 15-minute staleness
/// rule, first three stories as "Trending Highlights", the rest as "Latest Stories", paging).
@MainActor
@Observable
public final class FeedStore {
    public static let staleAfter: TimeInterval = 15 * 60
    public static let trendingCount = 3
    public static let pageSize = 20

    public private(set) var category: String
    public private(set) var allGroups: [ArticleGroup] = []
    public private(set) var isRefreshing = false
    /// False until the database has answered at least once for the current category.
    public private(set) var hasLoaded = false
    public private(set) var lastOutcome: NewsRepository.RefreshOutcome?
    public private(set) var visibleCount = FeedStore.pageSize
    public private(set) var customFeeds: [CustomFeed] = []
    public var selectedPublisher: String?

    private let repository: NewsRepository
    private let articles: ArticleStore
    private let customFeedStore: CustomFeedStore
    private let preferences: AppPreferences
    private let now: @Sendable () -> Date

    @ObservationIgnored private var observation: Task<Void, Never>?
    @ObservationIgnored private var customFeedObservation: Task<Void, Never>?
    @ObservationIgnored private var refresh: Task<Void, Never>?
    @ObservationIgnored private var lastRefreshed: [String: Date] = [:]
    @ObservationIgnored private var latest: [Article] = []
    @ObservationIgnored private var pendingPublish: Task<Void, Never>?

    public init(
        repository: NewsRepository,
        articles: ArticleStore,
        customFeeds customFeedStore: CustomFeedStore,
        preferences: AppPreferences,
        now: @escaping @Sendable () -> Date = Date.init
    ) {
        self.repository = repository
        self.articles = articles
        self.customFeedStore = customFeedStore
        self.preferences = preferences
        self.now = now
        self.category = CategoryLayout.categories(preferences: preferences, customFeeds: []).first ?? NewsCategory.all[0]
    }

    // MARK: Derived

    /// The Home tabs: selected curated categories plus the tab labels of custom feeds, in the user's
    /// chosen order (`CategoryLayout`).
    public var categories: [String] { CategoryLayout.categories(preferences: preferences, customFeeds: customFeeds) }

    private var filtered: [ArticleGroup] {
        guard let publisher = selectedPublisher else { return allGroups }
        return allGroups.filter { $0.primary.sourceName == publisher }
    }

    public var trending: [ArticleGroup] { Array(filtered.prefix(Self.trendingCount)) }

    /// Stories after the trending ones, limited to what has been paged in.
    public var stories: [ArticleGroup] { Array(filtered.dropFirst(Self.trendingCount).prefix(visibleCount)) }

    public var hasMoreStories: Bool { filtered.count - Self.trendingCount > visibleCount }

    /// Publishers present in the category, for the filter carousel (max 10, first-seen order).
    public var publishers: [String] {
        var seen = Set<String>()
        return allGroups.map(\.primary.sourceName).filter { seen.insert($0).inserted }.prefix(10).map { $0 }
    }

    public var isEmpty: Bool { hasLoaded && filtered.isEmpty }

    // MARK: Intents

    /// Starts observing the current category and refreshes it if it is stale.
    public func start() {
        observe(category)
        refreshIfStale()
        observeCustomFeeds()
    }

    public func select(_ newCategory: String) {
        guard newCategory != category else { return }
        category = newCategory
        selectedPublisher = nil
        visibleCount = Self.pageSize
        hasLoaded = false
        allGroups = []
        observe(newCategory)
        refreshIfStale()
    }

    /// Call after Settings changed what Home shows (sources, categories, order, region). Moves to a
    /// valid tab if the current one is gone, and re-fetches the current tab now.
    public func settingsDidChange(regionChanged: Bool = false) {
        if regionChanged { lastRefreshed.removeAll() }
        lastRefreshed[category] = nil
        selectFirstCategoryIfCurrentIsGone()
        regroup()
        refreshIfStale()
    }

    /// Pull-to-refresh: always fetches.
    public func refreshNow() async {
        await performRefresh(category: category, silent: false)
    }

    public func loadMore() {
        guard hasMoreStories else { return }
        visibleCount += Self.pageSize
    }

    /// Re-groups when the "group similar stories" setting changes.
    public func regroup() { publish(latest) }

    // MARK: Internals

    private func refreshIfStale() {
        let current = category
        let last = lastRefreshed[current] ?? .distantPast
        guard now().timeIntervalSince(last) > Self.staleAfter else { return }
        refresh?.cancel()
        refresh = Task { await performRefresh(category: current, silent: true) }
    }

    private func performRefresh(category refreshed: String, silent: Bool) async {
        if !silent { isRefreshing = true }
        // Read the feeds fresh: a feed added a moment ago may not have reached `customFeeds` yet.
        let feeds = (try? await customFeedStore.all()) ?? customFeeds
        let outcome = await repository.refresh(category: refreshed, customFeeds: feeds)
        lastRefreshed[refreshed] = now()
        lastOutcome = outcome
        if !silent { isRefreshing = false }
    }

    /// Quiet period that coalesces the trickle of per-article writes (image and body enrichment lands
    /// one row at a time) into one list update, so the feed does not re-render for every row.
    public static let updateDebounce: Duration = .milliseconds(300)

    private func observe(_ observed: String) {
        observation?.cancel()
        pendingPublish?.cancel()
        observation = Task { [articles] in
            var isFirst = true
            do {
                for try await list in articles.observe(category: observed) {
                    guard !Task.isCancelled, observed == self.category else { return }
                    self.latest = list
                    if isFirst {
                        isFirst = false
                        self.publish(list)   // the first answer is shown at once
                    } else {
                        self.pendingPublish?.cancel()
                        self.pendingPublish = Task {
                            try? await Task.sleep(for: Self.updateDebounce)
                            guard !Task.isCancelled, observed == self.category else { return }
                            self.publish(self.latest)
                        }
                    }
                }
            } catch {
                // The database being unreadable leaves the last good list on screen.
            }
        }
    }

    private func observeCustomFeeds() {
        guard customFeedObservation == nil else { return }
        customFeedObservation = Task { [customFeedStore] in
            do {
                for try await feeds in customFeedStore.observeAll() {
                    self.customFeeds = feeds
                    self.selectFirstCategoryIfCurrentIsGone()
                }
            } catch {
                // Keep the last good list.
            }
        }
    }

    private func selectFirstCategoryIfCurrentIsGone() {
        let available = categories
        if let first = available.first, !available.contains(category) { select(first) }
    }

    private func publish(_ list: [Article]) {
        allGroups = preferences[Prefs.groupSimilarStories]
            ? StoryGrouper.group(list)
            : list.map { ArticleGroup(primary: $0) }
        hasLoaded = true
    }
}
