# 02 — Local Development & Environment Setup

> **Last verified commit:** `aaac96970daf81cd5d75cf5fa56dfebf2ecb6b48`  
> **Analysis Date:** 2026-08-25  
> **Project Root Directory:** `d:/News`

---

## 1. Prerequisites & Toolchain Matrix

Before setting up the repository locally, ensure the following tools and SDK components are installed:

| Tool / Dependency | Required Version | Purpose | Verification Command |
| :--- | :--- | :--- | :--- |
| **Operating System** | Windows 10/11, macOS (Apple Silicon/Intel), or Linux (x86_64) | Host OS | `cmd.exe /c ver` (Windows) or `uname -a` |
| **Java Development Kit (JDK)** | JDK 11, 17, or 21 (Temurin / Oracle / Zulu) | Gradle build execution & Kotlin compilation | `java -version` |
| **Android Studio** | Android Studio Ladybug (2024.2.1+) or Koala / Jellyfish | Primary IDE & Android SDK Manager | `Studio -> Help -> About` |
| **Android SDK Platform** | **Android API 36** (Android 16 / Vanilla Ice Cream Preview) | Target & Compile SDK | Android Studio SDK Manager |
| **Android SDK Build-Tools** | `36.0.0` or `34.0.0+` | DEX compilation & AAPT2 resource packing | Android Studio SDK Manager |
| **Android NDK** | *Not Required* | Pure Kotlin/Java codebase (no C++ JNI) | N/A |
| **Gradle Wrapper** | Included in repository (`gradle-8.x`) | Deterministic build automation | `./gradlew --version` |

---

## 2. Step-by-Step Setup Guide

### Step 1: Clone Repository
```bash
# Working Directory: Any workspace directory
git clone https://github.com/sm32d/streamfolio.git
cd streamfolio
```

### Step 2: Configure Android SDK Location
Android Studio automatically creates `local.properties` upon opening the project. If building via command line, create `local.properties` in the root directory:

**Windows (`d:/News/local.properties`):**
```properties
sdk.dir=C\:\\Users\\<YourUsername>\\AppData\\Local\\Android\\Sdk
```

**macOS / Linux (`~/streamfolio/local.properties`):**
```properties
sdk.dir=/Users/<YourUsername>/Library/Android/sdk
```

---

## 3. Essential Development Commands

All commands below must be executed from the **repository root directory** (`d:/News`).

### Build Commands

```powershell
# Clean build cache and temporary artifacts
./gradlew clean

# Assemble Debug APK (Output: app/build/outputs/apk/debug/app-debug.apk)
./gradlew assembleDebug

# Assemble Release APK (Minified with R8, unsigned)
./gradlew assembleRelease

# Assemble Release Android App Bundle (AAB for Play Store distribution)
./gradlew bundleRelease
```

### Testing Commands

```powershell
# Run all local JVM unit tests (BackupHelperTest, DefaultFeedsTest, etc.)
./gradlew testDebugUnitTest

# Run a specific unit test class
./gradlew testDebugUnitTest --tests "uk.sume.news.BackupHelperTest"

# Run a specific test method
./gradlew testDebugUnitTest --tests "uk.sume.news.BackupHelperTest.testSerializationDeserialization"

# Run Android Instrumented UI tests (Requires connected physical device or running emulator)
./gradlew connectedDebugAndroidTest
```

### Code Quality & Static Analysis

```powershell
# Run Android Lint analysis across all modules
./gradlew lintDebug

# View generated Lint HTML report
# Location: app/build/reports/lint-results-debug.html
```

### Version Update Utility Script

The repository includes a PowerShell automation script to synchronize version codes across Gradle and UI files:

```powershell
# Execute version bump utility (prompts for new versionName & versionCode)
.\update-version.ps1
```
*Script updates:*
1. `app/build.gradle.kts` (`versionCode`, `versionName`)
2. `app/src/main/java/uk/sume/streamfolio/ui/screens/settings/SettingsMainScreen.kt` (Settings footer version label)

---

