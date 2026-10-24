import SwiftUI
import StreamFolioCore
import StreamFolioTokens
import StreamFolioUI

/// First-run wizard: welcome → feeds and categories → on-device AI → region and language.
/// Mirrors Android's `OnboardingScreen` (its "import a backup" step arrives with backup support).
/// Nothing is saved until the last step, so backing out never leaves half-applied settings.
struct OnboardingScreen: View {
    @Environment(AppEnvironment.self) private var env

    @State private var step = 0
    /// Which way the last step change went; decides the slide direction of the page transition.
    @State private var movingForward = true
    @State private var defaultFeedsEnabled = true
    @State private var categories = Set(NewsCategory.all)
    @State private var cacheDays = 36_500
    @State private var aiEnabled = false
    @State private var translationEnabled = true
    @State private var summaryEnabled = true
    @State private var smartTagsEnabled = true
    @State private var language = LanguageCatalog.detected()
    @State private var region = RegionCatalog.detected().code

    private let aiAvailability = OnDeviceAI.availability

    /// Region and language only matter if curated feeds or translation are on.
    private var showsPreferencesStep: Bool { defaultFeedsEnabled || (aiEnabled && translationEnabled) }
    private var stepCount: Int { showsPreferencesStep ? 4 : 3 }

    var body: some View {
        VStack(spacing: 0) {
            SFStepIndicator(current: step, total: stepCount)
                .padding(.horizontal, SFSpacing.gutter)
                .padding(.vertical, SFSpacing.s10)

            ScrollView {
                Group {
                    switch step {
                    case 0: WelcomeStep()
                    case 1: FeedsStep(enabled: $defaultFeedsEnabled, categories: $categories, cacheDays: $cacheDays)
                    case 2: AIStep(enabled: $aiEnabled, translation: $translationEnabled, summary: $summaryEnabled,
                                   smartTags: $smartTagsEnabled, availability: aiAvailability)
                    default: RegionStep(showsLanguage: aiEnabled && translationEnabled, language: $language,
                                        showsRegion: defaultFeedsEnabled, region: $region)
                    }
                }
                .padding(.horizontal, SFSpacing.gutter)
                .padding(.bottom, SFSpacing.s12)
            }
            .scrollIndicators(.hidden)
            .id(step)
            .transition(pageTransition)
        }
        .background { SFBackdrop() }
        .safeAreaInset(edge: .bottom, spacing: 0) { navigation }
        .onChange(of: stepCount) { _, count in if step >= count { step = count - 1 } }
    }

    private var navigation: some View {
        HStack(spacing: SFSpacing.s6) {
            if step > 0 {
                SFSecondaryButton("Back") { go(step - 1) }
                    .transition(.opacity)
            }
            SFPrimaryButton(step == stepCount - 1 ? "Start Reading" : "Continue", systemImage: "arrow.right") {
                if step == stepCount - 1 { finish() } else { go(step + 1) }
            }
        }
        .padding(.horizontal, SFSpacing.gutter)
        .padding(.vertical, SFSpacing.s8)
    }

    /// Forward pages enter from the trailing edge and the old page leaves to the leading edge; going
    /// back mirrors that.
    private var pageTransition: AnyTransition {
        movingForward
            ? .asymmetric(insertion: .move(edge: .trailing), removal: .move(edge: .leading))
            : .asymmetric(insertion: .move(edge: .leading), removal: .move(edge: .trailing))
    }

    /// The direction is set first and the step changed on the next turn, because a leaving page keeps
    /// the transition it was last rendered with.
    private func go(_ newStep: Int) {
        movingForward = newStep > step
        Task { @MainActor in
            withAnimation(.smooth) { step = newStep }
        }
    }

    /// Persists every choice at once, then lets the root view swap to the app.
    private func finish() {
        let prefs = env.preferences
        prefs[Prefs.language] = language
        prefs[Prefs.region] = region
        prefs[Prefs.cacheHistoryDays] = cacheDays
        prefs[Prefs.defaultFeedsEnabled] = defaultFeedsEnabled
        prefs[Prefs.selectedCategories] = categories
        prefs[Prefs.aiEnabled] = aiEnabled
        prefs[Prefs.aiTranslationEnabled] = translationEnabled
        prefs[Prefs.aiSummaryEnabled] = aiAvailability.isAvailable && summaryEnabled
        prefs[Prefs.aiSmartTagsEnabled] = smartTagsEnabled
        prefs[Prefs.hasSeenAiSpotlight] = true
        prefs[Prefs.completedOnboarding] = true
    }
}

