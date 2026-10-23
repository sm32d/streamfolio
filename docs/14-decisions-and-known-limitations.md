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
- **Status:** Implemented (source ships **disabled**; see "Rollout state" below). Terms agreed with the publisher by email; the publisher's own wording is not stored in the repo.
- **Context:** The publisher of *Joho no Todai* (English feed `https://joho-todai.com/en/rss/`) permits use in StreamFolio's curated sources only on limited, revocable terms: publication name, original headline, publication date, direct article link and the feed's short text `description`, cached on the reader's device; no images (including images embedded in `description`); no article bodies or `content:encoded`; no translation, generated summaries, model training, redistribution or sublicensing. Fetching is limited to one request per device every 30 minutes, using conditional requests and an identifying StreamFolio User-Agent, subject to any longer refresh or back-off instruction from the publisher's server. On withdrawal, fetching must stop on existing installs and their cached fields must be removed when they next connect.
- **Decision:**
  - A host-based registry, [LinkOnlySources.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/data/network/LinkOnlySources.kt), marks feeds and article links as "link-only". Behavior keys off the host, so it also applies if a user adds the same feed as a custom feed, via OPML, or opens a shared link.
  - [RssParser.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/data/network/RssParser.kt) ignores `content`, `content:encoded`, `enclosure`, `media:content`, `media:thumbnail` and `itunes:image` for link-only feeds, strips all HTML from `description` (dropping embedded images) and truncates it to `LinkOnlySources.MAX_DESCRIPTION_CHARS` (500).
  - [NewsRepository.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/data/network/NewsRepository.kt) fetches link-only feeds through `fetchLinkOnlyFeedXml()`: per-device 30-minute minimum interval, `If-None-Match` / `If-Modified-Since`, `User-Agent: StreamFolio/<versionName> (Android feed reader; headline-and-link only)`, and obedience to `Cache-Control: max-age`, feed `<ttl>`, `Retry-After`, 429/503 (1 h default back-off) and 404/410 (24 h). Rules live in [LinkOnlyFetchPolicy.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/data/network/LinkOnlyFetchPolicy.kt). Online search never fetches link-only feeds.
  - For link-only articles the app never fetches the article page or thumbnails (`resolveThumbnail`, `fetchArticleBody`, `scrapeTransientArticle` return early), never pre-scrapes bodies, skips on-device AI summarization during tagging (keyword tags only), blocks Listen/queue (TTS), AI summary and translation, shows a letter avatar instead of a favicon, and opens the article in the Detail screen's **Web View** (the Reader tab is hidden) so the article is read on the publisher's website. They are excluded from backup export and skipped on restore.
  - **Remote enable list:** a static JSON file in this repository, [source-config/sources.json](file:///d:/News/source-config/sources.json), fetched from `https://raw.githubusercontent.com/sm32d/streamfolio/master/source-config/sources.json` (constant `SourceConfig.URL`). Format: `{"version":1,"sources":{"<id>":{"enabled":true|false}}}`. The app checks it when a feed refresh starts, at most once per hour. A link-only source that is missing from the file or has `enabled: false` is not fetched, is hidden from Settings > Providers, and its cached articles (including bookmarks and list associations) and throttle state are deleted from the device. If the file cannot be fetched or parsed, the last known state is kept; if no list has ever been fetched, link-only sources are treated as disabled.
- **Rationale:** Meets the publisher's withdrawal requirement without running a backend (ADR-001 stays intact). A static file in the repo needs no hosting.
- **Where it is listed:** category **Technology** in `DefaultFeedsConfig` for `US`, `GB`, `SG`, `IN`, `CA`, `AU`, `HK`, `KR`, `JP`, `ZA` and `AE` (regions whose curated feeds are English-language). Not listed for `FR`, `DE`, `BR`. It appears in Settings > Providers only while the remote list enables it, and the single feed URL is fetched once per device regardless of how many regions list it.
- **Rollout state (verify before release):** `source-config/sources.json` currently has `"joho-todai-en": { "enabled": false }`. Nothing is fetched from the publisher until that value is set to `true` **and the file is reachable at `SourceConfig.URL`** (requires the repository to be public and the file to be on the `master` branch). Whether the repository is public is `Unknown / Requires Confirmation`. Withdrawal end-to-end has not been tested against a live URL.

---

## 2. Known Limitations & Technical Debt

| Limitation / Technical Debt | Impact | Recommended Solution |
| :--- | :--- | :--- |
| **Monolithic `NewsViewModel` (1,332 lines)** | `NewsViewModel.kt` acts as a God Object managing feeds, reader mode, TTS bindings, and settings. | Split into domain-specific ViewModels: `FeedViewModel`, `ReaderViewModel`, `TtsViewModel`, `SettingsViewModel`. |
| **Lack of Dependency Injection (DI)** | Manual instantiation of `NewsRepository` and `PreferencesHelper` in activities, workers, and viewmodels complicates unit testing. | Introduce **Hilt** or **Koin** for dependency injection and repository singleton scoping. |
| **Google News Redirect Resolution Fragility** | `resolveGoogleNewsUrl()` uses heuristic parsing of raw redirect tokens. Future Google News protocol changes may cause resolution failures. | Add fallback direct-load in WebView when Google News redirect resolution fails. |
| **Missing CI/CD Workflow Files** | No automated testing or build checks run on GitHub PRs. | Commit `.github/workflows/android-ci.yml` (as detailed in [10-ci-cd-and-release.md](file:///d:/News/docs/10-ci-cd-and-release.md)). |
| **Withdrawal of a link-only source is not instant** | Devices apply a withdrawal only on their next feed refresh with a connection (the list is cached for up to 1 hour, and `raw.githubusercontent.com` caches for a few minutes). Offline devices keep cached headlines until they reconnect. | Acceptable under the agreed terms ("when they next connect"). Do not shorten promises made to the publisher without changing `SourceConfig.CHECK_INTERVAL_MS`. |
| **Remote source list depends on GitHub** | Renaming/moving/privatizing the repository or the `master` branch stops existing installs from receiving withdrawals (they keep last known state). | Keep `SourceConfig.URL` and `source-config/sources.json` stable. |
| **Fetch limit is per device** | A publisher sees requests from every device; there is no shared proxy or cache. | Intentional (ADR-001). Offer a lower per-device rate if a publisher asks. |
| **Limited XML Parser Unit Tests** | `RssParser` lacks isolated unit test fixtures covering corrupted or non-standard Atom/RSS feeds. | Add unit test suite in `app/src/test/` using static fixtures from `sample_xml/`. |
