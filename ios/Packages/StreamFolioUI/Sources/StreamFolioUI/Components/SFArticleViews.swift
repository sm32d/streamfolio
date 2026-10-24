import SwiftUI
import StreamFolioCore
import StreamFolioTokens

// MARK: - Formatting

enum SFTime {
    /// "2 hr. ago"; empty when the feed gave no usable date.
    static func relative(_ article: Article, now: Date = .now) -> String {
        guard let date = article.publishedAt else { return "" }
        return min(date, now).formatted(.relative(presentation: .numeric, unitsStyle: .abbreviated))
    }
}

// MARK: - Avatar

/// Publisher badge: two-letter initials on a color picked deterministically from the name. It never
/// loads a favicon, so it is safe for link-only sources and keeps the app from calling third parties.
public struct SFPublisherAvatar: View {
    private let name: String
    private let size: Double

    public init(name: String, size: Double = 20) {
        self.name = name
        self.size = size
    }

    private static let palette: [Color] = [
        SFColor.primary, SFColor.secondary, SFColor.indigo, SFColor.teal, SFColor.violet, SFColor.amber, SFColor.coral, SFColor.green,
    ]

    private var initials: String {
        let words = name.split(whereSeparator: { !$0.isLetter && !$0.isNumber })
        let letters = words.count >= 2 ? [words[0].first, words[1].first] : Array(name.filter(\.isLetter).prefix(2)).map { Optional($0) }
        let text = letters.compactMap { $0 }.map(String.init).joined().uppercased()
        return text.isEmpty ? "?" : text
    }

    private var color: Color {
        // djb2: stable across launches (Swift's String.hashValue is randomized per process).
        let hash = name.unicodeScalars.reduce(UInt64(5381)) { ($0 &* 33) &+ UInt64($1.value) }
        return Self.palette[Int(hash % UInt64(Self.palette.count))]
    }

    public var body: some View {
        Text(initials)
            .font(.system(size: size * 0.42, weight: .bold, design: .rounded))
            .foregroundStyle(SFColor.onPrimary)
            .frame(width: size, height: size)
            .background(color, in: .circle)
            .accessibilityHidden(true)
    }
}

// MARK: - Row

/// A story in a list: thumbnail, headline, source and time, dimmed once read, with the number of
/// other publishers covering it. The same row serves Home, Search, Saved and widgets' previews.
public struct SFArticleRow: View {
    private let group: ArticleGroup
    private let isSaved: Bool

    public init(group: ArticleGroup, isSaved: Bool) {
        self.group = group
        self.isSaved = isSaved
    }

    public init(article: Article, isSaved: Bool) {
        self.init(group: ArticleGroup(primary: article), isSaved: isSaved)
    }

    public var body: some View {
        let article = group.primary
        let capabilities = ArticleCapabilities(for: article)
        HStack(alignment: .top, spacing: SFSpacing.s6) {
            if capabilities.showsThumbnail {
                SFThumbnail(urlString: article.thumbnailUrl)
                    .frame(width: SFSpacing.thumbLarge, height: SFSpacing.thumbLarge)
                    .clipShape(.rect(cornerRadius: SFRadius.r16))
            }
            VStack(alignment: .leading, spacing: SFSpacing.s3) {
                Text(article.title)
                    .font(SFFont.cardTitle)
                    .foregroundStyle(SFColor.onSurface)
                    .lineLimit(3)
                    .multilineTextAlignment(.leading)
                if !capabilities.showsThumbnail, !article.description.isEmpty {
                    Text(article.description)
                        .font(SFFont.bodySmall)
                        .foregroundStyle(SFColor.textSecondary)
                        .lineLimit(3)
                }
                Spacer(minLength: 0)
                HStack(spacing: SFSpacing.s3) {
                    SFPublisherAvatar(name: article.sourceName, size: 18)
                    Text(article.sourceName)
                        .lineLimit(1)
                    let time = SFTime.relative(article)
                    if !time.isEmpty { Text("· \(time)").lineLimit(1) }
                    Spacer(minLength: 0)
                    if isSaved {
                        Image(systemName: "bookmark.fill").foregroundStyle(SFColor.primary).accessibilityLabel("Saved")
                    }
                }
                .font(SFFont.caption)
                .foregroundStyle(SFColor.textMuted)
                if !group.secondary.isEmpty {
                    Label("Also covered by \(group.secondary.count)", systemImage: "rectangle.stack")
                        .font(SFFont.caption)
                        .foregroundStyle(SFColor.primary)
                }
            }
        }
        .padding(SFSpacing.cardPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .sfCard()
        .opacity(article.isRead ? SFAlpha.readArticle : 1)
        .contentShape(.rect(cornerRadius: SFRadius.card))
        .accessibilityElement(children: .combine)
    }
}

// MARK: - Trending card

/// Large lead-story card with a scrim so white text stays legible over any photo.
public struct SFTrendingCard: View {
    private let article: Article
    private let isSaved: Bool

    public init(article: Article, isSaved: Bool) {
        self.article = article
        self.isSaved = isSaved
    }

    public var body: some View {
        let capabilities = ArticleCapabilities(for: article)
        ZStack(alignment: .bottomLeading) {
            if capabilities.showsThumbnail {
                SFThumbnail(urlString: article.thumbnailUrl, maxPointSize: 700)
            } else {
                SFGradient.brand
            }
            SFGradient.scrim
            VStack(alignment: .leading, spacing: SFSpacing.s4) {
                Text("Trending")
                    .font(SFFont.micro)
                    .foregroundStyle(SFColor.onPrimary)
                    .padding(.horizontal, SFSpacing.s5)
                    .padding(.vertical, SFSpacing.s2)
                    .sfGlassCapsuleProminent(interactive: false)
                Spacer(minLength: 0)
                Text(article.title)
                    .font(SFFont.sheetTitle)
                    .foregroundStyle(SFColor.onPrimary)
                    .lineLimit(3)
                HStack(spacing: SFSpacing.s3) {
                    SFPublisherAvatar(name: article.sourceName, size: 18)
                    Text(article.sourceName)
                    let time = SFTime.relative(article)
                    if !time.isEmpty { Text("· \(time)") }
                    Spacer(minLength: 0)
                    if isSaved { Image(systemName: "bookmark.fill").accessibilityLabel("Saved") }
                }
                .font(SFFont.caption)
                .foregroundStyle(SFColor.onPrimary.opacity(0.85))
            }
            .padding(SFSpacing.s8)
        }
        .frame(maxWidth: .infinity)
        .frame(height: 280)
        .clipShape(.rect(cornerRadius: SFRadius.hero))
        .contentShape(.rect(cornerRadius: SFRadius.hero))
        .accessibilityElement(children: .combine)
    }
}

// MARK: - Thumbnail

/// A thumbnail that silently falls back to the brand gradient when there is no (usable) URL.
public struct SFThumbnail: View {
    private let url: URL?
    private let maxPointSize: CGFloat

    public init(urlString: String?, maxPointSize: CGFloat = 300) {
        if let urlString, !urlString.isEmpty, urlString != "failed" { url = URL(string: urlString) } else { url = nil }
        self.maxPointSize = maxPointSize
    }

    public var body: some View {
        #if canImport(UIKit)
        SFRemoteImage(url: url, maxPointSize: maxPointSize)
        #else
        SFGradient.thumbnailFallback
        #endif
    }
}
