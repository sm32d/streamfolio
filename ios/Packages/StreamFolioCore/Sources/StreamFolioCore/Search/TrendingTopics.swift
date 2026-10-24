import Foundation

/// The "Trending Topics" chips on Search: the most frequent categories, tags and headline words among
/// the latest articles of the sources the user has active. Port of `NewsViewModel.trendingTopics`.
public enum TrendingTopics {
    public static let defaults = ["AI", "Climate", "Markets", "Tech", "Science", "Sports", "Health", "Space"]

    private static let stopwords: Set<String> = [
        "the", "and", "for", "with", "this", "that", "from", "your", "about", "would",
        "their", "will", "more", "news", "says", "said", "after", "over", "first", "years",
        "year", "could", "three", "were", "what", "when", "who", "which", "have", "been",
        "also", "into", "some", "other", "them", "then", "they", "than",
        "should", "these", "those", "against", "before", "between",
        "under", "through", "during", "without", "around", "among",
    ]

    /// - Parameters:
    ///   - articles: the latest articles (Android uses the newest 50 overall).
    ///   - activeFeedUrls: only articles from these feeds count.
    public static func compute(from articles: [Article], activeFeedUrls: Set<String>, limit: Int = 8) -> [String] {
        let relevant = articles.filter { activeFeedUrls.contains($0.sourceUrl) }
        guard !relevant.isEmpty else { return defaults }

        var counts: [String: Int] = [:]
        for article in relevant {
            let category = article.category.trimmed
            if !category.isEmpty, category != "SEARCH", category != "All" { counts[category, default: 0] += 5 }

            for tag in (article.tags ?? "").split(separator: ",").map({ $0.trimmed }) where !tag.isEmpty && tag.lowercased() != "news" {
                counts[tag, default: 0] += 10
            }

            let words = article.title
                .split(whereSeparator: { !($0.isASCII && ($0.isLetter || $0.isNumber || $0 == "#")) })
                .map(String.init)
                .filter { $0.count > 2 && !stopwords.contains($0.lowercased()) && $0.lowercased() != "news" && $0.contains(where: \.isLetter) }
            for word in words {
                let display = word == word.uppercased() ? word : word.prefix(1).uppercased() + word.dropFirst()
                counts[display, default: 0] += 1
            }
        }

        let ranked = counts
            .sorted { $0.value != $1.value ? $0.value > $1.value : $0.key < $1.key }   // ties: alphabetical, so the list is stable
            .map(\.key)
            .filter { $0.lowercased() != "search" && $0.lowercased() != "all" }
            .prefix(limit)
        return ranked.isEmpty ? defaults : Array(ranked)
    }
}
