import SwiftUI
import StreamFolioCore
import StreamFolioTokens

/// The reader's text settings (`reader_font_family`, `reader_font_size`, `reader_line_spacing`).
public struct SFReaderStyle: Equatable {
    public var family: String
    public var size: Double
    public var lineSpacing: Double

    public init(family: String, size: Double, lineSpacing: Double) {
        self.family = family
        self.size = size
        self.lineSpacing = lineSpacing
    }

    public init(_ preferences: AppPreferences) {
        self.init(
            family: preferences[Prefs.readerFontFamily],
            size: preferences[Prefs.readerFontSize],
            lineSpacing: preferences[Prefs.readerLineSpacing]
        )
    }

    var design: Font.Design { family == "serif" ? .serif : .default }
}

/// Body text at the user's chosen size. The chosen size is a *base*: it is scaled by Dynamic Type, so
/// a larger system text size still enlarges the article.
public struct SFReaderBody: View {
    private let paragraphs: [String]
    private let style: SFReaderStyle
    @ScaledMetric(relativeTo: .body) private var scale = 1.0

    public init(paragraphs: [String], style: SFReaderStyle) {
        self.paragraphs = paragraphs
        self.style = style
    }

    public var body: some View {
        let size = style.size * scale
        VStack(alignment: .leading, spacing: SFSpacing.s8) {
            ForEach(Array(paragraphs.enumerated()), id: \.offset) { _, paragraph in
                Text(paragraph)
                    .font(.system(size: size, design: style.design))
                    // Natural line height is about 1.2 × the font size; spacing is the extra on top.
                    .lineSpacing(max(0, size * (style.lineSpacing - 1.2)))
                    .foregroundStyle(SFColor.onSurface)
                    .textSelection(.enabled)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
    }
}

/// Hero image, category, headline and the publisher/date/reading-time line.
public struct SFReaderHeader: View {
    private let article: Article
    private let readingTime: String
    private let style: SFReaderStyle
    @ScaledMetric(relativeTo: .title) private var scale = 1.0

    public init(article: Article, readingTime: String, style: SFReaderStyle) {
        self.article = article
        self.readingTime = readingTime
        self.style = style
    }

    public var body: some View {
        let capabilities = ArticleCapabilities(for: article)
        VStack(alignment: .leading, spacing: SFSpacing.s8) {
            hero(showsImage: capabilities.showsThumbnail)

            VStack(alignment: .leading, spacing: SFSpacing.s4) {
                Text(article.category)
                    .font(SFFont.caption.weight(.bold))
                    .textCase(.uppercase)
                    .foregroundStyle(SFColor.primary)
                Text(article.title)
                    .font(.system(size: style.size * 1.3 * scale, weight: .heavy, design: style.design))
                    .foregroundStyle(SFColor.onSurface)
                    .lineSpacing(style.size * 1.3 * 0.1)
                    .accessibilityAddTraits(.isHeader)
                    .textSelection(.enabled)
            }

            HStack(spacing: SFSpacing.s5) {
                SFPublisherAvatar(name: article.sourceName, size: 32)
                VStack(alignment: .leading, spacing: SFSpacing.s1) {
                    Text(article.sourceName).font(SFFont.bodySmall.weight(.semibold)).foregroundStyle(SFColor.onSurface)
                    HStack(spacing: SFSpacing.s3) {
                        if let date = article.publishedAt {
                            Text(date, format: .relative(presentation: .numeric, unitsStyle: .abbreviated))
                            Text("•").foregroundStyle(SFColor.textDisabled)
                        }
                        Text(readingTime)
                    }
                    .font(SFFont.caption)
                    .foregroundStyle(SFColor.textMuted)
                }
            }
            .accessibilityElement(children: .combine)
        }
    }

    @ViewBuilder
    private func hero(showsImage: Bool) -> some View {
        let height: CGFloat = showsImage && hasImage ? 240 : 200
        ZStack {
            if showsImage && hasImage {
                SFThumbnail(urlString: article.thumbnailUrl, maxPointSize: 900)
            } else {
                SFGradient.thumbnailFallback
                Text(article.sourceName)
                    .font(SFFont.subpageTitle)
                    .foregroundStyle(SFColor.onPrimary)
                    .padding(SFSpacing.s8)
            }
        }
        .frame(maxWidth: .infinity)
        .frame(height: height)
        .clipShape(.rect(cornerRadius: SFRadius.hero))
        .accessibilityHidden(true)
    }

    private var hasImage: Bool {
        guard let url = article.thumbnailUrl else { return false }
        return !url.isEmpty && url != ArticleContentService.thumbnailFailed
    }
}

/// "Aa" popover content: font family, text size and line spacing.
public struct SFTypographyPanel: View {
    @Binding private var family: String
    @Binding private var size: Double
    @Binding private var lineSpacing: Double

    public init(family: Binding<String>, size: Binding<Double>, lineSpacing: Binding<Double>) {
        self._family = family
        self._size = size
        self._lineSpacing = lineSpacing
    }

    private static let sizeStep = 2.0
    private static let spacings: [(label: String, value: Double)] = [("1.2×", 1.2), ("1.5×", 1.5), ("1.8×", 1.8)]

    public var body: some View {
        VStack(alignment: .leading, spacing: SFSpacing.s8) {
            row("Font") {
                Picker("Font", selection: $family) {
                    Text("Sans-Serif").tag("sans_serif")
                    Text("Serif").tag("serif")
                }
                .pickerStyle(.segmented)
            }
            row("Text size · \(Int(size))") {
                HStack(spacing: SFSpacing.s4) {
                    Button { size = max(12, size - Self.sizeStep) } label: { Text("A−").font(SFFont.rowTitle.weight(.bold)).frame(width: 44, height: 36) }
                        .disabled(size <= 12)
                    Button { size = min(30, size + Self.sizeStep) } label: { Text("A+").font(SFFont.sectionTitle.weight(.bold)).frame(width: 44, height: 36) }
                        .disabled(size >= 30)
                }
                .buttonStyle(.bordered)
                .tint(SFColor.primary)
            }
            row("Line spacing") {
                Picker("Line spacing", selection: $lineSpacing) {
                    ForEach(Self.spacings, id: \.value) { Text($0.label).tag($0.value) }
                }
                .pickerStyle(.segmented)
            }
        }
        .padding(SFSpacing.s8)
        .frame(minWidth: 300)
    }

    private func row<Content: View>(_ title: LocalizedStringKey, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: SFSpacing.s4) {
            Text(title).font(SFFont.bodySmall.weight(.bold)).foregroundStyle(SFColor.onSurface)
            content()
        }
    }
}

/// Grey placeholder lines while the article text loads.
public struct SFTextSkeleton: View {
    @State private var pulse = false
    public init() {}

