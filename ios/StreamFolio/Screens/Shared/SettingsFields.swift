import SwiftUI
import StreamFolioCore
import StreamFolioTokens
import StreamFolioUI

// Settings that appear both in onboarding and in Settings. Each is written once, so the two places
// always offer the same choices with the same labels and rules.

/// "Default Curated Feeds" switch and, while it is on, the category chips. At least one category
/// stays selected.
struct CuratedFeedsPicker: View {
    @Binding var enabled: Bool
    @Binding var categories: Set<String>

    var body: some View {
        VStack(alignment: .leading, spacing: SFSpacing.s11) {
            SFToggleCard("Default Curated Feeds", subtitle: "Direct RSS/Atom news categories", systemImage: "dot.radiowaves.up.forward", isOn: $enabled)

            if enabled {
                VStack(alignment: .leading, spacing: SFSpacing.s6) {
                    SFSectionLabel("Categories")
                    SFFlowLayout(spacing: SFSpacing.s4) {
                        ForEach(NewsCategory.all, id: \.self) { category in
                            SFSelectableChip(
                                LocalizedStringKey(category), systemImage: SFCategoryIcon.systemName(for: category),
                                isSelected: categories.contains(category)
                            ) { toggle(category) }
                        }
                    }
                    Text("\(categories.count) of \(NewsCategory.all.count) selected")
                        .font(SFFont.meta).foregroundStyle(SFColor.textFaint)
                }
                .transition(.opacity.combined(with: .move(edge: .top)))
            }
        }
        .animation(.smooth, value: enabled)
    }

    private func toggle(_ category: String) {
        if categories.contains(category) {
            if categories.count > 1 { categories.remove(category) }
        } else {
            categories.insert(category)
        }
    }
}

/// How long un-saved articles are kept on the device.
struct CacheRetentionField: View {
    @Binding var days: Int

    private static let presets: [(value: Int, title: String)] = [
        (1, "1 Day"), (3, "3 Days"), (7, "7 Days"), (14, "14 Days"), (30, "30 Days"), (36_500, "Keep All"),
    ]

    /// A value that came from a backup and is not a preset is still shown, not blanked.
    private var options: [(value: Int, title: String)] {
        Self.presets.contains { $0.value == days } ? Self.presets : Self.presets + [(days, "\(days) Days")]
    }

    var body: some View {
        SFMenuField("Offline Cache History", systemImage: "clock.arrow.circlepath", options: options, selection: $days)
    }
}

/// The master on-device AI switch and the three services under it.
struct AIServiceToggles: View {
    @Binding var enabled: Bool
    @Binding var translation: Bool
    @Binding var summary: Bool
    @Binding var smartTags: Bool
    let availability: OnDeviceAI.Availability

    var body: some View {
        VStack(alignment: .leading, spacing: SFSpacing.s10) {
            SFToggleCard("Enable Local AI features", subtitle: "Powered privately by Apple Intelligence", systemImage: "sparkles", isOn: $enabled)

            if enabled {
                VStack(alignment: .leading, spacing: SFSpacing.s7) {
                    SFSectionLabel("Individual services")
                    SFSubToggleRow("Offline AI Translation", subtitle: "Translate foreign feeds entirely offline", isOn: $translation)
                    SFSubToggleRow(
                        "Key Insights Generator",
                        subtitle: availability.isAvailable ? "Generate 3-bullet point summaries" : "Unavailable on this device; you can still read the normal article content without AI insights",
                        note: unavailableNote,
                        isAvailable: availability.isAvailable,
                        isOn: availability.isAvailable ? $summary : .constant(false)
                    )
                    SFSubToggleRow(
                        "Smart Categories & Tags",
                        subtitle: availability.isAvailable ? "Automatically suggest tags with on-device AI" : "Use basic keyword tagging on unsupported devices",
                        note: "Tags are experimental and may occasionally be inaccurate.",
                        isBeta: true,
                        isOn: $smartTags
                    )
                }
                .transition(.opacity.combined(with: .move(edge: .top)))
            }
        }
        .animation(.smooth, value: enabled)
    }

    private var unavailableNote: LocalizedStringKey? {
        guard case .unavailable(let reason) = availability else { return nil }
        return LocalizedStringKey(reason.explanation)
    }
}

/// Country whose curated feeds are shown.
struct RegionField: View {
    @Binding var region: String

    var body: some View {
        SFMenuField("Region / Country", systemImage: "mappin.and.ellipse",
                    options: RegionCatalog.all.map { ($0.code, $0.formattedName) }, selection: $region)
    }
}

/// Language articles are translated into.
struct LanguageField: View {
    @Binding var language: String

    var body: some View {
        SFMenuField("Target Language", systemImage: "character.bubble",
                    options: LanguageCatalog.all.map { ($0.code, $0.name) }, selection: $language)
    }
}