## 4. Running & Debugging on Emulators / Devices

### Using Android Studio
1. Open Android Studio and select `File > Open > d:/News`.
2. Wait for Gradle Sync to complete.
3. Select `app` in the run configuration dropdown.
4. Select a virtual device (AVD with API 26+) or a physical device with **USB Debugging enabled**.
5. Click **Run (`Shift + F10`)** or **Debug (`Shift + F9`)**.

### Installing via ADB (Command Line)
```powershell
# Build and install the debug APK onto the active device. Debug builds use the package
# uk.sume.streamfolio.debug, so they install next to (not over) the Play/release app.
./gradlew installDebug

# Alternatively install an existing APK via ADB
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch MainActivity directly via ADB
adb shell am start -n uk.sume.streamfolio.debug/uk.sume.streamfolio.MainActivity
```

---

## 5. Common Setup Failures & Troubleshooting

### Issue 1: `SDK location not found. Define location with an ANDROID_HOME environment variable`
- **Cause:** Missing `local.properties` or incorrect path syntax.
- **Fix:** Open project in Android Studio to auto-generate `local.properties`, or manually set `ANDROID_HOME` in environment variables.

### Issue 2: `Failed to find target with hash string 'android-36'`
- **Cause:** Android SDK Platform API 36 is not installed in the local Android SDK.
- **Fix:** In Android Studio, navigate to `Settings > Languages & Frameworks > Android SDK`, check **Android API 36 (Preview / Android 16)**, and click **Apply**.

### Issue 3: Unit Tests Blocked or Flaky in `DefaultFeedsTest`
- **Cause:** `DefaultFeedsTest` makes live network calls to verify all curated RSS publisher feeds. Strict firewalls, proxies, or rate-limited endpoints (`economictimes.indiatimes.com`, `arabnews.com`) can cause network timeouts.
- **Fix:** The test suite handles known flaky feeds gracefully via `knownFlakyFeeds` filtering. For offline testing, run isolated non-network tests:
  ```powershell
  ./gradlew testDebugUnitTest --tests "uk.sume.news.BackupHelperTest"
  ```

### Issue 4: KAPT Schema Directory Warning
- **Cause:** KAPT room schema export configured in `app/build.gradle.kts` (`room.schemaLocation = "$projectDir/schemas"`).
- **Fix:** Ensure `app/schemas` directory is preserved in git. Schemas are committed for Room schema migration validation.

---

## 6. iOS Port (`ios/`) — in progress (Phase 0, added 2026-10-24)

The iOS app is a separate native SwiftUI project; it does not affect the Gradle build. Full instructions: [ios/README.md](../ios/README.md).

| Tool | Version verified | Purpose |
| :--- | :--- | :--- |
| macOS + **Xcode** | Xcode 27.1 (27A9275), SDK `iphoneos27.1` | Build, simulator, signing |
| **Swift** | 6.4 (language mode 6) | App and packages |
| **XcodeGen** | 2.46.0 | Generates `ios/StreamFolio.xcodeproj` from `ios/project.yml` |
| iOS Simulator runtimes | iOS 27.1 and 26.5 installed; the 27.1 runtime lists only the **iPhone Duo** (foldable) device type on this machine | Run and screenshot |

```bash
cd ios && xcodegen generate
swift test --package-path Packages/StreamFolioCore
swift test --package-path Packages/StreamFolioUI
```

- Deployment target is iOS 26.0 (Liquid Glass first shipped in 26); iOS 27 APIs are gated with `#available`.
- Apple Developer Team ID: set as `DEVELOPMENT_TEAM` in `ios/project.yml` (2026-10-24). Whether the team has an App Store Connect app record for `uk.sume.streamfolio` is `Unknown / Requires Confirmation`.
- Xcode ships SwiftUI guidance for coding agents; export it with `xcrun mcpbridge run-agent skills export` (writes `./xcode-skills`, do not commit).
- Foundation Models and Translation on the Simulator: `Unknown / Requires Confirmation` — plan on a physical device for Phase 5.