    public var body: some View {
        VStack(alignment: .leading, spacing: SFSpacing.s6) {
            ForEach(0..<9, id: \.self) { index in
                RoundedRectangle(cornerRadius: SFRadius.xs)
                    .frame(height: 14)
                    .frame(maxWidth: index % 4 == 3 ? 220 : .infinity, alignment: .leading)
            }
        }
        .foregroundStyle(SFColor.onSurface.opacity(pulse ? 0.12 : 0.06))
        .onAppear { withAnimation(.easeInOut(duration: 1).repeatForever(autoreverses: true)) { pulse = true } }
        .accessibilityLabel("Loading article")
    }
}

/// Inline hint that the full text could not be extracted and the Web View has it.
public struct SFInfoBanner: View {
    private let text: LocalizedStringKey
    private let systemImage: String

    public init(_ text: LocalizedStringKey, systemImage: String = "info.circle.fill") {
        self.text = text
        self.systemImage = systemImage
    }

    public var body: some View {
        Label(text, systemImage: systemImage)
            .font(SFFont.bodySmall.weight(.medium))
            .foregroundStyle(SFColor.primary)
            .padding(SFSpacing.s8)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(SFColor.primary.opacity(0.08), in: .rect(cornerRadius: SFRadius.r16))
    }
}
