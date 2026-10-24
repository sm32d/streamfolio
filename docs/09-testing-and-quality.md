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

## 6. iOS Tests (`ios/`, added 2026-10-24)

Swift Testing (`import Testing`), run on macOS with SwiftPM — no simulator needed:

```bash
swift test --package-path ios/Packages/StreamFolioCore
swift test --package-path ios/Packages/StreamFolioUI
```

- `StreamFolioCoreTests/UrlSecurityValidatorTests` — port of `UrlSecurityValidator` behavior: public URLs accepted; every private/reserved IPv4 range, RFC1918 and CGNAT boundaries, blocked metadata hostnames, `http`/`https` only, `requireHttps`/`normalizeToHttps`. iOS-only hardening is tested too: IPv6 literals (loopback, link-local, unique-local, mapped/NAT64/6to4 embeds), ambiguous numeric hosts (`2130706433`, `127.1`, `0x7f.0.0.1`), trailing-dot and `*.localhost` names, and rejection of explicit non-http schemes.
- `StreamFolioTokensTests/PaletteParityTests` — every color in `Color.kt` has a Swift counterpart with the same value (a new Android color with no Swift token fails the test); accent colors, alpha ladder, radii and spacing match `design_system/tokens/*.css`.
- `StreamFolioTokensTests/DesignSystemRulesTests` — source scans: no raw `glassEffect`, hex colors, `UIColor(`, literal corner radii or fixed font sizes under `ios/StreamFolio/`; `glassEffect` only inside `StreamFolioUI/Glass/`.

Core suites added with the data layer, reader, search, saved and settings (134 tests in total, all offline and deterministic):
- `RssParserTests` — every `sample_xml/` feed parses to the expected item count with no markup left in titles/descriptions; CDATA and entities; Atom; image from `media:thumbnail` or the first `<img>` (relative and `http` URLs resolved to https); "Headline - Publisher" splitting; items without a link skipped; truncated XML keeps earlier items; DOCTYPE/XXE inert; link-only feeds keep only title, link, date and a ≤500-character plain description; OPML.
- `CuratedFeedsTests` — `curated-feeds.json` equals `DefaultFeedsConfig.kt` (every URL per region in order, and 62 publisher rules); lookup honours region, disabled and cross-region feeds; link-only host matching and `ArticleCapabilities`.
- `LinkOnlyPolicyTests` — port of the Android fetch-policy and source-list tests (30-minute floor, longer server instructions honoured, no 24-hour ceiling, 304 keeps the stored ttl, one-year overflow bound, `Retry-After` as seconds or HTTP date).
- `RepositoryTests` — end to end with canned HTTP: refresh stores grouped articles; a failing feed is skipped; refreshing again keeps read state; link-only fetched only after a validated list, never when the list is unreachable/disabled/unreadable; at most one request per 30 minutes then conditional (`If-None-Match`/`If-Modified-Since`); 8 concurrent refreshes send one request; a 429 with `Retry-After: 172800` is obeyed for 48 h; a transport error does not throttle; withdrawal deletes articles, bookmarks and throttle state; the source list is reused for 60 s then re-validated.
- `ReaderTests` — `ArticleExtractor` (article container vs page-wide paragraphs, promo/caption/dateline filtering, entities, image precedence og → twitter → JSON-LD → first image, relative and http image URLs, Google logos and private hosts rejected); `ArticleContentService` (fetch once then serve from the database, unparseable vs failed, failures not cached, link-only articles make **no request** for body, page or image, thumbnail or the `failed` sentinel, prefetch limited to the newest N without a body, charset); `ReaderStore` (decision table, open marks read and keeps the article live, link-only opens in Web View); reading time. `RepositoryTests` also covers background enrichment.
- `SearchAndSavedTests` — query matching (whole-word for ≤3 characters, quotes ignored, publisher matched online only, LIKE wildcards escaped); trending topics (defaults, tag > category > word ranking, stopwords and SEARCH dropped); `SearchService` (fetches active feeds, caches only matches under SEARCH, replaces the previous search, keeps a saved result, never fetches link-only feeds, blank query / curated feeds off do nothing); `SearchStore` (phases, recents ordering/cap/persistence, choosing a suggestion); saved collections (counts, previews, All Saved, delete-with-move, rename, remove from a collection vs from All Saved).
- `PersistenceTests`, `StoryGrouperTests`, `PreferencesTests`, `BookmarkTests`, `ShareLinksTests`, `FeedStoreTests` — schema, default list, upsert/prune/observe/withdrawal, grouping, defaults and clamping, bookmark rules, share-link round trip and rejection cases, FeedStore trending/story split.
- `SettingsCoreTests` (27 tests) — custom feeds (HTTPS/private-host/blank rejection, edit, delete, OPML import with folder selection, renaming and duplicate skipping); OPML (nested folders, `Imported`, lower-case `xmlurl`, hostile input, export escaping and round trip, importable list); `CategoryLayout` (selection, order, custom tabs, never-empty fallback, stable ties); `ProviderSelection` (home vs other region, toggling result, "what is toggled is what is fetched", region order); backup (full round trip between two installs, Android file shape and key names, an Android-made file restoring, range/type rejection, unsafe URLs, link-only exclusion in both directions, a bad file changing nothing, dangling list links); custom feeds in refresh and search (tab filled with feed id/title, only matching tab fetched, OPML-as-feed expansion with re-validation, a broken feed skipped, search over custom feeds with curated off, single-feed fetch).
- UI package: 8 token-parity and design-rule tests (see above). The design-rule scan now covers real screens.

Not covered by automated tests: the Files picker/exporter and the confirmation dialogs of Settings, any SwiftUI view (checked by hand on simulators, see `docs/07`), real network behavior, Foundation Models / Translation, and anything on a physical device. The URLSession rebinding gap needs a hostile resolver to test. Note the Android `BackupHelperTest` failure above: do not treat it as the iOS specification until its cause is known.

**Known failing test:** `BackupHelperTest.testSerializationDeserialization` fails at `BackupHelperTest.kt:114` (`is_ai_enabled` read back as `false`) when run with the real `org.json`. Before `org.json` was added to the test classpath it could not run at all on the stubbed `JSONObject`. Whether it passed in any earlier setup is `Unknown / Requires Confirmation`.
