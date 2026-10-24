import Foundation
import Testing
@testable import StreamFolioTokens

/// Fails when the iOS tokens drift from the Android theme (`Color.kt`) or the shared
/// design-system CSS (`design_system/tokens/*.css`). Android is the source of truth.
@Suite("Palette parity with Android / design_system")
struct PaletteParityTests {

    // MARK: Color.kt

    /// `val Name = Color(0xFFRRGGBB)` → [Name: 0xRRGGBB]
    private func kotlinColors() throws -> [String: UInt32] {
        let source = try RepoLocator.read("app/src/main/java/uk/sume/streamfolio/ui/theme/Color.kt")
        let regex = try Regex(#"val (\w+) = Color\(0xFF([0-9A-Fa-f]{6})\)"#)
        var result: [String: UInt32] = [:]
        for match in source.matches(of: regex) {
            let name = String(match.output[1].substring ?? "")
            let hex = String(match.output[2].substring ?? "")
            result[name] = UInt32(hex, radix: 16)
        }
        return result
    }

    @Test("Every Color.kt color matches its Swift token")
    func kotlinColorsMatch() throws {
        let kotlin = try kotlinColors()
        let expected: [String: UInt32] = [
            "EmeraldPrimary": SFPalette.primary,
            "EmeraldSecondary": SFPalette.secondary,
            "MintAccent": SFPalette.tertiary,
            "ErrorRed": SFPalette.error,
            "AccentCoral": SFPalette.coral,
            "AccentAmber": SFPalette.amber,

            "DarkBackground": SFRole.background.dark,
            "DarkSurface": SFRole.surface.dark,
            "DarkSurfaceVariant": SFRole.surfaceVariant.dark,
            "LightBackground": SFRole.background.light,
            "LightSurface": SFRole.surface.light,
            "LightSurfaceVariant": SFRole.surfaceVariant.light,

            "PrimaryContainerDark": SFRole.primaryContainer.dark,
            "OnPrimaryContainerDark": SFRole.onPrimaryContainer.dark,
            "SecondaryContainerDark": SFRole.secondaryContainer.dark,
            "OnSecondaryContainerDark": SFRole.onSecondaryContainer.dark,
            "TertiaryContainerDark": SFRole.tertiaryContainer.dark,
            "OnTertiaryContainerDark": SFRole.onTertiaryContainer.dark,
            "OutlineDark": SFRole.outline.dark,
            "OutlineVariantDark": SFRole.outlineVariant.dark,

            "PrimaryContainerLight": SFRole.primaryContainer.light,
            "OnPrimaryContainerLight": SFRole.onPrimaryContainer.light,
            "SecondaryContainerLight": SFRole.secondaryContainer.light,
            "OnSecondaryContainerLight": SFRole.onSecondaryContainer.light,
            "TertiaryContainerLight": SFRole.tertiaryContainer.light,
            "OnTertiaryContainerLight": SFRole.onTertiaryContainer.light,
            "OutlineLight": SFRole.outline.light,
            "OutlineVariantLight": SFRole.outlineVariant.light,
        ]
        for (name, swiftValue) in expected {
            let kotlinValue = try #require(kotlin[name], "\(name) missing from Color.kt")
            #expect(kotlinValue == swiftValue, "\(name): Color.kt 0x\(String(kotlinValue, radix: 16)) vs Swift 0x\(String(swiftValue, radix: 16))")
        }
        // A new Android color that has no Swift counterpart yet must be a conscious decision.
        let unmapped = Set(kotlin.keys).subtracting(expected.keys)
        #expect(unmapped.isEmpty, "Color.kt colors without a Swift token: \(unmapped.sorted())")
    }

    @Test("Gradient stops appear in Color.kt")
    func gradientStops() throws {
        let source = try RepoLocator.read("app/src/main/java/uk/sume/streamfolio/ui/theme/Color.kt").uppercased()
        for stop in SFPalette.lightGradient + SFPalette.darkGradient {
            let literal = "0xFF" + String(format: "%06X", stop)
            #expect(source.contains(literal.uppercased()), "gradient stop \(literal) not in Color.kt")
        }
    }

    // MARK: CSS tokens

    private func cssValues(_ file: String) throws -> [String: String] {
        let css = try RepoLocator.read("design_system/tokens/\(file)")
        let regex = try Regex(#"--(sf-[\w-]+):([^;]+);"#)
        var result: [String: String] = [:]
        for match in css.matches(of: regex) {
            result[String(match.output[1].substring ?? "")] = String(match.output[2].substring ?? "").trimmingCharacters(in: .whitespaces)
        }
        return result
    }

    @Test("Accent colors match colors.css")
    func cssAccents() throws {
        let css = try cssValues("colors.css")
        let expected: [String: UInt32] = [
            "sf-green-500": SFPalette.green,
            "sf-violet-500": SFPalette.violet,
            "sf-indigo-500": SFPalette.indigo,
            "sf-teal-600": SFPalette.teal,
            "sf-red-400": SFPalette.red400,
            "sf-blue-400": SFPalette.blue400,
            "sf-coral-500": SFPalette.coral,
            "sf-amber-500": SFPalette.amber,
            "sf-blue-500": SFPalette.primary,
            "sf-blue-700": SFPalette.secondary,
            "sf-cyan-400": SFPalette.tertiary,
        ]
        for (variable, swiftValue) in expected {
            let raw = try #require(css[variable], "\(variable) missing from colors.css")
            let hex = raw.drop(while: { $0 == "#" })
            #expect(UInt32(hex, radix: 16) == swiftValue, "\(variable) = \(raw)")
        }
    }

    @Test("Alpha ladder matches colors.css")
    func cssAlphas() throws {
        let css = try cssValues("colors.css")
        func alpha(_ variable: String) throws -> Double {
            let raw = try #require(css[variable], "\(variable) missing")
            let tail = try #require(raw.split(separator: "/").last, "\(variable) has no alpha")
            return try #require(Double(tail.dropLast()), "\(variable): \(raw)")
        }
        #expect(try alpha("sf-text-body") == SFAlpha.textBody)
        #expect(try alpha("sf-text-secondary") == SFAlpha.textSecondary)
        #expect(try alpha("sf-text-muted") == SFAlpha.textMuted)
        #expect(try alpha("sf-text-label") == SFAlpha.textLabel)
        #expect(try alpha("sf-text-faint") == SFAlpha.textFaint)
        #expect(try alpha("sf-text-disabled") == SFAlpha.textDisabled)
        #expect(try alpha("sf-border-strong") == SFAlpha.borderStrong)
        #expect(try alpha("sf-border") == SFAlpha.border)
        #expect(try alpha("sf-border-soft") == SFAlpha.borderSoft)
        #expect(try alpha("sf-border-hairline") == SFAlpha.borderHairline)
    }

    @Test("Radii match shape.css")
    func cssRadii() throws {
        let css = try cssValues("shape.css")
        let expected: [String: Double] = [
            "sf-radius-xs": SFRadius.xs, "sf-radius-sm": SFRadius.sm, "sf-radius-md": SFRadius.md,
            "sf-radius-lg": SFRadius.lg, "sf-radius-14": SFRadius.r14, "sf-radius-16": SFRadius.r16,
            "sf-radius-18": SFRadius.r18, "sf-radius-20": SFRadius.card, "sf-radius-24": SFRadius.hero,
            "sf-radius-28": SFRadius.sheet, "sf-radius-32": SFRadius.chrome,
        ]
        for (variable, swiftValue) in expected {
            #expect(try pixels(css[variable], variable) == swiftValue, "\(variable)")
        }
    }

    @Test("Spacing matches spacing.css")
    func cssSpacing() throws {
        let css = try cssValues("spacing.css")
        let scale: [(Int, Double)] = [
            (1, SFSpacing.s1), (2, SFSpacing.s2), (3, SFSpacing.s3), (4, SFSpacing.s4), (5, SFSpacing.s5),
            (6, SFSpacing.s6), (7, SFSpacing.s7), (8, SFSpacing.s8), (9, SFSpacing.s9), (10, SFSpacing.s10),
            (11, SFSpacing.s11), (12, SFSpacing.s12),
        ]
        for (step, swiftValue) in scale {
            #expect(try pixels(css["sf-space-\(step)"], "sf-space-\(step)") == swiftValue)
        }
        #expect(try pixels(css["sf-gutter"], "sf-gutter") == SFSpacing.gutter)
        #expect(try pixels(css["sf-gutter-tight"], "sf-gutter-tight") == SFSpacing.gutterTight)
        #expect(try pixels(css["sf-card-pad"], "sf-card-pad") == SFSpacing.cardPadding)
        #expect(try pixels(css["sf-card-pad-lg"], "sf-card-pad-lg") == SFSpacing.cardPaddingLarge)
        #expect(try pixels(css["sf-touch-target"], "sf-touch-target") == SFSpacing.touchTarget)
    }

    private func pixels(_ raw: String?, _ name: String) throws -> Double {
        let value = try #require(raw, "\(name) missing")
        let number = value.split(separator: "p").first.map(String.init) ?? value
        return try #require(Double(number), "\(name): \(value)")
    }
}
