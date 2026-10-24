import Foundation

/// Builds and parses StreamFolio share links (port of `ShareLinks`).
///
/// A shared article looks like `https://streamfolio.sume.uk/open?url=<encoded article url>`.
/// Devices with the app installed open it in StreamFolio; everyone else lands on
/// `landing/open.html`, which offers the download and "Open in browser".
public enum ShareLinks {
    public static let host = "streamfolio.sume.uk"
    public static let openPath = "/open"

    public static func shareURL(for articleLink: String) -> URL? {
        let articleUrl = UrlSecurityValidator.normalizeToHttps(articleLink) ?? articleLink
        var components = URLComponents()
        components.scheme = "https"
        components.host = host
        components.path = openPath
        components.queryItems = [URLQueryItem(name: "url", value: articleUrl)]
        return components.url
    }

    /// Message body for the share sheet. Android appends a Play Store line; the App Store URL for
    /// iOS is `Unknown / Requires Confirmation`, so the iOS text ends with the link, which lands on
    /// the page that offers the right download for the visitor's device.
    public static func shareText(title: String, articleLink: String) -> String {
        let link = shareURL(for: articleLink)?.absoluteString ?? articleLink
        return "\(title)\n\nRead more: \(link)"
    }

    /// The safe https article URL carried by a share link, or nil if `url` isn't one.
    public static func parseArticleUrl(_ url: URL?) -> String? {
        guard let url, url.scheme == "https", url.host?.lowercased() == host, url.path.hasPrefix(openPath),
              let target = URLComponents(url: url, resolvingAgainstBaseURL: false)?.queryItems?.first(where: { $0.name == "url" })?.value
        else { return nil }
        return UrlSecurityValidator.normalizeToHttps(target)
    }
}
