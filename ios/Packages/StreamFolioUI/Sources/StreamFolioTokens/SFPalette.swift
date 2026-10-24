/// StreamFolio brand colors as raw 24-bit RGB values.
///
/// Source of truth is the Android theme (`ui/theme/Color.kt`, mirrored in
/// `design_system/tokens/colors.css`). `PaletteParityTests` fails if these drift.
/// Kotlin still names the brand blues `Emerald*` for historical reasons; here they are
/// named by role.
public enum SFPalette {
    // Brand
    public static let primary: UInt32 = 0x1E88E5      // EmeraldPrimary — icon mid-blue
    public static let secondary: UInt32 = 0x1565C0    // EmeraldSecondary — royal blue
    public static let tertiary: UInt32 = 0x00B0FF     // MintAccent — cyan highlight
    public static let error: UInt32 = 0xD32F2F        // ErrorRed

    // Per-action accents (swipe actions, badges)
    public static let coral: UInt32 = 0xF43F5E        // AccentCoral — skip / dismiss
    public static let amber: UInt32 = 0xF59E0B        // AccentAmber — share
    public static let green: UInt32 = 0x10B981        // save / queue success
    public static let violet: UInt32 = 0x8B5CF6       // mark as read
    public static let indigo: UInt32 = 0x6366F1       // AI spotlight gradient start
    public static let teal: UInt32 = 0x0D9488         // AI spotlight gradient end
    public static let red400: UInt32 = 0xEF5350       // inline "remove bookmark"
    public static let blue400: UInt32 = 0x3B82F6      // thumbnail fallback gradient start

    public static let white: UInt32 = 0xFFFFFF

    // Gradient stops
    public static let iconGradient: [UInt32] = [0x1E88E5, 0x1565C0]
    public static let aiGradient: [UInt32] = [0x6366F1, 0x0D9488]
    public static let thumbnailFallbackGradient: [UInt32] = [0x3B82F6, 0x10B981]
    public static let lightGradient: [UInt32] = [0xE0F2FE, 0xF0F9FF, 0xF8FAFC]
    public static let darkGradient: [UInt32] = [0x060D1E, 0x0A142D, 0x020409]
}

/// A color that differs between light and dark appearance.
public struct SFAdaptiveColor: Sendable, Equatable {
    public let light: UInt32
    public let dark: UInt32

    public init(light: UInt32, dark: UInt32) {
        self.light = light
        self.dark = dark
    }

    public init(_ both: UInt32) {
        self.init(light: both, dark: both)
    }
}

/// Semantic roles, mirroring the slots set in the Android `LightColorScheme`/`DarkColorScheme`.
public enum SFRole {
    public static let primary = SFAdaptiveColor(SFPalette.primary)
    public static let secondary = SFAdaptiveColor(SFPalette.secondary)
    public static let tertiary = SFAdaptiveColor(SFPalette.tertiary)
    public static let error = SFAdaptiveColor(SFPalette.error)
    public static let onPrimary = SFAdaptiveColor(SFPalette.white)

    public static let background = SFAdaptiveColor(light: 0xF0F6FC, dark: 0x060D1E)
    public static let surface = SFAdaptiveColor(light: 0xFFFFFF, dark: 0x0E1A33)
    public static let surfaceVariant = SFAdaptiveColor(light: 0xE1EBF5, dark: 0x1A2B4C)
    public static let onSurface = SFAdaptiveColor(light: 0x060D1E, dark: 0xF0F6FC)

    // Container/outline tones. Material3 would default these to purple if unset; keeping
    // them explicit is what keeps selected rows and card borders on-brand.
    public static let primaryContainer = SFAdaptiveColor(light: 0xD3E4FA, dark: 0x12386B)
    public static let onPrimaryContainer = SFAdaptiveColor(light: 0x0B2E5E, dark: 0xD3E4FA)
    public static let secondaryContainer = SFAdaptiveColor(light: 0xDCE9FB, dark: 0x0D2B57)
    public static let onSecondaryContainer = SFAdaptiveColor(light: 0x0D2B57, dark: 0xCFE0F5)
    public static let tertiaryContainer = SFAdaptiveColor(light: 0xDFF4FF, dark: 0x00364D)
    public static let onTertiaryContainer = SFAdaptiveColor(light: 0x00364D, dark: 0xB3E5FC)
    public static let outline = SFAdaptiveColor(light: 0x8DA3C2, dark: 0x3A537A)
    public static let outlineVariant = SFAdaptiveColor(light: 0xC3D3EA, dark: 0x26385C)
}

/// The app's text/border hierarchy is `onSurface` at fixed alphas (`colors.css` "alpha ladder").
public enum SFAlpha {
    public static let textStrong = 1.0
    public static let textBody = 0.85
    public static let textSecondary = 0.6
    public static let textMuted = 0.5
    public static let textLabel = 0.45
    public static let textFaint = 0.4
    public static let textDisabled = 0.3

    public static let borderStrong = 0.15
    public static let border = 0.12
    public static let borderSoft = 0.08
    public static let borderHairline = 0.06

    /// Read article cards are dimmed (`--sf-alpha-read`).
    public static let readArticle = 0.55
}

/// Page backdrop recipe from `getThemeBackgroundBrush()` in `Theme.kt`.
/// Liquid Glass needs colored content behind it to read as glass, so this stays the root background.
public enum SFBackdropRecipe {
    /// Light: primary@0.12 → surfaceVariant@0.15 → background
    public static let lightPrimaryOpacity = 0.12
    public static let lightSurfaceVariantOpacity = 0.15
    /// Dark: background → surfaceVariant@0.3 → background
    public static let darkSurfaceVariantOpacity = 0.3
}
