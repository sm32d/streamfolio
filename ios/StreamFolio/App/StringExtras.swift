import Foundation

extension StringProtocol {
    /// The text without leading and trailing whitespace and newlines.
    var trimmed: String { trimmingCharacters(in: .whitespacesAndNewlines) }
}
