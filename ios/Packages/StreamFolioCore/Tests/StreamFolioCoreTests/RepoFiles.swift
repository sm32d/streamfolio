import Foundation

/// Reads files from the repository (Android sources of truth, `sample_xml/`) regardless of where
/// `swift test` is started from.
enum RepoFiles {
    static let root: URL = {
        var dir = URL(fileURLWithPath: #filePath).deletingLastPathComponent()
        while dir.path != "/" {
            if FileManager.default.fileExists(atPath: dir.appendingPathComponent("sample_xml").path) { return dir }
            dir.deleteLastPathComponent()
        }
        fatalError("Could not locate repository root from \(#filePath)")
    }()

    static func data(_ path: String) throws -> Data { try Data(contentsOf: root.appendingPathComponent(path)) }
    static func string(_ path: String) throws -> String { String(decoding: try data(path), as: UTF8.self) }
}
