import Foundation
import Observation
import StreamFolioCore
import os

/// The app's long-lived services, built once at launch and shared through the SwiftUI environment.
/// Screens get everything they need from here; none of them creates a store or client of its own.
@MainActor
@Observable
final class AppEnvironment {
    let preferences: AppPreferences
    let database: AppDatabase
    let articles: ArticleStore
    let bookmarks: BookmarkStore
    let customFeeds: CustomFeedStore
    let backup: BackupService
    let providers: ProviderSelection
    let linkOnlyState = LinkOnlyStateStore()
    let actions: ArticleActions
    let repository: NewsRepository
    let content: ArticleContentService
    let feed: FeedStore
    let search: SearchStore
    let saved: SavedStore
    let interactions: ArticleInteractions
    let chrome = ChromeState()

    init(database: AppDatabase, preferences: AppPreferences, http: any HTTPFetching = SafeHTTPClient()) {
        self.database = database
        self.preferences = preferences
        articles = ArticleStore(database)
        bookmarks = BookmarkStore(database)
        customFeeds = CustomFeedStore(database)
        backup = BackupService(database: database, preferences: preferences)
        providers = ProviderSelection(preferences: preferences)
        actions = ArticleActions(articles: articles, bookmarks: bookmarks)
        let downloader = FeedDownloader(
            http: http,
            state: linkOnlyState,
            store: articles,
            appVersion: Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "unknown"
        )
        content = ArticleContentService(store: articles, http: http)
        repository = NewsRepository(store: articles, downloader: downloader, preferences: preferences, content: content)
        feed = FeedStore(repository: repository, articles: articles, customFeeds: customFeeds, preferences: preferences)
        let searchService = SearchService(store: articles, downloader: downloader, preferences: preferences, customFeeds: customFeeds)
        search = SearchStore(articles: articles, service: searchService, preferences: preferences)
        saved = SavedStore(bookmarks: bookmarks, preferences: preferences)
        interactions = ArticleInteractions(actions: actions)
    }

    /// State for one open article.
    func makeReader(for article: Article) -> ReaderStore {
        ReaderStore(article: article, articles: articles, service: content, actions: actions)
    }

    /// The real environment: SQLite in Application Support. If the file cannot be opened (disk full,
    /// corrupt) the app still launches on an in-memory database rather than crashing.
    static func live() -> AppEnvironment {
        let preferences = AppPreferences()
        let log = Logger(subsystem: "uk.sume.streamfolio", category: "AppEnvironment")
        do {
            let directory = try FileManager.default.url(for: .applicationSupportDirectory, in: .userDomainMask, appropriateFor: nil, create: true)
                .appendingPathComponent("StreamFolio", isDirectory: true)
            return AppEnvironment(database: try AppDatabase.open(in: directory), preferences: preferences)
        } catch {
            log.fault("Could not open the database, using memory only: \(String(describing: error), privacy: .public)")
            // An in-memory database cannot fail to open.
            return AppEnvironment(database: (try? AppDatabase.inMemory())!, preferences: preferences)
        }
    }
}
