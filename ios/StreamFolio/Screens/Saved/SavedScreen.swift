import SwiftUI
import StreamFolioCore
import StreamFolioTokens
import StreamFolioUI

/// The Saved tab: every collection (plus "All Saved") as tiles or rows.
struct SavedScreen: View {
    @Environment(AppEnvironment.self) private var env
    @State private var path = NavigationPath()
    @State private var showsCreate = false
    @Namespace private var zoom

    var body: some View {
        let saved = env.saved
        NavigationStack(path: $path) {
            Group {
                if saved.hasLoaded && saved.isEmpty {
                    ScrollView {
                        SFEmptyState(
                            illustration: Image("OtterNoBookmarks"),
                            title: "No saved bookmarks",
                            message: "Swipe on a story in the Home feed to save it to a collection."
                        )
                        .padding(.top, SFSpacing.s12)
                    }
                } else {
                    ScrollView {
                        if saved.isGrid { grid(saved) } else { list(saved) }
                    }
                    .reportsScrollAtTop()
                }
            }
            .background { SFBackdrop() }
            .navigationTitle("Saved")
            .toolbar {
                ToolbarItemGroup(placement: .topBarTrailing) {
                    Button { withAnimation(.smooth) { saved.isGrid.toggle() } } label: {
                        Label(saved.isGrid ? "Show as list" : "Show as grid", systemImage: saved.isGrid ? "list.bullet" : "square.grid.2x2")
                    }
                    Button { showsCreate = true } label: { Label("New collection", systemImage: "plus") }
                }
            }
            .navigationDestination(for: CollectionID.self) { id in
                CollectionScreen(id: id, openArticle: { path.append($0) })
            }
            .navigationDestination(for: Article.self) { article in
                ArticleDetailScreen(reader: env.makeReader(for: article))
                    .navigationTransition(.zoom(sourceID: article.link, in: zoom))
            }
        }
        .sheet(isPresented: $showsCreate) { CreateCollectionSheet() }
    }

    private func grid(_ saved: SavedStore) -> some View {
        LazyVGrid(columns: [GridItem(.flexible(), spacing: SFSpacing.s8), GridItem(.flexible())], spacing: SFSpacing.s8) {
            NavigationLink(value: CollectionID.all) {
                SFCollectionCard(title: "All Saved", count: saved.allSaved.count, preview: saved.allSavedPreview, isDefault: true)
            }
            ForEach(saved.collections) { list in
                NavigationLink(value: CollectionID.list(list.id)) {
                    SFCollectionCard(title: list.name, count: list.articleCount, preview: list.previewThumbnail, isDefault: list.isDefault)
                }
            }
            Button { showsCreate = true } label: { SFNewCollectionCard() }
        }
        .buttonStyle(.plain)
        .padding(.horizontal, SFSpacing.gutterTight)
        .padding(.vertical, SFSpacing.s6)
    }

    private func list(_ saved: SavedStore) -> some View {
        LazyVStack(spacing: SFSpacing.s6) {
            NavigationLink(value: CollectionID.all) {
                SFCollectionRow(title: "All Saved Articles", count: saved.allSaved.count, preview: saved.allSavedPreview, isDefault: true)
            }
            ForEach(saved.collections) { list in
                NavigationLink(value: CollectionID.list(list.id)) {
                    SFCollectionRow(title: list.name, count: list.articleCount, preview: list.previewThumbnail, isDefault: list.isDefault)
                }
            }
            Button { showsCreate = true } label: { SFNewCollectionRow() }
        }
        .buttonStyle(.plain)
        .padding(.horizontal, SFSpacing.gutterTight)
        .padding(.vertical, SFSpacing.s6)
    }
}
