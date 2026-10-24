import Foundation

/// Finds the repository root by walking up from this file until `design_system/tokens/colors.css` exists,
/// so parity tests can read the Android sources of truth regardless of where `swift test` is launched.
enum RepoLocator {
    static let root: URL = {
        var dir = URL(fileURLWithPath: #filePath).deletingLastPathComponent()
        let marker = "design_system/tokens/colors.css"
        while dir.path != "/" {
            if FileManager.default.fileExists(atPath: dir.appendingPathComponent(marker).path) {
                return dir
            }
            dir.deleteLastPathComponent()
        }
        fatalError("Could not locate repository root from \(#filePath)")
    }()

    static func read(_ relativePath: String) throws -> String {
        try String(contentsOf: root.appendingPathComponent(relativePath), encoding: .utf8)
    }

    /// Every `.swift` file below `relativeDirectory` (empty if the directory does not exist yet).
    static func swiftFiles(under relativeDirectory: String) -> [URL] {
        let base = root.appendingPathComponent(relativeDirectory)
        guard let walker = FileManager.default.enumerator(at: base, includingPropertiesForKeys: nil) else { return [] }
        return walker.compactMap { $0 as? URL }.filter { $0.pathExtension == "swift" }
    }
}
