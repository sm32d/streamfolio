import Foundation
import FoundationModels

/// Whether Apple's on-device language model can run here. Only `SystemLanguageModel` (on-device) is
/// ever used — never the cloud model providers that iOS 27 also exposes — to keep the privacy
/// promise of ADR-001/ADR-002. Android's counterpart is the Gemini Nano / AICore feature check.
public enum OnDeviceAI {
    public enum Availability: Sendable, Equatable {
        case available
        case unavailable(Reason)

        public var isAvailable: Bool { self == .available }
    }

    public enum Reason: Sendable, Equatable {
        case deviceNotEligible
        case appleIntelligenceNotEnabled
        case modelNotReady
        case unknown

        public var explanation: String {
            switch self {
            case .deviceNotEligible: "This device doesn't support Apple Intelligence."
            case .appleIntelligenceNotEnabled: "Turn on Apple Intelligence in Settings to use summaries."
            case .modelNotReady: "The on-device model is still downloading. Try again later."
            case .unknown: "On-device AI isn't available right now."
            }
        }
    }

    public static var availability: Availability {
        switch SystemLanguageModel.default.availability {
        case .available: .available
        case .unavailable(.deviceNotEligible): .unavailable(.deviceNotEligible)
        case .unavailable(.appleIntelligenceNotEnabled): .unavailable(.appleIntelligenceNotEnabled)
        case .unavailable(.modelNotReady): .unavailable(.modelNotReady)
        case .unavailable: .unavailable(.unknown)
        }
    }
}
