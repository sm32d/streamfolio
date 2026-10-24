import Foundation

/// The remote source list: a static JSON file in the project repository (no server). Shared with
/// Android — withdrawing a source there withdraws it here. Format:
/// `{"version":1,"sources":{"<source id>":{"enabled":true}}}`. A source missing from the file, or
/// with `enabled` false, is treated as withdrawn.
public enum SourceConfig {
    public static let url = URL(string: "https://raw.githubusercontent.com/sm32d/streamfolio/master/source-config/sources.json")!

    /// How long one validated download stays fresh. It only coalesces a burst of refreshes into one
    /// download; a link-only feed is never fetched unless the file was validated within this window.
    public static let freshnessMs: Int64 = 60 * 1000

    /// Ids of enabled sources, or nil when `data` is not a valid source list (the caller then keeps
    /// its last known state and link-only fetching stays suspended).
    public static func parseEnabledIds(_ data: Data) -> Set<String>? {
        guard let root = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any],
              let sources = root["sources"] as? [String: Any] else { return nil }
        var enabled: Set<String> = []
        for (id, value) in sources {
            if let entry = value as? [String: Any], (entry["enabled"] as? Bool) == true { enabled.insert(id) }
        }
        return enabled
    }

    public static func parseEnabledIds(_ json: String) -> Set<String>? {
        parseEnabledIds(Data(json.utf8))
    }
}
