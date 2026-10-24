import Foundation

/// Clusters near-duplicate headlines from different publishers into story groups.
/// Port of `NewsRepository.assignGroupIds` / `cleanTitle` / Jaccard similarity: titles are reduced to
/// word sets (punctuation, stopwords and publisher suffixes removed) and joined to a group when
/// the word sets overlap by at least 35%.
public enum StoryGrouper {

    public static let similarityThreshold = 0.35

    private static let stopwords: Set<String> = [
        "a", "an", "the", "in", "on", "at", "by", "of", "to", "for", "with",
        "and", "but", "or", "is", "are", "was", "were", "has", "have", "had",
        "that", "this", "these", "those", "will", "would", "could", "should",
        "after", "before", "about", "from", "into", "over", "under", "been",
        "says", "said", "reporting", "reports", "latest", "update", "live", "news",
    ]

    private static let sourceSuffixes = [
        "bbc news", "bbc", "reuters", "cnn", "ap", "ap news", "the guardian",
        "new york times", "nytimes", "washington post", "wapo", "fox news",
        "cna", "the straits times", "straits times", "bloomberg",
    ]

    /// The comparable word set of a headline.
    public static func words(in title: String) -> Set<String> {
        var cleaned = title.lowercased().trimmed
        cleaned = cleaned.replacing(/[^\p{L}\p{N}\s]/, with: " ")
        for suffix in sourceSuffixes {
            if cleaned.hasSuffix(suffix) { cleaned = String(cleaned.dropLast(suffix.count)).trimmed }
            if cleaned.hasPrefix(suffix) { cleaned = String(cleaned.dropFirst(suffix.count)).trimmed }
        }
        let tokens = cleaned.split(whereSeparator: \.isWhitespace).map(String.init)
        return Set(tokens.filter { $0.count > 2 && !stopwords.contains($0) })
    }

    public static func jaccard(_ a: Set<String>, _ b: Set<String>) -> Double {
        if a.isEmpty && b.isEmpty { return 1 }
        if a.isEmpty || b.isEmpty { return 0 }
        return Double(a.intersection(b).count) / Double(a.union(b).count)
    }

    /// Returns `articles` with `groupId` set. Every article gets one: an article with no similar
    /// coverage becomes its own group (`groupId == link`), so `groupId IS NULL` only ever means
    /// "predates grouping".
    public static func assignGroupIds(to articles: [Article], existing: [Article]) -> [Article] {
        if articles.isEmpty { return articles }

        struct Group { var leader: Article; var words: Set<String> }
        var groups: [Group] = []

        // Seed groups from what is already stored, preserving the first-match placement rule.
        for article in existing {
            let wordSet = words(in: article.title)
            if groups.contains(where: { jaccard($0.words, wordSet) >= similarityThreshold }) { continue }
            groups.append(Group(leader: article, words: wordSet))
        }

        return articles.map { article in
            var result = article
            let wordSet = words(in: article.title)
            var matched = article.groupId

            if matched == nil, let group = groups.first(where: { jaccard($0.words, wordSet) >= similarityThreshold }) {
                matched = group.leader.groupId ?? group.leader.link
            }
            if matched == nil {
                matched = article.link
                groups.append(Group(leader: article, words: wordSet))
            }
            result.groupId = matched
            return result
        }
    }

    /// Collapses a list into primary stories with their other-publisher coverage, keeping the order
    /// of first appearance. Uses the persisted `groupId` (cheap `groupBy`).
    public static func group(_ articles: [Article]) -> [ArticleGroup] {
        var order: [String] = []
        var buckets: [String: [Article]] = [:]
        for article in articles {
            let key = article.groupId ?? article.link
            if buckets[key] == nil { order.append(key) }
            buckets[key, default: []].append(article)
        }
        return order.compactMap { key in
            guard let members = buckets[key], let primary = members.first else { return nil }
            return ArticleGroup(primary: primary, secondary: Array(members.dropFirst()))
        }
    }
}
