import Foundation
import GRDB

/// Bookmark lists and which articles are in them (`BookmarkListDao` + the bookmark methods of
/// `NewsRepository`). `articles.isBookmarked` is kept equal to "is in at least one list".
public struct BookmarkStore: Sendable {
    private let writer: any DatabaseWriter

    public init(_ database: AppDatabase) {
        self.writer = database.writer
    }

    // MARK: Saving

    /// Quick save/unsave (swipe action, toolbar button). Saving adds the article to the default
    /// "Saved" list; unsaving removes it from every list.
    public func setSaved(_ isSaved: Bool, link: String) async throws {
        try await writer.write { db in
            if isSaved {
                let defaultId = try Int64.fetchOne(db, sql: "SELECT id FROM bookmark_lists WHERE isDefault = 1 LIMIT 1") ?? BookmarkList.defaultListId
                try ArticleBookmarkListCrossRef(articleLink: link, listId: defaultId).insert(db, onConflict: .replace)
            } else {
                try db.execute(sql: "DELETE FROM article_bookmark_lists WHERE articleLink = ?", arguments: [link])
            }
            try db.execute(sql: "UPDATE articles SET isBookmarked = ? WHERE link = ?", arguments: [isSaved, link])
        }
    }

    /// Replaces the lists an article belongs to ("Save to collection" sheet).
    public func setLists(_ listIds: Set<Int64>, link: String) async throws {
        try await writer.write { db in
            try db.execute(sql: "DELETE FROM article_bookmark_lists WHERE articleLink = ?", arguments: [link])
            for id in listIds { try ArticleBookmarkListCrossRef(articleLink: link, listId: id).insert(db, onConflict: .replace) }
            try db.execute(sql: "UPDATE articles SET isBookmarked = ? WHERE link = ?", arguments: [!listIds.isEmpty, link])
        }
    }

    public func removeFromList(link: String, listId: Int64) async throws {
        try await writer.write { db in
            try db.execute(sql: "DELETE FROM article_bookmark_lists WHERE articleLink = ? AND listId = ?", arguments: [link, listId])
            let remaining = try Int.fetchOne(db, sql: "SELECT COUNT(*) FROM article_bookmark_lists WHERE articleLink = ?", arguments: [link]) ?? 0
            if remaining == 0 { try db.execute(sql: "UPDATE articles SET isBookmarked = 0 WHERE link = ?", arguments: [link]) }
        }
    }

    // MARK: Lists

    /// Creates a list; returns its id, or nil for a blank name or a duplicate (names are unique).
    public func createList(named name: String) async throws -> Int64? {
        let trimmed = name.trimmed
        guard !trimmed.isEmpty else { return nil }
        return try await writer.write { db in
            var list = BookmarkList(name: trimmed)
            do { try list.insert(db) } catch let error as DatabaseError where error.resultCode == .SQLITE_CONSTRAINT { return nil }
            return list.id
        }
    }

    public func renameList(id: Int64, to name: String) async throws {
        let trimmed = name.trimmed
        guard !trimmed.isEmpty else { return }
        try await writer.write { db in
            try db.execute(sql: "UPDATE OR IGNORE bookmark_lists SET name = ? WHERE id = ? AND isDefault = 0", arguments: [trimmed, id])
        }
    }

    /// Deletes a custom list. Its articles move to `moveTo` (default: the default list) when asked,
    /// and articles left in no list are un-bookmarked.
    public func deleteList(id: Int64, moveArticlesTo destination: Int64? = nil) async throws {
        try await writer.write { db in
            let defaultId = try Int64.fetchOne(db, sql: "SELECT id FROM bookmark_lists WHERE isDefault = 1 LIMIT 1") ?? BookmarkList.defaultListId
            if let destination, destination != id, id != defaultId {
                try db.execute(sql: "UPDATE OR IGNORE article_bookmark_lists SET listId = ? WHERE listId = ?", arguments: [destination, id])
            }
            try db.execute(sql: "DELETE FROM article_bookmark_lists WHERE listId = ?", arguments: [id])
            try db.execute(sql: "DELETE FROM bookmark_lists WHERE id = ? AND isDefault = 0", arguments: [id])
            try db.execute(sql: """
                UPDATE articles SET isBookmarked = 0
                WHERE isBookmarked = 1 AND link NOT IN (SELECT articleLink FROM article_bookmark_lists)
                """)
        }
    }

