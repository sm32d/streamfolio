import SwiftUI
import StreamFolioTokens

// Reusable building blocks for onboarding, settings and any other form-like screen. Every screen
// composes these instead of re-declaring toggles, chips or buttons.

/// The primary call to action: brand-tinted glass capsule, full width.
public struct SFPrimaryButton: View {
    private let title: LocalizedStringKey
    private let systemImage: String?
    private let action: () -> Void

    public init(_ title: LocalizedStringKey, systemImage: String? = nil, action: @escaping () -> Void) {
        self.title = title
        self.systemImage = systemImage
        self.action = action
    }

    public var body: some View {
        Button(action: action) {
            HStack(spacing: SFSpacing.s3) {
                Text(title).font(SFFont.rowTitle.weight(.bold))
                if let systemImage { Image(systemName: systemImage).font(SFFont.rowTitle.weight(.bold)) }
            }
            .foregroundStyle(SFColor.onPrimary)
            .frame(maxWidth: .infinity, minHeight: 52)
            .contentShape(.capsule)
        }
        .buttonStyle(.plain)
        .sfGlassCapsuleProminent()
    }
}

/// A quiet text button (Back, Skip).
public struct SFSecondaryButton: View {
    private let title: LocalizedStringKey
    private let action: () -> Void

    public init(_ title: LocalizedStringKey, action: @escaping () -> Void) {
        self.title = title
        self.action = action
    }

    public var body: some View {
        Button(action: action) {
            Text(title)
                .font(SFFont.rowTitle.weight(.semibold))
                .foregroundStyle(SFColor.textSecondary)
                .frame(minHeight: 52)
                .padding(.horizontal, SFSpacing.s4)
        }
        .buttonStyle(.plain)
    }
}

/// Large two-line step title and subtitle used at the top of onboarding steps.
public struct SFStepHeader: View {
    private let title: LocalizedStringKey
    private let subtitle: LocalizedStringKey

