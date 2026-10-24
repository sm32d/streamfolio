import SwiftUI
import StreamFolioCore
import StreamFolioTokens
import StreamFolioUI

/// Every page reachable from the Settings tab.
enum SettingsRoute: Hashable {
    case preferences, gestures, content, feeds, backup, ai, reorder, providers
}

/// The Settings tab: a hub of links, each opening a sub-page. Mirrors Android's `SettingsMainScreen`
/// (the version-tap "StreamFolio FM" easter egg needs text-to-speech and arrives with Listen).
struct SettingsScreen: View {
    @State private var path: [SettingsRoute] = []

    var body: some View {
        NavigationStack(path: $path) {
            SettingsHub()
                .navigationTitle("Settings")
                .navigationSubtitle("Customise your StreamFolio experience.")
                .navigationDestination(for: SettingsRoute.self) { route in
                    switch route {
                    case .preferences: SettingsPreferencesScreen()
                    case .gestures: SettingsGesturesScreen()
                    case .content: SettingsManageContentScreen()
                    case .feeds: SettingsFeedsScreen()
                    case .backup: SettingsBackupScreen()
                    case .ai: SettingsAIScreen()
                    case .reorder: SettingsReorderScreen()
                    case .providers: SettingsProvidersScreen()
                    }
                }
        }
    }
}

/// A link row to a sub-page.
struct SettingsLink: View {
    let route: SettingsRoute
    let systemImage: String
    let tint: Color
    let title: LocalizedStringKey
    let subtitle: String

    var body: some View {
        NavigationLink(value: route) {
            SFSettingsRow(systemImage: systemImage, tint: tint, title: title, subtitle: subtitle)
        }
        .buttonStyle(.plain)
    }
}

private struct SettingsHub: View {
    @Environment(\.openURL) private var openURL

    private static let contactAddress = "sfolio@sume.uk"
    private static let studioURL = URL(string: "https://www.thejoblog.com")!
    private static let studioLogo = URL(string: "https://www.thejoblog.com/tjl.gif")

    private var version: String {
        let info = Bundle.main.infoDictionary
        let short = info?["CFBundleShortVersionString"] as? String ?? "Unknown"
        let build = info?["CFBundleVersion"] as? String
        return build.map { "Version \(short) (\($0))" } ?? "Version \(short)"
    }

    var body: some View {
        SFSettingsPage {
            SFSettingsSection("Personalisation") {
                SFSettingsCard {
                    SettingsLink(route: .preferences, systemImage: "slider.horizontal.3", tint: SFColor.primary,
                                 title: "Preferences", subtitle: "Language & region for your news feed")
                    SettingsLink(route: .gestures, systemImage: "hand.draw.fill", tint: SFColor.secondary,
                                 title: "Swipe Gestures", subtitle: "Customize left & right swipe actions")
                }
            }

            SFSettingsSection("Data Management") {
                SFSettingsCard {
                    SettingsLink(route: .content, systemImage: "list.bullet", tint: SFColor.tertiary,
                                 title: "Manage Content & Sources", subtitle: "Customise topics and curation channels")
                    SettingsLink(route: .feeds, systemImage: "dot.radiowaves.up.forward", tint: SFColor.primary,
                                 title: "Custom RSS Feeds", subtitle: "Add and manage private RSS publishers")
                    SettingsLink(route: .backup, systemImage: "externaldrive.fill", tint: SFColor.secondary,
                                 title: "Backup & Restore", subtitle: "Import/export feeds and settings backup")
                }
            }

            SFSettingsSection("AI Services") {
                SFSettingsCard {
                    SettingsLink(route: .ai, systemImage: "sparkles", tint: SFColor.tertiary,
                                 title: "AI Services & Preferences", subtitle: "Offline translation, key insights & smart tags")
                }
            }

            SFSettingsSection("About") {
                SFSettingsCard {
                    HStack(spacing: SFSpacing.s7) {
                        Image(systemName: "gearshape.fill")
                            .font(SFFont.sectionTitle)
                            .foregroundStyle(SFColor.onPrimary)
                            .frame(width: 44, height: 44)
                            .background(SFGradient.brand, in: .rect(cornerRadius: SFRadius.r14))
                            .accessibilityHidden(true)
                        VStack(alignment: .leading, spacing: SFSpacing.s1) {
                            Text("StreamFolio").font(SFFont.cardTitle).foregroundStyle(SFColor.onSurface)
                            Text(version).font(SFFont.meta).foregroundStyle(SFColor.textMuted)
                        }
                        Spacer(minLength: 0)
                    }
                    .padding(.horizontal, SFSpacing.cardPaddingLarge)
                    .padding(.vertical, SFSpacing.s8)
                    .accessibilityElement(children: .combine)

                    Button {
                        openURL(URL(string: "mailto:\(Self.contactAddress)?subject=StreamFolio%20Feedback")!)
                    } label: {
                        SFSettingsRow(systemImage: "envelope.fill", tint: SFColor.primary, title: "Contact Us", subtitle: Self.contactAddress)
                    }
                    .buttonStyle(.plain)
                }
            }

            SFSettingsSection("More from Dominik Studios") {
                Link(destination: Self.studioURL) {
                    HStack(spacing: SFSpacing.s8) {
                        SFRemoteImage(url: Self.studioLogo, maxPointSize: 104)
                            .frame(width: 52, height: 52)
                            .background(Color.white, in: .rect(cornerRadius: SFRadius.lg))
                            .clipShape(.rect(cornerRadius: SFRadius.lg))
                            .accessibilityHidden(true)
                        VStack(alignment: .leading, spacing: SFSpacing.s1) {
                            Text("The Job Log").font(SFFont.cardTitle.weight(.bold)).foregroundStyle(SFColor.onSurface)
                            Text("Your ultimate professional job hunting companion. Track application progress, interview schedules, and follow-ups elegantly.")
                                .font(SFFont.meta).foregroundStyle(SFColor.textSecondary).multilineTextAlignment(.leading)
                        }
                        Spacer(minLength: 0)
                        SFChevron()
                    }
                    .padding(SFSpacing.s8)
                    .sfCard()
                }
                .buttonStyle(.plain)
            }
        }
        .reportsScrollAtTop()
    }
}
