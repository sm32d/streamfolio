import SwiftUI
import StreamFolioTokens

/// A two-or-more way switch on glass (Reader / Web View): one glass capsule holding the options, with
/// a brand-blue indicator that slides under the selected one.
///
/// Used instead of a `Picker` in a system bottom toolbar, which did not receive taps on the foldable
/// iPhone Duo simulator (iOS 27.1) although it worked on iPhone Air (iOS 26.5). Glass is applied once,
/// to the whole capsule; the indicator is a plain fill, because glass nested in glass muddies both.
public struct SFSegmentedControl<Value: Hashable & Sendable>: View {
    public struct Option: Identifiable {
        public let value: Value
        public let title: LocalizedStringKey
        public var id: Value { value }

        public init(_ value: Value, _ title: LocalizedStringKey) {
            self.value = value
            self.title = title
        }
    }

    private let options: [Option]
    @Binding private var selection: Value
    @Namespace private var indicator

    public init(options: [Option], selection: Binding<Value>) {
        self.options = options
        self._selection = selection
    }

    public var body: some View {
        HStack(spacing: 0) {
            ForEach(options) { option in
                let isSelected = option.value == selection
                Button {
                    withAnimation(.smooth) { selection = option.value }
                } label: {
                    Text(option.title)
                        .font(SFFont.rowTitle.weight(.semibold))
                        .foregroundStyle(isSelected ? SFColor.onPrimary : SFColor.onSurface)
                        .frame(minWidth: 112, minHeight: 40)
                        .padding(.horizontal, SFSpacing.s6)
                        .background {
                            if isSelected {
                                Capsule()
                                    .fill(SFColor.primary)
                                    .matchedGeometryEffect(id: "indicator", in: indicator)
                            }
                        }
                        .contentShape(.capsule)
                }
                .buttonStyle(.plain)
                .accessibilityAddTraits(isSelected ? .isSelected : [])
            }
        }
        .padding(SFSpacing.s2)
        .sfGlassCapsule(interactive: false)
    }
}