    // MARK: Reads and observation

    public func listIds(forArticle link: String) async throws -> Set<Int64> {
        try await writer.read { db in
            Set(try Int64.fetchAll(db, sql: "SELECT listId FROM article_bookmark_lists WHERE articleLink = ?", arguments: [link]))
        }
    }

    public func observeLists() -> AsyncThrowingStream<[BookmarkList], Error> {
        stream(ValueObservation.tracking { db in
            try BookmarkList.order(sql: "isDefault DESC, createdAt ASC").fetchAll(db)
        })
    }

    public func observeArticles(inList listId: Int64) -> AsyncThrowingStream<[Article], Error> {
        stream(ValueObservation.tracking { db in
            try Article.fetchAll(db, sql: """
                SELECT a.* FROM articles a
                INNER JOIN article_bookmark_lists x ON a.link = x.articleLink
                WHERE x.listId = ? ORDER BY a.pubDate DESC
                """, arguments: [listId])
        })
    }

    /// Every list with its article count and a preview image, in display order.
    public func observeCollections() -> AsyncThrowingStream<[CollectionSummary], Error> {
        stream(ValueObservation.tracking { db in
            try CollectionSummary.fetchAll(db, sql: """
                SELECT l.id, l.name, l.isDefault, l.createdAt,
                  (SELECT COUNT(*) FROM article_bookmark_lists x WHERE x.listId = l.id) AS articleCount,
                  (SELECT a.thumbnailUrl FROM articles a
                     INNER JOIN article_bookmark_lists x ON a.link = x.articleLink
                     WHERE x.listId = l.id AND a.thumbnailUrl IS NOT NULL AND a.thumbnailUrl != '' AND a.thumbnailUrl != 'failed'
                     ORDER BY a.pubDate DESC LIMIT 1) AS previewThumbnail
                FROM bookmark_lists l ORDER BY l.isDefault DESC, l.createdAt ASC
                """)
        })
    }

    /// Every saved article (in at least one list), newest first — the "All Saved" collection.
    public func observeAllSaved() -> AsyncThrowingStream<[Article], Error> {
        stream(ValueObservation.tracking { db in
            try Article.fetchAll(db, sql: "SELECT * FROM articles WHERE isBookmarked = 1 ORDER BY pubDate DESC")
        })
    }

    public func observeCount(inList listId: Int64) -> AsyncThrowingStream<Int, Error> {
        stream(ValueObservation.tracking { db in
            try Int.fetchOne(db, sql: "SELECT COUNT(*) FROM article_bookmark_lists WHERE listId = ?", arguments: [listId]) ?? 0
        })
    }

    public func observeSavedLinks() -> AsyncThrowingStream<Set<String>, Error> {
        stream(ValueObservation.tracking { db in
            Set(try String.fetchAll(db, sql: "SELECT articleLink FROM article_bookmark_lists"))
        })
    }

    private func stream<Value: Sendable>(_ observation: ValueObservation<ValueReducers.Fetch<Value>>) -> AsyncThrowingStream<Value, Error> {
        let writer = self.writer
        return AsyncThrowingStream { continuation in
            let task = Task {
                do {
                    for try await value in observation.values(in: writer) { continuation.yield(value) }
                    continuation.finish()
                } catch {
                    continuation.finish(throwing: error)
                }
            }
            continuation.onTermination = { _ in task.cancel() }
        }
    }
}
