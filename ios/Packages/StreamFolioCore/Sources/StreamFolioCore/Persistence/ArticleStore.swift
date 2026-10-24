import Foundation
import GRDB

/// All article reads and writes. Port of Android's `ArticleDao`; queries keep the same semantics.
public struct ArticleStore: Sendable {
    private let writer: any DatabaseWriter

    public init(_ database: AppDatabase) {
        self.writer = database.writer
    }

    // MARK: Writes

    /// Inserts new articles and refreshes existing ones in a single transaction, keeping everything
    /// the user or the app derived locally (`ArticleDao.insertOrUpdateArticles`).
    public func upsert(_ articles: [Article]) async throws {
        try await writer.write { db in
            for var article in articles {
                if let existing = try Article.fetchOne(db, key: article.link) {
                    article.isBookmarked = existing.isBookmarked
                    article.isRead = existing.isRead
                    article.fullText = existing.fullText
                    article.aiSummary = existing.aiSummary
                    article.translatedTitle = existing.translatedTitle
                    article.translatedBody = existing.translatedBody
                    article.translatedLanguage = existing.translatedLanguage
                    article.detectedLanguage = existing.detectedLanguage
                    article.groupId = article.groupId ?? existing.groupId
                    try article.update(db)
                } else {
                    try article.insert(db)
                }
            }
        }
    }

    /// Deletes un-bookmarked articles older than `expiry` (`yyyy-MM-dd'T'HH:mm:ss.SSS'Z'`). Articles
    /// in any bookmark list are kept as well, as documented in `docs/05`.
    public func prune(olderThan expiry: String) async throws {
        try await writer.write { db in
            try db.execute(
                sql: """
                DELETE FROM articles
                WHERE isBookmarked = 0 AND pubDate < ?
                  AND link NOT IN (SELECT articleLink FROM article_bookmark_lists)
                """,
                arguments: [expiry]
            )
        }
    }

    public func setRead(_ isRead: Bool, link: String) async throws {
        try await writer.write { db in
            try db.execute(sql: "UPDATE articles SET isRead = ? WHERE link = ?", arguments: [isRead, link])
        }
    }

    /// Sets `isBookmarked` without touching lists. Callers that manage collections use `BookmarkStore`.
    func setBookmarked(link: String) async throws {
        try await writer.write { db in
            try db.execute(sql: "UPDATE articles SET isBookmarked = 1 WHERE link = ?", arguments: [link])
        }
    }

    public func setThumbnail(_ url: String?, link: String) async throws {
        try await writer.write { db in
            try db.execute(sql: "UPDATE articles SET thumbnailUrl = ? WHERE link = ?", arguments: [url, link])
        }
    }

    /// Replaces the cached results of the previous online search (un-bookmarked SEARCH articles) with
    /// `articles`, keeping any the user saved.
    public func replaceSearchResults(with articles: [Article]) async throws {
        try await writer.write { db in
            try db.execute(sql: "DELETE FROM articles WHERE category = 'SEARCH' AND isBookmarked = 0")
            for article in articles {
                // Already known (e.g. from a category feed): keep it where it is, untouched.
                if try Article.fetchOne(db, key: article.link) != nil { continue }
                try article.insert(db)
            }
        }
    }

    public func setFullText(_ text: String?, link: String) async throws {
        try await writer.write { db in
            try db.execute(sql: "UPDATE articles SET fullText = ? WHERE link = ?", arguments: [text, link])
        }
    }

    /// Removes every article (bookmarked or not) and its list links whose link or feed URL matches
    /// `pattern`. Used when a link-only source is withdrawn.
    public func deleteMatching(pattern: String) async throws {
        try await writer.write { db in
            try db.execute(sql: "DELETE FROM article_bookmark_lists WHERE articleLink LIKE ?", arguments: [pattern])
            try db.execute(sql: "DELETE FROM articles WHERE link LIKE ? OR sourceUrl LIKE ?", arguments: [pattern, pattern])
        }
    }

    // MARK: Reads

    public func article(link: String) async throws -> Article? {
        try await writer.read { db in try Article.fetchOne(db, key: link) }
    }

    /// The stored versions of `links` (in no particular order).
    public func articles(links: [String]) async throws -> [Article] {
        guard !links.isEmpty else { return [] }
        return try await writer.read { db in try Article.fetchAll(db, keys: links) }
    }

    /// The 50 newest articles of a category, used to place new articles into existing story groups.
    public func recent(category: String, limit: Int = 50) async throws -> [Article] {
        try await writer.read { db in
            try Article.filter(Column("category") == category).order(Column("pubDate").desc).limit(limit).fetchAll(db)
        }
    }

    /// Other articles in the same story group, newest first (indexed lookup on `groupId`).
    public func related(groupId: String, excluding link: String) async throws -> [Article] {
        try await writer.read { db in
            try Article.filter(Column("groupId") == groupId && Column("link") != link).order(Column("pubDate").desc).fetchAll(db)
        }
    }

    public func count() async throws -> Int {
        try await writer.read { db in try Article.fetchCount(db) }
    }

    // MARK: Observation

    /// Articles matching `query` in title or description, newest first (all categories). Short queries
    /// are narrowed to whole words.
    public func observeSearch(query: String) -> AsyncThrowingStream<[Article], Error> {
        let pattern = SearchMatching.likePattern(query)
        let observation = ValueObservation.tracking { db in
            try Article.fetchAll(db, sql: """
                SELECT * FROM articles
                WHERE title LIKE ? ESCAPE '\\' OR description LIKE ? ESCAPE '\\'
                ORDER BY pubDate DESC
                """, arguments: [pattern, pattern])
        }
        return AsyncThrowingStream { continuation in
            let task = Task {
                do {
                    for try await found in observation.values(in: writer) {
                        continuation.yield(found.filter { SearchMatching.matches($0, query: query, includingSource: false) })
                    }
                    continuation.finish()
                } catch {
                    continuation.finish(throwing: error)
                }
            }
            continuation.onTermination = { _ in task.cancel() }
        }
    }

    /// The newest `limit` articles overall (for trending topics).
    public func latest(limit: Int = 50) async throws -> [Article] {
        try await writer.read { db in try Article.order(Column("pubDate").desc).limit(limit).fetchAll(db) }
    }

    /// Emits one article now and after every change to it (nil once it is deleted).
    public func observe(link: String) -> AsyncThrowingStream<Article?, Error> {
        let observation = ValueObservation.tracking { db in try Article.fetchOne(db, key: link) }
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

    /// Emits the category's articles (newest first) now and after every change.
    public func observe(category: String) -> AsyncThrowingStream<[Article], Error> {
        let observation = ValueObservation.tracking { db in
            try Article.filter(Column("category") == category).order(Column("pubDate").desc).fetchAll(db)
        }
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
