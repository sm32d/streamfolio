import SwiftUI
import StreamFolioCore
import StreamFolioTokens
import StreamFolioUI

/// The Home feed: category chips, a trending carousel, then the latest stories. Behavior and data
/// come from `FeedStore`; this view only lays them out and routes taps.
struct HomeScreen: View {
    @Environment(AppEnvironment.self) private var env
    @State private var path: [Article] = []
    @Namespace private var zoom

    var body: some View {
        let feed = env.feed
        NavigationStack(path: $path) {
            content(feed)
                .background { SFBackdrop() }
                .navigationTitle("StreamFolio")
                .toolbar { regionMenu }
                .safeAreaInset(edge: .top, spacing: 0) {
                    SFCategoryChips(
                        items: feed.categories.map { .init(id: $0, title: LocalizedStringKey($0)) },
                        selection: Binding(get: { feed.category }, set: { feed.select($0) })
                    )
                }
                .navigationDestination(for: Article.self) { article in
                    ArticleDetailScreen(reader: env.makeReader(for: article))
                        .navigationTransition(.zoom(sourceID: article.link, in: zoom))
                }
        }
        .task { feed.start() }
        .onChange(of: env.preferences[Prefs.groupSimilarStories]) { feed.regroup() }
    }

    // MARK: Content

    @ViewBuilder
    private func content(_ feed: FeedStore) -> some View {
        if !env.preferences[Prefs.defaultFeedsEnabled] && !feed.hasLoaded {
            emptyState(title: "No sources enabled", message: "Enable the curated feeds in Settings to start reading.")
        } else if !feed.hasLoaded || (feed.isRefreshing && feed.allGroups.isEmpty) {
            ScrollView { SFSkeletonList().padding(.top, SFSpacing.s6) }
        } else if feed.isEmpty {
            ScrollView {
                emptyState(
                    title: feed.isRefreshing ? "Loading…" : "No articles available",
                    message: "Pull down to refresh, or choose another category."
                )
                .padding(.top, SFSpacing.s12)
            }
            .refreshable { await feed.refreshNow() }
        } else {
            storyList(feed)
        }
    }

    private func emptyState(title: LocalizedStringKey, message: LocalizedStringKey) -> some View {
        SFEmptyState(illustration: Image("OtterNoArticles"), title: title, message: message)
    }

    private func storyList(_ feed: FeedStore) -> some View {
        List {
            if !feed.trending.isEmpty {
                Section {
                    trendingCarousel(feed.trending)
                        .listRowInsets(EdgeInsets())
                        .listRowBackground(Color.clear)
                        .listRowSeparator(.hidden)
                } header: {
                    SFSectionHeader("Trending Highlights").padding(.leading, SFSpacing.gutter - SFSpacing.s8)
                }
            }
            Section {
                ForEach(feed.stories) { group in
                    StoryRow(group: group, zoom: zoom, open: open)
                }
                if feed.hasMoreStories {
                    ProgressView()
                        .frame(maxWidth: .infinity)
                        .listRowBackground(Color.clear)
                        .listRowSeparator(.hidden)
                        .onAppear { feed.loadMore() }
                }
            } header: {
                SFSectionHeader("Latest Stories").padding(.leading, SFSpacing.gutter - SFSpacing.s8)
            }
        }
        .listStyle(.plain)
        .scrollContentBackground(.hidden)
        .refreshable { await feed.refreshNow() }
        .reportsScrollAtTop()
    }

    private func trendingCarousel(_ groups: [ArticleGroup]) -> some View {
        ScrollView(.horizontal, showsIndicators: false) {
            LazyHStack(spacing: SFSpacing.s8) {
                ForEach(groups) { group in
                    Button { open(group.primary) } label: {
                        SFTrendingCard(article: group.primary, isSaved: group.primary.isBookmarked)
                    }
                    .buttonStyle(.plain)
                    .containerRelativeFrame(.horizontal)
                    .matchedTransitionSource(id: group.primary.link, in: zoom)
                    .contextMenu { ArticleMenu(article: group.primary) }
                }
            }
            .scrollTargetLayout()
        }
        .safeAreaPadding(.horizontal, SFSpacing.gutter)
        .scrollTargetBehavior(.viewAligned)
        .padding(.vertical, SFSpacing.s4)
    }

    private var regionMenu: some ToolbarContent {
        ToolbarItem(placement: .topBarTrailing) {
            Menu {
                Picker("Region", selection: Binding(
                    get: { env.preferences[Prefs.region] },
                    set: { changeRegion(to: $0) }
                )) {
                    ForEach(RegionCatalog.all) { Text($0.formattedName).tag($0.code) }
                }
            } label: {
                Text(RegionCatalog.info(for: env.preferences[Prefs.region]).flag)
                    .accessibilityLabel("Region: \(RegionCatalog.info(for: env.preferences[Prefs.region]).name)")
            }
        }
    }

    // MARK: Actions

    private func open(_ article: Article) {
        path.append(article)
    }

    private func changeRegion(to code: String) {
        env.preferences[Prefs.region] = code
        Task { await env.feed.refreshNow() }
    }
}
