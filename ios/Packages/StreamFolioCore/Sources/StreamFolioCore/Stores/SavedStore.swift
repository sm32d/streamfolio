import Foundation
import Observation

/// Which set of saved articles a collection screen shows.
public enum CollectionID: Hashable, Sendable {
    /// Every saved article (the "All Saved" tile, Android's list id -1).
    case all
    case list(Int64)
}

/// State behind the Saved tab: every collection with its count and preview image, the "All Saved" set,
/// and the grid/list preference. Port of the collection parts of `NewsViewModel`.
@MainActor
@Observable
public final class SavedStore {
    public private(set) var collections: [CollectionSummary] = []
    public private(set) var allSaved: [Article] = []
    public private(set) var hasLoaded = false

    private let bookmarks: BookmarkStore
    private let preferences: AppPreferences
    @ObservationIgnored private var observers: [Task<Void, Never>] = []

    public init(bookmarks: BookmarkStore, preferences: AppPreferences) {
        self.bookmarks = bookmarks
        self.preferences = preferences
    }

    public var isGrid: Bool {
        get { preferences[Prefs.bookmarkListViewIsGrid] }
        set { preferences[Prefs.bookmarkListViewIsGrid] = newValue }
    }

    /// The preview image of "All Saved": the newest saved article that has one.
    public var allSavedPreview: String? {
        allSaved.lazy.compactMap(\.thumbnailUrl).first { !$0.isEmpty && $0 != ArticleContentService.thumbnailFailed }
    }

    public var isEmpty: Bool { hasLoaded && allSaved.isEmpty && collections.allSatisfy { $0.articleCount == 0 } && collections.count <= 1 }

    public func start() {
        guard observers.isEmpty else { return }
        observers = [
            Task { [bookmarks] in
                do { for try await list in bookmarks.observeCollections() { self.collections = list; self.hasLoaded = true } } catch {}
            },
            Task { [bookmarks] in
                do { for try await list in bookmarks.observeAllSaved() { self.allSaved = list } } catch {}
            },
        ]
    }

    public func name(of id: CollectionID) -> String {
        switch id {
        case .all: "All Saved Articles"
        case .list(let listId): collections.first { $0.id == listId }?.name ?? "Collection"
        }
    }

    public func summary(of id: CollectionID) -> CollectionSummary? {
        if case .list(let listId) = id { return collections.first { $0.id == listId } }
        return nil
    }

    @discardableResult
    public func create(named name: String) async -> Int64? {
        try? await bookmarks.createList(named: name)
    }

    public func rename(_ id: Int64, to name: String) async { try? await bookmarks.renameList(id: id, to: name) }

    /// Deletes a collection; its articles move to `destination` when given.
    public func delete(_ id: Int64, moveTo destination: Int64?) async {
        try? await bookmarks.deleteList(id: id, moveArticlesTo: destination)
    }
}

/// The articles of one collection, live.
@MainActor
@Observable
public final class CollectionContents {
    public private(set) var articles: [Article] = []
    public private(set) var hasLoaded = false

    private let id: CollectionID
    private let bookmarks: BookmarkStore
    @ObservationIgnored private var observer: Task<Void, Never>?

    public init(id: CollectionID, bookmarks: BookmarkStore) {
        self.id = id
        self.bookmarks = bookmarks
    }

    public func start() {
        guard observer == nil else { return }
        let stream: AsyncThrowingStream<[Article], Error> = switch id {
        case .all: bookmarks.observeAllSaved()
        case .list(let listId): bookmarks.observeArticles(inList: listId)
        }
        observer = Task {
            do { for try await list in stream { self.articles = list; self.hasLoaded = true } } catch {}
        }
    }

    /// Swipe-to-remove: out of this collection, or out of everything when viewing "All Saved".
    public func remove(_ article: Article) async {
        switch id {
        case .all: try? await bookmarks.setSaved(false, link: article.link)
        case .list(let listId): try? await bookmarks.removeFromList(link: article.link, listId: listId)
        }
    }

    deinit { observer?.cancel() }
}
