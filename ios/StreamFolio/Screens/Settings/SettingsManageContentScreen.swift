import SwiftUI
import StreamFolioCore
import StreamFolioTokens
import StreamFolioUI

/// Curated feeds on/off and which categories to follow, plus links to reorder tabs and choose
/// publishers (Android `SettingsManageContentScreen`).
struct SettingsManageContentScreen: View {
    @Environment(AppEnvironment.self) private var env

    var body: some View {
        let prefs = env.preferences
        SFSettingsPage {
            SFHeroBanner("Feed Composition & Sources", subtitle: "Configure default news categories & custom RSS curation", systemImage: "square.grid.2x2.fill")

            SFSettingsSection("Curated Feed Categories") {
                CuratedFeedsPicker(
                    enabled: prefs.binding(Prefs.defaultFeedsEnabled) { env.feed.settingsDidChange() },
                    categories: prefs.binding(Prefs.selectedCategories) { env.feed.settingsDidChange() }
                )
            }

            SFSettingsSection("Advanced Curation") {
                SFSettingsCard {
                    SettingsLink(route: .reorder, systemImage: "arrow.up.arrow.down", tint: SFColor.secondary,
                                 title: "Reorder Category Tabs", subtitle: "Adjust the position and priority of your category tabs")
                    SettingsLink(route: .providers, systemImage: "square.stack.3d.up.fill", tint: SFColor.tertiary,
                                 title: "Default Feed Providers", subtitle: "Enable or disable curated news publishers")
                }
            }
        }
        .navigationTitle("Manage Content & Sources")
        .navigationBarTitleDisplayMode(.inline)
        .reportsScrollAtTop()
    }
}