// MARK: - Steps

private struct WelcomeStep: View {
    var body: some View {
        VStack(alignment: .leading, spacing: SFSpacing.s11) {
            Image(systemName: "dot.radiowaves.up.forward")
                .font(.largeTitle)
                .foregroundStyle(SFColor.onPrimary)
                .frame(width: 72, height: 72)
                .background(SFGradient.brand, in: .rect(cornerRadius: SFRadius.card))
                .accessibilityHidden(true)

            SFStepHeader("Welcome to\nStreamFolio", subtitle: "Your personalized, modern, and free news reader. Let's get you set up in just a few steps.")

            VStack(alignment: .leading, spacing: SFSpacing.s8) {
                SFFeatureRow(systemImage: "globe", title: "Global Coverage", subtitle: "News from top sources worldwide")
                SFFeatureRow(systemImage: "dot.radiowaves.up.forward", title: "Custom Feeds", subtitle: "Add your own RSS feeds")
                SFFeatureRow(systemImage: "waveform", title: "Text-to-Speech", subtitle: "Listen to articles hands-free")
                SFFeatureRow(systemImage: "bookmark.fill", title: "Bookmarks", subtitle: "Save stories to read later")
                SFFeatureRow(systemImage: "lock.shield.fill", title: "Private by design", subtitle: "No accounts, no tracking — everything stays on your device")
            }

            SFInfoPill(detectedLocale, systemImage: "sparkles")
        }
    }

    private var detectedLocale: String {
        let locale = Locale.current
        let language = locale.localizedString(forLanguageCode: locale.language.languageCode?.identifier ?? "en") ?? "English"
        let region = locale.region.flatMap { locale.localizedString(forRegionCode: $0.identifier) } ?? ""
        return "Detected: \(language)" + (region.isEmpty ? "" : " · \(region)")
    }
}

private struct FeedsStep: View {
    @Binding var enabled: Bool
    @Binding var categories: Set<String>
    @Binding var cacheDays: Int

    var body: some View {
        VStack(alignment: .leading, spacing: SFSpacing.s11) {
            SFStepHeader("Personalise\nYour Feed", subtitle: "Enable default curated categories, choose your interests, and set database retention.")
            CuratedFeedsPicker(enabled: $enabled, categories: $categories)
            CacheRetentionField(days: $cacheDays)
        }
    }
}

private struct AIStep: View {
    @Binding var enabled: Bool
    @Binding var translation: Bool
    @Binding var summary: Bool
    @Binding var smartTags: Bool
    let availability: OnDeviceAI.Availability

    var body: some View {
        VStack(alignment: .leading, spacing: SFSpacing.s10) {
            SFStepHeader("On-Device AI\nUpgrades", subtitle: "StreamFolio runs local models directly on your device for absolute privacy.")
            AIServiceToggles(enabled: $enabled, translation: $translation, summary: $summary, smartTags: $smartTags, availability: availability)
        }
    }
}

private struct RegionStep: View {
    let showsLanguage: Bool
    @Binding var language: String
    let showsRegion: Bool
    @Binding var region: String

    var body: some View {
        VStack(alignment: .leading, spacing: SFSpacing.s11) {
            switch (showsLanguage, showsRegion) {
            case (true, true): SFStepHeader("Translation &\nRegion", subtitle: "Set your target translation language and region.")
            case (false, true): SFStepHeader("Region\nPreferences", subtitle: "Select the country for your curated default news feeds.")
            default: SFStepHeader("Translation\nPreferences", subtitle: "Select your target language for offline AI translations.")
            }

            if showsRegion {
                RegionField(region: $region)
            }
            if showsLanguage {
                LanguageField(language: $language)
            }
        }
    }
}
