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

---

## 2. Known Limitations & Technical Debt

| Limitation / Technical Debt | Impact | Recommended Solution |
| :--- | :--- | :--- |
| **Monolithic `NewsViewModel` (1,332 lines)** | `NewsViewModel.kt` acts as a God Object managing feeds, reader mode, TTS bindings, and settings. | Split into domain-specific ViewModels: `FeedViewModel`, `ReaderViewModel`, `TtsViewModel`, `SettingsViewModel`. |
| **Lack of Dependency Injection (DI)** | Manual instantiation of `NewsRepository` and `PreferencesHelper` in activities, workers, and viewmodels complicates unit testing. | Introduce **Hilt** or **Koin** for dependency injection and repository singleton scoping. |
| **Google News Redirect Resolution Fragility** | `resolveGoogleNewsUrl()` uses heuristic parsing of raw redirect tokens. Future Google News protocol changes may cause resolution failures. | Add fallback direct-load in WebView when Google News redirect resolution fails. |
| **Missing CI/CD Workflow Files** | No automated testing or build checks run on GitHub PRs. | Commit `.github/workflows/android-ci.yml` (as detailed in [10-ci-cd-and-release.md](file:///d:/News/docs/10-ci-cd-and-release.md)). |
| **Limited XML Parser Unit Tests** | `RssParser` lacks isolated unit test fixtures covering corrupted or non-standard Atom/RSS feeds. | Add unit test suite in `app/src/test/` using static fixtures from `sample_xml/`. |
