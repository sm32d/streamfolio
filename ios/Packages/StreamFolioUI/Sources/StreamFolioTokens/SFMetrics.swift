/// Corner radii in points, from `design_system/tokens/shape.css` (Compose dp, used verbatim).
public enum SFRadius {
    public static let xs: Double = 6        // LIVE badge
    public static let sm: Double = 8        // tag chip
    public static let md: Double = 10
    public static let lg: Double = 12       // icon tile, buttons
    public static let r14: Double = 14
    public static let r16: Double = 16      // thumbnail, info field
    public static let r18: Double = 18      // search field
    public static let card: Double = 20     // article card, settings card, toast
    public static let hero: Double = 24     // trending card
    public static let sheet: Double = 28    // bottom-sheet top corners
    public static let chrome: Double = 32   // nav bar pill, mini player
}

/// Spacing in points, from `design_system/tokens/spacing.css`. The app is not on a strict 4pt grid.
public enum SFSpacing {
    public static let s1: Double = 2
    public static let s2: Double = 4
    public static let s3: Double = 6
    public static let s4: Double = 8
    public static let s5: Double = 10
    public static let s6: Double = 12
    public static let s7: Double = 14
    public static let s8: Double = 16
    public static let s9: Double = 18
    public static let s10: Double = 20
    public static let s11: Double = 24
    public static let s12: Double = 32

    /// The app's universal screen side padding.
    public static let gutter: Double = 24
    public static let gutterTight: Double = 20
    public static let cardPadding: Double = 12
    public static let cardPaddingLarge: Double = 18

    public static let thumbSmall: Double = 40
    public static let thumbMedium: Double = 56
    public static let thumbLarge: Double = 96

    public static let touchTarget: Double = 48
}

/// Stroke widths in points.
public enum SFStroke {
    public static let hairline: Double = 1
    public static let emphasis: Double = 1.5
    public static let selected: Double = 2
}

/// Article card/list animation constants carried over from `motion.css`.
public enum SFMotion {
    public static let pagerOffScale = 0.85
    public static let pagerOffAlpha = 0.5
}
