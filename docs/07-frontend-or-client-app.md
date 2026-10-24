# 07 — Frontend & Android Client Architecture

> **Last verified commit:** `aaac96970daf81cd5d75cf5fa56dfebf2ecb6b48`  
> **Analysis Date:** 2026-08-25  
> **UI Toolkit:** Jetpack Compose (BOM `2025.05.00`), Material Design 3, Navigation Compose `2.8.2`

---

## 0. High Refresh Rate Handling

[MainActivity.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/MainActivity.kt) calls `requestHighestRefreshRate()` in `onCreate()`, which sets `window.attributes.preferredDisplayModeId` to whichever `Display.Mode` has the highest `refreshRate`. Without this, some OEM display stacks auto-classify third-party app rendering into a refresh-rate category (Android's `FrameRateCategoryRate` mechanism) that maps to less than the display's peak rate even on a 120Hz-capable panel — verified via `adb shell dumpsys display` on a MediaTek device where `high` activity mapped to only 90Hz, capping this app there while system apps reached 120Hz. Setting the preferred display mode explicitly populates the `PRIORITY_APP_REQUEST_BASE_MODE_REFRESH_RATE` vote directly instead of leaving it to that per-device default guess (confirmed via the same `dumpsys` command: `mActiveModeId` moved from the 90Hz mode to the 120Hz one after this change).

---

## 1. Client App Architecture & Navigation Graph

StreamFolio's UI is built entirely using **declarative Jetpack Compose**, governed by Unidirectional Data Flow (UDF). Navigation is orchestrated by `AppNavigation` in [NavRoutes.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/ui/navigation/NavRoutes.kt), a single `NavHost`/`NavController` wrapped entirely in `SharedTransitionLayout` behind one `GlassmorphicNavBar` — Onboarding, Home, Search, Bookmarks, Settings and its 7 sub-pages, `DetailScreen`, and `BookmarkCollectionDetailScreen` are all routes on the same graph. Only the article **thumbnail image** is a `sharedElement` (it flies from its list/card position into the detail hero image); the **title is intentionally not** — interpolating a `Text` between very different font sizes/weights/families either reflows text every animation frame or pops visibly the instant the transition settles, so the title just fades in with the rest of the screen instead.

> **Tried and reverted:** a later change split Settings into a second, separately-mounted `NavHost` outside `SharedTransitionLayout`, since Settings never uses a shared element and paying for `SharedTransitionLayout`'s lookahead layout pass while a Settings screen is active is pure overhead. In practice, Navigation-Compose's `NavHost` isn't designed to be one of several permanently co-mounted siblings: every technique tried to keep two of them alive at once without tearing one down broke something else — conditionally composing/decomposing it destroyed its back-stack entries' lifecycle (blank screen returning to Settings via system back); hiding the inactive one by collapsing its layout size didn't actually stop it from drawing (both surfaces stayed visibly overlapped, from the very first frame, regardless of navigation state — reproduced even on the Onboarding screen); and hiding it by consuming its touch input broke input for the *other*, visible surface too, since Compose's pointer-consumption state is shared across the whole gesture rather than scoped to one subtree (Onboarding's Continue button stopped responding). Given two rounds of that breaking the app on a real device, the split was reverted back to the single-`NavHost` architecture described above. The performance concern it was chasing turned out to be minor compared to the actual bottleneck (see the "Similar Articles Lookup" note below), which is fixed independently of this.

- The list card (`ArticleListItem`, 256×256) and trending carousel (`TrendingCard`, 600×400) requests each set an explicit Coil `memoryCacheKey` of `"thumb_" + article.link`. `DetailScreen`'s hero image (which needs a larger decode to stay sharp at its bigger display size, so it can't just reuse the list's small request as-is) sets `placeholderMemoryCacheKey` to that same key plus `crossfade(true)`, so it paints instantly from whichever thumbnail bitmap is already cached instead of a blank flash, then crossfades to its own higher-resolution decode. All three requests also share the same custom `User-Agent` header (some publishers block the default OkHttp UA).
- The app-wide default Coil `ImageLoader` (used by every `AsyncImage` that doesn't pass its own `imageLoader`) is supplied by [StreamFolioApplication.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/StreamFolioApplication.kt) implementing `ImageLoaderFactory`, registered via `android:name=".StreamFolioApplication"` in `AndroidManifest.xml`. It sets `crossfade(true)` (so every image — favicons, thumbnails, everything — fades in instead of popping), a 25%-of-app-memory memory cache, a 100 MB disk cache, and the same custom `User-Agent` header as above applied globally via an OkHttp interceptor. `SettingsMainScreen`'s promo GIF still builds its own local `ImageLoader` (for GIF decoder support) and is unaffected by this default.

```mermaid
graph TD
    Start([App Launch]) --> OnboardCheck{Completed<br/>Onboarding?}
    OnboardCheck -->|No| OnboardingScreen["OnboardingScreen<br/>(Region, Categories, AI Setup)"]
    OnboardCheck -->|Yes| MainScaffold["Main Scaffold Host<br/>(GlassmorphicNavBar + Floating TTS MiniPlayer)"]

    OnboardingScreen -->|Finished| MainScaffold

    subgraph "Single NavHost (entirely inside SharedTransitionLayout)"
        HomeScreen["HomeScreen<br/>(Hero Trending, Category Pills, Grouped Articles)"]
        SearchScreen["SearchScreen<br/>(Live Search, History Chips, Online News)"]
        BookmarkScreen["BookmarkScreen<br/>(Collection Tiles, Grid/List Mode)"]
        SettingsScreen["SettingsMainScreen<br/>(Hub for Configuration)"]
        SettingsPrefs["SettingsPreferencesScreen"]
        SettingsAi["SettingsAiScreen"]
        SettingsFeeds["SettingsFeedsScreen"]
        SettingsProviders["SettingsProvidersScreen"]
        SettingsReorder["SettingsReorderScreen"]
        SettingsBackup["SettingsBackupScreen"]
        SettingsGestures["SettingsGesturesScreen"]
    end

    MainScaffold --> HomeScreen
    MainScaffold --> SearchScreen
    MainScaffold --> BookmarkScreen
    MainScaffold --> SettingsScreen

    HomeScreen -->|Tap Article| DetailScreen["DetailScreen<br/>(Reader Mode, Web View, AI Summary, Translation)"]
    SearchScreen -->|Tap Article| DetailScreen
    BookmarkScreen -->|Tap Collection| BookmarkDetailScreen["BookmarkCollectionDetailScreen"]
    BookmarkDetailScreen -->|Tap Article| DetailScreen

    SettingsScreen --> SettingsPrefs & SettingsAi & SettingsFeeds & SettingsProviders & SettingsReorder & SettingsBackup & SettingsGestures

    DetailScreen -->|Listen| TtsPlayer["TtsLyricsVisualizer & TtsMiniPlayer"]
```

---

## 2. Screen Specifications & Features

### 1. `HomeScreen` ([HomeScreen.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/ui/screens/HomeScreen.kt))
- **Header & Branding:** StreamFolio brand typography with quick region selector modal.
- **Horizontal Category Pills:** Scrollable chip row displaying user-selected categories (`CategoryPill.kt`).
- **Hero Trending Card:** High-prominence card showcasing the lead story with image thumbnail and publisher badge.
- **Article Feed & Swipe Actions:** Rendered via `LazyColumn` using `SwipeableCard.kt` supporting left/right swipe gestures (customizable: bookmark, share, mark as read).
- **Story Grouping:** Multi-source coverage clustered into collapsible secondary stories (`groupSimilarStories`).
- **Feedback & States:** Pull-to-refresh, animated `SkeletonLoader.kt`, `OfflineIndicator.kt` banner, and Otter mascot empty states.

---

### 2. `DetailScreen` ([DetailScreen.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/ui/screens/DetailScreen.kt))
- **Dual Reading Modes:**
  - **Clean Reader Mode:** Distraction-free text view rendered using Jsoup extracted HTML, customizable font family (`sans_serif`/`serif`), font size slider (12sp–30sp), and line height.
  - **Embedded Web Mode:** Full webview for rich media or interactive paywalled articles.
  - **Web Mode load failures:** the raw WebView error text (e.g. `net::ERR_ADDRESS_UNREACHABLE`) is never shown. A friendly state appears instead: "You're offline" (device has no network, per `NetworkMonitor`) or "Couldn't load this page" (online but the page failed), with a **Try again** button. The page reloads automatically when connectivity returns. Because WebView only reports fast network failures, a page that has made no progress (below 15%) after 20 seconds is also shown as failed (covers a VPN or network that accepts connections but passes no traffic). The web area uses `weight(1f)` + `clipToBounds()` so it stays below the header.
  - **Link-only sources** (e.g. Joho no Todai): the Reader/Web tab switcher is hidden and the screen opens directly in Web Mode; Listen, queue, AI summary and Translate are unavailable (see [ADR-006](file:///d:/News/docs/14-decisions-and-known-limitations.md)).
- **On-Device AI Summary:** Triggers Gemini Nano inference, displaying bulleted key takeaways in an interactive sheet.
- **ML Kit Translation:** Translates headline and body in real-time, displaying source and target language badges.
- **Audio Toolbar:** Immediate launch of TTS speech engine for current article text.
- **"Also Covered By" Panel:** Collapsible list of other sources reporting the same story, reusing the `groupId` already computed at ingest time (see [Flow 1](file:///d:/News/docs/01-architecture.md) in `01-architecture.md`) via a single indexed lookup — `NewsViewModel.loadSimilarArticles()` is deliberately deferred until the shared-element open animation finishes (`animatedVisibilityScope.transition.isRunning`), so it never competes with that transition.

---

### 3. `SearchScreen` ([SearchScreen.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/ui/screens/SearchScreen.kt))
- **Search Bar:** Real-time query input with clear button and animated placeholder.
- **Recent Searches:** Horizontally scrollable chip row of previous queries (persisted up to 10 entries).
- **Hybrid Search Engine:**
  - Fast local Room database search (`LIKE %query%`).
  - Online Google News search query integration (`searchNewsOnline()`) fetching live web results.

---

### 4. `BookmarkScreen` & `BookmarkCollectionDetailScreen`
- **Multi-Collection Organization:** Displays default `"Saved"` collection alongside custom user-created lists.
- **Layout Switching:** Toggle between 2-column grid tiles (`CollectionGridCard`) and compact list rows (`CollectionListRow`).
- **Collection Management:** Create, rename, delete collections, with safe reassignment of articles into the default list upon deletion.
- **Naming:** the page header reads "Saved" (matching the `GlassmorphicNavBar` tab label and the default list's own name) — it previously said "Bookmarks", inconsistent with the rest of the save/collection terminology used everywhere else in the app.
- **Card colors:** `CollectionGridCard`'s empty-state gradient (no preview thumbnail yet) uses the saturated `primary`/`secondary` brand colors, not their `*Container` counterparts — this card renders a plain white icon/badge/text directly on top, which needs a vivid background for contrast. `primaryContainer`/`secondaryContainer` are intentionally pale tints in light theme (correct for a subtle highlight elsewhere, e.g. `SaveToListBottomSheet`'s selected-row background), and washed the card out to near-invisibility against the equally pale light-theme page background when used here.

---

### 5. Settings Modular Sub-Pages (`ui/screens/settings/`)

| Screen File | Configuration Scope |
| :--- | :--- |
| [SettingsMainScreen.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/ui/screens/settings/SettingsMainScreen.kt) | Navigation hub, version info, quick cache statistics, GitHub links. |
| [SettingsPreferencesScreen.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/ui/screens/settings/SettingsPreferencesScreen.kt) | Language, region, dynamic colors toggle, story grouping toggle. |
| [SettingsAiScreen.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/ui/screens/settings/SettingsAiScreen.kt) | Master AI toggle, Gemini Nano AICore status check, translation & smart tags settings. |
| [SettingsFeedsScreen.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/ui/screens/settings/SettingsFeedsScreen.kt) | Custom RSS feed additions, URL validation, OPML import and export. |
| [SettingsProvidersScreen.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/ui/screens/settings/SettingsProvidersScreen.kt) | Enable/disable individual curated news publishers per category and cross-region. |
| [SettingsReorderScreen.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/ui/screens/settings/SettingsReorderScreen.kt) | Drag-and-drop / arrow reordering of category tabs. |
| [SettingsBackupScreen.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/ui/screens/settings/SettingsBackupScreen.kt) | Export application state to JSON, restore from JSON file with validation. |
| [SettingsGesturesScreen.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/ui/screens/settings/SettingsGesturesScreen.kt) | Customize swipe-left and swipe-right card gesture actions. |

---

## 3. Core Shared UI Components

| Component | File Path | Architectural Responsibility |
| :--- | :--- | :--- |
| **`GlassmorphicNavBar`** | [GlassmorphicNavBar.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/ui/components/GlassmorphicNavBar.kt) | Floating blurred navigation pill with icon morphing and active tab indicator. |
| **`TtsMiniPlayer`** | [TtsMiniPlayer.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/ui/components/TtsMiniPlayer.kt) | Floating audio player bar with scrub progress, speed picker (0.5x–3.0x), and sleep timer. |
| **`TtsLyricsVisualizer`** | [TtsMiniPlayer.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/ui/components/TtsMiniPlayer.kt#L550) | Full-screen Apple Music-style lyrics visualizer with real-time sentence/paragraph highlighting. |
| **`PublisherAvatar`** | [PublisherAvatar.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/ui/components/PublisherAvatar.kt) | Publisher fallback badge rendering 2-letter initials with deterministic palette hashing. The initial is centred both when no logo exists and when a logo fails to load. Link-only sources always show the initial (no favicon request). |
| **`BookmarkToastBanner`** | [BookmarkToastBanner.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/ui/components/BookmarkToastBanner.kt) | Animated bottom banner with "Undo" action dispatching `ToastEvent`. |

---

## 4. Design System, Theming & Tokens

The application design system is defined in `ui/theme/` and mirrored in `design_system/`:

- **Primary Colors:** Cobalt Blue (`#1E88E5`), Navy Surface (`#0E1A33`), Deep Background (`#060D1E`), Cyan Accent (`#00B0FF`).
- **Container/outline tones:** [Theme.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/ui/theme/Theme.kt)'s `darkColorScheme()`/`lightColorScheme()` calls explicitly set `primaryContainer`/`onPrimaryContainer`/`secondaryContainer`/`onSecondaryContainer`/`tertiaryContainer`/`onTertiaryContainer`/`outline`/`outlineVariant` (defined in [Color.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/ui/theme/Color.kt)) in the same blue family as the rest of the brand palette. Compose Material3's `darkColorScheme()`/`lightColorScheme()` factories silently default any slot you don't pass to the stock M3 baseline **purple** palette — since these container/outline slots were never set, selected-row backgrounds (e.g. `SaveToListBottomSheet`'s checked list row, using `colorScheme.primaryContainer`) and various card borders across `BookmarkScreen` and the Settings screens rendered purple/gray instead of on-brand blue, verified visually against a real device before and after.
- **Dynamic Theming:** Supports Android 12+ Material You Dynamic Color extraction when `useDynamicColors` is toggled — note dynamic color derives its own full tonal palette from the device wallpaper and is unaffected by the container/outline fix above, which only applies to the static `DarkColorScheme`/`LightColorScheme`.
- **Typography:** Custom `Roboto Flex` / `Roboto Serif` scale spanning `display` (34sp), `brand` (28sp), `card-title` (16sp), down to `nano` (9sp).
- **Corner Radii:** Strict hierarchy — Chrome pills (32dp), Cards (20–24dp), Chips/Buttons (12–16dp), Micro badges (6–8dp).

## iOS Client and Liquid Glass Design System (Phase 0, added 2026-10-24)

The iOS app (`ios/`, SwiftUI, iOS 26+) reuses this section's brand tokens but not its components: Compose cannot produce Liquid Glass, so the UI is rewritten natively. Android remains the behavioral specification. **Status (2026-10-24):** `OnboardingScreen`, `HomeScreen` (live curated feeds), `ArticleDetailScreen` (Reader + Web View), `SavedScreen` + `CollectionScreen`, `SearchScreen` and all of Settings (hub plus seven sub-pages) are real.

- **Onboarding** ([OnboardingScreen.swift](../ios/StreamFolio/Screens/Onboarding/OnboardingScreen.swift)): Welcome → Personalise Your Feed (curated feeds toggle, categories with at least one required, offline cache history) → On-Device AI (master switch, translation, key insights, smart tags; insights are disabled with the reason when `OnDeviceAI.availability` says Apple Intelligence is unavailable) → Region / Language (only the parts that apply; the step is skipped when neither curated feeds nor translation is on). The region and language default from the device locale. Nothing is written until "Start Reading". Android's "import a backup" step is **not built yet** (it needs backup/restore).
- **Home** ([HomeScreen.swift](../ios/StreamFolio/Screens/Home/HomeScreen.swift)): inline title with the glass category chips in a top `safeAreaBar` (the system soft scroll edge fades stories under them; the section headings are plain list rows that scroll away, because pinned `Section` headers drew a translucent band under the chips, and a large title is not used because the edge effect dimmed it in dark mode); region menu (changing it refreshes); "Trending Highlights" carousel (first three stories, `SFTrendingCard`); "Latest Stories" list (`SFArticleRow`, paged 20 at a time, "Also covered by N" for grouped stories, read stories dimmed, saved indicator); pull to refresh; skeleton while the first load runs; otter empty state; swipe actions use the configured left/right actions and a long-press menu offers Save/Share/Read; tapping opens the article with a zoom transition from the card and marks it read. Not yet: publisher filter carousel, AI spotlight / swipe-hint banners, offline banner, tag filter, TTS controls on cards. Rows and cards get their images in the background: after each refresh, stories whose feed had no image (e.g. The Straits Times) are resolved from the article page (`og:image` etc.), and the newest ten article bodies are fetched ahead of time so Reader opens instantly. The list is updated at most every 300 ms while that trickle lands.
- **Article page** ([ArticleDetailScreen.swift](../ios/StreamFolio/Screens/Detail/ArticleDetailScreen.swift), state in `ReaderStore`): a **Reader / Web View** switch (`SFSegmentedControl`: one glass capsule with a sliding brand-blue indicator) floating at the bottom as a safe-area inset — it did not fit beside the three top buttons on a phone, and a `Picker` in a system bottom toolbar ignored taps on the iPhone Duo simulator (iOS 27.1) though it worked on the iPhone Air (iOS 26.5); hidden for link-only publishers, which open straight in the Web View as agreed, the publisher name as the title, "Aa" text settings (font Sans-Serif/Serif, size 12–30 in steps of 2 with A−/A+, line spacing 1.2×/1.5×/1.8×; these are the `reader_*` preferences, applied live and scaled by Dynamic Type), share, and save (the button follows the live article). Reader shows the hero image (or the publisher name on the brand gradient), category, headline, publisher avatar, relative time and "N min read" (200 words/minute), then the extracted text; while loading a skeleton; if the text is shorter than 150 characters or could not be extracted, the feed description plus a hint to use Web View. Opening an article marks it read and, if the feed had no image, resolves one from the page. Web View: `WebPage`/`WebView` with a progress bar and a "Couldn't load this page" state with Try again. Not yet: AI summary, translation, Listen/queue, "also covered by" panel, tags, the 20-second no-progress and offline-banner handling in Web View, the typography panel is a popover rather than an inline card.
- **Saved** ([SavedScreen.swift](../ios/StreamFolio/Screens/Saved/SavedScreen.swift), `SavedStore`): every collection as tiles (2-column grid, 145 pt, preview photo under a scrim or the brand gradient, count badge) or rows (grid/list toggle persisted as `bookmark_list_view_mode_is_grid`), starting with "All Saved" and ending with a dashed "New Collection" tile; the default list is named "Saved", as on Android. Empty state with the otter when nothing is saved. A collection opens `CollectionScreen` (its articles as the shared `StoryRow`, swipe = Remove, or Unsave in All Saved; title + "N saved articles"; "…" menu on custom collections: Rename, Delete). Deleting an empty collection just confirms; a non-empty one offers "Move Articles to Another Collection…" (destination picker) or "Delete Collection and Its Saves". Names are trimmed and unique; the default list cannot be renamed or deleted.
- **Saving:** swiping a story (default: swipe left) saves it to the default "Saved" collection in one step; long-press (or long-press the save button on the article page) → "Save to Collection…" opens `SaveToCollectionSheet` to tick several collections (it starts from the story's current ones, else the collection used last, else "Saved"), create a new one inline, and Done. Android's swipe opens the sheet instead; on iOS the sheet is one step further so a swipe stays instant.
- **Search** ([SearchScreen.swift](../ios/StreamFolio/Screens/Search/SearchScreen.swift), `SearchStore`/`SearchService`): the system search tab. While empty: "Trending Topics" chips (most frequent categories, tags and headline words of the newest 50 articles from your active sources; defaults when there are none) and "Recent Searches" (newest first, max 8, remove one or Clear All with confirmation) or the otter "Start searching". Typing is debounced 500 ms, then (1) the local database is searched (title/description, all categories; queries of 3 characters or fewer match whole words) and results update live, and (2) the active curated feeds are fetched in parallel (8 at a time), filtered by title/description/publisher and cached under category `SEARCH` (replacing the previous search's unsaved results). Link-only publishers are never fetched by search — only their cached headlines show. Results use the shared `StoryRow`. Not done: Android's animated typewriter placeholder.
- **Settings** ([Screens/Settings](../ios/StreamFolio/Screens/Settings/SettingsScreen.swift)): a `NavigationStack` hub (`SettingsRoute`) with the same groups and rows as Android — Personalisation (Preferences, Swipe Gestures), Data Management (Manage Content & Sources, Custom RSS Feeds, Backup & Restore), AI Services, About (version and build, Contact Us mail link) and "More from Dominik Studios" (The Job Log link). Sub-pages use the system back button and an inline title instead of Android's custom top bar. **Preferences:** region, offline cache history (1/3/7/14/30 days, Keep All), Group Similar Stories (Home regroups at once); the Dynamic Colors row is not shown. **Swipe Gestures:** left/right action menus (Toggle Bookmark, Share, Toggle Read, None). **Manage Content & Sources:** curated feeds switch + category chips (at least one stays on), links to Reorder and Providers; changes refresh Home. **Custom RSS Feeds:** list with tab-label filter, add/edit sheet (name, HTTPS URL, tab label; an unsafe URL shows "Please enter a valid HTTPS URL"), delete with confirmation; the label becomes a Home tab. **Providers:** curated publishers by region or by category with region/category filters, per-publisher switches (turning one on fetches it now), link-only publishers listed only while the remote list enables them; the page builds rows lazily (about 400 rows made the first open very slow). **Reorder Category Tabs:** drag handles (always in edit mode) with a live preview strip, VoiceOver "Move Up/Down" actions; Home follows immediately. **AI Services:** Apple Intelligence availability card, the master switch and three services, target language when translation is on, and a note that the features themselves are not built yet. **Backup & Restore:** export/import settings JSON, export/import OPML through the Files picker; restore asks for confirmation; OPML import opens a sheet to tick and rename folders; results show in alerts (Android uses toasts).
- **Shared by onboarding and Settings:** `CuratedFeedsPicker`, `CacheRetentionField`, `AIServiceToggles`, `RegionField`, `LanguageField` ([SettingsFields.swift](../ios/StreamFolio/Screens/Shared/SettingsFields.swift)) — written once, so both places offer the same choices and rules. UI building blocks in `StreamFolioUI`: `SFSettingsPage` (optionally lazy), `SFSettingsSection`, `SFSettingsCard`, `SFSettingsRow`, `SFSettingsToggleRow`, `SFHeroBanner`, `SFIconTile`, `SFTextField`, `SFFlowLayout`, `SFCategoryIcon`, `sfSheetBackground()` (every sheet: an opaque brand backdrop so the screen behind never shows through). `AppPreferences.binding(_:)` gives screens a binding to any preference.
- **Tab bar:** the system tab bar minimizes while scrolling down (`tabBarMinimizeBehavior(.onScrollDown)`), but a screen reports when its scroll content is at the very top (`reportsScrollAtTop()` → `ChromeState.isAtTop`) and the tab bar is pinned open there (`.never`). Reason: after a fast fling back to the top the system bar could stay collapsed for a few seconds. Applied to Home, Saved, collections and Search results.
- **One row, everywhere:** `StoryRow` (swipe actions, long-press menu, tap-to-open with the zoom transition) is the single row used by Home, Search and collections; `ArticleInteractions` runs its actions and `RootView` presents the share and "Save to Collection" sheets once for the whole app.
- **Shared components:** `SFArticleRow`, `SFTrendingCard`, `SFPublisherAvatar` (initials on a deterministic color; never loads a favicon), `SFReaderHeader`/`SFReaderBody`/`SFTypographyPanel`/`SFTextSkeleton`/`SFInfoBanner` (reader), `SFCollectionCard`/`SFCollectionRow`/`SFNewCollectionCard`/`SFCollectionPickerRow`, `SFSegmentedControl`, `SFRemoteImage`/`ImagePipeline` (SSRF-checked, 25 MB memory + 100 MB disk cache, downsampled; never requests images from link-only publishers), `SFToggleCard`, `SFSubToggleRow`, `SFSelectableChip`, `SFMenuField`, `SFStepIndicator`, `SFPrimaryButton`/`SFSecondaryButton`, `SFEmptyState`, `SFSkeletonList`, `SFSwipeActionStyle`.

- **Navigation:** the system `TabView` is the glass tab bar (Home, Saved, Settings, and Search using `Tab(role: .search)` so it sits apart at the trailing edge). This replaces the hand-built `GlassmorphicNavBar` pill and changes the tab order relative to Android (Home, Search, Saved, Settings). `tabBarMinimizeBehavior(.onScrollDown)` shrinks the bar on scroll; `tabViewBottomAccessory` is reserved for the TTS mini player (placeholder today).
- **Where glass is used:** controls and navigation only — tab bar, category chips (`SFCategoryChips`, one `GlassEffectContainer`, selected chip tinted `SFColor.primary`), prominent buttons. Content (cards, reader text, thumbnails) stays solid: `sfCard()` is a `surface` fill with a hairline border, like the Android cards.
- **Backdrop:** `SFBackdrop` reproduces `getThemeBackgroundBrush()` (light: primary 12% → surfaceVariant 15% → background; dark: background → surfaceVariant 30% → background). Glass needs colored content behind it to read as glass.
- **Tokens:** `SFPalette` / `SFRole` / `SFAlpha` (colors, light/dark pairs, text/border alpha ladder), `SFRadius`, `SFSpacing`, `SFStroke` in `StreamFolioTokens`; `SFColor`, `SFFont`, `SFGradient`, `SFBackdrop` in `StreamFolioUI`. Values equal `Color.kt` and `design_system/tokens/*.css`; `PaletteParityTests` enforces it. The Android `Emerald*` names are historical — the values are cobalt blue.
- **Typography:** Android bundles no font (it uses the platform default, Roboto). iOS uses the platform default (SF Pro) at the nearest Dynamic Type text style (`SFFont.display` = large title … `SFFont.nano` = smallest caption), so text scales with the user's size setting. No font files are bundled.
- **Dropped on iOS:** Material You dynamic color (`use_dynamic_colors` stays in backups but is ignored); `design_system/tokens/motion.css` states the Android app uses tweens only, while glass uses the system's springs.
- **Contrast note:** white on the brand primary `#1E88E5` is about 3.7:1 (calculated, not measured on device), below 4.5:1 for small text. Use bold/large text or `secondary` (`#1565C0`) on tinted glass.
- **Rules enforced by tests:** screens contain no raw `glassEffect`, hex colors, `UIColor(`, literal corner radii or fixed font sizes (`DesignSystemRulesTests`).
- **Verified visually (2026-10-24, simulator screenshots):** the shell was built with the iOS 27.1 SDK and checked on the iPhone Duo (iOS 27.1, light and dark) and the iPhone Air (iOS 26.5, light): brand-blue selected chip, solid cards, glass tab bar, bottom accessory, and the tab bar minimizing on scroll all render. Not yet checked: a standard iPhone on iOS 27.1 (the installed 27.1 runtime only lists the Duo), dark mode on the Air, Increase Contrast / Reduce Transparency, Dynamic Type. On the Duo the tab bar becomes a trailing vertical rail and the status strip overlaps the chip row; layouts must adapt to that. The placeholder "Nothing playing" accessory was removed again: the real mini player will return in the TTS phase once it can hide when idle (`tabViewBottomAccessory(isEnabled:)` is iOS 26.1+). The article page (Reader in light and dark, Aa popover, Reader ⇄ Web View) was checked on both the iPhone Duo (iOS 27.1) and the iPhone Air (iOS 26.5): on the Duo the top buttons become a vertical rail and do not overlap. **Settings (2026-10-24):** the hub, Preferences, Swipe Gestures (menu change), Manage Content, Reorder (drag, with Home following), Custom RSS Feeds (add through the sheet; the "Gadgets" tab filled with the feed's stories), Providers, AI Services (switch on/off) and Backup (export wrote a valid v2 file) were exercised on the iPhone Air (iOS 26.5); the hub and Custom RSS Feeds on the iPhone Duo (iOS 27.1), where the back and add buttons sit on the vertical rail without overlapping. Not exercised by hand: restore, OPML import/export, edit and delete of a feed, Increase Contrast, Dynamic Type. Onboarding and Home were additionally checked on the iPhone Air (iOS 26.5; onboarding in full, Home live) and Home on the iPhone Duo (iOS 27.1). A carousel bug was found and fixed this way: a `scaledToFill` image inside a ZStack inflated the card's layout width, so `SFRemoteImage` now draws the photo as an overlay of the gradient.

## Share Links and App Links (added 2026-10-23)

- Sharing an article (Home swipe action, Home long-press menu, `DetailScreen` share button) builds its text with `ShareLinks.buildShareText()` in [ShareLinks.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/util/ShareLinks.kt): the title, `Read more: https://streamfolio.sume.uk/open?url=<url-encoded https article URL>`, and `Get StreamFolio: <Play Store URL>`. The article's own URL is no longer shared directly.
- `MainActivity` declares a `VIEW` intent filter (`autoVerify="true"`, `https`, host `streamfolio.sume.uk`, `pathPrefix /open`). `handleArticleIntent()` takes the article URL from the widget `article_url` extra or, failing that, `ShareLinks.parseArticleUrl(intent.data)`, and passes it through `UrlSecurityValidator.normalizeToHttps` into `setPendingArticleUrl` (opens `DetailScreen`).
- Without the app, or when Android does not auto-open the link, the browser loads [landing/open.html](file:///d:/News/landing/open.html): "Download StreamFolio" (Play Store), "Open in browser" (the original article), and on Android "Open in StreamFolio" (an `intent://` URI scoped to package `uk.sume.streamfolio`, falling back to the Play Store).
- **Direct app opening:** [landing/.well-known/assetlinks.json](file:///d:/News/landing/.well-known/assetlinks.json) lists package `uk.sume.streamfolio` with two SHA-256 certificate fingerprints: the Play app signing key (supplied by the maintainer, Unknown / Requires Confirmation) and the key that signs locally built APKs such as `app/release/streamfolio_v1.3.4.apk` (read with `apksigner verify --print-certs`, starts `C2:C5:6D`). `landing/.nojekyll` makes GitHub Pages serve the `.well-known` folder. The site must be deployed and the app reinstalled/updated before Android marks the link `verified` (check: `adb shell pm get-app-links uk.sume.streamfolio`). Until verified, Android 12+ shows the web page and the "Open in StreamFolio" button is the route into the app. Hosting must also serve `/open` as `open.html`: the site is on Vercel, where [landing/vercel.json](file:///d:/News/landing/vercel.json) rewrites `/open` to `/open.html` (assumes the Vercel project root is `landing/` — Unknown / Requires Confirmation). Not tested on a device.
