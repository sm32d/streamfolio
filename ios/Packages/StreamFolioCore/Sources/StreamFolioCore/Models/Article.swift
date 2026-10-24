import Foundation
import GRDB

/// A cached news article. Mirrors the Android `Article` Room entity (schema v9) column for column,
/// so the same SQLite shape — and the same backup JSON — works on both platforms.
public struct Article: Codable, Hashable, Sendable, Identifiable, FetchableRecord, PersistableRecord {
    public static let databaseTableName = "articles"

    /// Canonical article URL (primary key).
    public var link: String
    public var title: String
    public var description: String
    /// ISO-8601 UTC, `yyyy-MM-dd'T'HH:mm:ss.SSS'Z'` (sorts lexicographically).
    public var pubDate: String
    public var sourceName: String
    public var sourceUrl: String
    public var category: String
    public var thumbnailUrl: String?
    public var isBookmarked: Bool
    public var customFeedId: Int?
    public var fullText: String?
    public var tags: String?
    public var aiSummary: String?
    public var translatedTitle: String?
    public var translatedBody: String?
    public var translatedLanguage: String?
    public var detectedLanguage: String?
    public var isRead: Bool
    public var groupId: String?

    public var id: String { link }

    public init(
        link: String,
        title: String,
        description: String,
        pubDate: String,
        sourceName: String,
        sourceUrl: String,
        category: String,
        thumbnailUrl: String? = nil,
        isBookmarked: Bool = false,
        customFeedId: Int? = nil,
        fullText: String? = nil,
        tags: String? = nil,
        aiSummary: String? = nil,
        translatedTitle: String? = nil,
        translatedBody: String? = nil,
        translatedLanguage: String? = nil,
        detectedLanguage: String? = nil,
        isRead: Bool = false,
        groupId: String? = nil
    ) {
        self.link = link
        self.title = title
        self.description = description
        self.pubDate = pubDate
        self.sourceName = sourceName
        self.sourceUrl = sourceUrl
        self.category = category
        self.thumbnailUrl = thumbnailUrl
        self.isBookmarked = isBookmarked
        self.customFeedId = customFeedId
        self.fullText = fullText
        self.tags = tags
        self.aiSummary = aiSummary
        self.translatedTitle = translatedTitle
        self.translatedBody = translatedBody
        self.translatedLanguage = translatedLanguage
        self.detectedLanguage = detectedLanguage
        self.isRead = isRead
        self.groupId = groupId
    }

    /// Parsed publication date; nil when the feed gave none or an unrecognised format.
    public var publishedAt: Date? { ArticleDate.parse(pubDate) }
}

/// The storage format for `Article.pubDate`.
public enum ArticleDate {
    // ISO8601DateFormatter is thread-safe once configured.
    nonisolated(unsafe) private static let formatter: ISO8601DateFormatter = {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return formatter
    }()

    public static func string(from date: Date) -> String { formatter.string(from: date) }
    public static func parse(_ string: String) -> Date? { formatter.date(from: string) }
}

/// A primary story with the other publishers covering it ("Also covered by").
public struct ArticleGroup: Sendable, Hashable, Identifiable {
    public let primary: Article
    public let secondary: [Article]
    public var id: String { primary.link }

    public init(primary: Article, secondary: [Article] = []) {
        self.primary = primary
        self.secondary = secondary
    }
}
