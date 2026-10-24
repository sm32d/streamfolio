import SwiftUI

/// Typography. Android uses the platform default face (Roboto) at fixed sp sizes; iOS uses the
/// platform default face (SF Pro) at the nearest Dynamic Type text style so it scales with the
/// user's text size. Roles come from `design_system/tokens/typography.css`.
public enum SFFont {
    /// Screen titles (Search, Saved, Settings) — 34
    public static let display = Font.largeTitle.weight(.bold)
    /// "StreamFolio" home wordmark — 28, black
    public static let brand = Font.title.weight(.black)
    /// Sub-page title — 24
    public static let subpageTitle = Font.title2.weight(.bold)
    /// Sheet title — 20
    public static let sheetTitle = Font.title3.weight(.semibold)
    /// "Trending Highlights", "Latest Stories" — 18
    public static let sectionTitle = Font.headline.weight(.bold)
    /// Trending card title, collection title — 16
    public static let cardTitle = Font.callout.weight(.semibold)
    /// Settings row title — 15
    public static let rowTitle = Font.subheadline.weight(.medium)
    /// Body — 14
    public static let body = Font.subheadline
    /// Secondary body — 13
    public static let bodySmall = Font.footnote
    /// Metadata — 12
    public static let meta = Font.caption
    /// Source name, timestamps, reading time — 11
    public static let caption = Font.caption2
    /// Nav labels, badges — 10
    public static let micro = Font.caption2.weight(.semibold)
    /// "LIVE" badge — 9
    public static let nano = Font.caption2.weight(.heavy)
}
