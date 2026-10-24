import Foundation
import Observation

/// State behind the Search tab: the query, local + online results, recent searches and trending
/// topics. Port of the search parts of `NewsViewModel`.
///
/// Typing is debounced by 500 ms. Then two things run: the local database search (results appear at
/// once and keep updating) and the online search, which fetches the active feeds and caches matches —
/// those show up in the local results as they are stored.
@MainActor
@Observable
public final class SearchStore {
    public enum Phase: Equatable, Sendable { case idle, loading, noResults, results }

    public static let debounce: Duration = .milliseconds(500)
    public static let maxRecents = 8

    public var query = "" { didSet { if query != oldValue { queryChanged() } } }
    public private(set) var results: [Article] = []
    public private(set) var isSearching = false
    public private(set) var recentSearches: [String]
    public private(set) var trending: [String] = TrendingTopics.defaults

    private let articles: ArticleStore
    private let service: SearchService
    private let preferences: AppPreferences
    @ObservationIgnored private var search: Task<Void, Never>?

    public init(articles: ArticleStore, service: SearchService, preferences: AppPreferences) {
        self.articles = articles
        self.service = service
        self.preferences = preferences
        self.recentSearches = preferences[Prefs.recentSearches]
    }

    public var phase: Phase {
        if SearchMatching.clean(query).isEmpty { return .idle }
        if !results.isEmpty { return .results }
        return isSearching ? .loading : .noResults
    }

    // MARK: Recents

    /// Remembers a search the user committed to (return key, tapping a suggestion).
    public func remember(_ term: String) {
        let trimmed = term.trimmed
        guard !trimmed.isEmpty else { return }
        var list = recentSearches.filter { $0 != trimmed }
        list.insert(trimmed, at: 0)
        store(Array(list.prefix(Self.maxRecents)))
    }

    public func removeRecent(_ term: String) { store(recentSearches.filter { $0 != term }) }
    public func clearRecents() { store([]) }

    /// Runs `term` as the current search and remembers it.
    public func choose(_ term: String) {
        query = term
        remember(term)
    }

    private func store(_ list: [String]) {
        recentSearches = list
        preferences[Prefs.recentSearches] = list
    }

    // MARK: Trending

    /// Recomputes trending topics from the newest articles of the active sources.
    public func refreshTrending() async {
        guard let latest = try? await articles.latest(limit: 50) else { return }
        trending = TrendingTopics.compute(from: latest, activeFeedUrls: service.activeFeedUrls())
    }

    // MARK: Searching

    private func queryChanged() {
        search?.cancel()
        let current = query
        guard !SearchMatching.clean(current).isEmpty else {
            results = []
            isSearching = false
            return
        }
        isSearching = true
        search = Task { [articles, service] in
            try? await Task.sleep(for: Self.debounce)
            guard !Task.isCancelled else { return }

            let observer = Task {
                do {
                    for try await found in articles.observeSearch(query: current) {
                        guard !Task.isCancelled, self.query == current else { return }
                        self.results = found
                    }
                } catch {}
            }
            await service.searchOnline(current)
            if !Task.isCancelled, self.query == current { self.isSearching = false }
            // The observer keeps running (cancelled with this task's successor) so cached matches appear.
            await withTaskCancellationHandler { await observer.value } onCancel: { observer.cancel() }
        }
    }
}
