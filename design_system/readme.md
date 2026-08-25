# StreamFolio Design System

StreamFolio is a modern RSS and news-reading app for Android — Kotlin, Jetpack Compose, Material 3. It pulls a user's feeds into one clean reading surface: curated regional publishers plus custom RSS, grouped article coverage, bookmark collections, text-to-speech playback with a queue, and on-device AI for translation, summaries and semantic tags. It ships free on Google Play as `uk.sume.streamfolio`, version 1.3.0, GPL-3.0.

There is exactly one product: the Android app. No marketing site, no web client, no docs site. This design system therefore has one UI kit.

## Sources

Everything here was read from the app's source, not from screenshots.

- **GitHub:** <https://github.com/sm32d/streamfolio> (branch `master`) — the Kotlin/Compose app. Worth exploring further than this system does; the design lives in:
  - `app/src/main/java/uk/sume/streamfolio/ui/theme/` — `Color.kt`, `Theme.kt`, `Type.kt`
  - `app/src/main/java/uk/sume/streamfolio/ui/components/` — 16 shared composables
  - `app/src/main/java/uk/sume/streamfolio/ui/screens/` — Home, Detail, Search, Bookmark, Onboarding, and nine Settings sub-screens
  - `app/src/main/res/drawable-xxxhdpi/` — the otter illustrations
  - `app/src/main/ic_launcher-playstore.png` — the app icon
- **Play Store:** <https://play.google.com/store/apps/details?id=uk.sume.streamfolio>
- No Figma file, no brand guidelines document, no slide template were provided.

See `github.md` for the sync record.

---

## Content fundamentals

**Second person, present tense, plain.** The app talks to the reader about their feed, never about itself. "Customise your StreamFolio experience." "Your saved collections and reads." "Search articles and topics."

**British spelling.** `Customise`, `Personalisation`, `organise`. It is inconsistent in one place — `Customize left & right swipe actions` — so prefer `-ise` and treat the `-ize` instance as a bug, not a pattern.

**Sentence case everywhere except two places.** Titles, subtitles, button labels and toasts are sentence case. The exceptions are uppercase section labels in Settings (`DATA MANAGEMENT`, 11sp, +1 tracking) and the publisher line on article cards, which is the source name in bold primary.

**Titles are nouns; subtitles are verb phrases.** `Custom RSS Feeds` / "Add and manage private RSS publishers". `Backup & Restore` / "Import/export feeds and settings backup". `Preferences` / "Language & region for your news feed". Subtitles never end with a period.

**Decisions are questions.** Destructive confirmations title as a question and explain the consequence in one sentence: "Stop audio playback?" / "Dismissing the player will stop the current article and clear the audio queue."

**Empty states teach the gesture.** They never just state the absence: "No saved bookmarks" is followed by "Swipe right on articles in the Home feed to save them to collections." Error and unavailable states say what is missing and what happens instead: "AI insights require Gemini Nano (AICore), which is not available on this device. The standard article preview is shown below instead."

**Privacy is stated, not implied.** The AI copy always closes on locality: "Private and run 100% locally", "On-Device" as a badge next to AI output.

**No emoji in the product.** The repo README uses them heavily; the app uses none. Icons carry all the visual shorthand.

**One deliberate exception to all of the above: StreamFolio FM.** Tapping the version row five times unlocks an AI radio-DJ persona, and it is the only place slang, caps and exclamation marks appear: "Yo! You're locked into StreamFolio FM 99.9! All the top headlines, zero noise. Stay tuned!" Its lines are assembled from rotating fragments — intros ("Now spinning on 99.9 FM: ", "This just in: ", "Breaking news coming your way: "), attributions ("From %s.", "Courtesy of %s.", "via %s."), transitions ("Alright, let's get into this...") and quips ("Stay locked in.", "This one's got people talking."). Do not let this voice leak into product copy.

**Interface vocabulary.** Saved (not Favourites) · Collections (not Folders or Lists in UI, though the data model calls them lists) · Publishers (not Sources, in UI) · Curation sources (in Settings) · Smart Tags · Key Insights · Reader / Web · Tune Off.

---

## Visual foundations

