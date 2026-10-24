import Foundation

/// How a search query matches an article. Port of the filters in `NewsRepository.searchNewsOnline` and
/// `NewsViewModel.searchResults`.
public enum SearchMatching {
    /// Quotes are ignored and surrounding whitespace trimmed.
    public static func clean(_ query: String) -> String {
        query.replacingOccurrences(of: "\"", with: "").trimmed
    }

    /// Short queries (3 characters or fewer, e.g. "AI") must match whole words so "AI" does not find
    /// "said" or "Hainan"; longer queries match anywhere, case-insensitively. Online search also
    /// matches the publisher name (`includingSource`); the local database search does not.
    public static func matches(_ article: Article, query: String, includingSource: Bool) -> Bool {
        let cleaned = clean(query)
        guard !cleaned.isEmpty else { return false }
        var fields = [article.title, article.description]
        if includingSource { fields.append(article.sourceName) }

        if cleaned.count <= 3 {
            let escaped = NSRegularExpression.escapedPattern(for: cleaned)
            guard let regex = try? NSRegularExpression(pattern: "\\b\(escaped)\\b", options: .caseInsensitive) else { return false }
            return fields.contains { regex.firstMatch(in: $0, range: NSRange($0.startIndex..., in: $0)) != nil }
        }
        let lower = cleaned.lowercased()
        return fields.contains { $0.lowercased().contains(lower) }
    }

    /// SQL `LIKE` pattern with `%`, `_` and the escape character neutralised.
    static func likePattern(_ query: String) -> String {
        let escaped = clean(query)
            .replacingOccurrences(of: "\\", with: "\\\\")
            .replacingOccurrences(of: "%", with: "\\%")
            .replacingOccurrences(of: "_", with: "\\_")
        return "%\(escaped)%"
    }
}
