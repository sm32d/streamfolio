import Foundation
import GRDB

/// Full-settings backup and restore (`BackupHelper` + the backup methods of `NewsViewModel`).
///
/// The file format is Android's backup v2, field for field, so a backup made on either platform
/// restores on the other. Every value read from a file is untrusted: URLs go through
/// `UrlSecurityValidator`, text is length-bounded, preferences are range-checked, and link-only
/// publishers' articles are never written to or restored from a backup (their data stays in the device
/// cache only, so it cannot outlive the publisher withdrawing the source).
public struct BackupService: Sendable {
    public static let version = 2
    /// Largest backup the app will read.
    public static let maxBytes = 32 * 1024 * 1024

    public enum BackupError: Error, Equatable, LocalizedError {
        case notABackup
        case tooLarge
        case unsupportedVersion(Int)

        public var errorDescription: String? {
            switch self {
            case .notABackup: "This file is not a StreamFolio backup."
            case .tooLarge: "This file is too large to be a StreamFolio backup."
            case .unsupportedVersion(let version): "Backup version \(version) is not supported."
            }
        }
    }

    public struct RestoreSummary: Sendable, Equatable {
        public var feeds = 0
        public var articles = 0
        public var lists = 0
    }

    private let writer: any DatabaseWriter
    private let preferences: AppPreferences

    public init(database: AppDatabase, preferences: AppPreferences) {
        self.writer = database.writer
        self.preferences = preferences
    }

    /// Android's quick check before showing the "Restore Settings" confirmation.
    public static func looksLikeBackup(_ data: Data) -> Bool {
        guard data.count <= maxBytes, let text = String(data: data, encoding: .utf8) else { return false }
        return text.contains("custom_feeds") && text.contains("preferences")
    }

    // MARK: Export

    public func export() async throws -> Data {
        let content = try await writer.read { db in
            (
                feeds: try CustomFeed.order(Column("id")).fetchAll(db),
                articles: try Article.fetchAll(db, sql: "SELECT * FROM articles WHERE isBookmarked = 1 OR isRead = 1"),
                lists: try BookmarkList.order(Column("id")).fetchAll(db),
                refs: try ArticleBookmarkListCrossRef.fetchAll(db)
            )
        }

        var preferenceObject: [String: Any] = [:]
        for entry in BackupPreferences.all { preferenceObject[entry.jsonKey] = entry.export(preferences) }

        let root: [String: Any] = [
            "version": Self.version,
            "preferences": preferenceObject,
            "custom_feeds": content.feeds.map { ["title": $0.title, "url": $0.url, "category": $0.category] },
            "articles": content.articles.filter { !LinkOnlySources.isLinkOnly($0) }.map(BackupArticle.json),
            "bookmark_lists": content.lists.map { ["id": NSNumber(value: $0.id ?? 0), "name": $0.name, "isDefault": $0.isDefault, "createdAt": NSNumber(value: $0.createdAt)] as [String: Any] },
            "article_bookmark_cross_refs": content.refs
                .filter { !LinkOnlySources.isLinkOnly(url: $0.articleLink) }
                .map { ["articleLink": $0.articleLink, "listId": NSNumber(value: $0.listId)] as [String: Any] },
        ]
        return try JSONSerialization.data(withJSONObject: root, options: [.prettyPrinted, .sortedKeys, .withoutEscapingSlashes])
    }

    // MARK: Restore

    /// Replaces custom feeds, adds the backup's articles, lists and links, and applies its preferences.
    /// The file is parsed completely before anything is changed, so a bad file changes nothing.
    @discardableResult
    public func restore(_ data: Data) async throws -> RestoreSummary {
        guard data.count <= Self.maxBytes else { throw BackupError.tooLarge }
        guard let root = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any] else { throw BackupError.notABackup }
        let version = (root["version"] as? NSNumber)?.intValue ?? 1
        guard version >= 1 else { throw BackupError.unsupportedVersion(version) }

