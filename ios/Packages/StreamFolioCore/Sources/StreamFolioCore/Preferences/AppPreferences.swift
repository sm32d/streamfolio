import Foundation
import Observation

/// The app's preferences, observable by SwiftUI. Reads and writes go through `Preference`s, so
/// every screen, the repository and backup see the same keys, defaults and ranges.
///
/// Observation is deliberately coarse: any write bumps `revision`, and any read depends on it.
/// Preferences change rarely, so re-evaluating the views that read them is cheaper than a property
/// per key to keep in sync.
@Observable
public final class AppPreferences: @unchecked Sendable {   // all state is in thread-safe UserDefaults
    @ObservationIgnored private let defaults: UserDefaults
    private var revision = 0

    public init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    public subscript<Value>(key: Preference<Value>) -> Value {
        get {
            _ = revision   // register the dependency
            return key.read(defaults)
        }
        set {
            key.write(newValue, to: defaults)
            revision += 1
        }
    }

    /// Whether `key` has ever been written (distinguishes "default" from "chosen").
    public func hasValue<Value>(for key: Preference<Value>) -> Bool {
        defaults.object(forKey: key.name) != nil
    }
}