    public init(_ title: LocalizedStringKey, subtitle: LocalizedStringKey) {
        self.title = title
        self.subtitle = subtitle
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: SFSpacing.s4) {
            Text(title)
                .font(SFFont.display)
                .foregroundStyle(SFColor.onSurface)
                .accessibilityAddTraits(.isHeader)
            Text(subtitle)
                .font(SFFont.rowTitle.weight(.regular))
                .foregroundStyle(SFColor.textSecondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

/// Small uppercase label above a group of controls.
public struct SFSectionLabel: View {
    private let text: LocalizedStringKey
    public init(_ text: LocalizedStringKey) { self.text = text }

    public var body: some View {
        Text(text)
            .font(SFFont.meta.weight(.semibold))
            .tracking(1)
            .textCase(.uppercase)
            .foregroundStyle(SFColor.onSurface.opacity(SFAlpha.textLabel))
            .accessibilityAddTraits(.isHeader)
    }
}

/// Icon tile + title/subtitle + switch, highlighted when on (the "Default Curated Feeds" / "Local AI"
/// cards).
public struct SFToggleCard: View {
    private let title: LocalizedStringKey
    private let subtitle: LocalizedStringKey
    private let systemImage: String
    @Binding private var isOn: Bool

    public init(_ title: LocalizedStringKey, subtitle: LocalizedStringKey, systemImage: String, isOn: Binding<Bool>) {
        self.title = title
        self.subtitle = subtitle
        self.systemImage = systemImage
        self._isOn = isOn
    }

    public var body: some View {
        Toggle(isOn: $isOn.animation(.smooth)) {
            HStack(spacing: SFSpacing.s7) {
                Image(systemName: systemImage)
                    .font(SFFont.sectionTitle)
                    .foregroundStyle(SFColor.onPrimary)
                    .frame(width: 40, height: 40)
                    .background(isOn ? SFColor.primary : SFColor.onSurface.opacity(SFAlpha.borderStrong), in: .rect(cornerRadius: SFRadius.lg))
                VStack(alignment: .leading, spacing: SFSpacing.s1) {
                    Text(title).font(SFFont.rowTitle.weight(.semibold)).foregroundStyle(SFColor.onSurface)
                    Text(subtitle).font(SFFont.meta).foregroundStyle(SFColor.textMuted)
                }
            }
        }
        .tint(SFColor.primary)
        .padding(.horizontal, SFSpacing.cardPaddingLarge)
        .padding(.vertical, SFSpacing.s8)
        .background(isOn ? SFColor.primary.opacity(0.08) : SFColor.surfaceVariant.opacity(0.4), in: .rect(cornerRadius: SFRadius.r18))
        .overlay {
            RoundedRectangle(cornerRadius: SFRadius.r18)
                .strokeBorder(isOn ? SFColor.primary.opacity(0.3) : .clear, lineWidth: SFStroke.hairline)
        }
    }
}

/// A smaller switch row for sub-options, with optional note, beta badge and unavailable state.
public struct SFSubToggleRow: View {
    private let title: LocalizedStringKey
    private let subtitle: LocalizedStringKey
    private let note: LocalizedStringKey?
    private let isBeta: Bool
    private let isAvailable: Bool
    @Binding private var isOn: Bool

    public init(
        _ title: LocalizedStringKey, subtitle: LocalizedStringKey, note: LocalizedStringKey? = nil,
        isBeta: Bool = false, isAvailable: Bool = true, isOn: Binding<Bool>
    ) {
        self.title = title
        self.subtitle = subtitle
        self.note = note
        self.isBeta = isBeta
        self.isAvailable = isAvailable
        self._isOn = isOn
    }

    public var body: some View {
        Toggle(isOn: $isOn) {
            VStack(alignment: .leading, spacing: SFSpacing.s1) {
                HStack(spacing: SFSpacing.s3) {
                    Text(title).font(SFFont.body.weight(.medium)).foregroundStyle(SFColor.onSurface)
                    if isBeta {
                        Text("BETA")
                            .font(SFFont.nano)
                            .foregroundStyle(SFColor.amber)
                            .padding(.horizontal, SFSpacing.s3).padding(.vertical, SFSpacing.s1)
                            .background(SFColor.amber.opacity(0.15), in: .rect(cornerRadius: SFRadius.xs))
                    }
                }
                Text(subtitle).font(SFFont.caption).foregroundStyle(SFColor.textMuted)
                if let note {
                    Text(note).font(SFFont.micro).foregroundStyle(isAvailable ? SFColor.amber : SFColor.error)
                }
            }
            .opacity(isAvailable ? 1 : 0.5)
        }
        .tint(SFColor.primary)
        .disabled(!isAvailable)
        .padding(.horizontal, SFSpacing.s7)
        .padding(.vertical, SFSpacing.s6)
        .background(SFColor.surfaceVariant.opacity(0.3), in: .rect(cornerRadius: SFRadius.r14))
    }
}

/// A selectable pill (onboarding categories). Selected uses the brand blue; no glass — these sit in
/// scrolling content, not floating over it.
public struct SFSelectableChip: View {
    private let title: LocalizedStringKey
    private let systemImage: String?
    private let isSelected: Bool
    private let action: () -> Void

    public init(_ title: LocalizedStringKey, systemImage: String? = nil, isSelected: Bool, action: @escaping () -> Void) {
        self.title = title
        self.systemImage = systemImage
        self.isSelected = isSelected
        self.action = action
    }

    public var body: some View {
        Button(action: action) {
            HStack(spacing: SFSpacing.s3) {
                if let systemImage { Image(systemName: systemImage).font(SFFont.meta) }
                Text(title)
                if isSelected && systemImage != nil { Image(systemName: "checkmark.circle.fill").font(SFFont.meta) }
            }
            .font(SFFont.bodySmall.weight(isSelected ? .semibold : .regular))
            .foregroundStyle(isSelected ? SFColor.onPrimary : SFColor.onSurface.opacity(0.75))
            .padding(.horizontal, SFSpacing.s7)
            .padding(.vertical, SFSpacing.s5)
            .background(isSelected ? SFColor.primary : SFColor.surfaceVariant.opacity(0.5), in: .rect(cornerRadius: SFRadius.lg))
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}

/// Labelled picker row: icon, current value, chevrons; opens a menu of options.
public struct SFMenuField<Option: Hashable>: View {
    private let label: LocalizedStringKey
    private let systemImage: String
    private let options: [(value: Option, title: String)]
    @Binding private var selection: Option

    public init(_ label: LocalizedStringKey, systemImage: String, options: [(value: Option, title: String)], selection: Binding<Option>) {
        self.label = label
        self.systemImage = systemImage
        self.options = options
        self._selection = selection
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: SFSpacing.s4) {
            SFSectionLabel(label)
            Menu {
                Picker(selection: $selection) {
                    ForEach(options, id: \.value) { Text($0.title).tag($0.value) }
                } label: { EmptyView() }
            } label: {
                HStack(spacing: SFSpacing.s6) {
                    Image(systemName: systemImage).foregroundStyle(SFColor.primary)
                    Text(options.first { $0.value == selection }?.title ?? "")
                        .font(SFFont.rowTitle)
                        .foregroundStyle(SFColor.onSurface)
                    Spacer()
                    Image(systemName: "chevron.up.chevron.down").font(SFFont.meta).foregroundStyle(SFColor.textFaint)
                }
                .padding(.horizontal, SFSpacing.s8)
                .frame(minHeight: 56)
                .background(SFColor.surfaceVariant.opacity(0.5), in: .rect(cornerRadius: SFRadius.r16))
                .overlay { RoundedRectangle(cornerRadius: SFRadius.r16).strokeBorder(SFColor.borderSoft, lineWidth: SFStroke.hairline) }
            }
            .buttonStyle(.plain)
        }
    }
}

/// Feature highlight row: tinted icon tile, title, subtitle.
public struct SFFeatureRow: View {
    private let systemImage: String
    private let title: LocalizedStringKey
    private let subtitle: LocalizedStringKey

    public init(systemImage: String, title: LocalizedStringKey, subtitle: LocalizedStringKey) {
        self.systemImage = systemImage
        self.title = title
        self.subtitle = subtitle
    }

    public var body: some View {
        HStack(spacing: SFSpacing.s7) {
            Image(systemName: systemImage)
                .font(SFFont.sectionTitle)
                .foregroundStyle(SFColor.primary)
                .frame(width: 44, height: 44)
                .background(SFColor.surfaceVariant.opacity(0.6), in: .rect(cornerRadius: SFRadius.r14))
            VStack(alignment: .leading, spacing: SFSpacing.s1) {
                Text(title).font(SFFont.body.weight(.semibold)).foregroundStyle(SFColor.onSurface)
                Text(subtitle).font(SFFont.meta).foregroundStyle(SFColor.textMuted)
            }
        }
        .accessibilityElement(children: .combine)
    }
}

/// Dots showing progress through a multi-step flow. The active step is a longer capsule.
public struct SFStepIndicator: View {
    private let current: Int
    private let total: Int

    public init(current: Int, total: Int) {
        self.current = current
        self.total = total
    }

    public var body: some View {
        HStack(spacing: SFSpacing.s4) {
            ForEach(0..<total, id: \.self) { index in
                Capsule()
                    .fill(index == current ? SFColor.primary : (index < current ? SFColor.primary.opacity(0.5) : SFColor.onSurface.opacity(SFAlpha.borderStrong)))
                    .frame(width: index == current ? 28 : 8, height: 8)
            }
            Spacer()
            Text("\(current + 1) / \(total)")
                .font(SFFont.meta.weight(.medium))
                .foregroundStyle(SFColor.textFaint)
        }
        .animation(.smooth, value: current)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Step \(current + 1) of \(total)")
    }
}

/// Small tinted info pill ("Detected: English · Singapore").
public struct SFInfoPill: View {
    private let text: String
    private let systemImage: String

    public init(_ text: String, systemImage: String) {
        self.text = text
        self.systemImage = systemImage
    }

    public var body: some View {
        Label(text, systemImage: systemImage)
            .font(SFFont.meta.weight(.medium))
            .foregroundStyle(SFColor.primary)
            .padding(.horizontal, SFSpacing.s7)
            .padding(.vertical, SFSpacing.s5)
            .background(SFColor.primary.opacity(0.08), in: .rect(cornerRadius: SFRadius.lg))
    }
}

/// Illustration + title + message (+ actions) for empty and error states.
public struct SFEmptyState<Actions: View>: View {
    private let illustration: Image?
    private let title: LocalizedStringKey
    private let message: LocalizedStringKey
    private let actions: Actions

    public init(illustration: Image? = nil, title: LocalizedStringKey, message: LocalizedStringKey, @ViewBuilder actions: () -> Actions) {
        self.illustration = illustration
        self.title = title
        self.message = message
        self.actions = actions()
    }

    public var body: some View {
        VStack(spacing: SFSpacing.s6) {
            if let illustration {
                illustration.resizable().scaledToFit().frame(maxWidth: 220).accessibilityHidden(true)
            }
            Text(title).font(SFFont.sheetTitle).foregroundStyle(SFColor.onSurface).multilineTextAlignment(.center)
            Text(message).font(SFFont.body).foregroundStyle(SFColor.textSecondary).multilineTextAlignment(.center)
            actions
        }
        .padding(.horizontal, SFSpacing.gutter + SFSpacing.s4)
        .frame(maxWidth: .infinity)
    }
}

public extension SFEmptyState where Actions == EmptyView {
    init(illustration: Image? = nil, title: LocalizedStringKey, message: LocalizedStringKey) {
        self.init(illustration: illustration, title: title, message: message) { EmptyView() }
    }
}

/// Placeholder rows while the first load is in flight.
public struct SFSkeletonList: View {
    @State private var pulse = false
    private let rows: Int

    public init(rows: Int = 6) { self.rows = rows }

    public var body: some View {
        VStack(spacing: SFSpacing.s6) {
            ForEach(0..<rows, id: \.self) { _ in
                HStack(alignment: .top, spacing: SFSpacing.s6) {
                    RoundedRectangle(cornerRadius: SFRadius.r16).frame(width: SFSpacing.thumbLarge, height: SFSpacing.thumbLarge)
                    VStack(alignment: .leading, spacing: SFSpacing.s4) {
                        RoundedRectangle(cornerRadius: SFRadius.xs).frame(height: 16)
                        RoundedRectangle(cornerRadius: SFRadius.xs).frame(height: 16)
                        RoundedRectangle(cornerRadius: SFRadius.xs).frame(width: 120, height: 12)
                    }
                }
                .padding(SFSpacing.cardPadding)
                .sfCard()
            }
        }
        .foregroundStyle(SFColor.onSurface.opacity(pulse ? 0.12 : 0.06))
        .padding(.horizontal, SFSpacing.gutter)
        .onAppear { withAnimation(.easeInOut(duration: 1).repeatForever(autoreverses: true)) { pulse = true } }
        .accessibilityLabel("Loading stories")
    }
}
