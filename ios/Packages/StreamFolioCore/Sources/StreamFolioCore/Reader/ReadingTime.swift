import Foundation

/// "3 min read" — 200 words per minute over the cached body, else the description, else the title.
public enum ReadingTime {
    public static let wordsPerMinute = 200

    public static func minutes(for article: Article) -> Int {
        let text: String
        if let full = article.fullText, !full.isEmpty { text = full }
        else if !article.description.isEmpty { text = article.description }
        else { text = article.title }
        let words = text.split(whereSeparator: \.isWhitespace).count
        return max(1, (words + wordsPerMinute - 1) / wordsPerMinute)
    }

    public static func label(for article: Article) -> String { "\(minutes(for: article)) min read" }
}