**Colour.** One brand hue: cobalt blue, taken directly from the app-icon gradient. `#1E88E5` primary, `#1565C0` secondary, `#00B0FF` as a light cyan accent. (The Kotlin constants are still named `EmeraldPrimary` / `MintAccent` from an earlier green identity — the names are stale, the values are blue.) Neutrals are not grey: the light scheme runs `#F0F6FC` background on `#FFFFFF` surfaces with `#E1EBF5` variant; the dark scheme is near-black navy — `#060D1E`, `#0E1A33`, `#1A2B4C`. Semantic accents are hardcoded per action rather than themed: green `#10B981` for save/queue, cyan `#00B0FF` for read status, amber `#F59E0B` for share, violet `#8B5CF6` for mark-as-read, red `#EF5350` for remove, `#D32F2F` for destructive.

**Alpha, not extra colours.** The entire text and border hierarchy is `onSurface` at descending alpha — 85% body, 60% descriptions, 50% subtitles, 45% section labels, 40% timestamps, 30% chevrons, then 12/8/6% for borders. There are no grey tokens. Learn the ladder and you have the whole palette.

**Typography.** Roboto — Compose's `FontFamily.Default`, unchanged. The only named text style in `Type.kt` is `bodyLarge` at 16sp/24sp with 0.5sp tracking; every other size is written inline at the call site. The scale in use: 34 screen title, 28 wordmark (Black 900, the only Black in the app), 24 sub-page title, 20 sheet title, 18 section header, 16 card title, 15 row title, 14 body, 13 dense, 12 meta, 11 caption, 10 nav label, 9 the LIVE chip. Weights run 400–900; SemiBold 600 for row titles, Bold 700 for anything that leads, ExtraBold 800 only on the FM station ident.

**Backgrounds.** Every screen sits on a vertical three-stop gradient — in light mode primary at 12% → surfaceVariant at 15% → background; in dark mode background → surfaceVariant at 30% → background. It is subtle, cool and always present. No photography, no patterns, no textures, no noise. The only illustration in the product is the otter, and it appears only in empty states.

**Gradients have exactly four jobs.** The icon gradient (`#1565C0 → #1E88E5`) for brand tiles; blue→green (`#3B82F6 → #10B981`) for missing article images; indigo→teal (`#6366F1 → #0D9488`) for the on-device AI spotlight and nothing else; and a bottom-up black scrim at 85% for text over imagery. Never invent a fifth.

**Cards.** Two treatments, cleanly separated. Content cards — articles, settings groups, bookmark rows — use a translucent surface (60%), a 1px `onSurface` 6–7% hairline, 20px radius and **no shadow**. Chrome — the nav bar, the audio mini player, the radio card — uses surface at 98%, a 1px *white* 15% hairline, a 32px radius, a wide soft shadow and a backdrop blur. If it floats, it has a shadow and a white edge; if it sits in the flow, it has a hairline and none.

**Radius encodes hierarchy.** 32 chrome · 28 bottom-sheet top corners · 24 media heroes and pill buttons · 20 content cards and pills · 18 the search field · 16 thumbnails and fields · 14 sheet CTAs · 12 icon tiles and standard buttons · 10 small tiles · 8 chips · 6 the smallest badge. Circles are reserved for avatars, play buttons and floating action circles.

**Spacing.** Compose dp values used verbatim; 10, 14 and 18 are as common as 8, 12 and 16, so do not snap anything to a 4/8 grid. The one universal rule is a **24px side gutter** — headers, category strips, feed cards, empty states and the nav bar all align to it. Bookmark grids and the AI banner use 20px. Stacked article cards sit 8px apart with 12px of internal padding; settings and radio cards use 18px internal padding. Feeds carry 96px of bottom padding to clear the nav pill, 160px when the mini player is docked. Minimum touch target 48px.

**Transparency and blur.** Used for chrome and for anything that overlays: nav bar and mini player at 98% with a blur, category pills at 75%, content cards at 60%, surfaceVariant fields at 50%. Settings blurs to 16px behind the FM card. Content itself is never blurred.

**Animation.** Tweens only — no springs, no bounce, no overshoot. 150ms for snap-backs, 200ms for indicator fades and card returns, 250/300ms for toast exit/enter and swipe flings, 1000ms linear looping for the shimmer, 280–420ms linear alternating for the equalizer bars. Off-centre pager pages scale to 0.85 and fade to 0.5. Shared-element transitions carry a card's thumbnail and title into the reader. Loading uses a shimmer skeleton on a cold load and a 3px linear progress bar over existing content — never a spinner in the middle of a feed.

