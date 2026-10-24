import Foundation

/// On-device state for link-only sources: the last known remote enabled list and per-feed throttle /
/// conditional-request data. Kept in its own defaults suite (`link_only_state`), never in backups.
/// Port of `LinkOnlyStateStore`; key names are identical.
public final class LinkOnlyStateStore: @unchecked Sendable {   // UserDefaults is thread-safe
    private let defaults: UserDefaults

    private enum Key {
        static let enabled = "enabled_ids"
        static let checkedAt = "config_checked_at"
        static let next = "next_fetch:"
        static let ttl = "ttl_ms:"
        static let etag = "etag:"
        static let lastModified = "last_modified:"
    }

    public init(suiteName: String = "link_only_state") {
        self.defaults = UserDefaults(suiteName: suiteName) ?? .standard
    }

    /// Ids of sources the remote list currently enables. Empty until a list has been fetched.
    public var enabledIds: Set<String> {
        get { Set(defaults.stringArray(forKey: Key.enabled) ?? []) }
        set { defaults.set(Array(newValue).sorted(), forKey: Key.enabled) }
    }

    public var configCheckedAt: Int64 {
        get { Int64(defaults.object(forKey: Key.checkedAt) as? Int ?? 0) }
        set { defaults.set(Int(newValue), forKey: Key.checkedAt) }
    }

    public func isEnabled(_ sourceId: String) -> Bool { enabledIds.contains(sourceId) }

    /// Whether the list was validated recently enough to rely on (a clock set back counts as stale).
    public func isConfigFresh(now: Int64) -> Bool {
        let checked = configCheckedAt
        return checked != 0 && now >= checked && now - checked < SourceConfig.freshnessMs
    }

    /// Whether the source is listed as enabled by the last validated list (for display, e.g.
    /// Settings > Providers). URLs that are not link-only always pass.
    public func isUrlAllowed(_ url: String?) -> Bool {
        guard let source = LinkOnlySources.source(for: url) else { return true }
        return isEnabled(source.id)
    }

    /// Whether a link-only feed may be fetched right now: its source must be enabled by a list that
    /// was validated within `SourceConfig.freshnessMs`. If the list has expired and could not be
    /// refreshed, fetching is suspended until a fresh list is validated.
    public func mayFetch(_ url: String?, now: Int64) -> Bool {
        guard let source = LinkOnlySources.source(for: url) else { return true }
        return isEnabled(source.id) && isConfigFresh(now: now)
    }

    public func ttlMs(_ feedUrl: String) -> Int64 { int64(Key.ttl + feedUrl) }
    public func setTtlMs(_ value: Int64, feedUrl: String) { defaults.set(Int(value), forKey: Key.ttl + feedUrl) }

    public func nextFetchAt(_ feedUrl: String) -> Int64 { int64(Key.next + feedUrl) }
    public func setNextFetchAt(_ value: Int64, feedUrl: String) { defaults.set(Int(value), forKey: Key.next + feedUrl) }

    public func etag(_ feedUrl: String) -> String? { defaults.string(forKey: Key.etag + feedUrl) }
    public func lastModified(_ feedUrl: String) -> String? { defaults.string(forKey: Key.lastModified + feedUrl) }

    public func saveValidators(etag: String?, lastModified: String?, feedUrl: String) {
        defaults.set(etag, forKey: Key.etag + feedUrl)
        defaults.set(lastModified, forKey: Key.lastModified + feedUrl)
    }

    /// Drops all throttle and validator state (used when a source is withdrawn).
    public func clearFeedState() {
        for key in defaults.dictionaryRepresentation().keys
        where [Key.next, Key.etag, Key.lastModified, Key.ttl].contains(where: key.hasPrefix) {
            defaults.removeObject(forKey: key)
        }
    }

    private func int64(_ key: String) -> Int64 { Int64(defaults.object(forKey: key) as? Int ?? 0) }
}
