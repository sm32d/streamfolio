import Foundation

/// One subscription read from an OPML file: the folder it sat in becomes the Home tab label.
public struct OpmlFeed: Sendable, Hashable {
    public var title: String
    public var xmlUrl: String
    public var category: String

    public init(title: String, xmlUrl: String, category: String) {
        self.title = title
        self.xmlUrl = xmlUrl
        self.category = category
    }
}

/// Reads and writes OPML subscription lists. Port of Android's `OpmlHelper`; the parser keeps its
/// folder rules: an `<outline>` with an `xmlUrl` is a feed filed under the nearest enclosing folder
/// (or "Imported"), one without is a folder.
public enum Opml {
    public static let defaultCategory = "Imported"

    /// Largest file the app will read: a subscription list is text, so anything bigger is not one.
    public static let maxBytes = 5 * 1024 * 1024

    public static func parse(_ data: Data) -> [OpmlFeed] {
        let delegate = Delegate()
        let parser = XMLParser(data: data)
        parser.shouldResolveExternalEntities = false   // DOCTYPE/XXE payloads stay inert
        parser.delegate = delegate
        parser.parse()   // a malformed tail keeps the feeds read so far
        return delegate.feeds
    }

    /// Feeds that are safe to subscribe to and not already subscribed — what the import sheet lists.
    public static func importable(_ parsed: [OpmlFeed], existing: [CustomFeed]) -> [OpmlFeed] {
        let known = Set(existing.map { $0.url.trimmed.lowercased() })
        return parsed.compactMap { feed in
            guard let safe = UrlSecurityValidator.sanitizeUrl(feed.xmlUrl, requireHttps: true),
                  !known.contains(safe.lowercased())
            else { return nil }
            var copy = feed
            copy.xmlUrl = safe
            return copy
        }
    }

    public static func export(_ feeds: [CustomFeed]) -> String {
        var categories: [String] = []
        var byCategory: [String: [CustomFeed]] = [:]
        for feed in feeds {
            if byCategory[feed.category] == nil { categories.append(feed.category) }
            byCategory[feed.category, default: []].append(feed)
        }

        var xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<opml version=\"2.0\">\n  <head>\n    <title>StreamFolio Subscriptions</title>\n  </head>\n  <body>\n"
        for category in categories {
            let folder = escape(category)
            xml += "    <outline text=\"\(folder)\" title=\"\(folder)\">\n"
            for feed in byCategory[category] ?? [] {
                let title = escape(feed.title)
                xml += "      <outline type=\"rss\" text=\"\(title)\" title=\"\(title)\" xmlUrl=\"\(escape(feed.url))\"/>\n"
            }
            xml += "    </outline>\n"
        }
        xml += "  </body>\n</opml>\n"
        return xml
    }

    private static func escape(_ text: String) -> String {
        text.replacingOccurrences(of: "&", with: "&amp;")
            .replacingOccurrences(of: "<", with: "&lt;")
            .replacingOccurrences(of: ">", with: "&gt;")
            .replacingOccurrences(of: "\"", with: "&quot;")
            .replacingOccurrences(of: "'", with: "&apos;")
    }

    private final class Delegate: NSObject, XMLParserDelegate {
        var feeds: [OpmlFeed] = []
        private var folders: [String] = []
        private var isFolder: [Bool] = []

        func parser(_ parser: XMLParser, didStartElement elementName: String, namespaceURI: String?, qualifiedName qName: String?, attributes attributeDict: [String: String] = [:]) {
            guard elementName.lowercased() == "outline" else { return }
            let xmlUrl = attributeDict["xmlUrl"] ?? attributeDict["xmlurl"]
            let text = (attributeDict["text"] ?? attributeDict["title"] ?? "Untitled Feed").trimmed
            if let xmlUrl {
                isFolder.append(false)
                feeds.append(OpmlFeed(title: text, xmlUrl: xmlUrl.trimmed, category: (folders.last ?? Opml.defaultCategory).trimmed))
            } else {
                isFolder.append(true)
                folders.append(text)
            }
        }

        func parser(_ parser: XMLParser, didEndElement elementName: String, namespaceURI: String?, qualifiedName qName: String?) {
            guard elementName.lowercased() == "outline", let wasFolder = isFolder.popLast() else { return }
            if wasFolder { _ = folders.popLast() }
        }
    }
}