**States.** There are no CSS-style hover states — it is a touch product. Presses are Material ripples inside a rounded clip, except on pills and rows, where ripple is disabled (`indication = null`) and replaced by a **long-press haptic**, which fires on essentially every interaction: pill taps, switch flips, bookmark toggles, refresh, swipe thresholds. Selection is a colour change, not a shape change: a selected pill fills primary and its label goes bold white; a selected publisher avatar gains a 2px primary ring; a selected tab tints icon and label primary with no indicator bar. Read articles drop to 55% opacity. Disabled controls grey their glyph to `onSurface` 30%.

**Swipe is a first-class interaction.** Article cards swipe left and right to run user-configured actions (save / share / mark read), revealing a 20px-radius background tinted at 20% of the action's colour with an icon and a short caps-free label ("Save", "Share", "Unread") pinned to the swipe edge. The mini player drags down to dismiss with a confirmation sheet at 150px.

**Layout rules.** Edge-to-edge: transparent status and navigation bars, content drawn under them with inset padding. The bottom nav is a floating pill, not an attached bar. Modals are always bottom sheets with a drag handle and 28px top corners — the app contains no centred dialogs.

---

## Iconography

**Material Icons, via Compose.** The app uses `androidx.compose.material.icons` exclusively: `Icons.Default.*` (filled) for actions and `Icons.Outlined.*` for additive or softer states — the bookmark toggle is the clearest case, `Icons.Outlined.BookmarkAdd` when unsaved, `Icons.Filled.Bookmark` when saved. There is no custom icon set, no icon font file, no SVG sprite in the repo, and no PNG icons.

For web recreations this system loads **Material Symbols Rounded** from Google Fonts and wraps it as `Icon`. Material Symbols is the same icon program's current release, so glyph names map almost 1:1 (`bookmark`, `bookmark_add`, `play_arrow`, `playlist_add`, `rss_feed`). Flagged as a substitution: the rounded style is slightly softer than the Compose default set, and the variable `FILL` axis stands in for the filled/outlined split. Swap in Material Symbols Outlined if you need an exact match.

Sizes in use: 12 (sleep-timer badge), 16, 18 (card actions), 20 (settings tiles, sheet rows), 22, 24 (nav bar, top bars), 28 (hero play), 64 (icon-only empty states).

Glyph inventory as used: home · search · bookmark · bookmark_add · settings · play_arrow · pause · skip_next · playlist_add · queue_music · hearing · bedtime · stop · radio · article · auto_awesome · translate · rss_feed · list · tune · swipe · backup · email · wifi_off · library_books · gesture · newspaper · folder · folder_special · create_new_folder · grid_view · view_list · add · close · done · delete · share · refresh · content_copy · info · history · trending_up · arrow_back · keyboard_arrow_right · keyboard_arrow_down · keyboard_arrow_up · arrow_drop_down · format_size.

**No emoji, no unicode glyph icons.** The one unicode character used decoratively is a middle dot `•` separating a timestamp from a reading time.

**Brand marks.** `assets/logo-app-icon.png` is the real app icon: a single cream stroke that draws an **S** and resolves into three broadcast arcs, on a cobalt gradient. `assets/logo-app-icon-round.webp` is the round launcher variant, `assets/logo-mark-foreground.webp` the adaptive-icon foreground. The repo contains **no SVG, no wordmark lockup and no monochrome variant** — where a wordmark is needed, set "StreamFolio" in Roboto Black at the primary colour, exactly as the Home header does.

**Illustration.** Four hand-drawn otters, one per empty state — `otter-search-welcome`, `otter-no-search`, `otter-no-articles`, `otter-no-bookmarks`. Warm brown line-and-wash on soft blue water, transparent background, drawn at 1080px. They are the brand's only illustration and its only warm colour. Do not draw new otters; ask for them.

---

## Index

**Root**
- `styles.css` — the single entry point consumers link; `@import`s only
- `readme.md` (this file) · `SKILL.md` · `github.md` · `thumbnail.html`

**`tokens/`** — `fonts.css` · `colors.css` · `typography.css` · `spacing.css` · `shape.css` · `elevation.css` · `motion.css` · `base.css`

**`assets/`** — `logo-app-icon.png` · `logo-app-icon-round.webp` · `logo-mark-foreground.webp` · `otter-no-articles.png` · `otter-no-bookmarks.png` · `otter-no-search.png` · `otter-search-welcome.png`

**`guidelines/`** — 18 specimen cards across Colors, Type, Shape, Spacing, Motion and Brand.

**Components** — one directory per concern, each with a `@dsCard` showcase.

