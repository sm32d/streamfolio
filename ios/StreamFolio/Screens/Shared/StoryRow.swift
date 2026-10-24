import SwiftUI
import StreamFolioCore
import StreamFolioTokens
import StreamFolioUI

/// One story in a list — the same row, swipe actions and long-press menu on Home, Search and Saved.
///
/// Must be used as the direct content of a `List` row. Tapping calls `open`; the thumbnail is the
/// source of the zoom transition into the article.
struct StoryRow: View {
    @Environment(AppEnvironment.self) private var env

    let group: ArticleGroup
    let zoom: Namespace.ID
    let open: (Article) -> Void
    /// Replaces the configured trailing swipe with "Remove" (collections).
    var removeLabel: LocalizedStringKey?
    var onRemove: ((Article) -> Void)?

    var body: some View {
        let article = group.primary
        let prefs = env.preferences
        Button { open(article) } label: {
            SFArticleRow(group: group, isSaved: article.isBookmarked)
        }
        .buttonStyle(.plain)
        .matchedTransitionSource(id: article.link, in: zoom)
        .listRowBackground(Color.clear)
        .listRowSeparator(.hidden)
        .listRowInsets(EdgeInsets(top: SFSpacing.s3, leading: SFSpacing.gutter, bottom: SFSpacing.s3, trailing: SFSpacing.gutter))
        .swipeActions(edge: .trailing) {
            if let onRemove {
                Button(role: .destructive) { onRemove(article) } label: { Label(removeLabel ?? "Remove", systemImage: "trash") }
            } else {
                SFSwipeActionButton(action: prefs[Prefs.swipeLeftAction], article: article) { env.interactions.perform($0, on: article) }
            }
        }
        .swipeActions(edge: .leading) {
            SFSwipeActionButton(action: prefs[Prefs.swipeRightAction], article: article) { env.interactions.perform($0, on: article) }
        }
        .contextMenu { ArticleMenu(article: article) }
    }
}

/// The long-press menu for an article. Also used by the trending cards.
struct ArticleMenu: View {
    @Environment(AppEnvironment.self) private var env
    let article: Article

    var body: some View {
        ForEach(["bookmark", "share", "read"], id: \.self) { action in
            SFSwipeActionButton(action: action, article: article) { env.interactions.perform($0, on: article) }
        }
        Button { env.interactions.manageCollections(of: article) } label: {
            Label("Save to Collection…", systemImage: "folder.badge.plus")
        }
    }
}
