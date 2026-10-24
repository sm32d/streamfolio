import SwiftUI
import StreamFolioTokens

// Building blocks of the Settings hub and its sub-pages. Every settings screen composes these, so a
// row, card or banner looks and behaves the same on all of them (Android: `SettingsComponents.kt`).

/// A rounded square holding an SF Symbol: the icon of a settings row, filled with its accent colour.
public struct SFIconTile: View {
    private let systemImage: String
    private let tint: Color
    private let isFilled: Bool
    private let size: Double

    /// - Parameter isFilled: solid tile with a white symbol (rows), or a soft tint with a coloured
    ///   symbol (banners, status).
    public init(systemImage: String, tint: Color = SFColor.primary, isFilled: Bool = true, size: Double = SFSpacing.thumbSmall) {
        self.systemImage = systemImage
        self.tint = tint
        self.isFilled = isFilled
        self.size = size
    }

    public var body: some View {
        Image(systemName: systemImage)
            .font(SFFont.sectionTitle)
            .foregroundStyle(isFilled ? SFColor.onPrimary : tint)
            .frame(width: size, height: size)
            .background(isFilled ? tint : tint.opacity(0.15), in: .rect(cornerRadius: SFRadius.lg))
            .accessibilityHidden(true)
    }
}

/// Scrolling container for a settings page: gutters, spacing between sections, the brand backdrop.
/// Pages with very long lists pass `isLazy` so rows are only built as they scroll into view.
public struct SFSettingsPage<Content: View>: View {
    private let isLazy: Bool
    private let content: Content

    public init(isLazy: Bool = false, @ViewBuilder content: () -> Content) {
        self.isLazy = isLazy
        self.content = content()
    }

    public var body: some View {
        ScrollView {
            if isLazy {
                LazyVStack(alignment: .leading, spacing: SFSpacing.s11) { content }.padding(layout)
            } else {
                VStack(alignment: .leading, spacing: SFSpacing.s11) { content }.padding(layout)
            }
        }
        .scrollDismissesKeyboard(.interactively)
        .background { SFBackdrop() }
    }

    private var layout: EdgeInsets {
        EdgeInsets(top: SFSpacing.s8, leading: SFSpacing.gutter, bottom: SFSpacing.s8, trailing: SFSpacing.gutter)
    }
}

/// A titled group on a settings page: the small uppercase label above its content.
public struct SFSettingsSection<Content: View>: View {
    private let title: LocalizedStringKey?
    private let content: Content

    public init(_ title: LocalizedStringKey? = nil, @ViewBuilder content: () -> Content) {
        self.title = title
        self.content = content()
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: SFSpacing.s5) {
            if let title { SFSectionLabel(title) }
            content
        }
    }
}

/// The gradient card at the top of a sub-page: what this page is for.
public struct SFHeroBanner: View {
    private let title: LocalizedStringKey
    private let subtitle: LocalizedStringKey
    private let systemImage: String

    public init(_ title: LocalizedStringKey, subtitle: LocalizedStringKey, systemImage: String) {
        self.title = title
        self.subtitle = subtitle
        self.systemImage = systemImage
    }

    public var body: some View {
        HStack(spacing: SFSpacing.s6) {
            Image(systemName: systemImage)
                .font(SFFont.sectionTitle)
                .foregroundStyle(SFColor.primary)
                .frame(width: 42, height: 42)
                .background(SFColor.primary.opacity(0.15), in: .circle)
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: SFSpacing.s2) {
                Text(title).font(SFFont.sectionTitle).foregroundStyle(SFColor.onSurface)
                Text(subtitle).font(SFFont.meta).foregroundStyle(SFColor.textSecondary)
            }
            Spacer(minLength: 0)
        }
        .padding(SFSpacing.s10)
        .background(
            LinearGradient(
                colors: [SFColor.primaryContainer.opacity(0.85), SFColor.surfaceVariant.opacity(0.6)],
                startPoint: .topLeading, endPoint: .bottomTrailing
            ),
            in: .rect(cornerRadius: SFRadius.hero)
        )
        .overlay { RoundedRectangle(cornerRadius: SFRadius.hero).strokeBorder(SFColor.primary.opacity(0.25), lineWidth: SFStroke.hairline) }
        .accessibilityElement(children: .combine)
    }
}

/// Card holding a stack of rows, with hairline dividers between them.
public struct SFSettingsCard<Content: View>: View {
    private let content: Content

    public init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    public var body: some View {
        VStack(spacing: 0) {
            Group(subviews: content) { subviews in
                ForEach(subviews) { subview in
                    subview
                    if subview.id != subviews.last?.id {
                        Divider().overlay(SFColor.borderSoft).padding(.horizontal, SFSpacing.cardPaddingLarge)
                    }
                }
            }
        }
        .frame(maxWidth: .infinity)
        .sfCard()
    }
}

/// A navigation/action row: icon tile, title, subtitle and a trailing view (a chevron by default).
/// Wrap it in a `NavigationLink` or `Button` with `.buttonStyle(.plain)`.
public struct SFSettingsRow<Trailing: View>: View {
    private let systemImage: String
    private let tint: Color
    private let title: LocalizedStringKey
    private let subtitle: String
    private let trailing: Trailing

