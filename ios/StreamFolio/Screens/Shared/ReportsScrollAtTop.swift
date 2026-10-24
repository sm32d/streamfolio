import SwiftUI

/// Tells the tab bar whether this screen's scroll content is at the top (see `ChromeState`).
///
/// `tabBarMinimizeBehavior(.onScrollDown)` expands the bar when you scroll up, but after a fast fling to
/// the top it can stay collapsed for a few seconds. Reporting the position lets `RootTabView` pin the bar
/// open whenever the content is at the top, so it is always expanded there.
private struct ReportsScrollAtTop: ViewModifier {
    @Environment(AppEnvironment.self) private var env

    func body(content: Content) -> some View {
        content.onScrollGeometryChange(for: Bool.self) { geometry in
            geometry.contentOffset.y + geometry.contentInsets.top <= 4
        } action: { _, isAtTop in
            if env.chrome.isAtTop != isAtTop { env.chrome.isAtTop = isAtTop }
        }
    }
}

extension View {
    /// Apply to the primary scroll view (`List`, `ScrollView`) of a tab's root screen.
    func reportsScrollAtTop() -> some View { modifier(ReportsScrollAtTop()) }
}
