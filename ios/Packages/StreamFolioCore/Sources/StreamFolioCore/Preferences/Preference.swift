import Foundation

/// A typed preference: its storage key, default and valid range, defined exactly once. Settings UI,
/// background tasks and backup import/export all go through these definitions, so key names,
/// defaults and clamping can never disagree. Key names are the Android `SharedPreferences` names
/// (`docs/03` §4), which keeps backups interchangeable.
public struct Preference<Value: PreferenceValue>: Sendable {
    public let name: String
    public let defaultValue: Value
    private let normalize: @Sendable (Value) -> Value

    public init(_ name: String, default defaultValue: Value, normalize: @escaping @Sendable (Value) -> Value = { $0 }) {
        self.name = name
        self.defaultValue = defaultValue
        self.normalize = normalize
    }

    func read(_ defaults: UserDefaults) -> Value {
        guard let stored = Value.read(from: defaults, key: name) else { return defaultValue }
        return normalize(stored)
    }

    func write(_ value: Value, to defaults: UserDefaults) {
        Value.write(normalize(value), to: defaults, key: name)
    }

    /// Clamps/validates a value from outside (a backup file) the same way a write would.
    public func validated(_ value: Value) -> Value { normalize(value) }
}

public protocol PreferenceValue: Sendable {
    static func read(from defaults: UserDefaults, key: String) -> Self?
    static func write(_ value: Self, to defaults: UserDefaults, key: String)
}

extension Bool: PreferenceValue {
    public static func read(from defaults: UserDefaults, key: String) -> Bool? { defaults.object(forKey: key) as? Bool }
    public static func write(_ value: Bool, to defaults: UserDefaults, key: String) { defaults.set(value, forKey: key) }
}

extension String: PreferenceValue {
    public static func read(from defaults: UserDefaults, key: String) -> String? { defaults.string(forKey: key) }
    public static func write(_ value: String, to defaults: UserDefaults, key: String) { defaults.set(value, forKey: key) }
}

extension Int: PreferenceValue {
    public static func read(from defaults: UserDefaults, key: String) -> Int? { defaults.object(forKey: key) as? Int }
    public static func write(_ value: Int, to defaults: UserDefaults, key: String) { defaults.set(value, forKey: key) }
}

extension Int64: PreferenceValue {
    public static func read(from defaults: UserDefaults, key: String) -> Int64? { (defaults.object(forKey: key) as? NSNumber)?.int64Value }
    public static func write(_ value: Int64, to defaults: UserDefaults, key: String) { defaults.set(NSNumber(value: value), forKey: key) }
}

extension Double: PreferenceValue {
    public static func read(from defaults: UserDefaults, key: String) -> Double? { defaults.object(forKey: key) as? Double }
    public static func write(_ value: Double, to defaults: UserDefaults, key: String) { defaults.set(value, forKey: key) }
}

extension Set<String>: PreferenceValue {
    public static func read(from defaults: UserDefaults, key: String) -> Set<String>? { defaults.stringArray(forKey: key).map(Set.init) }
    public static func write(_ value: Set<String>, to defaults: UserDefaults, key: String) { defaults.set(Array(value).sorted(), forKey: key) }
}

extension Array<String>: PreferenceValue {
    public static func read(from defaults: UserDefaults, key: String) -> [String]? { defaults.stringArray(forKey: key) }
    public static func write(_ value: [String], to defaults: UserDefaults, key: String) { defaults.set(value, forKey: key) }
}

/// Every preference the app stores. Mirrors Android `PreferencesHelper`.
public enum Prefs {
    // Onboarding & feed selection
    public static let completedOnboarding = Preference("completed_onboarding", default: false)
    public static let language = Preference("language", default: "en")
    public static let region = Preference("region", default: "US")
    public static let selectedCategories = Preference("selected_categories", default: Set(NewsCategory.all))
    /// Android key name (`google_news_enabled`) kept for backup compatibility; it means "curated feeds on".
    public static let defaultFeedsEnabled = Preference("google_news_enabled", default: true)
    public static let categoryOrder = Preference("category_order", default: NewsCategory.all)
    public static let disabledFeedUrls = Preference("disabled_feed_urls", default: Set<String>())
    public static let enabledCrossRegionFeeds = Preference("enabled_cross_region_feeds", default: Set<String>())
    public static let cacheHistoryDays = Preference("cache_history_days", default: 36_500) { min(max($0, 1), 36_500) }

    // Reader
    public static let readerFontFamily = Preference("reader_font_family", default: "sans_serif") {
        ["sans_serif", "serif"].contains($0) ? $0 : "sans_serif"
    }
    public static let readerFontSize = Preference("reader_font_size", default: 16.0) { min(max($0, 12), 30) }
    public static let readerLineSpacing = Preference("reader_line_spacing", default: 1.5) { min(max($0, 1), 3) }

    // On-device AI
    public static let aiEnabled = Preference("ai_enabled", default: false)
    public static let aiTranslationEnabled = Preference("ai_translation_enabled", default: true)
    public static let aiSummaryEnabled = Preference("ai_summary_enabled", default: true)
    public static let aiSmartTagsEnabled = Preference("ai_smart_tags_enabled", default: true)
    public static let hasSeenAiSpotlight = Preference("has_seen_ai_spotlight", default: false)

    // Gestures, audio, display
    public static let swipeLeftAction = Preference("swipe_left_action", default: "bookmark") { SwipeAction.valid($0, fallback: "bookmark") }
    public static let swipeRightAction = Preference("swipe_right_action", default: "share") { SwipeAction.valid($0, fallback: "share") }
    public static let hasSeenSwipeHint = Preference("has_seen_swipe_hint", default: false)
    public static let ttsSpeechRate = Preference("tts_speech_rate", default: 1.0) { min(max($0, 0.5), 3) }
    /// Material You has no iOS equivalent. Kept so a backup round-trips; ignored by the UI.
    public static let useDynamicColors = Preference("use_dynamic_colors", default: false)
    public static let groupSimilarStories = Preference("group_similar_stories", default: true)
    public static let bookmarkListViewIsGrid = Preference("bookmark_list_view_mode_is_grid", default: true)
    public static let lastSelectedBookmarkListId = Preference("last_selected_bookmark_list_id", default: Int64(-1))

    // Search
    public static let recentSearches = Preference("recent_searches", default: [String]()) { Array($0.prefix(10)) }
}

/// Values accepted for `swipe_left_action` / `swipe_right_action`.
public enum SwipeAction {
    public static let all = ["bookmark", "share", "read", "none"]
    static func valid(_ value: String, fallback: String) -> String { all.contains(value) ? value : fallback }
}
