import SwiftUI
import StreamFolioCore
import StreamFolioTokens
import StreamFolioUI

/// "Save to Collection" / "Manage Collections": tick the collections an article belongs to.
struct SaveToCollectionSheet: View {
    let article: Article

    @Environment(AppEnvironment.self) private var env
    @Environment(\.dismiss) private var dismiss
    @State private var selected: Set<Int64>?
    @State private var isCreating = false
    @State private var newName = ""
    @State private var nameTaken = false
    @FocusState private var nameFocused: Bool

    var body: some View {
        let lists = env.saved.collections
        VStack(alignment: .leading, spacing: SFSpacing.s8) {
            HStack {
                Label(article.isBookmarked ? "Manage Collections" : "Save to Collection", systemImage: "folder.badge.plus")
                    .font(SFFont.sheetTitle)
                    .foregroundStyle(SFColor.onSurface)
                Spacer()
                Button { dismiss() } label: { Image(systemName: "xmark.circle.fill").font(SFFont.sheetTitle).foregroundStyle(SFColor.textDisabled) }
                    .buttonStyle(.plain)
                    .accessibilityLabel("Close")
            }

            ScrollView {
                VStack(spacing: SFSpacing.s4) {
                    ForEach(lists) { list in
                        let isOn = selected?.contains(list.id) ?? false
                        Button { toggle(list.id) } label: {
                            SFCollectionPickerRow(name: list.name, isDefault: list.isDefault, isSelected: isOn)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            .scrollIndicators(.hidden)

            if isCreating {
                VStack(alignment: .leading, spacing: SFSpacing.s3) {
                    HStack(spacing: SFSpacing.s4) {
                        TextField("New collection name", text: $newName)
                            .textFieldStyle(.roundedBorder)
                            .focused($nameFocused)
                            .submitLabel(.done)
                            .onSubmit(create)
                        Button("Add", action: create).buttonStyle(.borderedProminent).disabled(newName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    }
                    if nameTaken { Text("A collection with that name already exists.").font(SFFont.meta).foregroundStyle(SFColor.error) }
                }
            } else {
                Button { isCreating = true; nameFocused = true } label: {
                    Label("Create New Collection", systemImage: "plus").font(SFFont.rowTitle.weight(.bold)).frame(maxWidth: .infinity)
                }
                .tint(SFColor.primary)
            }

            SFPrimaryButton("Done") { Task { await save() } }
        }
        .padding(SFSpacing.gutter)
        .sfSheetBackground()
        .presentationDetents([.medium, .large])
        .task { await loadInitialSelection() }
    }

    private func toggle(_ id: Int64) {
        var set = selected ?? []
        if set.contains(id) { set.remove(id) } else { set.insert(id) }
        selected = set
    }

    private func create() {
        Task {
            if let id = await env.saved.create(named: newName) {
                toggleOn(id)
                newName = ""
                nameTaken = false
                isCreating = false
            } else {
                nameTaken = true
            }
        }
    }

    private func toggleOn(_ id: Int64) { selected = (selected ?? []).union([id]) }

    /// Existing memberships, else the collection used last time, else the default one.
    private func loadInitialSelection() async {
        let assigned = (try? await env.bookmarks.listIds(forArticle: article.link)) ?? []
        if !assigned.isEmpty { selected = assigned; return }
        let lists = env.saved.collections
        let last = env.preferences[Prefs.lastSelectedBookmarkListId]
        let fallback = lists.first(where: \.isDefault)?.id ?? BookmarkList.defaultListId
        selected = [lists.contains { $0.id == last } ? last : fallback]
    }

    private func save() async {
        let ids = selected ?? []
        try? await env.bookmarks.setLists(ids, link: article.link)
        if let first = ids.sorted().first { env.preferences[Prefs.lastSelectedBookmarkListId] = first }
        dismiss()
    }
}
