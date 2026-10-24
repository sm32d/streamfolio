import Foundation

/// Which curated publishers are switched on (Settings > Default Feed Providers).
///
/// A provider belongs to a region and a category. In the user's own region every provider is on unless
/// listed in `disabled_feed_urls`; in any other region it is off unless listed in
/// `enabled_cross_region_feeds`. Both sets hold `"REGION|Category|url"` keys, exactly as Android does,
/// and `CuratedFeeds.feeds` reads the same sets — so what is toggled here is what is fetched.
public struct ProviderSelection: Sendable {
    private let preferences: AppPreferences

    public init(preferences: AppPreferences) {
        self.preferences = preferences
    }

    public var activeRegion: String { preferences[Prefs.region].uppercased() }

    public func isActiveRegion(_ provider: CuratedProvider) -> Bool { provider.region == activeRegion }

    public func isEnabled(_ provider: CuratedProvider) -> Bool {
        isActiveRegion(provider)
            ? !preferences[Prefs.disabledFeedUrls].contains(provider.id)
            : preferences[Prefs.enabledCrossRegionFeeds].contains(provider.id)
    }

    /// Switches a provider. Returns true when it was just turned on (the caller fetches it now).
    @discardableResult
    public func setEnabled(_ enabled: Bool, for provider: CuratedProvider) -> Bool {
        let wasEnabled = isEnabled(provider)
        if isActiveRegion(provider) {
            var disabled = preferences[Prefs.disabledFeedUrls]
            if enabled { disabled.remove(provider.id) } else { disabled.insert(provider.id) }
            preferences[Prefs.disabledFeedUrls] = disabled
        } else {
            var extra = preferences[Prefs.enabledCrossRegionFeeds]
            if enabled { extra.insert(provider.id) } else { extra.remove(provider.id) }
            preferences[Prefs.enabledCrossRegionFeeds] = extra
        }
        return enabled && !wasEnabled
    }

    /// Region codes with the user's own region first, the rest by country name.
    public static func sortedRegions(_ regions: some Sequence<String>, active: String) -> [String] {
        regions.sorted { lhs, rhs in
            switch (lhs == active, rhs == active) {
            case (true, false): true
            case (false, true): false
            default: RegionCatalog.info(for: lhs).name < RegionCatalog.info(for: rhs).name
            }
        }
    }
}
