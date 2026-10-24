import SwiftUI
import StreamFolioCore
import StreamFolioTokens
import StreamFolioUI

/// The articles of one collection (or "All Saved").
struct CollectionScreen: View {
    let id: CollectionID
    let openArticle: (Article) -> Void

    @Environment(AppEnvironment.self) private var env
    @Environment(\.dismiss) private var dismiss
    @State private var contents: CollectionContents?
    @State private var renaming = false
    @State private var newName = ""
    @State private var confirmingDelete = false
    @State private var choosingDestination = false
    @Namespace private var zoom

    private var summary: CollectionSummary? { env.saved.summary(of: id) }
    private var isCustom: Bool { summary.map { !$0.isDefault } ?? false }

    var body: some View {
        let articles = contents?.articles ?? []
        Group {
            if contents?.hasLoaded == true && articles.isEmpty {
                ScrollView {
                    SFEmptyState(
                        illustration: Image("OtterNoBookmarks"),
                        title: "This collection is empty",
                        message: "Add articles to this collection when saving news."
                    )
                    .padding(.top, SFSpacing.s12)
                }
            } else {
                List {
                    ForEach(articles) { article in
                        StoryRow(
                            group: ArticleGroup(primary: article), zoom: zoom, open: openArticle,
                            removeLabel: id == .all ? "Unsave" : "Remove",
                            onRemove: { item in Task { await contents?.remove(item) } }
                        )
                    }
                }
                .listStyle(.plain)
                .scrollContentBackground(.hidden)
                .reportsScrollAtTop()
            }
        }
        .background { SFBackdrop() }
        .navigationTitle(env.saved.name(of: id))
        .navigationSubtitle(articles.count == 1 ? "1 saved article" : "\(articles.count) saved articles")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if isCustom {
                ToolbarItem(placement: .topBarTrailing) {
                    Menu {
                        Button { newName = summary?.name ?? ""; renaming = true } label: { Label("Rename", systemImage: "pencil") }
                        Button(role: .destructive) { confirmingDelete = true } label: { Label("Delete Collection", systemImage: "trash") }
                    } label: { Label("Collection options", systemImage: "ellipsis.circle") }
                }
            }
        }
        .alert("Rename Collection", isPresented: $renaming) {
            TextField("Name", text: $newName)
            Button("Cancel", role: .cancel) {}
            Button("Rename") {
                if case .list(let listId) = id { Task { await env.saved.rename(listId, to: newName) } }
            }
        }
        .confirmationDialog(deleteTitle, isPresented: $confirmingDelete, titleVisibility: .visible) {
            if articles.isEmpty {
                Button("Delete Collection", role: .destructive) { delete(moveTo: nil) }
            } else {
                Button("Move Articles to Another Collection…") { choosingDestination = true }
                Button("Delete Collection and Its Saves", role: .destructive) { delete(moveTo: nil) }
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text(articles.isEmpty ? "This can't be undone." : "Move the saved articles somewhere first, or remove them from your saved articles.")
        }
        .sheet(isPresented: $choosingDestination) { destinationPicker }
        .task {
            if contents == nil { contents = CollectionContents(id: id, bookmarks: env.bookmarks) }
            contents?.start()
        }
    }

    private var deleteTitle: String { "Delete “\(env.saved.name(of: id))”?" }

    private var destinationPicker: some View {
        VStack(alignment: .leading, spacing: SFSpacing.s8) {
            Text("Move articles to…").font(SFFont.sheetTitle).foregroundStyle(SFColor.onSurface)
            ScrollView {
                VStack(spacing: SFSpacing.s4) {
                    ForEach(env.saved.collections.filter { summary?.id != $0.id }) { list in
                        Button {
                            choosingDestination = false
                            delete(moveTo: list.id)
                        } label: { SFCollectionPickerRow(name: list.name, isDefault: list.isDefault, isSelected: false) }
                            .buttonStyle(.plain)
                    }
                }
            }
        }
        .padding(SFSpacing.gutter)
        .sfSheetBackground()
        .presentationDetents([.medium])
    }

    private func delete(moveTo destination: Int64?) {
        guard case .list(let listId) = id else { return }
        Task {
            await env.saved.delete(listId, moveTo: destination)
            dismiss()
        }
    }
}
