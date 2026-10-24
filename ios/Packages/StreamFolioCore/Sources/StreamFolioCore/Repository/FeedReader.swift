import Foundation

/// Downloads one feed and turns it into articles. Used for the user's own feeds (refresh) and for
/// online search, so both treat URLs, OPML "feeds" and link-only publishers the same way.
///
/// - Only safe HTTPS URLs are fetched (`UrlSecurityValidator`).
/// - An OPML document in place of a feed is expanded into the feeds it lists, each validated again.
/// - With `skipLinkOnly`, link-only publishers are not fetched at all (search must not bypass their
///   per-device fetch limit).
struct FeedReader: Sendable {
    let downloader: FeedDownloader
    let parser: RssParser

    /// The articles in the feed, or nil if it could not be fetched.
    func articles(
        from urlString: String,
        category: String,
        customFeed: CustomFeed? = nil,
        skipLinkOnly: Bool = false
    ) async -> [Article]? {
        guard let url = UrlSecurityValidator.sanitizeUrl(urlString, requireHttps: true),
              !(skipLinkOnly && LinkOnlySources.isLinkOnly(url: url)),
              let data = await downloader.feedData(for: url)
        else { return nil }

        guard parser.isOpml(data) else {
            return parser.parse(data, category: category, customFeedId: customFeed?.id, feedUrl: url, customFeedTitle: customFeed?.title)
        }
        var aggregated: [Article] = []
        for child in parser.parseOpmlUrls(data) {
            guard let childUrl = UrlSecurityValidator.sanitizeUrl(child, requireHttps: true),
                  !(skipLinkOnly && LinkOnlySources.isLinkOnly(url: childUrl)),
                  let childData = await downloader.feedData(for: childUrl)
            else { continue }
            aggregated += parser.parse(childData, category: category, customFeedId: customFeed?.id, feedUrl: childUrl, customFeedTitle: customFeed?.title)
        }
        return aggregated
    }
}
