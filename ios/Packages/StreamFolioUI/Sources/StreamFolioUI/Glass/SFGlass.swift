import SwiftUI

/// The only place raw `glassEffect` is applied to single controls. Screens call these modifiers.
///
/// Rule of thumb (Apple's Liquid Glass guidance, and this app's design): glass belongs on the
/// control/navigation layer floating over content — chips, buttons, bars. Content (cards, reader
/// text, thumbnails) is never glass. Brand blue tints only *prominent* controls so it keeps meaning.
public extension View {
    /// Neutral glass capsule, e.g. an unselected chip or a floating control.
    func sfGlassCapsule(interactive: Bool = true) -> some View {
        glassEffect(interactive ? .regular.interactive() : .regular, in: .capsule)
    }

    /// Brand-tinted glass capsule for prominent / selected controls (selected chip, Listen, primary CTA).
    func sfGlassCapsuleProminent(interactive: Bool = true) -> some View {
        glassEffect(.regular.tint(SFColor.primary).interactive(interactive), in: .capsule)
    }

    /// Glass in an arbitrary shape (round icon buttons, rounded bars).
    func sfGlass<S: Shape>(in shape: S, prominent: Bool = false, interactive: Bool = true) -> some View {
        glassEffect(
            prominent ? .regular.tint(SFColor.primary).interactive(interactive) : (interactive ? .regular.interactive() : .regular),
            in: shape
        )
    }
}
