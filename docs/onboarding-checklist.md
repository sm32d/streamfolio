# New Developer Onboarding Checklist

> **Last verified commit:** `aaac96970daf81cd5d75cf5fa56dfebf2ecb6b48`  
> **Analysis Date:** 2026-08-25

---

## 🚀 Welcome to StreamFolio!

This checklist guides a new engineer through setting up the local environment, building the Android app, understanding core subsystems, and submitting their first pull request.

---

## 📅 Day 1: Environment Setup & First Build

- [ ] **Install Prerequisites:**
  - [ ] Install **JDK 17 or JDK 21** (e.g. Eclipse Temurin).
  - [ ] Install **Android Studio** (Ladybug 2024.2.1+ or Koala).
  - [ ] Open Android Studio SDK Manager and install **Android API 36 (Android 16 Preview)** SDK Platform and Build Tools.
- [ ] **Clone Repository & Configure SDK:**
  ```powershell
  git clone https://github.com/sm32d/streamfolio.git
  cd streamfolio
  ```
  - [ ] Ensure `local.properties` contains `sdk.dir=<path_to_android_sdk>`.
- [ ] **Execute Baseline Build & Unit Tests:**
  ```powershell
  # 1. Clean and build debug APK
  ./gradlew assembleDebug

  # 2. Run unit tests
  ./gradlew testDebugUnitTest
  ```
- [ ] **Launch on Device / Emulator:**
  - [ ] Start an Android Emulator with API 26+ (recommended: API 34/35/36).
  - [ ] Run the app from Android Studio (`Shift + F10`) or via CLI:
    ```powershell
    ./gradlew installDebug
    adb shell am start -n uk.sume.streamfolio/.MainActivity
    ```
  - [ ] Verify the Onboarding flow loads, feeds populate, and articles can be opened.

---

## 📅 Day 2: Codebase & Architecture Deep Dive

- [ ] **Read the Core Documentation:**
  - [ ] Read [00-project-overview.md](file:///d:/News/docs/00-project-overview.md) (Product overview & tech stack).
  - [ ] Read [01-architecture.md](file:///d:/News/docs/01-architecture.md) (UDF architecture & sequence diagrams).
  - [ ] Read [05-data-model-and-database.md](file:///d:/News/docs/05-data-model-and-database.md) (Room SQLite schema & migrations).
  - [ ] Read [06-authentication-and-security.md](file:///d:/News/docs/06-authentication-and-security.md) (SSRF & Zero-Auth model).
  - [ ] Read [07-frontend-or-client-app.md](file:///d:/News/docs/07-frontend-or-client-app.md) (Compose navigation & screens).
- [ ] **Trace Critical Code Paths in IDE:**
  - [ ] Trace feed fetching: [HomeScreen.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/ui/screens/HomeScreen.kt) $\rightarrow$ [NewsViewModel.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/ui/viewmodel/NewsViewModel.kt) $\rightarrow$ [NewsRepository.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/data/network/NewsRepository.kt) $\rightarrow$ [RssParser.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/data/network/RssParser.kt) $\rightarrow$ [ArticleDao.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/data/local/ArticleDao.kt).
  - [ ] Trace TTS playback: [DetailScreen.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/ui/screens/DetailScreen.kt) $\rightarrow$ [TtsPlaybackManager.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/playback/TtsPlaybackManager.kt) $\rightarrow$ [TtsPlaybackService.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/playback/TtsPlaybackService.kt).

---

## 📅 Day 3: First Contribution & Pull Request

- [ ] **Review Contribution Rules:** Read [CLAUDE.md](file:///d:/News/CLAUDE.md) and [13-contributing.md](file:///d:/News/docs/13-contributing.md).
- [ ] **Create a Feature Branch:**
  ```powershell
  git checkout -b feat/my-first-improvement
  ```
- [ ] **Implement Change & Synchronize Documentation:**
  - [ ] If changing database schema, add a new Room migration.
  - [ ] If modifying settings or routes, update matching docs under `/docs`.
- [ ] **Run Pre-PR Validation:**
  ```powershell
  ./gradlew testDebugUnitTest
  ./gradlew lintDebug
  ```
- [ ] **Commit & Submit PR:** Follow Conventional Commits format (`feat(scope): description`).
