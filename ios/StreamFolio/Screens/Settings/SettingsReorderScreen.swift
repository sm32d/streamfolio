import SwiftUI
import StreamFolioCore
import StreamFolioTokens
import StreamFolioUI

/// Drag the Home category tabs into the order you want (Android `SettingsReorderScreen`). The tabs
/// shown are exactly the ones Home shows (`CategoryLayout`), so a custom feed's tab label appears here
/// too.
struct SettingsReorderScreen: View {
    @Environment(AppEnvironment.self) private var env
    @State private var order: [String] = []

    var body: some View {
        List {
            Section {
                SFHeroBanner("Category Tab Layout", subtitle: "Drag the handles to reorder your dashboard tabs", systemImage: "arrow.up.arrow.down")
                    .listRowInsets(EdgeInsets()).listRowBackground(Color.clear)
            }

            Section {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: SFSpacing.s4) {
                        ForEach(Array(order.enumerated()), id: \.element) { index, category in
                            Text(category)
                                .font(SFFont.bodySmall.weight(.bold))
                                .foregroundStyle(index == 0 ? SFColor.onPrimary : SFColor.onSurface.opacity(0.85))
                                .padding(.horizontal, SFSpacing.s8).padding(.vertical, SFSpacing.s4)
                                .background(index == 0 ? SFColor.primary : SFColor.surface.opacity(0.75), in: .capsule)
                        }
                    }
                    .padding(SFSpacing.s6)
                }
                .listRowInsets(EdgeInsets())
                .listRowBackground(SFColor.surfaceVariant.opacity(0.4))
                .accessibilityHidden(true)
            } header: {
                SFSectionLabel("Live tab bar preview")
            }

            Section {
                ForEach(order, id: \.self) { category in
                    HStack(spacing: SFSpacing.s6) {
                        SFIconTile(systemImage: SFCategoryIcon.systemName(for: category), isFilled: false, size: 36)
                        Text(category).font(SFFont.cardTitle).foregroundStyle(SFColor.onSurface)
                    }
                    .accessibilityAction(named: "Move Up") { move(category, by: -1) }
                    .accessibilityAction(named: "Move Down") { move(category, by: 1) }
                }
                .onMove { source, destination in
                    order.move(fromOffsets: source, toOffset: destination)
                    save()
                }
                .listRowBackground(SFColor.surface.opacity(0.5))
            } header: {
                SFSectionLabel("Category priority order")
            }
        }
        .scrollContentBackground(.hidden)
        .background { SFBackdrop() }
        .environment(\.editMode, .constant(.active))   // drag handles are always shown
        .sensoryFeedback(.selection, trigger: order)
        .navigationTitle("Reorder Category Tabs")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { order = env.feed.categories }
        .onChange(of: env.feed.categories) { _, categories in
            // Something else changed the tab set (a restore, a feed edit): follow it.
            if Set(categories) != Set(order) { order = categories }
        }
    }

    private func move(_ category: String, by offset: Int) {
        guard let index = order.firstIndex(of: category), order.indices.contains(index + offset) else { return }
        order.swapAt(index, index + offset)
        save()
    }

    private func save() {
        env.preferences[Prefs.categoryOrder] = order
    }
}
