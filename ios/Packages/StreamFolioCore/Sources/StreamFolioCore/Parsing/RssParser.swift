import Foundation
import SwiftSoup

/// Parses RSS 2.0 (`<channel>/<item>`) and Atom (`<feed>/<entry>`) into `Article`s, and reads OPML.
/// Port of Android's `RssParser`, including its field precedence ("last tag wins") and the
/// link-only rules: for a link-only publisher only title, link, date and a short plain-text
/// description are read; bodies and every image field are ignored.
///
/// Security: `XMLParser` does not resolve external entities (`shouldResolveExternalEntities` stays
/// false), so DOCTYPE/XXE payloads are inert. Titles and descriptions are reduced to plain text
/// (all markup removed) before they reach the database.
public struct RssParser: Sendable {

    public init() {}

    // MARK: Feed parsing

    public func parse(
        _ data: Data,
        category: String,
        customFeedId: Int? = nil,
        feedUrl: String? = nil,
        customFeedTitle: String? = nil
    ) -> [Article] {
        let delegate = FeedDelegate(
            category: category,
            customFeedId: customFeedId,
            feedUrl: feedUrl,
            customFeedTitle: customFeedTitle,
            linkOnly: LinkOnlySources.isLinkOnly(url: feedUrl)
        )
        let parser = XMLParser(data: data)
        parser.shouldProcessNamespaces = false
        parser.shouldResolveExternalEntities = false
        parser.delegate = delegate
        parser.parse()   // a malformed tail keeps the articles parsed so far, like Android
        return delegate.articles
    }

    public func parse(
        _ xml: String,
        category: String,
        customFeedId: Int? = nil,
        feedUrl: String? = nil,
        customFeedTitle: String? = nil
    ) -> [Article] {
        parse(Data(xml.utf8), category: category, customFeedId: customFeedId, feedUrl: feedUrl, customFeedTitle: customFeedTitle)
    }

    // MARK: OPML

    /// `true` if the document is an OPML list rather than a feed.
    public func isOpml(_ data: Data) -> Bool {
        let prefix = String(decoding: data.prefix(500), as: UTF8.self).lowercased()
        let trimmed = prefix.drop(while: \.isWhitespace)
        return trimmed.contains("<opml") && !trimmed.contains("<rss") && !trimmed.contains("<feed")
    }

    /// `xmlUrl` of every `<outline>` (nested feeds included).
    public func parseOpmlUrls(_ data: Data) -> [String] {
        let delegate = OpmlDelegate()
        let parser = XMLParser(data: data)
        parser.shouldResolveExternalEntities = false
        parser.delegate = delegate
        parser.parse()
        return delegate.urls
    }
}

// MARK: - Plain text

enum PlainText {
    /// Strips all markup and decodes entities. Equivalent in purpose to Android's
    /// `Jsoup.clean(input, Safelist.none())`, but returns plain text (no `&amp;` re-escaping).
    static func from(_ html: String?) -> String {
        guard let html, !html.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return "" }
        if !html.contains("<") && !html.contains("&") { return html.trimmingCharacters(in: .whitespacesAndNewlines) }
        let text = (try? SwiftSoup.parse(html).text()) ?? html
        return text.trimmingCharacters(in: .whitespacesAndNewlines)
    }
}

// MARK: - Delegates

private final class OpmlDelegate: NSObject, XMLParserDelegate {
    var urls: [String] = []

    func parser(_ parser: XMLParser, didStartElement elementName: String, namespaceURI: String?, qualifiedName qName: String?, attributes attributeDict: [String: String] = [:]) {
        guard elementName.lowercased() == "outline",
              let url = attributeDict["xmlUrl"], !url.trimmingCharacters(in: .whitespaces).isEmpty else { return }
        urls.append(url)
    }
}

private final class FeedDelegate: NSObject, XMLParserDelegate {
    private(set) var articles: [Article] = []

    private let category: String
    private let customFeedId: Int?
    private let feedUrl: String?
    private let customFeedTitle: String?
    private let linkOnly: Bool

    private var channelTitle = CuratedFeeds.unknownSource
    private var builder: ArticleBuilder?
    private var buffer = ""
    private var linkFromHref = false
    private var contentHadUrl = false

    /// Elements ignored entirely for link-only feeds.
    private static let linkOnlyIgnored: Set<String> = [
        "content", "content:encoded", "enclosure", "media:content", "media:thumbnail", "itunes:image",
    ]

    init(category: String, customFeedId: Int?, feedUrl: String?, customFeedTitle: String?, linkOnly: Bool) {
        self.category = category
        self.customFeedId = customFeedId
        self.feedUrl = feedUrl
        self.customFeedTitle = customFeedTitle
        self.linkOnly = linkOnly
    }

