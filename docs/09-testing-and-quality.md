# 09 — Testing & Quality Assurance Strategy

> **Last verified commit:** `aaac96970daf81cd5d75cf5fa56dfebf2ecb6b48`  
> **Analysis Date:** 2026-08-25

---

## 1. Testing Strategy & Directory Structure

StreamFolio implements a dual-tier testing structure leveraging standard Android testing frameworks:

```
app/src/
├── test/java/uk/sume/news/             # Local JVM Unit Tests (Runs without device/emulator)
│   ├── BackupHelperTest.kt             # JSON backup serialization, deserialization & preference mutation
│   ├── DefaultFeedsTest.kt             # Live network feed verification & cleartext redirect audit
│   └── ExampleUnitTest.kt              # Baseline smoke test
└── androidTest/java/uk/sume/news/      # Instrumented Device Tests (Requires physical device or AVD)
    └── ExampleInstrumentedTest.kt      # Android context & package verification
```

---

## 2. Test Execution Guide

All test commands should be executed from the **project root directory** (`d:/News`):

### Running Unit Tests

```powershell
# Run full unit test suite
./gradlew testDebugUnitTest

# Run BackupHelper serialization test suite only
./gradlew testDebugUnitTest --tests "uk.sume.news.BackupHelperTest"

# Run curated feed validation test suite (Requires active internet connection)
./gradlew testDebugUnitTest --tests "uk.sume.news.DefaultFeedsTest"
```

### Running Instrumented UI Tests

```powershell
# Run instrumented tests on an attached Android device or emulator
./gradlew connectedDebugAndroidTest
```

### Running Android Lint & Code Quality Gates

```powershell
# Execute Android Lint static code analysis
./gradlew lintDebug

# Reports generated at: app/build/reports/lint-results-debug.html
```

---

## 3. Existing Test Suites Analysis

### 1. `BackupHelperTest.kt` ([BackupHelperTest.kt](file:///d:/News/app/src/test/java/uk/sume/news/BackupHelperTest.kt))
- **Scope:** Verifies the complete state round-trip serialization and deserialization.
- **Mocking:** Uses an in-memory `MockSharedPreferences` and `MockContext` without Robolectric or framework dependencies.
- **Assertions:**
  - Verifies custom preferences (AI enabled, custom swipe actions) correctly serialize into JSON.
  - Verifies JSON structure contains `preferences`, `custom_feeds`, and `articles` arrays.
  - Confirms state mutation and subsequent restoration via `applyPreferences`.

### 2. `DefaultFeedsTest.kt` ([DefaultFeedsTest.kt](file:///d:/News/app/src/test/java/uk/sume/news/DefaultFeedsTest.kt))
- **Scope:** Live HTTP test validating all curated news feeds across all 14 regions (link-only publishers are skipped by the live network tests via `liveTestProviders()`; they are still covered by the offline publisher-mapping test) (`US`, `GB`, `SG`, `IN`, `CA`, `AU`, `FR`, `DE`, `HK`, `KR`, `JP`, `BR`, `ZA`, `AE`).
- **Validations:**
  - `allDefaultFeedsShouldReturnArticles`: Asserts every feed is reachable and returns $\ge 1$ parsed article.
  - `noFeedShouldRedirectToCleartextHttp`: Verifies no HTTPS feed silently redirects to unencrypted HTTP without being declared in `network_security_config.xml`.
  - `noFeedShouldBeEmpty`: Asserts XML parser extracts valid headlines from each feed.
  - `allFeedsShouldHavePublisherMapping`: Asserts every curated feed URL maps to a human-readable publisher title.

---

## 4. Untested Critical Paths & Recommended Test Backlog

The following critical application paths currently lack automated unit test coverage:

| Target Component | Untested Behavior | Recommended Test Implementation | Priority |
| :--- | :--- | :--- | :--- |
| **`UrlSecurityValidator`** | SSRF blocking for private IP ranges, link-local addresses, and cloud metadata hostnames. | Unit tests asserting `isUrlSafe()` returns `false` for `127.0.0.1`, `10.0.0.1`, `192.168.1.1`, `169.254.169.254`, and `metadata.google.internal`. | **High** |
| **`RssParser`** | Atom 1.0 vs RSS 2.0 XML parsing, OPML parsing, and XXE docdecl suppression. | Unit test suite with static XML fixtures under `sample_xml/`. | **High** |
| **`Room Migrations`** | Schema migration integrity across database versions 5 $\rightarrow$ 6 $\rightarrow$ 7 $\rightarrow$ 8. | AndroidX `MigrationTestHelper` instrumented tests verifying data retention on real SQLite migrations. | **Medium** |
| **`TtsPlaybackManager`** | Audio playlist queue restoration from Room DB across process death. | Unit test verifying queue indexing and links JSON deserialization. | **Medium** |
| **`NewsViewModel`** | StateFlow emissions during category selection and pull-to-refresh. | Coroutine `Turbine` test asserting UI state flows. | **Low** |

---

## 5. Link-Only Source Tests

[LinkOnlySourcesTest.kt](file:///d:/News/app/src/test/java/uk/sume/news/LinkOnlySourcesTest.kt) (offline, no network):
- Host matching for `LinkOnlySources.forUrl` / `isLinkOnlyArticle`.
- `RssParser` on a feed shaped like the publisher's: only title, link, date and short description survive; `content:encoded`, `media:content`, `enclosure` and embedded images are ignored; description is truncated at 500 characters; other feeds still read images.
- `LinkOnlyFetchPolicy`: the 30-minute floor, longer `max-age` / `ttl` / `Retry-After` honored, no 24-hour ceiling (48 h and 72 h instructions honored, one-year overflow bound), stored `ttl` still applied after a 304.
- `SourceConfig.parseEnabledIds`: missing/disabled means off; unreadable file is not validated.

```powershell
./gradlew testDebugUnitTest --tests "uk.sume.news.LinkOnlySourcesTest"
```

JVM tests use the real `org.json` (`testImplementation("org.json:json:20231013")`) because `android.jar` only provides non-functional stubs. Not covered by automated tests: the network fetch path, the purge on withdrawal, and the Detail screen behavior (these need manual verification on a device).

**Known failing test:** `BackupHelperTest.testSerializationDeserialization` fails at `BackupHelperTest.kt:114` (`is_ai_enabled` read back as `false`) when run with the real `org.json`. Before `org.json` was added to the test classpath it could not run at all on the stubbed `JSONObject`. Whether it passed in any earlier setup is `Unknown / Requires Confirmation`.
