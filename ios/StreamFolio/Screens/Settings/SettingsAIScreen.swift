import SwiftUI
import StreamFolioCore
import StreamFolioTokens
import StreamFolioUI

/// On-device AI switches (Android `SettingsAiScreen`). Android names Gemini Nano; iOS uses Apple's
/// on-device model only (`OnDeviceAI`) — never a cloud provider, per the privacy promise.
struct SettingsAIScreen: View {
    @Environment(AppEnvironment.self) private var env

    private let availability = OnDeviceAI.availability

    var body: some View {
        let prefs = env.preferences
        SFSettingsPage {
            SFHeroBanner("On-Device AI Engine", subtitle: "100% private & offline — nothing leaves this device", systemImage: "sparkles")

            statusCard

            AIServiceToggles(
                enabled: prefs.binding(Prefs.aiEnabled),
                translation: prefs.binding(Prefs.aiTranslationEnabled),
                summary: prefs.binding(Prefs.aiSummaryEnabled),
                smartTags: prefs.binding(Prefs.aiSmartTagsEnabled),
                availability: availability
            )

            if prefs[Prefs.aiEnabled] && prefs[Prefs.aiTranslationEnabled] {
                VStack(alignment: .leading, spacing: SFSpacing.s6) {
                    LanguageField(language: prefs.binding(Prefs.language))
                    SFInfoBanner("iOS asks you to download a language pack the first time you translate into a language. Once it is downloaded, translation runs entirely on this device.")
                }
            }

            SFInfoBanner("Key takeaways, translation and tags are not available in this version yet. Your choices are saved and will apply when they arrive.", systemImage: "hourglass")
        }
        .animation(.smooth, value: prefs[Prefs.aiEnabled])
        .navigationTitle("AI Services")
        .navigationBarTitleDisplayMode(.inline)
        .reportsScrollAtTop()
    }

    /// Whether this device can run Apple's on-device model (Android: the Gemini Nano status card).
    private var statusCard: some View {
        let available = availability.isAvailable
        return HStack(spacing: SFSpacing.s6) {
            SFIconTile(systemImage: available ? "checkmark" : "info", tint: available ? SFColor.secondary : SFColor.error, isFilled: false, size: 34)
            VStack(alignment: .leading, spacing: SFSpacing.s1) {
                Text(available ? "Apple Intelligence Supported" : "Apple Intelligence Unavailable")
                    .font(SFFont.rowTitle.weight(.bold)).foregroundStyle(SFColor.onSurface)
                Text(statusDetail).font(SFFont.caption).foregroundStyle(SFColor.textSecondary)
            }
            Spacer(minLength: 0)
        }
        .padding(SFSpacing.s7)
        .background((available ? SFColor.secondaryContainer.opacity(0.3) : SFColor.error.opacity(0.1)), in: .rect(cornerRadius: SFRadius.r16))
        .overlay {
            RoundedRectangle(cornerRadius: SFRadius.r16)
                .strokeBorder((available ? SFColor.secondary : SFColor.error).opacity(0.3), lineWidth: SFStroke.hairline)
        }
        .accessibilityElement(children: .combine)
    }

    private var statusDetail: String {
        switch availability {
        case .available: "Your device supports on-device key takeaways and smart tags."
        case .unavailable(let reason): reason.explanation + " Offline translation does not need it."
        }
    }
}
