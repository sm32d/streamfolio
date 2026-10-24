import Foundation

/// Which category tabs Home shows and in what order. One definition used by Home (`FeedStore`), the
/// Reorder screen and Manage Content, so what you arrange in Settings is exactly what Home draws.
/// Port of the `categories` computation in Android's `HomeScreen` / `SettingsReorderScreen`.
public enum CategoryLayout {
    /// - Curated categories appear only while curated feeds are on, filtered by the user's selection
    ///   (never empty: falls back to "Top Stories").
    /// - Each custom feed's tab label is appended (once).
    /// - The result is stably sorted by the saved order; categories missing from it go last.
    public static func categories(
        defaultFeedsEnabled: Bool,
        selected: Set<String>,
        order: [String],
        customCategories: [String]
    ) -> [String] {
        let curated: [String]
        if defaultFeedsEnabled {
            let chosen = NewsCategory.all.filter(selected.contains)
            curated = chosen.isEmpty ? [NewsCategory.all[0]] : chosen
        } else {
            curated = []
        }
        let raw = (curated + customCategories).uniqued()
        let rank = Dictionary(order.enumerated().map { ($1, $0) }, uniquingKeysWith: { first, _ in first })
        // Swift's sort is not guaranteed stable for ties; sort by (rank, original position).
        return raw.enumerated()
            .sorted { lhs, rhs in
                let (l, r) = (rank[lhs.element] ?? Int.max, rank[rhs.element] ?? Int.max)
                return l != r ? l < r : lhs.offset < rhs.offset
            }
            .map(\.element)
    }

    /// The layout for the current preferences and custom feeds.
    public static func categories(preferences: AppPreferences, customFeeds: [CustomFeed]) -> [String] {
        categories(
            defaultFeedsEnabled: preferences[Prefs.defaultFeedsEnabled],
            selected: preferences[Prefs.selectedCategories],
            order: preferences[Prefs.categoryOrder],
            customCategories: customFeeds.map(\.category)
        )
    }
}