    func parser(_ parser: XMLParser, didStartElement elementName: String, namespaceURI: String?, qualifiedName qName: String?, attributes attrs: [String: String] = [:]) {
        buffer = ""
        let name = elementName.lowercased()
        if name == "item" || name == "entry" {
            builder = ArticleBuilder(sourceUrl: feedUrl, linkOnly: linkOnly)
            return
        }
        guard builder != nil else { return }

        switch name {
        case "link":
            linkFromHref = false
            if let href = attrs["href"]?.trimmed, !href.isEmpty {
                builder?.link = href
                linkFromHref = true
            }
        case _ where linkOnly && Self.linkOnlyIgnored.contains(name):
            break
        case "content", "content:encoded":
            contentHadUrl = attrs["url"] != nil
            if let url = attrs["url"] { builder?.thumbnailUrl = url.trimmed }
        case "source":
            if !linkOnly, let url = attrs["url"]?.trimmed, !url.isEmpty { builder?.sourceUrl = url }
        case "enclosure":
            let type = attrs["type"] ?? ""
            if let url = attrs["url"]?.trimmed, !url.isEmpty,
               type.hasPrefix("image/") || type.contains("image") || url.looksLikeImagePath {
                builder?.thumbnailUrl = url
            }
        case "media:content":
            let type = attrs["type"] ?? ""
            if let url = attrs["url"], type.isEmpty || type.hasPrefix("image/") || type.contains("image") {
                builder?.thumbnailUrl = url.trimmed
            }
        case "media:thumbnail":
            builder?.thumbnailUrl = attrs["url"]?.trimmed
        case "itunes:image":
            builder?.thumbnailUrl = attrs["href"]?.trimmed
        default:
            break
        }
    }

    func parser(_ parser: XMLParser, foundCharacters string: String) { buffer += string }

    func parser(_ parser: XMLParser, foundCDATA CDATABlock: Data) {
        buffer += String(decoding: CDATABlock, as: UTF8.self)
    }

    func parser(_ parser: XMLParser, didEndElement elementName: String, namespaceURI: String?, qualifiedName qName: String?) {
        let name = elementName.lowercased()
        let text = buffer.trimmed

        if name == "item" || name == "entry" {
            if let article = builder?.build(category: category, customFeedId: customFeedId, channelFallback: channelTitle, customFeedTitle: customFeedTitle) {
                articles.append(article)
            }
            builder = nil
            return
        }

        guard builder != nil else {
            if name == "title" { channelTitle = text.isEmpty ? CuratedFeeds.unknownSource : text }
            return
        }

        switch name {
        case "title": builder?.title = text
        case "link": if !linkFromHref { builder?.link = text }
        case "description", "summary": builder?.description = text
        case "content", "content:encoded":
            if !linkOnly && !contentHadUrl { builder?.description = text }
        case "pubdate", "published", "updated": builder?.pubDate = text
        case "source": builder?.sourceName = text
        default: break
        }
    }
}

// MARK: - Article assembly

private struct ArticleBuilder {
    var title: String?
    var link: String?
    var description: String?
    var pubDate: String?
    var sourceName: String?
    var sourceUrl: String?
    var thumbnailUrl: String?
    let linkOnly: Bool

    init(sourceUrl: String?, linkOnly: Bool) {
        self.sourceUrl = sourceUrl
        self.linkOnly = linkOnly
    }

