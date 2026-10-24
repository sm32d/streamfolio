# 14 — Architecture Decisions & Known Limitations

> **Last verified commit:** `aaac96970daf81cd5d75cf5fa56dfebf2ecb6b48`  
> **Analysis Date:** 2026-08-25

---

## 1. Key Inferred Architecture Decision Records (ADRs)

The following architectural decisions have been inferred from code analysis, commit history, and schema migrations:

### ADR-001: Zero-Cloud / 100% On-Device Architecture
- **Status:** Accepted / Permanent
- **Decision:** StreamFolio operates without any central web application server, cloud database, or remote user authentication. All subscriptions, reading history, and bookmarks live strictly on the user's Android device in SQLite.
- **Rationale:** Guarantees absolute user privacy, zero subscription costs for infrastructure, offline operability, and immunity to centralized data breaches.

---

### ADR-002: On-Device GenAI Summarization & Offline Translation
- **Status:** Accepted
- **Decision:** Use Google ML Kit GenAI Summarization (Gemini Nano / AICore) and ML Kit Translate instead of cloud-based LLM APIs (e.g. OpenAI or Gemini Cloud API keys).
- **Rationale:** Prevents leaking user reading habits or proprietary news content to external cloud API endpoints. Allows features to function with zero API key configuration and zero ongoing operational cost.

---

### ADR-003: Non-Destructive Room Migrations (v5+)
- **Status:** Accepted
- **Decision:** Enforce explicit `Migration(N, N+1)` definitions for schema version 5 and all future updates in [Migrations.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/data/local/Migrations.kt), restricting `fallbackToDestructiveMigrationFrom(1, 2, 3, 4)` to legacy installs.
- **Rationale:** User bookmarks, reading history, and custom collections must never be wiped during app updates.

---

### ADR-004: Layered Defensive SSRF Interception
- **Status:** Accepted
- **Decision:** Pair static URL validation ([UrlSecurityValidator.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/util/UrlSecurityValidator.kt)) with an OkHttp network interceptor ([SsrfProtectionInterceptor.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/data/network/SsrfProtectionInterceptor.kt)) that resolves DNS at execution time.
- **Rationale:** Prevents both direct IP probing (e.g. `http://169.254.169.254/`) and DNS rebinding attacks where an attacker-controlled hostname resolves to a private IP at fetch time.

---

### ADR-005: MediaSession Foreground Audio Service for TTS
- **Status:** Accepted
- **Decision:** Implement TTS playback via an explicit Android Foreground Service (`TtsPlaybackService`) integrated with `MediaSessionCompat`.
- **Rationale:** Enables background playback when the app is minimized, lockscreen media controls, Bluetooth headphone button handling, and Android Auto interoperability.

