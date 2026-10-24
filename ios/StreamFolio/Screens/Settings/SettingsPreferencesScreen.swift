import SwiftUI
import StreamFolioCore
import StreamFolioTokens
import StreamFolioUI

/// Region, offline retention and story grouping (Android `SettingsPreferencesScreen`). Dynamic Colors
/// (Material You) has no iOS counterpart, so its row is not shown; the stored value is kept for backups.
struct SettingsPreferencesScreen: View {
    @Environment(AppEnvironment.self) private var env

    var body: some View {
        let prefs = env.preferences
        SFSettingsPage {
            SFHeroBanner("Reading & Regional Profile", subtitle: "Tailor news sources and retention", systemImage: "slider.horizontal.3")

            // Both fields carry their own labels.
            RegionField(region: prefs.binding(Prefs.region) { env.feed.settingsDidChange(regionChanged: true) })
            CacheRetentionField(days: prefs.binding(Prefs.cacheHistoryDays))

            SFSettingsSection("Feed Curation") {
                SFSettingsCard {
                    SFSettingsToggleRow(
                        systemImage: "square.3.layers.3d", tint: SFColor.secondary,
                        title: "Group Similar Stories", subtitle: "Consolidate duplicate stories from different sources",
                        isOn: prefs.binding(Prefs.groupSimilarStories)
                    )
                }
            }

            SFInfoBanner("Changing language or region will refresh your news feed immediately.")
        }
        .navigationTitle("Preferences")
        .navigationBarTitleDisplayMode(.inline)
        .reportsScrollAtTop()
    }
}
