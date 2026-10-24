import Foundation

/// A curated feed entry, for Settings > Providers.
public struct CuratedProvider: Sendable, Hashable, Identifiable {
    public let region: String
    public let category: String
    public let publisherName: String
    public let url: String
    public var id: String { "\(region)|\(category)|\(url)" }
}

/// Curated RSS/Atom feeds per region and category (`DefaultFeedsConfig` on Android).
///
/// The data lives in `Resources/curated-feeds.json`, generated from the Kotlin source by
/// `ios/scripts/generate_curated_feeds.py`; `CuratedFeedsParityTests` fails if the two drift.
public struct CuratedFeeds: Sendable {

    private struct File: Decodable {
        struct Region: Decodable {
            struct Category: Decodable { let name: String; let urls: [String] }
            let code: String
            let categories: [Category]
        }
        struct Rule: Decodable { let match: [String]; let name: String }
        let regions: [Region]
        let publisherRules: [Rule]
    }

    private let regions: [File.Region]
    private let rules: [File.Rule]

    public static let unknownSource = "Unknown Source"

    /// The bundled catalog. Traps only if the shipped resource is missing or corrupt, which the
    /// unit tests would catch.
    public static let shared: CuratedFeeds = {
        guard let url = Bundle.module.url(forResource: "curated-feeds", withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let file = try? JSONDecoder().decode(File.self, from: data)
        else { fatalError("curated-feeds.json is missing or invalid") }
        return CuratedFeeds(regions: file.regions, rules: file.publisherRules)
    }()

    private init(regions: [File.Region], rules: [File.Rule]) {
        self.regions = regions
        self.rules = rules
    }

    /// Feed URLs for `region`/`category`, minus feeds the user disabled, plus feeds from other
    /// regions the user explicitly enabled. Unknown regions fall back to US.
    public func feeds(
        region: String,
        category: String,
        disabledFeedUrls: Set<String> = [],
        enabledCrossRegionFeeds: Set<String> = []
    ) -> [String] {
        let upper = region.uppercased()
        let regionEntry = regions.first { $0.code == upper } ?? regions.first { $0.code == "US" }
        let standard = regionEntry?.categories.first { $0.name == category }?.urls ?? []
        let active = standard.filter { !disabledFeedUrls.contains("\(upper)|\(category)|\($0)") }

        var crossRegion: [String] = []
        for other in regions where other.code != upper {
            for url in other.categories.first(where: { $0.name == category })?.urls ?? []
            where enabledCrossRegionFeeds.contains("\(other.code)|\(category)|\(url)") {
                crossRegion.append(url)
            }
        }
        return (active + crossRegion).uniqued()
    }

    /// Canonical publisher name for a feed or article URL, in Android's first-match-wins order.
    public func publisherName(for url: String) -> String {
        for rule in rules where rule.match.contains(where: { url.contains($0) }) {
            return rule.name
        }
        return Self.unknownSource
    }

    public func allProviders() -> [CuratedProvider] {
        regions.flatMap { region in
            region.categories.flatMap { category in
                category.urls.map {
                    CuratedProvider(region: region.code, category: category.name, publisherName: publisherName(for: $0), url: $0)
                }
            }
        }
    }

    /// Region codes in catalog order.
    public var regionCodes: [String] { regions.map(\.code) }
}

extension Sequence where Element: Hashable {
    func uniqued() -> [Element] {
        var seen = Set<Element>()
        return filter { seen.insert($0).inserted }
    }
}
