import Foundation
import GRDB

/// A user-added RSS/Atom feed (`custom_feeds`).
public struct CustomFeed: Codable, Hashable, Sendable, Identifiable, FetchableRecord, MutablePersistableRecord {
    public static let databaseTableName = "custom_feeds"

    public var id: Int?
    public var title: String
    public var url: String
    public var category: String

    public init(id: Int? = nil, title: String, url: String, category: String) {
        self.id = id
        self.title = title
        self.url = url
        self.category = category
    }

    public mutating func didInsert(_ inserted: InsertionSuccess) {
        id = Int(inserted.rowID)
    }
}

/// A bookmark collection (`bookmark_lists`). Row id 1 is the default "Saved" list.
public struct BookmarkList: Codable, Hashable, Sendable, Identifiable, FetchableRecord, MutablePersistableRecord {
    public static let databaseTableName = "bookmark_lists"
    public static let defaultListId: Int64 = 1
    public static let defaultListName = "Saved"

    public var id: Int64?
    public var name: String
    public var isDefault: Bool
    /// Unix time in milliseconds.
    public var createdAt: Int64

    public init(id: Int64? = nil, name: String, isDefault: Bool = false, createdAt: Int64 = Int64(Date().timeIntervalSince1970 * 1000)) {
        self.id = id
        self.name = name
        self.isDefault = isDefault
        self.createdAt = createdAt
    }

    public mutating func didInsert(_ inserted: InsertionSuccess) {
        id = inserted.rowID
    }
}

/// Many-to-many link between articles and bookmark lists (`article_bookmark_lists`).
public struct ArticleBookmarkListCrossRef: Codable, Hashable, Sendable, FetchableRecord, PersistableRecord {
    public static let databaseTableName = "article_bookmark_lists"

    public var articleLink: String
    public var listId: Int64

    public init(articleLink: String, listId: Int64) {
        self.articleLink = articleLink
        self.listId = listId
    }
}

/// A list as shown on the Saved screen.
public struct CollectionSummary: Codable, Hashable, Sendable, Identifiable, FetchableRecord {
    public var id: Int64
    public var name: String
    public var isDefault: Bool
    public var createdAt: Int64
    public var articleCount: Int
    public var previewThumbnail: String?
}
