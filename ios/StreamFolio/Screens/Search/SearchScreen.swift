import SwiftUI
import StreamFolioCore
import StreamFolioTokens
import StreamFolioUI

/// Search: trending topics and recent searches while idle, live results as you type.
struct SearchScreen: View {
    @Environment(AppEnvironment.self) private var env
    @State private var path: [Article] = []
    @State private var confirmingClear = false
    @Namespace private var zoom

    var body: some View {
        @Bindable var search = env.search
        NavigationStack(path: $path) {
            content(search)
                .background { SFBackdrop() }
                .navigationTitle("Search")
                .searchable(text: $search.query, prompt: "Search articles and topics")
                .onSubmit(of: .search) { search.remember(search.query) }
                .navigationDestination(for: Article.self) { article in
                    ArticleDetailScreen(reader: env.makeReader(for: article))
                        .navigationTransition(.zoom(sourceID: article.link, in: zoom))
                }
                .overlay(alignment: .top) {
                    if search.isSearching && search.phase != .idle {
                        ProgressView().progressViewStyle(.linear).tint(SFColor.primary).padding(.horizontal, SFSpacing.gutter)
                    }
                }
        }
        .task { await search.refreshTrending() }
        .confirmationDialog("Clear recent searches?", isPresented: $confirmingClear, titleVisibility: .visible) {
            Button("Clear", role: .destructive) { search.clearRecents() }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This will permanently remove all recent searches.")
        }
    }

    @ViewBuilder
    private func content(_ search: SearchStore) -> some View {
        switch search.phase {
        case .idle: idle(search)
        case .loading:
            ProgressView().controlSize(.large).frame(maxWidth: .infinity, maxHeight: .infinity)
        case .noResults:
            ScrollView {
                SFEmptyState(illustration: Image("OtterNoSearch"), title: "No articles found", message: "Try a different keyword or broaden your search.")
                    .padding(.top, SFSpacing.s12)
            }
        case .results:
            List {
                ForEach(search.results) { article in
                    StoryRow(group: ArticleGroup(primary: article), zoom: zoom) { path.append($0) }
                }
            }
            .listStyle(.plain)
            .scrollContentBackground(.hidden)
            .reportsScrollAtTop()
        }
    }

    private func idle(_ search: SearchStore) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: SFSpacing.s8) {
                SFSectionHeader("Trending Topics")
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: SFSpacing.s4) {
                        ForEach(search.trending, id: \.self) { topic in
                            Button { search.choose(topic) } label: {
                                Label(topic, systemImage: "chart.line.uptrend.xyaxis")
                                    .font(SFFont.bodySmall.weight(.medium))
                                    .foregroundStyle(SFColor.onSurface)
                                    .padding(.horizontal, SFSpacing.s7).padding(.vertical, SFSpacing.s5)
                                    .background(SFColor.surfaceVariant.opacity(0.5), in: .capsule)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
                .scrollClipDisabled()

                if search.recentSearches.isEmpty {
                    SFEmptyState(illustration: Image("OtterSearchWelcome"), title: "Start searching", message: "Type a keyword to find articles and topics.")
                        .padding(.top, SFSpacing.s8)
                } else {
                    HStack {
                        SFSectionHeader("Recent Searches")
                        Spacer()
                        Button("Clear All") { confirmingClear = true }
                            .font(SFFont.bodySmall.weight(.bold))
                    }
                    ForEach(search.recentSearches, id: \.self) { term in
                        HStack(spacing: SFSpacing.s7) {
                            Button { search.choose(term) } label: {
                                Label(term, systemImage: "clock.arrow.circlepath")
                                    .font(SFFont.body)
                                    .foregroundStyle(SFColor.onSurface)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                    .padding(.vertical, SFSpacing.s5)
                                    .contentShape(.rect)
                            }
                            .buttonStyle(.plain)
                            Button { search.removeRecent(term) } label: {
                                Image(systemName: "xmark").font(SFFont.meta).foregroundStyle(SFColor.textFaint)
                            }
                            .buttonStyle(.plain)
                            .accessibilityLabel("Remove \(term)")
                        }
                    }
                }
            }
            .padding(.horizontal, SFSpacing.gutter)
            .padding(.vertical, SFSpacing.s6)
        }
        .reportsScrollAtTop()
    }
}
