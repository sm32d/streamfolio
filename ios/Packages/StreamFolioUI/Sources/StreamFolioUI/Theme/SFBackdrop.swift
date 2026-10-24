import SwiftUI
import StreamFolioTokens

/// The page backdrop (`getThemeBackgroundBrush()` in the Android theme). It is also what gives
/// Liquid Glass something colorful to refract, so every top-level screen sits on it.
public struct SFBackdrop: View {
    @Environment(\.colorScheme) private var colorScheme

    public init() {}

    public var body: some View {
        LinearGradient(colors: stops, startPoint: .top, endPoint: .bottom)
            .ignoresSafeArea()
    }

    private var stops: [Color] {
        if colorScheme == .dark {
            [SFColor.background, SFColor.surfaceVariant.opacity(SFBackdropRecipe.darkSurfaceVariantOpacity), SFColor.background]
        } else {
            [
                SFColor.primary.opacity(SFBackdropRecipe.lightPrimaryOpacity),
                SFColor.surfaceVariant.opacity(SFBackdropRecipe.lightSurfaceVariantOpacity),
                SFColor.background,
            ]
        }
    }
}

public extension View {
    /// The brand backdrop as a sheet's background. Opaque underneath, so the screen behind a sheet
    /// never shows through the translucent top of the gradient.
    func sfSheetBackground() -> some View {
        presentationBackground {
            ZStack {
                SFColor.background
                SFBackdrop()
            }
        }
    }
}
