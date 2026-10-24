import Foundation
import Testing

/// Enforces the reuse rules from the iOS port plan: screens consume the design system, they do not
/// re-declare styling. These are plain source scans so a violation fails `swift test` immediately.
@Suite("Design system usage rules")
struct DesignSystemRulesTests {

    /// Styling that must only be produced by StreamFolioUI components/modifiers.
    private let forbiddenInScreens: [(pattern: String, why: String)] = [
        (#"\.glassEffect\("#, "use an sfGlass* modifier / SF component, never raw glassEffect"),
        (#"GlassEffectContainer"#, "glass grouping lives in StreamFolioUI components"),
        (#"\.glassEffectID\("#, "glass morphing lives in StreamFolioUI components"),
        (#"Color\(red:"#, "use SFColor tokens"),
        (#"Color\(hex"#, "use SFColor tokens"),
        (#"UIColor\("#, "use SFColor tokens"),
        (#"#colorLiteral"#, "use SFColor tokens"),
        (#"cornerRadius:\s*\d"#, "use SFRadius tokens"),
        (#"\.cornerRadius\(\s*\d"#, "use SFRadius tokens"),
        (#"\.font\(\.system\(size:"#, "use SFFont (Dynamic Type) styles"),
    ]

    private func violations(in directory: String, excluding allowed: (URL) -> Bool = { _ in false }) throws -> [String] {
        var found: [String] = []
        for file in RepoLocator.swiftFiles(under: directory) where !allowed(file) {
            let text = try String(contentsOf: file, encoding: .utf8)
            for rule in forbiddenInScreens where text.range(of: rule.pattern, options: .regularExpression) != nil {
                found.append("\(file.lastPathComponent): \(rule.why) (\(rule.pattern))")
            }
        }
        return found
    }

    @Test("App screens use design-system components instead of raw styling")
    func screensAreClean() throws {
        let screens = try violations(in: "ios/StreamFolio")
        #expect(screens.isEmpty, "\(screens.joined(separator: "\n"))")
    }

    @Test("Raw glassEffect is confined to StreamFolioUI/Glass")
    func glassEffectIsConfined() throws {
        var found: [String] = []
        for file in RepoLocator.swiftFiles(under: "ios/Packages/StreamFolioUI/Sources/StreamFolioUI") {
            guard !file.path.contains("/Glass/") else { continue }
            let text = try String(contentsOf: file, encoding: .utf8)
            if text.range(of: #"\.glassEffect\(|GlassEffectContainer|\.glassEffectID\("#, options: .regularExpression) != nil {
                found.append(file.lastPathComponent)
            }
        }
        #expect(found.isEmpty, "glass primitives outside Glass/: \(found)")
    }
}