| Directory | Components |
| --- | --- |
| `components/foundation/` | `Icon` |
| `components/navigation/` | `CategoryPill`, `GlassmorphicNavBar`, `SubPageTopBar` |
| `components/article/` | `ArticleCard`, `TrendingCard`, `PublisherAvatar` |
| `components/feedback/` | `EmptyState`, `OfflineIndicator`, `SkeletonLoader`, `ToastBanner`, `SwipeHintBanner` |
| `components/settings/` | `SettingsCard`, `CardDivider`, `SettingsRow`, `SectionLabel`, `StreamFolioSwitch`, `SelectorField`, `InfoNote` |
| `components/sheets/` | `BottomSheet`, `SheetActionRow`, `ConfirmationSheet` |
| `components/collections/` | `CollectionGridCard`, `AddCollectionGridCard`, `CollectionListRow` |
| `components/promo/` | `AiSpotlightBanner`, `RadioCard` |
| `components/player/` | `TtsMiniPlayer` |

Every component maps to a real composable in the app:

| Component | Source |
| --- | --- |
| `CategoryPill` | `ui/components/CategoryPill.kt` |
| `GlassmorphicNavBar` | `ui/components/GlassmorphicNavBar.kt` |
| `SubPageTopBar`, `SectionLabel`, `SettingsCard`, `CardDivider`, `SettingsRow`, `InfoNote`, `StreamFolioSwitch` (`HapticSwitch`), `SelectorField` (`SettingsSelectorField`) | `ui/components/SettingsComponents.kt` |
| `ArticleCard` (`ArticleListItem`), `TrendingCard`, `SwipeHintBanner`, `AiSpotlightBanner`, `SheetActionRow` (`BottomSheetActionRow`) | `ui/screens/HomeScreen.kt` |
| `PublisherAvatar` | `ui/components/PublisherAvatar.kt` |
| `EmptyState` | `ui/components/EmptyState.kt` |
| `OfflineIndicator` | `ui/components/OfflineIndicator.kt` |
| `SkeletonLoader` | `ui/components/SkeletonLoader.kt` |
| `ToastBanner` (`BookmarkToastBanner`) | `ui/components/BookmarkToastBanner.kt` |
| `ConfirmationSheet` (`ConfirmationModalSheet`) | `ui/components/ConfirmationModalSheet.kt` |
| `CollectionGridCard`, `AddCollectionGridCard`, `CollectionListRow` | `ui/screens/BookmarkScreen.kt` |
| `RadioCard` (`StreamFolioRadioCard`) | `ui/components/StreamFolioRadioCard.kt` |
| `TtsMiniPlayer` | `ui/components/TtsMiniPlayer.kt` |
| `BottomSheet` | the `ModalBottomSheet` pattern shared across screens |

**`ui_kits/android-app/`** — interactive 412×892 recreation of the app: feed, reader, search, bookmarks, settings. See its README for the flows that work and the known deviations.

**`templates/news-feed-screen/`** — a starting-point screen for consuming projects.

### Intentional additions

- **`Icon`** — the app has no icon wrapper; Compose provides `Icon` natively. A web equivalent is required, so this system ships one over Material Symbols Rounded.
- **`BottomSheet`** — the app repeats the `ModalBottomSheet` configuration (28dp top corners, drag handle, solid surface, 24dp padding) inline in six places rather than factoring it out. Extracted here so the modal treatment stays consistent.

### Not recreated

Composables read but deliberately left out, because they are app plumbing rather than reusable design: `SwipeableCard` (the Tinder-style drag wrapper, superseded in Home by `SwipeToDismissBox`), `RegionPickerModal`, `SaveToListBottomSheet`, `TtsLyricsVisualizer` (the ~1,300-line full-screen player), `OnboardingScreen`'s five steps, and the home-screen app widget (`res/layout/streamfolio_widget*.xml`). If any of these should become components, say so and they will be built.

## Substitutions to confirm

1. **Fonts.** The app ships no font files — `Type.kt` uses `FontFamily.Default`, which resolves to the platform Roboto. This system loads Roboto, Roboto Flex and Roboto Serif from Google Fonts. If StreamFolio has adopted a specific face, send the files.
2. **Icons.** Material Symbols Rounded stands in for the Compose Material Icons set. See Iconography above.
3. **Article imagery.** No editorial images exist in the repo, so cards and the reader use the app's authentic missing-image fallback throughout. Real thumbnails are scraped per article at runtime.
