import Foundation
import GRDB

/// The on-device SQLite database. The schema is Room schema v9 (`app/schemas/.../8.json` plus
/// `MIGRATION_8_9`'s `groupId` index), recreated as a single GRDB migration because an iOS
/// install never has an older database to migrate. Later changes become `registerMigration`s.
public struct AppDatabase: Sendable {
    public let writer: any DatabaseWriter

    public init(_ writer: any DatabaseWriter) throws {
        self.writer = writer
        try Self.migrator.migrate(writer)
        try Self.ensureDefaultBookmarkList(writer)
    }

    /// File-backed database in `directory` (`news_database.sqlite`, same base name as Android).
    public static func open(in directory: URL) throws -> AppDatabase {
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        var configuration = Configuration()
        configuration.prepareDatabase { db in
            try db.execute(sql: "PRAGMA foreign_keys = ON")
        }
        let pool = try DatabasePool(path: directory.appendingPathComponent("news_database.sqlite").path, configuration: configuration)
        return try AppDatabase(pool)
    }

    /// Throwaway database for tests and previews.
    public static func inMemory() throws -> AppDatabase {
        try AppDatabase(DatabaseQueue())
    }

    // MARK: Schema

    static var migrator: DatabaseMigrator {
        var migrator = DatabaseMigrator()
        #if DEBUG
        migrator.eraseDatabaseOnSchemaChange = false   // never silently wipe: user bookmarks live here
        #endif

        migrator.registerMigration("v1-room-v9-schema") { db in
            try db.create(table: "articles") { t in
                t.primaryKey("link", .text)
                t.column("title", .text).notNull()
                t.column("description", .text).notNull()
                t.column("pubDate", .text).notNull()
                t.column("sourceName", .text).notNull()
                t.column("sourceUrl", .text).notNull().defaults(to: "")
                t.column("category", .text).notNull()
                t.column("thumbnailUrl", .text)
                t.column("isBookmarked", .integer).notNull().defaults(to: 0)
                t.column("customFeedId", .integer)
                t.column("fullText", .text)
                t.column("tags", .text)
                t.column("aiSummary", .text)
                t.column("translatedTitle", .text)
                t.column("translatedBody", .text)
                t.column("translatedLanguage", .text)
                t.column("detectedLanguage", .text)
                t.column("isRead", .integer).notNull().defaults(to: 0)
                t.column("groupId", .text)
            }
            try db.create(index: "index_articles_groupId", on: "articles", columns: ["groupId"])
            // Not in Room, but every feed query filters by category and sorts by date.
            try db.create(index: "index_articles_category_pubDate", on: "articles", columns: ["category", "pubDate"])

            try db.create(table: "custom_feeds") { t in
                t.autoIncrementedPrimaryKey("id")
                t.column("title", .text).notNull()
                t.column("url", .text).notNull()
                t.column("category", .text).notNull()
            }

            try db.create(table: "tts_playlist_state") { t in
                t.primaryKey("id", .integer)
                t.column("currentIndex", .integer).notNull()
                t.column("linksJson", .text).notNull()
            }

            try db.create(table: "bookmark_lists") { t in
                t.autoIncrementedPrimaryKey("id")
                t.column("name", .text).notNull()
                t.column("isDefault", .integer).notNull()
                t.column("createdAt", .integer).notNull()
            }
            try db.create(index: "index_bookmark_lists_name", on: "bookmark_lists", columns: ["name"], unique: true)

            try db.create(table: "article_bookmark_lists") { t in
                t.column("articleLink", .text).notNull()
                t.column("listId", .integer).notNull()
                t.primaryKey(["articleLink", "listId"])
            }
            try db.create(index: "index_article_bookmark_lists_articleLink", on: "article_bookmark_lists", columns: ["articleLink"])
            try db.create(index: "index_article_bookmark_lists_listId", on: "article_bookmark_lists", columns: ["listId"])
        }
        return migrator
    }

    /// Runs on every open, not only first creation: a fresh Android install once had zero lists
    /// (the seed lived only in a migration) and silently failed to bookmark anything.
    static func ensureDefaultBookmarkList(_ writer: any DatabaseWriter) throws {
        try writer.write { db in
            try db.execute(
                sql: "INSERT OR IGNORE INTO bookmark_lists (id, name, isDefault, createdAt) VALUES (?, ?, 1, ?)",
                arguments: [BookmarkList.defaultListId, BookmarkList.defaultListName, Int64(Date().timeIntervalSince1970 * 1000)]
            )
        }
    }
}
