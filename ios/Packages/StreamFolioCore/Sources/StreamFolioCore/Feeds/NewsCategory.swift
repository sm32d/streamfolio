import Foundation

/// The eight curated news categories, in their default display order. Category names are the
/// strings persisted in `articles.category`, preferences and backups, so they are not localized here.
public enum NewsCategory {
    public static let all: [String] = [
        "Top Stories", "World", "Business", "Technology", "Science", "Sports", "Health", "Entertainment",
    ]
}

/// A supported curated-news region (`RegionUtils` on Android).
public struct RegionInfo: Sendable, Hashable, Identifiable {
    public let code: String
    public let name: String
    public let flag: String
    public var id: String { code }

    public var formattedName: String { "\(flag) \(name)" }
}

public enum RegionCatalog {
    /// Sorted by name, like Android.
    public static let all: [RegionInfo] = [
        RegionInfo(code: "AU", name: "Australia", flag: "🇦🇺"),
        RegionInfo(code: "BR", name: "Brazil", flag: "🇧🇷"),
        RegionInfo(code: "CA", name: "Canada", flag: "🇨🇦"),
        RegionInfo(code: "FR", name: "France", flag: "🇫🇷"),
        RegionInfo(code: "DE", name: "Germany", flag: "🇩🇪"),
        RegionInfo(code: "HK", name: "Hong Kong", flag: "🇭🇰"),
        RegionInfo(code: "IN", name: "India", flag: "🇮🇳"),
        RegionInfo(code: "JP", name: "Japan", flag: "🇯🇵"),
        RegionInfo(code: "SG", name: "Singapore", flag: "🇸🇬"),
        RegionInfo(code: "ZA", name: "South Africa", flag: "🇿🇦"),
        RegionInfo(code: "KR", name: "South Korea", flag: "🇰🇷"),
        RegionInfo(code: "AE", name: "United Arab Emirates", flag: "🇦🇪"),
        RegionInfo(code: "GB", name: "United Kingdom", flag: "🇬🇧"),
        RegionInfo(code: "US", name: "United States", flag: "🇺🇸"),
    ].sorted { $0.name < $1.name }

    public static let fallback = RegionInfo(code: "US", name: "United States", flag: "🇺🇸")

    public static func info(for code: String) -> RegionInfo {
        let upper = code.uppercased()
        return all.first { $0.code == upper } ?? fallback
    }

    /// The region matching the device locale, or US (Android onboarding does the same).
    public static func detected(from locale: Locale = .current) -> RegionInfo {
        guard let code = locale.region?.identifier else { return fallback }
        return all.first { $0.code == code } ?? fallback
    }
}

/// Languages offered for translation/UI preferences (onboarding `TranslationRegionStep`).
public enum LanguageCatalog {
    public static let all: [(code: String, name: String)] = [
        ("en", "English"), ("es", "Español"), ("fr", "Français"),
        ("de", "Deutsch"), ("hi", "हिन्दी"), ("zh", "中文"),
    ]

    public static func name(for code: String) -> String {
        all.first { $0.code == code }?.name ?? "English"
    }

    /// The device language if supported, otherwise English.
    public static func detected(from locale: Locale = .current) -> String {
        guard let code = locale.language.languageCode?.identifier else { return "en" }
        return all.contains { $0.code == code } ? code : "en"
    }
}
