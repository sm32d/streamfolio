import SwiftUI
import StreamFolioCore
import StreamFolioTokens
import StreamFolioUI

/// Switch individual curated publishers on or off, by region or by category (Android
/// `SettingsProvidersScreen`). The on/off rules live in `ProviderSelection`.
struct SettingsProvidersScreen: View {
    @Environment(AppEnvironment.self) private var env
    @State private var groupByCategory = false
    @State private var regionFilter: String?
    @State private var categoryFilter: String?

    /// Link-only providers are listed only while the remote source list enables them.
    private var providers: [CuratedProvider] {
        CuratedFeeds.shared.allProviders().filter { env.linkOnlyState.isUrlAllowed($0.url) }
    }

    var body: some View {
        let all = providers
        SFSettingsPage(isLazy: true) {
            SFHeroBanner("Default Feed Publishers", subtitle: "Customize active news sources per region & category", systemImage: "square.stack.3d.up.fill")

            VStack(alignment: .leading, spacing: SFSpacing.s8) {
                SFSegmentedControl(
                    options: [.init(false, "Group by Region"), .init(true, "Group by Category")],
                    selection: $groupByCategory
                )
                filters(all)
            }

            if groupByCategory { byCategory(all) } else { byRegion(all) }
        }
        .navigationTitle("Default Feed Providers")
        .navigationBarTitleDisplayMode(.inline)
    }

    // MARK: Filters

    @ViewBuilder
    private func filters(_ all: [CuratedProvider]) -> some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: SFSpacing.s4) {
                if groupByCategory {
                    SFSelectableChip("All Categories", isSelected: categoryFilter == nil) { categoryFilter = nil }
                    ForEach(NewsCategory.all, id: \.self) { category in
                        SFSelectableChip(
                            LocalizedStringKey(category), systemImage: SFCategoryIcon.systemName(for: category),
                            isSelected: categoryFilter == category
                        ) { categoryFilter = categoryFilter == category ? nil : category }
                    }
                } else {
                    SFSelectableChip("All Regions", isSelected: regionFilter == nil) { regionFilter = nil }
                    ForEach(sortedRegions(all), id: \.self) { code in
                        SFSelectableChip(LocalizedStringKey(RegionCatalog.info(for: code).name), isSelected: regionFilter == code) {
                            regionFilter = regionFilter == code ? nil : code
                        }
                    }
                }
            }
        }
    }

    private func sortedRegions(_ all: [CuratedProvider]) -> [String] {
        ProviderSelection.sortedRegions(Set(all.map(\.region)), active: env.providers.activeRegion)
    }

    // MARK: Groupings

    @ViewBuilder
    private func byRegion(_ all: [CuratedProvider]) -> some View {
        ForEach(sortedRegions(all).filter { regionFilter == nil || $0 == regionFilter }, id: \.self) { code in
            let inRegion = all
                .filter { $0.region == code && (categoryFilter == nil || $0.category == categoryFilter) }
                .sorted { $0.category < $1.category }
            if !inRegion.isEmpty {
                let isActive = code == env.providers.activeRegion
                VStack(alignment: .leading, spacing: SFSpacing.s5) {
                    HStack {
                        Text(RegionCatalog.info(for: code).formattedName).font(SFFont.sectionTitle).foregroundStyle(SFColor.primary)
                        Spacer()
                        Text(isActive ? "Active Region" : "Inactive")
                            .font(SFFont.micro)
                            .foregroundStyle(isActive ? SFColor.primary : SFColor.textFaint)
                            .padding(.horizontal, SFSpacing.s4).padding(.vertical, SFSpacing.s2)
                            .background(isActive ? SFColor.primary.opacity(0.15) : SFColor.onSurface.opacity(0.05), in: .rect(cornerRadius: SFRadius.sm))
                    }
                    .accessibilityAddTraits(.isHeader)
                    SFSettingsCard {
                        ForEach(inRegion) { ProviderRow(provider: $0, detail: .category) }
                    }
                }
            }
        }
    }

    @ViewBuilder
    private func byCategory(_ all: [CuratedProvider]) -> some View {
        let categories = Set(all.map(\.category)).sorted().filter { categoryFilter == nil || $0 == categoryFilter }
        ForEach(categories, id: \.self) { category in
            let inCategory = all
                .filter { $0.category == category && (regionFilter == nil || $0.region == regionFilter) }
                .sorted { $0.publisherName < $1.publisherName }
            if !inCategory.isEmpty {
                VStack(alignment: .leading, spacing: SFSpacing.s5) {
                    Label(category, systemImage: SFCategoryIcon.systemName(for: category))
                        .font(SFFont.sectionTitle).foregroundStyle(SFColor.primary)
                        .accessibilityAddTraits(.isHeader)
                    SFSettingsCard {
                        ForEach(inCategory) { ProviderRow(provider: $0, detail: .region) }
                    }
                }
            }
        }
    }
}

private struct ProviderRow: View {
    enum Detail { case category, region }

    let provider: CuratedProvider
    let detail: Detail

    @Environment(AppEnvironment.self) private var env

    var body: some View {
        let isOn = env.providers.isEnabled(provider)
        Toggle(isOn: Binding(get: { isOn }, set: set)) {
            HStack(spacing: SFSpacing.s7) {
                SFPublisherAvatar(name: provider.publisherName, size: SFSpacing.thumbSmall)
                VStack(alignment: .leading, spacing: SFSpacing.s1) {
                    Text(provider.publisherName).font(SFFont.cardTitle.weight(.bold)).foregroundStyle(SFColor.onSurface)
                    switch detail {
                    case .category:
                        Label(provider.category, systemImage: SFCategoryIcon.systemName(for: provider.category))
                            .font(SFFont.meta).foregroundStyle(SFColor.textMuted)
                    case .region:
                        Text(RegionCatalog.info(for: provider.region).formattedName).font(SFFont.meta).foregroundStyle(SFColor.textMuted)
                    }
                }
            }
            .opacity(isOn ? 1 : 0.6)
        }
        .tint(SFColor.primary)
        .padding(.horizontal, SFSpacing.cardPaddingLarge)
        .padding(.vertical, SFSpacing.s6)
    }

    private func set(_ enabled: Bool) {
        // Turning a publisher on fetches it now so it shows up without waiting for the next refresh.
        guard env.providers.setEnabled(enabled, for: provider) else { return }
        Task { await env.repository.refreshSingleFeed(url: provider.url, category: provider.category) }
    }
}
