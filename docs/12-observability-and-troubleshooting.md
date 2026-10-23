# 12 — Observability, Logging & Troubleshooting Guide

> **Last verified commit:** `aaac96970daf81cd5d75cf5fa56dfebf2ecb6b48`  
> **Analysis Date:** 2026-08-25

---

## 1. Observability Architecture & Privacy Policy

StreamFolio implements a **Zero-Cloud-Telemetry** privacy model. There are no remote crash reporting agents (such as Firebase Crashlytics or Sentry) or tracking analytics SDKs embedded in the app.

### Diagnostic Logging Framework
All diagnostic events are logged locally to **Android System Logcat** using standard `android.util.Log` primitives:

| Log Tag | Source Class | Logged Events |
| :--- | :--- | :--- |
| `NewsRepository` | [NewsRepository.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/data/network/NewsRepository.kt) | Feed fetch errors (with redacted hostnames), parsing failures, scraper timeouts. |
| `NewsSyncWorker` | [NewsSyncWorker.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/worker/NewsSyncWorker.kt) | Periodic background synchronization start, category progress, success/retry status. |
| `AiHelperFactory` | [AiHelperFactory.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/data/network/AiHelperFactory.kt) | ML Kit GenAI / Gemini Nano initialization status and reflection availability. |
| `StreamFolioBackup` | [BackupHelper.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/util/BackupHelper.kt) | Backup parsing warnings, skipped unsafe URLs, schema version mismatch. |

---

## 2. Viewing Diagnostics via ADB

```powershell
# Filter logcat for all StreamFolio diagnostic logs
adb logcat -v time -s NewsRepository:D NewsSyncWorker:D AiHelperFactory:D StreamFolioBackup:D

# Filter specifically for errors and exceptions
adb logcat *:E | Select-String "uk.sume.streamfolio"

# Clear log buffer
adb logcat -c
```

---

## 3. Troubleshooting Runbooks

### Runbook 1: Empty Feed / "No Articles Found"
- **Symptoms:** Category tabs show the Otter empty-state illustration; pull-to-refresh completes without new items.
- **Likely Causes:**
  1. Device is offline or connected to a captive portal without internet.
  2. Curated feed provider has changed its XML format or domain.
  3. Custom feed URL entered by user is blocked by SSRF validation or fails HTTPS requirement.
- **Diagnostic Steps:**
  ```powershell
  adb logcat -d -s NewsRepository:E
  ```
- **Remediation:**
  1. Verify device internet connection.
  2. Navigate to **Settings > Manage Content > Providers** to ensure selected providers are enabled.
  3. If using custom RSS feeds, confirm the URL starts with `https://` and returns valid XML in a desktop browser.

---

### Runbook 2: Gemini Nano AI Summary Shows "Model Unavailable"
- **Symptoms:** Tapping "AI Summary" on an article displays an error or "AICore unavailable".
- **Likely Causes:**
  1. Device hardware does not support Gemini Nano (supported only on flagship devices like Pixel 8+, Samsung Galaxy S24+).
  2. Google AICore system service is out of date or still downloading model weights.
- **Diagnostic Steps:**
  ```powershell
  adb logcat -d -s AiHelperFactory:E
  ```
- **Remediation:**
  1. Open Google Play Store and update **Android System Intelligence** and **AICore**.
  2. Ensure the device has at least 2 GB of free internal storage for model weights.
  3. Note that standard full-text reader mode and Jsoup scraping remain fully operational even if Gemini Nano is unavailable.

---

### Runbook 3: Background TTS Playback Stops Unexpectedly
- **Symptoms:** Audio stops when switching apps or locking the screen; media notification disappears.
- **Likely Causes:**
  1. OEM Battery Optimization (e.g. Samsung/Xiaomi battery saver) killed the foreground service.
  2. `POST_NOTIFICATIONS` permission was denied by the user on Android 13+.
- **Remediation:**
  1. Grant **Notification permission** in Android App Settings (`Settings > Apps > StreamFolio > Permissions`).
  2. Set Battery usage to **Unrestricted** in Android settings.

---

### Runbook 4: Home Screen Widget Shows Blank Items or Fails to Update
- **Symptoms:** StreamFolio widget displays empty placeholders or stale news.
- **Diagnostic Steps:**
  ```powershell
  adb shell dumpsys appwidget
  ```
- **Remediation:**
  1. Tap the refresh icon on the top-right of the widget to trigger an immediate broadcast update.
  2. Remove and re-add the widget to the home screen.
  3. Open `WidgetConfigureActivity` to ensure the selected category or feed list contains cached articles.

### Runbook 5: Link-Only Source (Joho no Todai) Does Not Appear
- **Symptoms:** No *Joho no Todai* headlines in Technology; the source is missing from **Settings > Manage Content > Providers**.
- **Likely Causes:**
  1. The remote source list has the source missing or `enabled: false` (the shipped default), or has never been downloaded (device offline, repository not public, file not on `master`). Link-only sources are off until a list enabling them has been downloaded.
  2. The per-device 30-minute limit, a server back-off (`429`/`503`) or a `404`/`410` back-off is active (see [04](file:///d:/News/docs/04-api-documentation.md)).
- **Diagnostic Steps:**
  ```powershell
  adb logcat -d -s NewsRepository:W NewsRepository:E
  adb shell run-as uk.sume.streamfolio.debug cat shared_prefs/link_only_state.xml   # debug build only
  ```
  Check `https://raw.githubusercontent.com/sm32d/streamfolio/master/source-config/sources.json` opens in a browser.
- **Remediation:** Publish the file with `"joho-todai-en": { "enabled": true }` (see [11](file:///d:/News/docs/11-deployment-and-operations.md)); wait for the next refresh (up to 1 hour for the list, then the feed limit).

### Runbook 6: Crash `NullPointerException: Parameter specified as non-null is null: method ...Article.<init>, parameter link`
- **Observed:** Once, on release 1.3.2 (versionCode 23) on a physical device, 2026-10-23. Decoded with `app/build/outputs/mapping/release/mapping.txt` (same `pg_map_id`): `CoroutinesRoom.createFlow` → `ArticleDao_Impl.getArticlesByCategory` (`SELECT * FROM articles WHERE category = ?`) read a row whose `link` was `NULL`, although the schema declares `link TEXT NOT NULL`.
- **Root cause:** `Unknown / Requires Confirmation`. The database of the affected install was not inspected (release builds are not debuggable). Only one crash was in the device log.
- **Mitigation shipped:** Room-backed article Flows in `NewsRepository` (`resilientQuery`) now retry twice, log `Room query '<name>' failed`, then fall back to an empty list instead of crashing the process.
- **If it recurs:** capture `adb logcat -d -s NewsRepository:E`; a repeated log line means a persistent bad row — consider a migration that deletes `articles` rows with `link IS NULL OR link = ''`.
