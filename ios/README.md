# StreamFolio for iOS

Native SwiftUI port of StreamFolio with Apple's Liquid Glass design and the StreamFolio brand. **Status:** onboarding, the live Home feed (curated feeds for 14 regions, link-only terms enforced), the article page (Reader + Web View), Saved collections, Search and every Settings page work — preferences, swipe gestures, curated sources and providers, custom RSS feeds, category-tab order, AI switches, and Android-compatible backup/restore and OPML import/export. AI summary/translation, TTS, the widget and background refresh are not built. Plan and rationale: [ADR-007 / ADR-008](../docs/14-decisions-and-known-limitations.md).

## Requirements

- Xcode 27.1 (iOS 27.1 SDK; the app builds with deployment target **iOS 26.0**, iOS 27 APIs are behind `#available`)
- Swift 6.4 toolchain (language mode 6, strict concurrency `complete`)
- [XcodeGen](https://github.com/yonaskolb/XcodeGen): `brew install xcodegen`

## Build and test

```bash
# Generate StreamFolio.xcodeproj from project.yml (the .xcodeproj is git-ignored)
cd ios && xcodegen generate
```

```bash
# Build for the iOS Simulator (no signing needed)
xcodebuild -project StreamFolio.xcodeproj -scheme StreamFolio -sdk iphonesimulator27.1 -destination 'generic/platform=iOS Simulator' build CODE_SIGNING_ALLOWED=NO
```

```bash
# Package unit tests run on macOS with SwiftPM
swift test --package-path Packages/StreamFolioCore
```

```bash
swift test --package-path Packages/StreamFolioUI
```

`DEVELOPMENT_TEAM` is set in `project.yml` (set it there, not in Xcode: the `.xcodeproj` is generated and a regenerate discards Xcode-side signing edits). The App Store Connect record and final bundle ID are `Unknown / Requires Confirmation`; `project.yml` uses `uk.sume.streamfolio` as a placeholder.

## Layout

| Path | Purpose |
| :--- | :--- |
| `project.yml` | XcodeGen spec — the source of truth for targets and build settings |
| `StreamFolio/` | App target: `App/` (`AppEnvironment`), `Navigation/` (tab shell), `Screens/` (Home, Detail, Onboarding) — screens contain no raw styling |
| `scripts/generate_curated_feeds.py` | Regenerates the bundled `curated-feeds.json` from Android's `DefaultFeedsConfig.kt` (a test fails if they differ) |
| `Packages/StreamFolioUI` | `StreamFolioTokens` (pure-Swift design tokens, parity-tested against Android) and `StreamFolioUI` (SwiftUI theme, glass wrappers, article rows/cards, form components, image pipeline) |
| `Packages/StreamFolioCore` | UI-free logic: models, GRDB persistence, RSS/Atom/OPML parser, curated feeds, story grouping, safe networking, link-only policy, bookmarks, preferences, `FeedStore`, `ArticleContentService`/`ArticleExtractor`/`ReaderStore`, `ArticleActions`, custom feeds, `Opml`, `BackupService`, `CategoryLayout`, `ProviderSelection` (134 tests) |

## Rules that keep the code reusable

- Every color/radius/spacing/font comes from `StreamFolioUI` / `StreamFolioTokens`. `DesignSystemRulesTests` fails if `ios/StreamFolio/` uses raw `glassEffect`, hex colors, `UIColor(`, literal corner radii or fixed font sizes, or if `glassEffect` appears in `StreamFolioUI` outside `Glass/`.
- `PaletteParityTests` fails if the Swift tokens differ from `app/.../ui/theme/Color.kt` or `design_system/tokens/*.css`. Android is the source of truth.
- Glass is for controls and navigation only; content (cards, reader text, thumbnails) stays solid.
