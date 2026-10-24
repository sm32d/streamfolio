import SwiftUI
import StreamFolioTokens
#if canImport(UIKit)
import UIKit
#elseif canImport(AppKit)
import AppKit
#endif

extension Color {
    init(rgb: UInt32, opacity: Double = 1) {
        self.init(
            .sRGB,
            red: Double((rgb >> 16) & 0xFF) / 255,
            green: Double((rgb >> 8) & 0xFF) / 255,
            blue: Double(rgb & 0xFF) / 255,
            opacity: opacity
        )
    }

    /// A color that follows the current light/dark appearance.
    init(_ adaptive: SFAdaptiveColor) {
        #if canImport(UIKit)
        self.init(uiColor: UIColor { traits in
            UIColor(Color(rgb: traits.userInterfaceStyle == .dark ? adaptive.dark : adaptive.light))
        })
        #else
        self.init(nsColor: NSColor(name: nil) { appearance in
            let isDark = appearance.bestMatch(from: [.darkAqua, .aqua]) == .darkAqua
            return NSColor(Color(rgb: isDark ? adaptive.dark : adaptive.light))
        })
        #endif
    }
}

/// Brand colors for use in views. Every color in the app comes from here (see `DesignSystemRulesTests`).
public enum SFColor {
    public static let primary = Color(SFRole.primary)
    public static let secondary = Color(SFRole.secondary)
    public static let tertiary = Color(SFRole.tertiary)
    public static let error = Color(SFRole.error)
    public static let onPrimary = Color(SFRole.onPrimary)

    public static let background = Color(SFRole.background)
    public static let surface = Color(SFRole.surface)
    public static let surfaceVariant = Color(SFRole.surfaceVariant)
    public static let onSurface = Color(SFRole.onSurface)

    public static let primaryContainer = Color(SFRole.primaryContainer)
    public static let onPrimaryContainer = Color(SFRole.onPrimaryContainer)
    public static let secondaryContainer = Color(SFRole.secondaryContainer)
    public static let onSecondaryContainer = Color(SFRole.onSecondaryContainer)
    public static let tertiaryContainer = Color(SFRole.tertiaryContainer)
    public static let onTertiaryContainer = Color(SFRole.onTertiaryContainer)
    public static let outline = Color(SFRole.outline)
    public static let outlineVariant = Color(SFRole.outlineVariant)

    // Per-action accents
    public static let coral = Color(rgb: SFPalette.coral)
    public static let amber = Color(rgb: SFPalette.amber)
    public static let green = Color(rgb: SFPalette.green)
    public static let violet = Color(rgb: SFPalette.violet)
    public static let indigo = Color(rgb: SFPalette.indigo)
    public static let teal = Color(rgb: SFPalette.teal)

    // Text hierarchy: onSurface at fixed alphas.
    public static let textBody = onSurface.opacity(SFAlpha.textBody)
    public static let textSecondary = onSurface.opacity(SFAlpha.textSecondary)
    public static let textMuted = onSurface.opacity(SFAlpha.textMuted)
    public static let textFaint = onSurface.opacity(SFAlpha.textFaint)
    public static let textDisabled = onSurface.opacity(SFAlpha.textDisabled)

    public static let borderHairline = onSurface.opacity(SFAlpha.borderHairline)
    public static let borderSoft = onSurface.opacity(SFAlpha.borderSoft)
    public static let border = onSurface.opacity(SFAlpha.border)
}

public enum SFGradient {
    /// AI spotlight (indigo → teal).
    public static let ai = LinearGradient(
        colors: SFPalette.aiGradient.map { Color(rgb: $0) },
        startPoint: .topLeading, endPoint: .bottomTrailing
    )
    /// Thumbnail fallback and app icon share the same diagonal.
    public static let thumbnailFallback = LinearGradient(
        colors: SFPalette.thumbnailFallbackGradient.map { Color(rgb: $0) },
        startPoint: .topLeading, endPoint: .bottomTrailing
    )
    /// Darkens the lower part of an image so white text on top stays legible (`sf-gradient-scrim`).
    public static let scrim = LinearGradient(
        stops: [.init(color: .clear, location: 0.25), .init(color: .black.opacity(0.85), location: 1)],
        startPoint: .top, endPoint: .bottom
    )
    public static let brand = LinearGradient(
        colors: SFPalette.iconGradient.map { Color(rgb: $0) },
        startPoint: .topLeading, endPoint: .bottomTrailing
    )
}
