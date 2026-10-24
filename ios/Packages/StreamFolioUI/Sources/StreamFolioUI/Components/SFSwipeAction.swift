import SwiftUI
import StreamFolioCore

/// How a configured swipe action (`bookmark`, `share`, `read`) looks. One definition, so the swipe
/// buttons, the context menu and the Settings > Gestures preview always agree on icon, wording and
/// color (Android: bookmark = brand/green, share = amber, read = violet).
public struct SFSwipeActionStyle {
    public let title: LocalizedStringKey
    public let systemImage: String
    public let tint: Color

    public static func style(for action: String, isSaved: Bool = false, isRead: Bool = false) -> SFSwipeActionStyle? {
        switch action {
        case "bookmark":
            SFSwipeActionStyle(title: isSaved ? "Unsave" : "Save", systemImage: isSaved ? "bookmark.slash.fill" : "bookmark.fill", tint: SFColor.green)
        case "share":
            SFSwipeActionStyle(title: "Share", systemImage: "square.and.arrow.up", tint: SFColor.amber)
        case "read":
            SFSwipeActionStyle(title: isRead ? "Unread" : "Read", systemImage: isRead ? "circle" : "checkmark.circle.fill", tint: SFColor.violet)
        default:
            nil
        }
    }
}

/// A swipe-action button for an article, built from a configured action name.
public struct SFSwipeActionButton: View {
    private let action: String
    private let article: Article
    private let perform: (String) -> Void

    public init(action: String, article: Article, perform: @escaping (String) -> Void) {
        self.action = action
        self.article = article
        self.perform = perform
    }

    public var body: some View {
        if let style = SFSwipeActionStyle.style(for: action, isSaved: article.isBookmarked, isRead: article.isRead) {
            Button { perform(action) } label: { Label(style.title, systemImage: style.systemImage) }
                .tint(style.tint)
        }
    }
}

/// Section title over a list ("Trending Highlights", "Latest Stories").
public struct SFSectionHeader: View {
    private let title: LocalizedStringKey

    public init(_ title: LocalizedStringKey) { self.title = title }

    public var body: some View {
        Text(title)
            .font(SFFont.sectionTitle)
            .foregroundStyle(SFColor.onSurface)
            .textCase(nil)
            .accessibilityAddTraits(.isHeader)
    }
}
