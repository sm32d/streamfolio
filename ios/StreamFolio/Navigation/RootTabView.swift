import SwiftUI
import StreamFolioUI

enum AppTab: Hashable {
    case home, saved, settings, search
}

/// Root navigation. The Android app draws its own floating pill (`GlassmorphicNavBar`); on iOS the
/// system `TabView` is the Liquid Glass tab bar, so there is nothing custom to maintain.
/// Search uses the search tab role so it sits apart at the trailing edge, the iOS convention.
struct RootTabView: View {
    @Environment(AppEnvironment.self) private var env
    @State private var selection: AppTab = .home

    var body: some View {
        TabView(selection: $selection) {
            Tab("Home", systemImage: "house.fill", value: AppTab.home) {
                HomeScreen()
            }
            Tab("Saved", systemImage: "bookmark.fill", value: AppTab.saved) {
                SavedScreen()
            }
            Tab("Settings", systemImage: "gearshape.fill", value: AppTab.settings) {
                SettingsScreen()
            }
            Tab(value: AppTab.search, role: .search) {
                SearchScreen()
            }
        }
        // Minimize on scroll down, but always fully expanded while the content is at the top.
        .tabBarMinimizeBehavior(env.chrome.isAtTop ? .never : .onScrollDown)
        .onChange(of: selection) { env.chrome.isAtTop = true }
        // The TTS mini player becomes `tabViewBottomAccessory` in Phase 4. It is not shown until it can
        // hide itself when idle (`tabViewBottomAccessory(isEnabled:)` needs iOS 26.1).
    }
}
