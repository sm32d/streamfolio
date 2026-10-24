import SwiftUI
import StreamFolioCore
import StreamFolioUI

@main
struct StreamFolioApp: App {
    @State private var environment = AppEnvironment.live()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environment(environment)
                .tint(SFColor.primary)
        }
    }
}

/// Onboarding until the user finishes it, then the app.
struct RootView: View {
    @Environment(AppEnvironment.self) private var env

    var body: some View {
        Group {
            if env.preferences[Prefs.completedOnboarding] {
                RootTabView()
            } else {
                OnboardingScreen()
            }
        }
        .animation(.smooth, value: env.preferences[Prefs.completedOnboarding])
        // Presented once, here, so every list shares them.
        .sheet(item: Bindable(env.interactions).sharing) { ActivityView(items: $0.items).presentationDetents([.medium, .large]) }
        .sheet(item: Bindable(env.interactions).managingCollections) { SaveToCollectionSheet(article: $0) }
        .task { env.saved.start() }
    }
}
