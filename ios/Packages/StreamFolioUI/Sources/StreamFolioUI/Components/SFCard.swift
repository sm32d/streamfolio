import SwiftUI
import StreamFolioTokens

/// Content card surface. Content is intentionally *not* glass: the card is a solid surface with a
/// hairline border (the Android cards use a 1dp border instead of a shadow).
public struct SFCardBackground: ViewModifier {
    private let radius: Double

    public init(radius: Double = SFRadius.card) {
        self.radius = radius
    }

    public func body(content: Content) -> some View {
        content
            .background(SFColor.surface, in: .rect(cornerRadius: radius))
            .overlay {
                RoundedRectangle(cornerRadius: radius)
                    .strokeBorder(SFColor.borderHairline, lineWidth: SFStroke.hairline)
            }
    }
}

public extension View {
    func sfCard(radius: Double = SFRadius.card) -> some View {
        modifier(SFCardBackground(radius: radius))
    }
}
