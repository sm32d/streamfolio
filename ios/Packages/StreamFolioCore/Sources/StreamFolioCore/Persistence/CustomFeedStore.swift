import Foundation
import GRDB

/// The user's own RSS/Atom feeds (`CustomFeedDao` plus the custom-feed methods of `NewsViewModel`).
/// Every URL is validated here, once, so Settings, OPML import and backup restore cannot disagree on
/// what a safe feed URL is: HTTPS only, public hosts only (`UrlSecurityValidator`).
public struct CustomFeedStore: Sendable {
    /// Android's bounds on restored feeds; applied to every path that creates a feed.
    public static let maxTitleLength = 200
    public static let maxCategoryLength = 100

    private let writer: any DatabaseWriter

    public init(_ database: AppDatabase) {
        self.writer = database.writer
    }

    /// A validated feed, or nil when the URL is unsafe or a field is blank.
    public static func makeFeed(title: String, url: String, category: String, id: Int? = nil) -> CustomFeed? {
        let title = String(title.trimmed.prefix(maxTitleLength))
        let category = String(category.trimmed.prefix(maxCategoryLength))
        guard !title.isEmpty, !category.isEmpty,
              let safeUrl = UrlSecurityValidator.sanitizeUrl(url.trimmed, requireHttps: true)
        else { return nil }
        return CustomFeed(id: id, title: title, url: safeUrl, category: category)
    }

    // MARK: Writes

    /// Adds a feed. Returns nil (and stores nothing) for an unsafe URL or blank field.
    @discardableResult
    public func add(title: String, url: String, category: String) async throws -> CustomFeed? {
        guard let feed = Self.makeFeed(title: title, url: url, category: category) else { return nil }
        return try await writer.write { db in
            var inserted = feed
            try inserted.insert(db)
            return inserted
        }
    }

    /// Replaces a feed's title, URL and tab label. Returns nil for an unsafe URL or blank field.
    @discardableResult
    public func update(_ feed: CustomFeed, title: String, url: String, category: String) async throws -> CustomFeed? {
        guard let id = feed.id, let updated = Self.makeFeed(title: title, url: url, category: category, id: id) else { return nil }
        try await writer.write { db in try updated.update(db) }
        return updated
    }

    public func delete(_ feed: CustomFeed) async throws {
        guard let id = feed.id else { return }
        try await writer.write { db in _ = try CustomFeed.deleteOne(db, key: id) }
    }

    /// Imports parsed OPML feeds: only the ticked folders, renamed through `categoryMapping`, safe
    /// URLs only, skipping feeds already subscribed (case-insensitive URL). Returns how many were added.
    @discardableResult
    public func importFeeds(_ feeds: [OpmlFeed], categoryMapping: [String: String], selectedCategories: Set<String>) async throws -> Int {
        try await writer.write { db in
            var known = Set(try CustomFeed.fetchAll(db).map { $0.url.trimmed.lowercased() })
            var added = 0
            for opml in feeds where selectedCategories.contains(opml.category) {
                let mapped = categoryMapping[opml.category]?.trimmed ?? ""
                guard var feed = Self.makeFeed(title: opml.title, url: opml.xmlUrl, category: mapped.isEmpty ? opml.category : mapped),
                      known.insert(feed.url.lowercased()).inserted
                else { continue }
                try feed.insert(db)
                added += 1
            }
            return added
        }
    }

    // MARK: Reads

    public func all() async throws -> [CustomFeed] {
        try await writer.read { db in try CustomFeed.order(Column("id")).fetchAll(db) }
    }

    public func observeAll() -> AsyncThrowingStream<[CustomFeed], Error> {
        let writer = self.writer
        let observation = ValueObservation.tracking { db in try CustomFeed.order(Column("id")).fetchAll(db) }
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
