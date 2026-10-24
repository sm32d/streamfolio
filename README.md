# 📱 StreamFolio

<div align="left">

[![Android Platform](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Room Database](https://img.shields.io/badge/Database-Room%202.6.1%20(SQLite%20v8)-F4511E?style=for-the-badge&logo=sqlite&logoColor=white)](https://developer.android.com/training/data-storage/room)
[![License](https://img.shields.io/badge/License-GPL--3.0-blue?style=for-the-badge)](LICENSE)
[![Get it on Google Play](https://img.shields.io/badge/Google_Play-StreamFolio-green?style=for-the-badge&logo=google-play)](https://play.google.com/store/apps/details?id=uk.sume.streamfolio)

**A modern, zero-telemetry, privacy-focused RSS & Atom news reader for Android with On-Device AI Summarization and Background Audio Playback.**

</div>

---

## 📖 Overview

**StreamFolio** (`uk.sume.streamfolio`) is an elegant, open-source RSS and Atom news reader engineered specifically for Android. In contrast to algorithmic aggregators and ad-heavy news applications, StreamFolio is built on a strict **100% on-device architecture**: no remote application servers, zero telemetry SDKs, no cloud user accounts, and complete offline availability.

It combines multi-region curated news feeds (14 countries), custom RSS/Atom feed management with OPML 2.0 import/export, on-device GenAI summarization powered by **Gemini Nano** (via Google AICore / ML Kit), on-device ML translation, and background Text-To-Speech (TTS) audio with Android `MediaSessionCompat` lockscreen controls.

---

## 📚 Complete Project Documentation (`/docs`)

Comprehensive architectural, operational, security, and developer documentation is organized under the [`/docs`](docs/) directory:

| Document | Description | Target Audience |
| :--- | :--- | :--- |
| [**00 — Project Overview**](docs/00-project-overview.md) | Product mission, key problems solved, personas, system boundaries, and stack. | All Stakeholders |
| [**01 — Architecture**](docs/01-architecture.md) | C4 context/container diagrams, UDF patterns, and sequence flows (Sync, AI, TTS). | Architects & Developers |
| [**02 — Local Development**](docs/02-local-development.md) | Toolchain matrix (JDK 17, SDK 36), IDE setup, build, test, and debug commands. | Engineers & Contributors |
| [**03 — Configuration & Environments**](docs/03-configuration-and-environments.md) | Environment variables, Gradle settings, and `SharedPreferences` reference. | DevOps & Developers |
| [**04 — API & External Integrations**](docs/04-api-documentation.md) | Outbound RSS/Atom APIs, Google News integration, IPC intents, and Backup JSON schema. | Integrators & QA |
| [**05 — Data Model & Database**](docs/05-data-model-and-database.md) | Room SQLite ERD, table definitions, migration history (v5 $\rightarrow$ v8), retention rules. | Database & Core Engineers |
| [**06 — Authentication & Security**](docs/06-authentication-and-security.md) | Zero-Auth model, SSRF defenses, DNS rebinding interceptor, and threat analysis. | Security Auditors |
| [**07 — Frontend & Android Client**](docs/07-frontend-or-client-app.md) | Compose navigation graph, screen deep dives, shared components, and design system tokens. | UI / Android Engineers |
| [**08 — Background Services & OS Integrations**](docs/08-backend-services.md) | WorkManager `NewsSyncWorker`, MediaSession `TtsPlaybackService`, AppWidget service. | Core & Systems Engineers |
| [**09 — Testing & Quality Assurance**](docs/09-testing-and-quality.md) | Unit tests, live feed verification suite, Lint analysis, and untested critical paths. | QA & Engineers |
| [**10 — CI/CD & Release Engineering**](docs/10-ci-cd-and-release.md) | Build commands, R8 minification, versioning script (`update-version.ps1`), app signing. | DevOps & Release Leads |
| [**11 — Deployment & Operations**](docs/11-deployment-and-operations.md) | Distribution channels (Play Store, GitHub, Landing), staged rollouts, rollback safety. | Release & Operations |
| [**12 — Observability & Troubleshooting**](docs/12-observability-and-troubleshooting.md) | ADB Logcat diagnostics, error tag conventions, and failure runbooks. | Support & DevOps |
| [**13 — Contributing & Engineering Standards**](docs/13-contributing.md) | Git branching, Conventional Commits, validation checklist, and ADR template. | All Contributors |
| [**14 — Architecture Decisions & Limitations**](docs/14-decisions-and-known-limitations.md) | Inferred ADRs, known technical debt, and architectural trade-offs. | Architects & Leads |
| [**15 — Glossary & Terminology**](docs/15-glossary.md) | Definitions for Android, RSS, Security, and On-Device AI terminology. | All Team Members |
| [**Onboarding Checklist**](docs/onboarding-checklist.md) | Structured Day 1 to Day 3 step-by-step onboarding guide for new developers. | New Engineers |

---

## ✨ Core Features

- 🌐 **14 Supported Curated Regions:** Pre-configured news providers for `US`, `GB`, `SG`, `IN`, `CA`, `AU`, `FR`, `DE`, `HK`, `KR`, `JP`, `BR`, `ZA`, `AE`.
- 📂 **Custom Feeds & OPML 2.0:** Subscribe to any valid HTTPS RSS/Atom feed; bulk import and export subscriptions.
- 🤖 **On-Device GenAI Summaries:** Instant bulleted key takeaways generated locally via **Gemini Nano** without cloud API calls or subscriptions.
- 🌍 **On-Device Translation:** Offline headline and body translation powered by Google ML Kit NLP models.
- 🎧 **Background TTS & Media Controls:** Native Android foreground audio playback with lockscreen controls, variable speeds (0.5x–3.0x), and synchronized lyrics-style visualizer.
- 📑 **Custom Bookmark Collections:** Create customized bookmark lists with fluid 2-column grid and compact list view modes.
- 🔗 **Link-Only Publisher Sources:** Some curated sources (e.g. Joho no Todai) are used as headline-and-link only, with a per-device fetch limit, no images/bodies/AI features, and remote withdrawal. See [ADR-006](docs/14-decisions-and-known-limitations.md).
- 🛡️ **SSRF & Defensive Networking:** Hardened OkHttp client blocking private IP probes, loopback subnets, and DNS-rebinding attacks.
- 📱 **Home Screen AppWidget:** Glassmorphic widget with configurable category filters and one-tap reading launch.
- 🔄 **WorkManager Background Sync:** Automated 6-hour background sync ensuring cached feeds are ready for offline reading.

---

## 🖥️ Screenshots

<div align="center">

<img width="260" alt="Home Feed" src="https://github.com/user-attachments/assets/2bf5d171-9358-441c-b7e8-9a801f7c58cf" />
<img width="260" alt="Article Reader" src="https://github.com/user-attachments/assets/b87c13bc-8558-4ee1-9a9b-ae3210264cc0" />
<img width="260" alt="AI Summary" src="https://github.com/user-attachments/assets/95ad05fd-86ad-4738-8387-94bf9b5f487f" />
<img width="260" alt="Bookmarks Grid" src="https://github.com/user-attachments/assets/806ef57f-7dac-409c-b91c-b2d70e0679aa" />
<img width="260" alt="Search Screen" src="https://github.com/user-attachments/assets/94edb3bf-14f9-4e3c-bfb4-7cb7e12e09c9" />
<img width="260" alt="TTS Player" src="https://github.com/user-attachments/assets/b54c6cdb-5fa1-4e0d-83ab-8dd0212563b3" />

</div>

---

## 🛠️ Technology Stack

| Domain | Technology / Library | Version |
| :--- | :--- | :--- |
| **Language & Toolchain** | Kotlin | `2.0.21` (Java 11 target) |
| **Android Gradle Plugin** | AGP | `8.13.2` |
| **SDK Levels** | Compile: 36, Target: 36, Min: 26 | Android 8.0 Oreo to Android 16 Preview |
| **UI Toolkit** | Jetpack Compose BOM | `2025.05.00` |
| **Design System** | Material Design 3 | Compose M3 (`androidx.compose.material3`) |
| **Local Persistence** | AndroidX Room | `2.6.1` (SQLite Schema v8) |
| **Networking & Security** | Square OkHttp | `4.12.0` (with custom `SsrfProtectionInterceptor`) |
| **HTML / XML Parsers** | Jsoup & XmlPullParser | `1.18.1` / Standard XML pull |
| **Image Loading** | Coil Compose & GIF | `2.7.0` |
| **Background Sync** | AndroidX WorkManager | `2.9.1` (`NewsSyncWorker`) |
| **Audio & Media** | AndroidX Media | `1.7.0` (`MediaSessionCompat`) |
| **On-Device GenAI** | ML Kit GenAI Summarization | `1.0.0-beta1` (Gemini Nano / AICore) |
| **Offline Translation** | ML Kit Translate & LangID | `17.0.3` / `17.0.6` |

---

## 🚀 Quick Start Guide

### Prerequisites
- **JDK 17 or 21** installed and configured (`JAVA_HOME`).
- **Android Studio** (Ladybug 2024.2.1+ or Koala).
- **Android API 36 SDK Platform** installed via Android Studio SDK Manager.

### Building & Running Locally

```powershell
# 1. Clone the repository
git clone https://github.com/sm32d/streamfolio.git
cd streamfolio

# 2. Build Debug APK
./gradlew assembleDebug

# 3. Run Unit Tests
./gradlew testDebugUnitTest

# 4. Install and launch on an attached device or emulator
./gradlew installDebug
adb shell am start -n uk.sume.streamfolio/.MainActivity
```

For comprehensive environment setup, refer to [docs/02-local-development.md](docs/02-local-development.md).

> **iOS (in progress):** a native SwiftUI port using Apple's Liquid Glass lives in [`ios/`](ios/README.md) (Xcode 27.1, iOS 26+). Onboarding, the live Home feed, the Reader/Web article page, Saved collections, Search and all Settings pages (preferences, gestures, sources, custom RSS feeds, providers, tab order, AI switches, backup/restore and OPML) work; AI summaries/translation, TTS, the widget and background refresh are not built yet.

---

## 📁 Repository Structure

```
streamfolio/
├── app/                                # Main Android application module
│   ├── schemas/                        # Exported Room database schema definitions (v5, v6, v7, v8)
│   ├── src/
│   │   ├── main/                       # Kotlin source code, resources, manifest
│   │   ├── test/                       # Local JVM unit tests (BackupHelperTest, DefaultFeedsTest)
│   │   └── androidTest/                # Instrumented Android UI tests
│   ├── build.gradle.kts                # Application module build script & dependencies
│   └── proguard-rules.pro              # R8 minification and keep rules
├── ios/                                # Native SwiftUI iOS port with Liquid Glass (in progress, see ios/README.md)
├── design_system/                      # Brand assets, React/HTML UI kit, CSS design tokens
├── docs/                               # Complete project documentation suite
├── gradle/                             # Gradle wrapper & version catalog (libs.versions.toml)
├── landing/                            # Public marketing website (HTML, CSS, JS)
├── sample_xml/                         # Reference RSS/Atom/OPML payloads
├── build.gradle.kts                    # Root project build configuration
├── settings.gradle.kts                 # Project module settings
├── update-version.ps1                  # Version bump utility script
├── CLAUDE.md                           # AI pair programming guidelines and rules
└── README.md                           # This README file
```

---

## 🤝 Contributing

We welcome contributions! Please review our [Contributing Guidelines](docs/13-contributing.md) and [Onboarding Checklist](docs/onboarding-checklist.md) before opening a Pull Request.

1. Fork the repository and create a feature branch (`git checkout -b feat/my-feature`).
2. Implement your changes and ensure `./gradlew testDebugUnitTest` and `./gradlew lintDebug` pass.
3. Keep documentation under `/docs` updated in the same change set (per [CLAUDE.md](CLAUDE.md)).
4. Submit a Pull Request with a clear Conventional Commit message.

---

## 📄 License

This project is licensed under the [GNU General Public License v3.0](LICENSE) - see the [LICENSE](LICENSE) file for details.

<div align="center">

Made with ❤️ for privacy and open news by [sm32d](https://github.com/sm32d)

</div>
