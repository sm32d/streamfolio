import SwiftUI
import StreamFolioCore

extension AppPreferences {
    /// A SwiftUI binding to one preference. Reads register with Observation, so the view updates when
    /// the value changes anywhere (including a backup restore).
    func binding<Value>(_ key: Preference<Value>) -> Binding<Value> {
        Binding(get: { self[key] }, set: { self[key] = $0 })
    }

    /// Same, but runs `afterChange` after every change (e.g. to refresh Home). SwiftUI writes bindings
    /// from the main actor, which is where `afterChange` runs.
    func binding<Value>(_ key: Preference<Value>, afterChange: @escaping @MainActor @Sendable () -> Void) -> Binding<Value> {
        Binding(get: { self[key] }, set: { self[key] = $0; MainActor.assumeIsolated { afterChange() } })
    }
}