    public init(
        systemImage: String, tint: Color = SFColor.primary, title: LocalizedStringKey, subtitle: String,
        @ViewBuilder trailing: () -> Trailing
    ) {
        self.systemImage = systemImage
        self.tint = tint
        self.title = title
        self.subtitle = subtitle
        self.trailing = trailing()
    }

    public var body: some View {
        HStack(spacing: SFSpacing.s7) {
            SFIconTile(systemImage: systemImage, tint: tint)
            VStack(alignment: .leading, spacing: SFSpacing.s1) {
                Text(title).font(SFFont.cardTitle).foregroundStyle(SFColor.onSurface)
                Text(subtitle).font(SFFont.meta).foregroundStyle(SFColor.textMuted).multilineTextAlignment(.leading)
            }
            Spacer(minLength: SFSpacing.s4)
            trailing
        }
        .padding(.horizontal, SFSpacing.cardPaddingLarge)
        .padding(.vertical, SFSpacing.s7)
        .frame(maxWidth: .infinity, alignment: .leading)
        .contentShape(.rect)
    }
}

/// The disclosure indicator at the trailing edge of a navigation row.
public struct SFChevron: View {
    public init() {}
    public var body: some View {
        Image(systemName: "chevron.right").font(SFFont.meta.weight(.semibold)).foregroundStyle(SFColor.textFaint)
    }
}

public extension SFSettingsRow where Trailing == SFChevron {
    init(systemImage: String, tint: Color = SFColor.primary, title: LocalizedStringKey, subtitle: String) {
        self.init(systemImage: systemImage, tint: tint, title: title, subtitle: subtitle) { SFChevron() }
    }
}

/// A settings row whose trailing control is a switch.
public struct SFSettingsToggleRow: View {
    private let systemImage: String
    private let tint: Color
    private let title: LocalizedStringKey
    private let subtitle: String
    private let isAvailable: Bool
    @Binding private var isOn: Bool

    public init(
        systemImage: String, tint: Color = SFColor.primary, title: LocalizedStringKey, subtitle: String,
        isAvailable: Bool = true, isOn: Binding<Bool>
    ) {
        self.systemImage = systemImage
        self.tint = tint
        self.title = title
        self.subtitle = subtitle
        self.isAvailable = isAvailable
        self._isOn = isOn
    }

    public var body: some View {
        Toggle(isOn: $isOn) {
            HStack(spacing: SFSpacing.s7) {
                SFIconTile(systemImage: systemImage, tint: isAvailable ? tint : SFColor.textDisabled)
                VStack(alignment: .leading, spacing: SFSpacing.s1) {
                    Text(title).font(SFFont.cardTitle).foregroundStyle(isAvailable ? SFColor.onSurface : SFColor.textDisabled)
                    Text(subtitle).font(SFFont.meta).foregroundStyle(isAvailable ? SFColor.textMuted : SFColor.error.opacity(0.7))
                }
            }
        }
        .tint(SFColor.primary)
        .disabled(!isAvailable)
        .padding(.horizontal, SFSpacing.cardPaddingLarge)
        .padding(.vertical, SFSpacing.s7)
    }
}

/// A single-line text input in the app's field style (sheets and forms).
public struct SFTextField: View {
    private let title: LocalizedStringKey
    private let prompt: String?
    @Binding private var text: String
    private let isURL: Bool

    public init(_ title: LocalizedStringKey, prompt: String? = nil, text: Binding<String>, isURL: Bool = false) {
        self.title = title
        self.prompt = prompt
        self._text = text
        self.isURL = isURL
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: SFSpacing.s3) {
            Text(title).font(SFFont.meta.weight(.semibold)).foregroundStyle(SFColor.textSecondary)
            field
                .padding(.horizontal, SFSpacing.s7)
                .frame(minHeight: SFSpacing.touchTarget)
                .background(SFColor.surfaceVariant.opacity(0.5), in: .rect(cornerRadius: SFRadius.r14))
                .overlay { RoundedRectangle(cornerRadius: SFRadius.r14).strokeBorder(SFColor.borderSoft, lineWidth: SFStroke.hairline) }
        }
    }
}

private extension SFTextField {
    @ViewBuilder var field: some View {
        let input = TextField(title, text: $text, prompt: prompt.map { Text(verbatim: $0) }).autocorrectionDisabled(isURL)
        #if os(iOS)
        input.textInputAutocapitalization(isURL ? .never : .words).keyboardType(isURL ? .URL : .default)
        #else
        input
        #endif
    }
}

/// The SF Symbol that stands for a news category (Android `getCategoryIcon`).
public enum SFCategoryIcon {
    public static func systemName(for category: String) -> String {
        switch category.lowercased() {
        case "top stories": "star.fill"
        case "world": "globe"
        case "business": "briefcase.fill"
        case "technology", "tech": "cpu"
        case "science": "atom"
        case "sports": "basketball.fill"
        case "health": "cross.case.fill"
        case "entertainment": "film"
        default: "dot.radiowaves.up.forward"
        }
    }
}
