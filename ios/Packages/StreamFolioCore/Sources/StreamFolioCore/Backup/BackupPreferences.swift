import Foundation

/// How a preference travels in a backup file. A value is accepted from a file only if it is valid
/// for its `Preference`: scalars outside the allowed range are rejected (not clamped), exactly like
/// Android's `BackupHelper`; collections are bounded by the preference's own normalisation.
protocol BackupCodable: PreferenceValue, Equatable {
    /// True for single values that must already be valid (a font size of 99 is ignored, not clamped).
    static var mustBeValidAsIs: Bool { get }
    static func fromJSON(_ value: Any) -> Self?
    var jsonValue: Any { get }
}

private func number(_ value: Any) -> NSNumber? {
    guard let number = value as? NSNumber, CFGetTypeID(number) != CFBooleanGetTypeID() else { return nil }
    return number
}

extension Bool: BackupCodable {
    static var mustBeValidAsIs: Bool { true }
    static func fromJSON(_ value: Any) -> Bool? {
        guard let number = value as? NSNumber, CFGetTypeID(number) == CFBooleanGetTypeID() else { return nil }
        return number.boolValue
    }
    var jsonValue: Any { self }
}

extension String: BackupCodable {
    static var mustBeValidAsIs: Bool { true }
    static func fromJSON(_ value: Any) -> String? { value as? String }
    var jsonValue: Any { self }
}

extension Int: BackupCodable {
    static var mustBeValidAsIs: Bool { true }
    static func fromJSON(_ value: Any) -> Int? {
        guard let n = number(value), n.doubleValue == n.doubleValue.rounded(), abs(n.doubleValue) < 1e15 else { return nil }
        return n.intValue
    }
    var jsonValue: Any { self }
}

extension Int64: BackupCodable {
    static var mustBeValidAsIs: Bool { true }
    static func fromJSON(_ value: Any) -> Int64? {
        guard let n = number(value), n.doubleValue == n.doubleValue.rounded(), abs(n.doubleValue) < 1e15 else { return nil }
        return n.int64Value
    }
    var jsonValue: Any { NSNumber(value: self) }
}

extension Double: BackupCodable {
    static var mustBeValidAsIs: Bool { true }
    static func fromJSON(_ value: Any) -> Double? { number(value)?.doubleValue }
    var jsonValue: Any { self }
}

extension Array<String>: BackupCodable {
    static var mustBeValidAsIs: Bool { false }
    static func fromJSON(_ value: Any) -> [String]? { (value as? [Any])?.compactMap { $0 as? String } }
    var jsonValue: Any { self }
}

extension Set<String>: BackupCodable {
    static var mustBeValidAsIs: Bool { false }
    static func fromJSON(_ value: Any) -> Set<String>? { (value as? [Any]).map { Set($0.compactMap { $0 as? String }) } }
    var jsonValue: Any { sorted() }
}

/// The preferences a backup carries, keyed by the names in the backup file (Android's, see
/// `docs/04`). One table drives both export and import, so they cannot drift apart.
enum BackupPreferences {
    struct Entry: Sendable {
        let jsonKey: String
        let export: @Sendable (AppPreferences) -> Any
        let apply: @Sendable (AppPreferences, Any) -> Void
    }

    private static func entry<V: BackupCodable>(_ jsonKey: String, _ preference: Preference<V>) -> Entry {
        Entry(
            jsonKey: jsonKey,
            export: { $0[preference].jsonValue },
            apply: { preferences, raw in
                guard let value = V.fromJSON(raw) else { return }
                let valid = preference.validated(value)
                if V.mustBeValidAsIs && valid != value { return }
                preferences[preference] = valid
            }
        )
    }

    static let all: [Entry] = [
        entry("language", Prefs.language),
        entry("region", Prefs.region),
        entry("selected_categories", Prefs.selectedCategories),
        entry("is_default_feeds_enabled", Prefs.defaultFeedsEnabled),
        entry("category_order", Prefs.categoryOrder),
        entry("disabled_feed_urls", Prefs.disabledFeedUrls),
        entry("enabled_cross_region_feeds", Prefs.enabledCrossRegionFeeds),
        entry("cache_history_days", Prefs.cacheHistoryDays),
        entry("reader_font_family", Prefs.readerFontFamily),
        entry("reader_font_size", Prefs.readerFontSize),
        entry("reader_line_spacing", Prefs.readerLineSpacing),
        entry("is_ai_enabled", Prefs.aiEnabled),
        entry("is_translation_enabled", Prefs.aiTranslationEnabled),
        entry("is_summary_enabled", Prefs.aiSummaryEnabled),
        entry("is_smart_tags_enabled", Prefs.aiSmartTagsEnabled),
        entry("has_seen_ai_spotlight", Prefs.hasSeenAiSpotlight),
        entry("swipe_left_action", Prefs.swipeLeftAction),
        entry("swipe_right_action", Prefs.swipeRightAction),
        entry("tts_speech_rate", Prefs.ttsSpeechRate),
        entry("use_dynamic_colors", Prefs.useDynamicColors),
        entry("group_similar_stories", Prefs.groupSimilarStories),
        entry("bookmark_list_view_mode_is_grid", Prefs.bookmarkListViewIsGrid),
        entry("last_selected_bookmark_list_id", Prefs.lastSelectedBookmarkListId),
        entry("has_seen_swipe_hint", Prefs.hasSeenSwipeHint),
        entry("recent_searches", Prefs.recentSearches),
    ]
}