    func build(category: String, customFeedId: Int?, channelFallback: String, customFeedTitle: String?) -> Article? {
        guard let articleLink = link, !articleLink.isEmpty else { return nil }
        let rawTitle = title ?? "No Title"

        var cleanTitle = rawTitle
        var cleanSource = customFeedTitle ?? sourceName ?? channelFallback
        if cleanSource.isEmpty { cleanSource = channelFallback }

        var thumbnail = thumbnailUrl
        if linkOnly {
            thumbnail = nil
        } else if thumbnail == nil || thumbnail!.isEmpty {
            thumbnail = Self.firstImageUrl(inHTML: description ?? "", articleLink: articleLink)
        }

        // Prefer a canonical publisher name resolved from the feed URL, then the article link.
        let curated = CuratedFeeds.shared
        var resolvedName = CuratedFeeds.unknownSource
        if let sourceUrl, !sourceUrl.isEmpty {
            let byFeed = curated.publisherName(for: sourceUrl)
            if byFeed != CuratedFeeds.unknownSource { resolvedName = byFeed }
        }
        if resolvedName == CuratedFeeds.unknownSource {
            let byArticle = curated.publisherName(for: articleLink)
            if byArticle != CuratedFeeds.unknownSource { resolvedName = byArticle }
        }

        if resolvedName != CuratedFeeds.unknownSource && customFeedTitle == nil {
            cleanSource = resolvedName
        } else if customFeedTitle == nil {
            cleanSource = Self.cleanPublisherName(cleanSource)
        }

        // "Headline - Publisher" → drop the trailing publisher when it looks like one.
        if let dash = rawTitle.range(of: " - ", options: .backwards),
           rawTitle.distance(from: dash.lowerBound, to: rawTitle.endIndex) < 40 + 3 {
            let suffix = rawTitle[dash.upperBound...].trimmed
            if let first = suffix.first, first.isUppercase {
                cleanTitle = rawTitle[..<dash.lowerBound].trimmed
                if customFeedTitle != nil || resolvedName != CuratedFeeds.unknownSource {
                    // keep the canonical or custom source name
                } else if sourceName == nil || sourceName!.isEmpty {
                    cleanSource = suffix
                }
            }
        }

        var cleanDescription = PlainText.from(description)
        if linkOnly && cleanDescription.count > LinkOnlySources.maxDescriptionChars {
            cleanDescription = String(cleanDescription.prefix(LinkOnlySources.maxDescriptionChars))
        }

        return Article(
            link: articleLink,
            title: PlainText.from(cleanTitle),
            description: cleanDescription,
            pubDate: Self.normalizePubDate(pubDate),
            sourceName: cleanSource,
            sourceUrl: sourceUrl ?? "",
            category: category,
            thumbnailUrl: thumbnail,
            customFeedId: customFeedId
        )
    }

    private static func cleanPublisherName(_ raw: String) -> String {
        let name = raw.trimmed
        for separator in [" - ", " | ", " : "] where name.contains(separator) {
            let parts = name.components(separatedBy: separator)
            let first = parts[0].trimmed
            let last = parts[parts.count - 1].trimmed
            let keywords = ["times", "news", "post", "daily", "herald", "today", "guardian", "journal"]
            let firstLower = first.lowercased()
            let lastLower = last.lowercased()
            if keywords.contains(where: firstLower.contains) { return first }
            if keywords.contains(where: lastLower.contains) { return last }
            if (3...25).contains(first.count) { return first }
            if (3...25).contains(last.count) { return last }
            return first
        }
        return name
    }

    /// First `<img src>` in the description, upgraded to https and validated.
    private static func firstImageUrl(inHTML html: String, articleLink: String) -> String? {
        guard let match = html.firstMatch(of: /(?i)<img[^>]+src=["']([^"']+)["']/) else { return nil }
        let src = String(match.output.1).trimmed
        if src.isEmpty { return nil }
        if src.hasPrefix("http://") || src.hasPrefix("https://") { return UrlSecurityValidator.normalizeToHttps(src) }
        if src.hasPrefix("//") { return UrlSecurityValidator.normalizeToHttps("https:\(src)") }
        if src.hasPrefix("/"), let host = URL(string: articleLink)?.host {
            return UrlSecurityValidator.normalizeToHttps("https://\(host)\(src)")
        }
        return nil
    }

    // MARK: Dates

    private static let inputFormats = [
        "EEE, dd MMM yyyy HH:mm:ss z",
        "EEE, dd MMM yyyy HH:mm:ss Z",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd HH:mm:ss",
        "EEE, dd MMM yyyy HH:mm:ss",
        "EEE, d MMM yyyy HH:mm:ss z",
        "EEE, d MMM yyyy HH:mm:ss Z",
    ]

    /// Normalizes to `yyyy-MM-dd'T'HH:mm:ss.SSS'Z'` UTC; returns the raw string if no format matches
    /// (same as Android) and "" when there is no date.
    static func normalizePubDate(_ raw: String?) -> String {
        guard let raw = raw?.trimmed, !raw.isEmpty else { return "" }
        for format in inputFormats {
            let formatter = DateFormatter()
            formatter.locale = Locale(identifier: "en_US_POSIX")
            formatter.timeZone = TimeZone(identifier: "UTC")   // used only when the string has no zone
            formatter.dateFormat = format
            if let date = formatter.date(from: raw) { return ArticleDate.string(from: date) }
        }
        if let date = ISO8601DateFormatter().date(from: raw) { return ArticleDate.string(from: date) }
        return raw
    }
}

extension StringProtocol {
    var trimmed: String { trimmingCharacters(in: .whitespacesAndNewlines) }
}

private extension String {
    var looksLikeImagePath: Bool {
        firstMatch(of: /(?i).+\.(png|jpe?g|webp|gif)(\?.*)?$/) != nil
    }
}
