import SwiftUI
import StreamFolioCore
import StreamFolioTokens
import StreamFolioUI

/// The user's own RSS feeds: list, filter by tab label, add, edit, delete (Android `SettingsFeedsScreen`).
struct SettingsFeedsScreen: View {
    @Environment(AppEnvironment.self) private var env
    @State private var feeds: [CustomFeed] = []
    @State private var hasLoaded = false
    @State private var filter = Self.all
    @State private var editor: FeedEditor?
    @State private var deleting: CustomFeed?

    private static let all = "All"

    private var categories: [String] { [Self.all] + Set(feeds.map(\.category)).sorted() }
    private var visible: [CustomFeed] { filter == Self.all ? feeds : feeds.filter { $0.category == filter } }

    var body: some View {
        Group {
            if hasLoaded && feeds.isEmpty {
                ScrollView {
                    SFEmptyState(
                        illustration: Image("OtterNoArticles"), title: "No custom feeds yet",
                        message: "Add RSS publishers to build personalized news categories on your home dashboard screen."
                    ) {
                        SFPrimaryButton("Add Your First Feed", systemImage: "plus") { editor = .add }
                    }
                    .padding(.top, SFSpacing.s12)
                }
                .background { SFBackdrop() }
            } else {
                SFSettingsPage {
                    SFHeroBanner("Custom RSS Curation Hub", subtitle: "Manage private RSS publishers and custom topic feeds", systemImage: "dot.radiowaves.up.forward")
                    SFSettingsSection("Active feeds · \(feeds.count)") {
                        VStack(alignment: .leading, spacing: SFSpacing.s8) {
                            filterStrip
                            SFSettingsCard {
                                ForEach(visible) { feed in row(feed) }
                            }
                        }
                    }
                }
            }
        }
        .navigationTitle("Custom RSS Feeds")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button("Add Feed", systemImage: "plus") { editor = .add }
            }
        }
        .task {
            do {
                for try await list in env.customFeeds.observeAll() {
                    feeds = list
                    hasLoaded = true
                    if !categories.contains(filter) { filter = Self.all }
                }
            } catch {}
        }
        .sheet(item: $editor) { FeedEditorSheet(editor: $0) }
        .confirmationDialog("Delete Feed", isPresented: Binding(get: { deleting != nil }, set: { if !$0 { deleting = nil } }), titleVisibility: .visible, presenting: deleting) { feed in
            Button("Delete Feed", role: .destructive) {
                Task {
                    try? await env.customFeeds.delete(feed)
                    env.feed.settingsDidChange()
                }
            }
        } message: { feed in
            Text("Are you sure you want to unsubscribe from \"\(feed.title)\"?")
        }
    }

    private var filterStrip: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: SFSpacing.s4) {
                ForEach(categories, id: \.self) { category in
                    SFSelectableChip(
                        LocalizedStringKey(category),
                        systemImage: category == Self.all ? nil : SFCategoryIcon.systemName(for: category),
                        isSelected: filter == category
                    ) { filter = category }
                }
            }
        }
    }

    private func row(_ feed: CustomFeed) -> some View {
        HStack(spacing: SFSpacing.s6) {
            SFPublisherAvatar(name: feed.title, size: SFSpacing.thumbSmall)
            VStack(alignment: .leading, spacing: SFSpacing.s1) {
                Text(feed.title).font(SFFont.cardTitle.weight(.bold)).foregroundStyle(SFColor.onSurface)
                Text(feed.url).font(SFFont.caption).foregroundStyle(SFColor.textMuted).lineLimit(1)
                Label(feed.category, systemImage: SFCategoryIcon.systemName(for: feed.category))
                    .font(SFFont.micro).foregroundStyle(SFColor.primary)
            }
            Spacer(minLength: 0)
            Button("Edit Feed", systemImage: "pencil") { editor = .edit(feed) }
                .labelStyle(.iconOnly).foregroundStyle(SFColor.primary)
            Button("Delete Feed", systemImage: "trash") { deleting = feed }
                .labelStyle(.iconOnly).foregroundStyle(SFColor.error)
        }
        .buttonStyle(.borderless)
        .padding(.horizontal, SFSpacing.cardPaddingLarge)
        .padding(.vertical, SFSpacing.s7)
    }
}

/// Add a new feed or edit an existing one.
enum FeedEditor: Identifiable {
    case add
    case edit(CustomFeed)

    var id: String {
        switch self {
        case .add: "add"
        case .edit(let feed): "edit-\(feed.id ?? 0)"
        }
    }
}

private struct FeedEditorSheet: View {
    let editor: FeedEditor

    @Environment(AppEnvironment.self) private var env
    @Environment(\.dismiss) private var dismiss
    @State private var title = ""
    @State private var url = ""
    @State private var category = ""
    @State private var urlRejected = false

    private var existing: CustomFeed? {
        if case .edit(let feed) = editor { feed } else { nil }
    }

    private var canSave: Bool { ![title, url, category].contains { $0.trimmed.isEmpty } }

    var body: some View {
        VStack(alignment: .leading, spacing: SFSpacing.s8) {
            Label(existing == nil ? "Add New Feed" : "Edit Feed", systemImage: existing == nil ? "plus" : "pencil")
                .font(SFFont.sheetTitle).foregroundStyle(SFColor.onSurface)

            SFTextField("Feed Name", prompt: "e.g. Wired Tech", text: $title)
            SFTextField("RSS Feed URL", prompt: "https://example.com/feed.xml", text: $url, isURL: true)
            SFTextField("Tab Label", prompt: "e.g. Tech, Finance", text: $category)

            if urlRejected {
                Text("Please enter a valid HTTPS URL").font(SFFont.meta).foregroundStyle(SFColor.error)
            }
            SFPrimaryButton(existing == nil ? "Add Feed" : "Save Changes", action: save).disabled(!canSave)
        }
        .padding(SFSpacing.gutter)
        .sfSheetBackground()
        .presentationDetents([.medium, .large])
        .onAppear {
            if let existing { (title, url, category) = (existing.title, existing.url, existing.category) }
        }
    }

    private func save() {
        guard CustomFeedStore.makeFeed(title: title, url: url, category: category) != nil else {
            urlRejected = true
            return
        }
        Task {
            if let existing {
                _ = try? await env.customFeeds.update(existing, title: title, url: url, category: category)
            } else {
                _ = try? await env.customFeeds.add(title: title, url: url, category: category)
            }
            env.feed.settingsDidChange()
            dismiss()
        }
    }
}
