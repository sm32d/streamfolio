import SwiftUI
import StreamFolioTokens

/// Horizontal category selector (replaces Android `CategoryPill`). Chips are glass; the selected
/// one is tinted with the brand blue. They sit in one `GlassEffectContainer` so neighbouring glass
/// blends and the selection change animates as a single surface.
public struct SFCategoryChips<ID: Hashable & Sendable>: View {
    private let items: [Item]
    @Binding private var selection: ID

    public struct Item: Identifiable {
        public let id: ID
        public let title: LocalizedStringKey

        public init(id: ID, title: LocalizedStringKey) {
            self.id = id
            self.title = title
        }
    }

    public init(items: [Item], selection: Binding<ID>) {
        self.items = items
        self._selection = selection
    }

    public var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            GlassEffectContainer(spacing: SFSpacing.s4) {
                HStack(spacing: SFSpacing.s4) {
                    ForEach(items) { item in
                        chip(item)
                    }
                }
                .padding(.horizontal, SFSpacing.gutter)
                .padding(.vertical, SFSpacing.s2)
            }
        }
        .scrollClipDisabled()
    }

    @ViewBuilder
    private func chip(_ item: Item) -> some View {
        let isSelected = item.id == selection
        Button {
            withAnimation(.smooth) { selection = item.id }
        } label: {
            Text(item.title)
                .font(SFFont.rowTitle)
                .foregroundStyle(isSelected ? SFColor.onPrimary : SFColor.textBody)
                .padding(.horizontal, SFSpacing.s8)
                .padding(.vertical, SFSpacing.s4)
        }
        .buttonStyle(.plain)
        .modifier(ChipGlass(isSelected: isSelected))
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}

private struct ChipGlass: ViewModifier {
    let isSelected: Bool

    func body(content: Content) -> some View {
        if isSelected {
            content.sfGlassCapsuleProminent()
        } else {
            content.sfGlassCapsule()
        }
    }
}
