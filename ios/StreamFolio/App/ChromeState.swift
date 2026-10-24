import Observation

/// Shared state about the screen content that the app chrome (the tab bar) reacts to.
@MainActor
@Observable
final class ChromeState {
    /// True while the visible screen's content is scrolled to its very top. The tab bar is always
    /// fully expanded there; the system's minimize-on-scroll only applies once content has moved.
    var isAtTop = true
}