### ADR-006: Link-Only Sources with a Static Remote Enable List
- **Status:** Implemented and enabled in `source-config/sources.json` (see "Rollout state" below). Terms agreed with the publisher by email; the publisher's own wording is not stored in the repo.
- **Context:** The publisher of *Joho no Todai* (English feed `https://joho-todai.com/en/rss/`) permits use in StreamFolio's curated sources only on limited, revocable terms: publication name, original headline, publication date, direct article link and the feed's short text `description`, cached on the reader's device; no images (including images embedded in `description`); no article bodies or `content:encoded`; no translation, generated summaries, model training, redistribution or sublicensing. Fetching is limited to one request per device every 30 minutes, using conditional requests and an identifying StreamFolio User-Agent, subject to any longer refresh or back-off instruction from the publisher's server. On withdrawal, fetching must stop on existing installs and their cached fields must be removed when they next connect.
- **Decision:**
  - A host-based registry, [LinkOnlySources.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/data/network/LinkOnlySources.kt), marks feeds and article links as "link-only". Behavior keys off the host, so it also applies if a user adds the same feed as a custom feed, via OPML, or opens a shared link.
  - [RssParser.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/data/network/RssParser.kt) ignores `content`, `content:encoded`, `enclosure`, `media:content`, `media:thumbnail` and `itunes:image` for link-only feeds, strips all HTML from `description` (dropping embedded images) and truncates it to `LinkOnlySources.MAX_DESCRIPTION_CHARS` (500).
  - [NewsRepository.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/data/network/NewsRepository.kt) fetches link-only feeds through `fetchLinkOnlyFeedXml()`: per-device 30-minute minimum interval, `If-None-Match` / `If-Modified-Since`, `User-Agent: StreamFolio/<versionName> (Android feed reader; headline-and-link only)`, and obedience to `Cache-Control: max-age`, feed `<ttl>`, `Retry-After`, 429/503 (1 h default back-off) and 404/410 (24 h). Rules live in [LinkOnlyFetchPolicy.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/data/network/LinkOnlyFetchPolicy.kt). Online search never fetches link-only feeds.
  - For link-only articles the app never fetches the article page or thumbnails (`resolveThumbnail`, `fetchArticleBody`, `scrapeTransientArticle` return early), never pre-scrapes bodies, skips on-device AI summarization during tagging (keyword tags only), blocks Listen/queue (TTS), AI summary and translation, shows a letter avatar instead of a favicon, and opens the article in the Detail screen's **Web View** (the Reader tab is hidden) so the article is read on the publisher's website. They are excluded from backup export and skipped on restore.
  - **Remote enable list:** a static JSON file in this repository, [source-config/sources.json](file:///d:/News/source-config/sources.json), fetched from `https://raw.githubusercontent.com/sm32d/streamfolio/master/source-config/sources.json` (constant `SourceConfig.URL`). Format: `{"version":1,"sources":{"<id>":{"enabled":true|false}}}`. The app downloads it at the start of every feed refresh (foreground, background sync and widget), before any link-only feed is requested; a download validated within the last 60 seconds is reused only to coalesce a burst of refreshes. A link-only source that is missing from the file or has `enabled: false` is not fetched, is hidden from Settings > Providers, and its cached articles (including bookmarks and list associations) and throttle state are deleted from the device. If the file cannot be fetched or parsed (or has not been validated within 60 seconds), fetching of link-only feeds is suspended until a fresh list is validated; if no list has ever been fetched, link-only sources are treated as disabled.
- **Rationale:** Meets the publisher's withdrawal requirement without running a backend (ADR-001 stays intact). A static file in the repo needs no hosting.
- **Where it is listed:** category **Technology** in `DefaultFeedsConfig` for `US`, `GB`, `SG`, `IN`, `CA`, `AU`, `HK`, `KR`, `JP`, `ZA` and `AE` (regions whose curated feeds are English-language). Not listed for `FR`, `DE`, `BR`. It appears in Settings > Providers only while the remote list enables it, and the single feed URL is fetched once per device regardless of how many regions list it.
- **Rollout state:** `source-config/sources.json` has `"joho-todai-en": { "enabled": true }`. Apps receive it only once that file is pushed to `master`; the repository was publicly reachable at `SourceConfig.URL` on 2026-10-23 (HTTP 200 for a file on `master`). Only builds that contain the link-only code (1.3.3+) read the file. Withdrawal end-to-end (set `enabled` to `false`, device purges) has not been tested against the live URL.
- **Follow-up conditions from the publisher (2026-10-23):** (1) withdrawal must be effective on the next connected use, so the source list is downloaded before any link-only fetch (foreground, background sync, widget), is reused for at most 60 seconds, and fetching is suspended while the list is expired and cannot be refreshed; a missing or disabled entry stops fetching and deletes cached fields; (2) the server's `Cache-Control`, `<ttl>` and `Retry-After` intervals are honoured even when longer than 24 hours (the `<ttl>` from the last 200 response also applies after a 304). Implemented in `refreshSourceConfigIfDue()`, `LinkOnlyStateStore.mayFetch()` and `LinkOnlyFetchPolicy`.

### ADR-007: Native SwiftUI iOS Port in `ios/`, Not a Shared UI Layer
- **Status:** Accepted (2026-10-24); Phase 0 in progress.
- **Context:** An iOS version should use Apple's Liquid Glass and keep the StreamFolio brand. Liquid Glass is delivered by system SwiftUI/UIKit chrome (tab bar, toolbars, sheets, `glassEffect`). The Android data layer (Room, OkHttp, Jsoup, ML Kit) is not Kotlin Multiplatform-shaped.
- **Decision:** Build a native SwiftUI app in `ios/` of this repository (deployment target iOS 26.0, iOS 27 APIs behind `#available`), with local Swift packages `StreamFolioCore` (UI-free logic) and `StreamFolioUI` (tokens, theme, glass wrappers, shared components). Persistence will be GRDB (SQLite) mirroring Room schema v9; the Android code is not changed. Android behavior is the specification; every intentional difference is documented.
- **Consequences:** Business rules exist twice (Kotlin and Swift) and are pinned by ported tests plus parity tests (`PaletteParityTests` reads `Color.kt` and the CSS tokens). Shared data should converge on files in this repo (`source-config/sources.json` is already shared; a shared curated-feeds JSON is a recommended follow-up). On-device only: iOS uses Apple's on-device models (Foundation Models `SystemLanguageModel`, Translation) and never the cloud model providers added in iOS 27, consistent with ADR-001 and ADR-002.

### ADR-008: Liquid Glass Only on the Control Layer; One Implementation per Behavior
- **Status:** Accepted (2026-10-24).
- **Decision:** Glass is used for navigation and controls (tab bar, chips, prominent buttons, mini player); content stays solid. Brand blue tints only prominent/selected controls. Screens never call `glassEffect` or declare colors, radii or font sizes; they use `StreamFolioUI`. Article actions, per-article capabilities (including link-only gating) and preferences each get a single implementation shared by every screen. `UIDesignRequiresCompatibility` is not used (a secondary source reports it no longer works with the iOS 27 SDK; `Unknown / Requires Confirmation` from Apple documentation).
- **Enforcement:** `DesignSystemRulesTests` and `PaletteParityTests` in `ios/Packages/StreamFolioUI`.

---

## 2. Known Limitations & Technical Debt

| Limitation / Technical Debt | Impact | Recommended Solution |
| :--- | :--- | :--- |
| **Monolithic `NewsViewModel` (1,332 lines)** | `NewsViewModel.kt` acts as a God Object managing feeds, reader mode, TTS bindings, and settings. | Split into domain-specific ViewModels: `FeedViewModel`, `ReaderViewModel`, `TtsViewModel`, `SettingsViewModel`. |
| **Lack of Dependency Injection (DI)** | Manual instantiation of `NewsRepository` and `PreferencesHelper` in activities, workers, and viewmodels complicates unit testing. | Introduce **Hilt** or **Koin** for dependency injection and repository singleton scoping. |
| **Google News Redirect Resolution Fragility** | `resolveGoogleNewsUrl()` uses heuristic parsing of raw redirect tokens. Future Google News protocol changes may cause resolution failures. | Add fallback direct-load in WebView when Google News redirect resolution fails. |
| **Missing CI/CD Workflow Files** | No automated testing or build checks run on GitHub PRs. | Commit `.github/workflows/android-ci.yml` (as detailed in [10-ci-cd-and-release.md](file:///d:/News/docs/10-ci-cd-and-release.md)). |
| **Withdrawal waits for a connected refresh** | A withdrawal takes effect at a device's first feed refresh with a connection after the file changes (plus a few minutes of `raw.githubusercontent.com` caching). Offline devices keep cached headlines until they reconnect. | Matches the agreed terms ("next connected use"). Keep `SourceConfig.FRESHNESS_MS` short. |
| **Remote source list depends on GitHub** | Renaming/moving/privatizing the repository or the `master` branch stops existing installs from receiving withdrawals (link-only fetching stays suspended until a list can be validated). | Keep `SourceConfig.URL` and `source-config/sources.json` stable. |
| **Fetch limit is per device** | A publisher sees requests from every device; there is no shared proxy or cache. | Intentional (ADR-001). Offer a lower per-device rate if a publisher asks. |
| **iOS: DNS-rebinding window** | `URLSession` has no hook to validate the address a connection is made to, unlike `SsrfProtectionInterceptor`. Implemented mitigation: URL validation, `getaddrinfo` pre-check, redirect validation and a post-connection `remoteAddress` check that discards the response (`SafeHTTPClient`). A request can still leave the device if DNS changes between the check and the connection. | Accepted for now; see `docs/06`. |
| **iOS: no guaranteed 6-hour background sync (planned)** | `BGAppRefreshTask` timing is decided by iOS. Background refresh is not built yet; feeds refresh when a category is opened and is stale (15 min) or on pull-to-refresh. | Add BGAppRefreshTask + refresh-on-foreground; document in `docs/08`. |
| **iOS: ordinary feeds send an iPhone Safari User-Agent** | Android sends Chrome-on-Android. Some publishers vary or block by agent; behavior per publisher is unverified on iOS. | Check the curated feeds on device; adjust per publisher if needed. |
| **iOS: images are large in the repo** | The four otter illustrations in `ios/StreamFolio/Assets.xcassets` are 0.9–2.6 MB PNGs copied from `design_system/assets/`. | Re-export as smaller/HEIC variants before release. |
| **iOS Settings: not ported** | The version-tap "StreamFolio FM" easter egg (needs text-to-speech) and the Material You "Dynamic Colors" row (no iOS equivalent; the value still round-trips in backups). The AI page stores the switches, but summaries, translation and tags do not exist yet, so the page says so. | Easter egg returns with Listen (Phase 4); AI pages become live in Phase 5. |
| **iOS Settings: lighter refreshes than Android** | Reordering tabs does not re-fetch (Android re-fetches the current tab); changing region refreshes the current tab and marks every other tab stale instead of syncing all categories in the background. | Revisit with background refresh (Phase 3). |
| **iOS: placeholder app icon** | `AppIcon.png` is the 512 px Android icon upscaled to 1024 (has transparency). | Replace with a proper Icon Composer icon in Phase 3. |
| **iOS app icon art** | `design_system/assets/logo-app-icon.png` is 512×512 with transparency; the App Store needs a 1024×1024 opaque icon (Icon Composer layers). | Redraw at 1024 before Phase 3. |
| **Limited XML Parser Unit Tests** | `RssParser` lacks isolated unit test fixtures covering corrupted or non-standard Atom/RSS feeds. | Add unit test suite in `app/src/test/` using static fixtures from `sample_xml/`. |
