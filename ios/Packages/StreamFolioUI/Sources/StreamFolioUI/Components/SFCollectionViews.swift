import SwiftUI
import StreamFolioCore
import StreamFolioTokens

/// Collection tile for the grid: preview photo (or brand gradient) under a scrim, count badge, name.
/// Uses the saturated brand colors for the empty state because white text sits directly on top.
public struct SFCollectionCard: View {
    private let title: String
    private let count: Int
    private let preview: String?
    private let isDefault: Bool

    public init(title: String, count: Int, preview: String?, isDefault: Bool) {
        self.title = title
        self.count = count
        self.preview = preview
        self.isDefault = isDefault
    }

    public var body: some View {
        ZStack(alignment: .bottomLeading) {
            if let preview, !preview.isEmpty, preview != ArticleContentService.thumbnailFailed {
                SFThumbnail(urlString: preview, maxPointSize: 400)
                SFGradient.scrim
            } else {
                SFGradient.brand
            }
            VStack(alignment: .leading) {
                HStack {
                    Image(systemName: isDefault ? "star.square.fill" : "folder.fill")
                        .font(SFFont.rowTitle)
                        .foregroundStyle(SFColor.onPrimary)
                        .frame(width: 36, height: 36)
                        .background(.white.opacity(0.22), in: .rect(cornerRadius: SFRadius.md))
                    Spacer()
                    Text("\(count)")
                        .font(SFFont.meta.weight(.bold))
                        .foregroundStyle(SFColor.onPrimary)
                        .padding(.horizontal, SFSpacing.s4).padding(.vertical, SFSpacing.s1)
                        .background(.black.opacity(0.45), in: .capsule)
                }
                Spacer(minLength: 0)
                Text(title).font(SFFont.cardTitle.weight(.bold)).foregroundStyle(SFColor.onPrimary).lineLimit(1)
                Text(count == 1 ? "1 item" : "\(count) items").font(SFFont.meta).foregroundStyle(SFColor.onPrimary.opacity(0.8))
            }
            .padding(SFSpacing.s7)
        }
        .frame(height: 145)
        .clipShape(.rect(cornerRadius: SFRadius.card))
        .overlay { RoundedRectangle(cornerRadius: SFRadius.card).strokeBorder(SFColor.borderHairline, lineWidth: SFStroke.hairline) }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(title), \(count) \(count == 1 ? "item" : "items")")
    }
}

/// The dashed-feel "New Collection" tile.
public struct SFNewCollectionCard: View {
    public init() {}

    public var body: some View {
        VStack(spacing: SFSpacing.s5) {
            Image(systemName: "plus")
                .font(SFFont.sectionTitle)
                .foregroundStyle(SFColor.primary)
                .frame(width: 44, height: 44)
                .background(SFColor.primary.opacity(0.12), in: .circle)
            Text("New Collection").font(SFFont.rowTitle.weight(.bold)).foregroundStyle(SFColor.primary)
        }
        .frame(maxWidth: .infinity)
        .frame(height: 145)
        .background(SFColor.surface.opacity(0.85), in: .rect(cornerRadius: SFRadius.card))
        .overlay {
            RoundedRectangle(cornerRadius: SFRadius.card)
                .strokeBorder(SFColor.primary.opacity(0.5), style: StrokeStyle(lineWidth: SFStroke.emphasis, dash: [6, 4]))
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("New collection")
    }
}

/// Collection row for the list layout.
public struct SFCollectionRow: View {
    private let title: String
    private let count: Int
    private let preview: String?
    private let isDefault: Bool

    public init(title: String, count: Int, preview: String?, isDefault: Bool) {
        self.title = title
        self.count = count
        self.preview = preview
        self.isDefault = isDefault
    }

    public var body: some View {
        HStack(spacing: SFSpacing.s6) {
            ZStack {
                if let preview, !preview.isEmpty, preview != ArticleContentService.thumbnailFailed {
                    SFThumbnail(urlString: preview, maxPointSize: 120)
                } else {
                    SFColor.primaryContainer
                    Image(systemName: isDefault ? "star.square.fill" : "folder.fill").foregroundStyle(SFColor.primary)
                }
            }
            .frame(width: SFSpacing.thumbMedium, height: SFSpacing.thumbMedium)
            .clipShape(.rect(cornerRadius: SFRadius.lg))

            VStack(alignment: .leading, spacing: SFSpacing.s1) {
                Text(title).font(SFFont.cardTitle.weight(.bold)).foregroundStyle(SFColor.onSurface).lineLimit(1)
                Text(count == 1 ? "1 article" : "\(count) articles").font(SFFont.meta).foregroundStyle(SFColor.textMuted)
            }
            Spacer(minLength: 0)
            Image(systemName: "chevron.right").font(SFFont.meta.weight(.semibold)).foregroundStyle(SFColor.textFaint)
        }
        .padding(SFSpacing.s6)
        .frame(maxWidth: .infinity, minHeight: 76)
        .sfCard(radius: SFRadius.r16)
        .contentShape(.rect(cornerRadius: SFRadius.r16))
        .accessibilityElement(children: .combine)
    }
}

/// "New Collection" as a list row.
public struct SFNewCollectionRow: View {
    public init() {}

    public var body: some View {
        HStack(spacing: SFSpacing.s6) {
            Image(systemName: "plus")
                .foregroundStyle(SFColor.primary)
                .frame(width: SFSpacing.thumbMedium, height: SFSpacing.thumbMedium)
                .background(SFColor.primary.opacity(0.12), in: .rect(cornerRadius: SFRadius.lg))
            Text("New Collection").font(SFFont.cardTitle.weight(.bold)).foregroundStyle(SFColor.primary)
            Spacer(minLength: 0)
        }
        .padding(SFSpacing.s6)
        .frame(maxWidth: .infinity, minHeight: 76)
        .background(SFColor.surface.opacity(0.85), in: .rect(cornerRadius: SFRadius.r16))
        .overlay {
            RoundedRectangle(cornerRadius: SFRadius.r16)
                .strokeBorder(SFColor.primary.opacity(0.5), style: StrokeStyle(lineWidth: SFStroke.emphasis, dash: [6, 4]))
        }
        .contentShape(.rect(cornerRadius: SFRadius.r16))
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("New collection")
    }
}

/// A selectable collection line (Save to Collection sheet).
public struct SFCollectionPickerRow: View {
    private let name: String
    private let isDefault: Bool
    private let isSelected: Bool

    public init(name: String, isDefault: Bool, isSelected: Bool) {
        self.name = name
        self.isDefault = isDefault
        self.isSelected = isSelected
    }

    public var body: some View {
        HStack(spacing: SFSpacing.s6) {
            Image(systemName: isDefault ? "star.square.fill" : "folder.fill")
                .foregroundStyle(isSelected ? SFColor.primary : SFColor.textSecondary)
            Text(name).font(SFFont.rowTitle.weight(isSelected ? .bold : .medium)).foregroundStyle(SFColor.onSurface)
            Spacer(minLength: 0)
            Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                .font(SFFont.sectionTitle)
                .foregroundStyle(isSelected ? SFColor.primary : SFColor.textDisabled)
        }
        .padding(.horizontal, SFSpacing.s7).padding(.vertical, SFSpacing.s6)
        .background(isSelected ? SFColor.primaryContainer.opacity(0.4) : SFColor.surfaceVariant.opacity(0.3), in: .rect(cornerRadius: SFRadius.r14))
        .contentShape(.rect(cornerRadius: SFRadius.r14))
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}