        let feeds = (root["custom_feeds"] as? [[String: Any]] ?? []).compactMap { object -> CustomFeed? in
            guard let url = object["url"] as? String, !url.isEmpty,
                  let safe = UrlSecurityValidator.sanitizeUrl(url, requireHttps: true) else { return nil }
            return CustomFeed(
                title: String((object["title"] as? String ?? "Custom Feed").prefix(CustomFeedStore.maxTitleLength)),
                url: safe,
                category: String((object["category"] as? String ?? "General").prefix(CustomFeedStore.maxCategoryLength))
            )
        }
        let articles = (root["articles"] as? [[String: Any]] ?? []).compactMap(BackupArticle.make)
        let lists = (root["bookmark_lists"] as? [[String: Any]] ?? []).compactMap { object -> BookmarkList? in
            guard let id = (object["id"] as? NSNumber)?.int64Value else { return nil }
            return BookmarkList(
                id: id,
                name: String((object["name"] as? String ?? "List").prefix(100)),
                isDefault: (object["isDefault"] as? Bool) ?? false,
                createdAt: (object["createdAt"] as? NSNumber)?.int64Value ?? Int64(Date().timeIntervalSince1970 * 1000)
            )
        }
        let refs = (root["article_bookmark_cross_refs"] as? [[String: Any]] ?? []).compactMap { object -> ArticleBookmarkListCrossRef? in
            guard let link = object["articleLink"] as? String,
                  let safe = UrlSecurityValidator.sanitizeUrl(link),
                  !LinkOnlySources.isLinkOnly(url: safe),
                  let listId = (object["listId"] as? NSNumber)?.int64Value else { return nil }
            return ArticleBookmarkListCrossRef(articleLink: safe, listId: listId)
        }

        try await writer.write { db in
            try db.execute(sql: "DELETE FROM custom_feeds")
            for var feed in feeds { try feed.insert(db) }
            for article in articles { try article.insert(db, onConflict: .replace) }
            if !lists.isEmpty {
                try db.execute(sql: "DELETE FROM bookmark_lists WHERE isDefault = 0")
                for var list in lists { try list.insert(db, onConflict: .replace) }
            }
            if !refs.isEmpty {
                try db.execute(sql: "DELETE FROM article_bookmark_lists")
                let existingLists = Set(try Int64.fetchAll(db, sql: "SELECT id FROM bookmark_lists"))
                // A link to a list that does not exist would hide the article from every collection.
                for ref in refs where existingLists.contains(ref.listId) { try ref.insert(db, onConflict: .replace) }
            }
        }
        try AppDatabase.ensureDefaultBookmarkList(writer)

        if let object = root["preferences"] as? [String: Any] {
            for entry in BackupPreferences.all {
                if let raw = object[entry.jsonKey] { entry.apply(preferences, raw) }
            }
        }
        return RestoreSummary(feeds: feeds.count, articles: articles.count, lists: lists.count)
    }
}

/// An article as it appears in a backup file.
private enum BackupArticle {
    static func json(_ article: Article) -> [String: Any] {
        func value(_ string: String?) -> Any { string ?? NSNull() }
        return [
            "link": article.link, "title": article.title, "description": article.description,
            "pubDate": article.pubDate, "sourceName": article.sourceName, "sourceUrl": article.sourceUrl,
            "category": article.category, "thumbnailUrl": value(article.thumbnailUrl),
            "isBookmarked": article.isBookmarked, "customFeedId": article.customFeedId.map { $0 as Any } ?? NSNull(),
            "fullText": value(article.fullText), "tags": value(article.tags), "aiSummary": value(article.aiSummary),
            "translatedTitle": value(article.translatedTitle), "translatedBody": value(article.translatedBody),
            "translatedLanguage": value(article.translatedLanguage), "detectedLanguage": value(article.detectedLanguage),
            "isRead": article.isRead,
        ]
    }

    /// nil for a link that is missing, unsafe, or from a link-only publisher.
    static func make(_ object: [String: Any]) -> Article? {
        guard let link = object["link"] as? String, !link.isEmpty,
              let safe = UrlSecurityValidator.sanitizeUrl(link),
              !LinkOnlySources.isLinkOnly(url: safe) else { return nil }

        func text(_ key: String, _ fallback: String, _ limit: Int) -> String { String(((object[key] as? String) ?? fallback).prefix(limit)) }
        func optional(_ key: String, _ limit: Int) -> String? { (object[key] as? String).map { String($0.prefix(limit)) } }

        return Article(
            link: safe,
            title: text("title", "Untitled", 500),
            description: text("description", "", 2000),
            pubDate: text("pubDate", "", 100),
            sourceName: text("sourceName", "Unknown", 200),
            sourceUrl: text("sourceUrl", "", 500),
            category: text("category", "General", 100),
            thumbnailUrl: optional("thumbnailUrl", 500).flatMap { UrlSecurityValidator.normalizeToHttps($0) },
            isBookmarked: (object["isBookmarked"] as? Bool) ?? true,
            customFeedId: (object["customFeedId"] as? NSNumber)?.intValue,
            fullText: optional("fullText", 50_000),
            tags: optional("tags", 500),
            aiSummary: optional("aiSummary", 10_000),
            translatedTitle: optional("translatedTitle", 500),
            translatedBody: optional("translatedBody", 50_000),
            translatedLanguage: optional("translatedLanguage", 50),
            detectedLanguage: optional("detectedLanguage", 50),
            isRead: (object["isRead"] as? Bool) ?? false
        )
    }
}
