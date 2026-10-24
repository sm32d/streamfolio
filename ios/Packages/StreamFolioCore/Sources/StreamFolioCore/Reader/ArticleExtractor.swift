import Foundation
import SwiftSoup

/// Pulls readable text and the lead image out of an article page. Port of the scraping half of
/// Android's `NewsRepository` (`fetchArticleBody`, `resolveThumbnail`, `isGibberishOrPromo`,
/// `extractImageFromStructuredData`). Pure functions over an HTML string, so it is fully testable.
public enum ArticleExtractor {

    /// Paragraphs inside the most specific article container, falling back to every `<p>` on the page.
    private static let articleParagraphs = "article p, main p, .post-content p, .article-content p, .story-body p"

    /// Clean paragraphs joined by a blank line, or nil if nothing readable was found.
    public static func body(fromHTML html: String, baseURL: String) -> String? {
        guard let doc = try? SwiftSoup.parse(html, baseURL) else { return nil }
        let scoped = (try? doc.select(articleParagraphs).array()) ?? []
        let elements = scoped.isEmpty ? ((try? doc.select("p").array()) ?? []) : scoped

        let paragraphs = elements
            .compactMap { (try? $0.text())?.trimmed }
            .filter { !$0.isEmpty && !isPromo($0) }
            .filter { $0.count > 30 }   // share-button labels, captions and other UI text
        let text = paragraphs.joined(separator: "\n\n")
        return text.isEmpty ? nil : text
    }

    /// The article's lead image (https, validated), or nil.
    public static func thumbnail(fromHTML html: String, baseURL: String) -> String? {
        guard let doc = try? SwiftSoup.parse(html, baseURL) else { return nil }

        func meta(_ selector: String) -> String {
            ((try? doc.select(selector).first()?.attr("content")) ?? nil)?.trimmed ?? ""
        }
        var image = meta("meta[property=og:image]")
        if image.isEmpty { image = meta("meta[name=twitter:image]") }
        if image.isEmpty { image = structuredDataImage(in: html) }
        if image.isEmpty, let first = try? doc.select("article img, main img").first() {
            image = (try? first.absUrl("src")) ?? ""
        }

        // Aggregator placeholders and logos are not article photos.
        if ["googleusercontent.com", "gstatic.com", "google.com"].contains(where: image.contains) { return nil }
        guard !image.isEmpty else { return nil }

        let absolute = URL(string: image, relativeTo: URL(string: baseURL))?.absoluteURL.absoluteString ?? image
        return UrlSecurityValidator.normalizeToHttps(absolute)
    }

    /// `"image": { ... "url": "…" }` blocks (JSON-LD, escaped or not) and one publisher CDN pattern.
    static func structuredDataImage(in html: String) -> String {
        let patterns: [Regex<AnyRegexOutput>] = [
            try! Regex(#""image"\s*:\s*\{[\s\S]*?"url"\s*:\s*"([^"]+)""#),
            try! Regex(#"\\"image\\"\s*:\s*\{[\s\S]*?\\"url\\"\s*:\s*\\"([^\\]+)\\""#),
            try! Regex(#"https://cassette\.sphdigital\.com\.sg/image/[^"\\\s<]+"#),
        ]
        for pattern in patterns {
            guard let match = html.firstMatch(of: pattern) else { continue }
            let raw = match.output.count > 1 ? (match.output[1].substring.map(String.init) ?? "") : String(html[match.range])
            let candidate = (raw.isEmpty ? String(html[match.range]) : raw)
                .replacingOccurrences(of: "\\/", with: "/")
                .trimmed
            let cleaned = candidate.hasSuffix("\\") ? String(candidate.dropLast()) : candidate
            if let safe = UrlSecurityValidator.normalizeToHttps(cleaned) { return safe }
        }
        return ""
    }

    private static let promoKeywords = [
        "subscribe", "subscription", "sign up", "sign-up", "register for free",
        "read more", "related story:", "related article:", "download the app",
        "follow us", "join our channel", "telegram", "whatsapp", "newsletter",
        "copyright", "all rights reserved", "sph media", "terms of use", "privacy policy",
        "advertisement", "ad blocker", "cookies help us", "enable javascript",
        "you do not have an active", "please log in", "already a subscriber",
        "read also:", "photo:", "image:", "source:", "follow us on", "read the full",
        "get a subscription", "this story is for",
    ]

    /// Paywall prompts, share/newsletter calls to action, legal footers and captions.
    static func isPromo(_ line: String) -> Bool {
        let lower = line.lowercased()
        if promoKeywords.contains(where: lower.contains) { return true }
        if line.count < 25 { return true }
        // iOS additions to Android's list: "PHOTOS: …" / "VIDEO: …" credits and "Published Oct 24, 2026 …"
        // datelines, which Android's keyword list misses.
        if lower.firstMatch(of: /^(photos?|videos?|images?|graphics?|credits?|source)\s*[:\/]/) != nil { return true }
        if line.count < 70, lower.firstMatch(of: /^(published|updated|posted)\b.*\d{4}/) != nil { return true }
        return false
    }
}
