import SwiftUI
import StreamFolioCore
import StreamFolioTokens
import StreamFolioUI

/// What a left/right swipe on a story does (Android `SettingsGesturesScreen`).
struct SettingsGesturesScreen: View {
    @Environment(AppEnvironment.self) private var env

    private static let options: [(value: String, title: String)] = [
        ("bookmark", "Toggle Bookmark"), ("share", "Share Article"), ("read", "Toggle Read Status"), ("none", "None / Disable"),
    ]

    var body: some View {
        let prefs = env.preferences
        SFSettingsPage {
            SFHeroBanner("Swipe Gesture Engine", subtitle: "Custom quick actions for article feed cards", systemImage: "hand.draw.fill")

            SFMenuField("Swipe Left Action", systemImage: "chevron.left", options: Self.options, selection: prefs.binding(Prefs.swipeLeftAction))
            SFMenuField("Swipe Right Action", systemImage: "chevron.right", options: Self.options, selection: prefs.binding(Prefs.swipeRightAction))

            SFInfoBanner("Swiping actions are responsive and trigger instantly with subtle haptic feedback.")
        }
        .sensoryFeedback(.selection, trigger: prefs[Prefs.swipeLeftAction])
        .sensoryFeedback(.selection, trigger: prefs[Prefs.swipeRightAction])
        .navigationTitle("Swipe Gestures")
        .navigationBarTitleDisplayMode(.inline)
        .reportsScrollAtTop()
    }
}
