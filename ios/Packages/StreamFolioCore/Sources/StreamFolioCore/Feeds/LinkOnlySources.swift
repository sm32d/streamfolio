import Foundation

/// A publisher whose feed may only be used as a headline-and-link source.
///
/// Agreed scope (publisher: Joho no Todai), identical on every platform: only publication name,
/// headline, publication date, direct article link and the feed's short text `description`, cached on
/// the reader's device. No images, no article bodies, no `content:encoded`, no translation,
/// summaries, TTS, offline body caching, redistribution or training. See `docs/14` (ADR-006).
public struct LinkOnlySource: Sendable, Hashable {
    public let id: String
    public let displayName: String
    /// Host names (and their subdomains) whose feeds and article links are covered.
    public let hosts: [String]
}

public enum LinkOnlySources {
    public static let johoTodaiEnID = "joho-todai-en"
    public static let johoTodaiEnFeed = "https://joho-todai.com/en/rss/"

    /// Longest `description` kept for a link-only article (a short text description only).
    public static let maxDescriptionChars = 500

    public static let all: [LinkOnlySource] = [
        LinkOnlySource(id: johoTodaiEnID, displayName: "Joho no Todai", hosts: ["joho-todai.com"]),
    ]

    public static func source(id: String) -> LinkOnlySource? {
        all.first { $0.id == id }
    }

    /// The link-only source that owns `url` (a feed URL or an article link), if any.
    public static func source(for url: String?) -> LinkOnlySource? {
        guard let url = url?.trimmingCharacters(in: .whitespacesAndNewlines), !url.isEmpty,
              let host = URL(string: url)?.host?.lowercased() else { return nil }
        return all.first { source in
            source.hosts.contains { host == $0 || host.hasSuffix(".\($0)") }
        }
    }

    public static func isLinkOnly(url: String?) -> Bool { source(for: url) != nil }

    public static func isLinkOnly(_ article: Article) -> Bool {
        source(for: article.link) != nil || source(for: article.sourceUrl) != nil
    }

    /// SQL `LIKE` patterns matching every stored link / feed URL of `source`.
    public static func likePatterns(for source: LinkOnlySource) -> [String] {
        source.hosts.map { "%\($0)%" }
    }
}

/// What the app may do with an article. The single place that interprets link-only status, so no
/// screen or repository path re-checks hosts. Android scatters these checks; iOS must not.
public struct ArticleCapabilities: Sendable, Hashable {
    public let showsThumbnail: Bool
    public let showsFavicon: Bool
    public let canShowReader: Bool
    public let canPrefetchBody: Bool
    public let canListen: Bool
    public let canSummarize: Bool
    public let canTranslate: Bool

    public init(for article: Article) {
        self.init(isLinkOnly: LinkOnlySources.isLinkOnly(article))
    }

    public init(isLinkOnly: Bool) {
        let full = !isLinkOnly
        showsThumbnail = full
        showsFavicon = full
        canShowReader = full
        canPrefetchBody = full
        canListen = full
        canSummarize = full
        canTranslate = full
    }
}
