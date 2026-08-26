# 09 — Testing & Quality Assurance Strategy

> **Last verified commit:** `ab6006967a8179f7bbee19a2e0d89c9ec248ba0a` (P0 + P1 + most of P2 implemented on top of this)
> **Analysis Date:** 2026-08-26

---

## 1. Current State

P0 and P1 are fully implemented (§1a, §1c, §1d) — 86 JVM unit tests (Robolectric-backed where noted), passing via `./gradlew testDebugUnitTest` except the pre-existing, inherently-flaky live-network `DefaultFeedsTest`. P2 (Compose UI tests) is now also substantially implemented (§1e) — 26 instrumented tests covering every screen and the shared components actually in use, passing via `./gradlew connectedDebugAndroidTest` against a real device (verified stable across repeated `--rerun` runs). Screenshot tests and full E2E journeys (P3) remain as described in §5 onward — still a recommendation, not yet built.

```
app/src/
├── test/java/uk/sume/news/                          # Legacy-package JVM unit tests
│   ├── BackupHelperTest.kt                          # JSON backup round-trip (mock-editor bug fixed, see §1a)
│   ├── DefaultFeedsTest.kt                          # Live network feed verification & cleartext redirect audit
│   └── ExampleUnitTest.kt                           # Baseline smoke test
├── test/java/uk/sume/streamfolio/                   # New P0/P1 tests (correct package, see housekeeping note)
│   ├── util/UrlSecurityValidatorTest.kt             # SSRF/scheme/private-IP validation
│   ├── util/BackupHelperMalformedInputTest.kt       # Malformed/truncated/oversized restore JSON
│   ├── util/OpmlHelperTest.kt                       # OPML import/export, malformed-input behavior
│   ├── data/network/SsrfProtectionInterceptorTest.kt # DNS/IP-literal-resolution SSRF blocking
│   ├── data/network/RssParserTest.kt                # RSS 2.0 / Atom / OPML detection / XXE suppression
│   ├── data/network/ArticleGroupingTest.kt          # Jaccard title-clustering (see §1a extraction)
│   ├── data/local/PreferencesHelperTest.kt          # Robolectric: every preference key round-trip (P1)
│   ├── data/local/MigrationsTest.kt                 # Robolectric + MigrationTestHelper: MIGRATION_5_6..8_9 (P1)
│   ├── playback/TtsPlaybackManagerTest.kt           # Robolectric: playlist/index bookkeeping + restore (P1, scoped - see §1c)
│   ├── ui/viewmodel/NewsViewModelTest.kt            # Robolectric: category selection, refresh, loadSimilarArticles (P1, see §1d)
│   └── testutil/FakeAndroidPrefs.kt                 # Shared fake SharedPreferences/Context for JVM tests
└── androidTest/java/uk/sume/streamfolio/            # New P2 tests: real on-device Compose UI tests (see §1e)
    ├── testutil/BaseAndroidUiTest.kt                # Shared launch/teardown for every P2 test class
    ├── testutil/AndroidTestSupport.kt               # Fixture seeding, prefs/DB reset
    ├── ui/screens/HomeScreenTest.kt                 # Category tabs, article tap-through, pull-to-refresh
    ├── ui/screens/DetailScreenTest.kt                # Reader/Web toggle, back nav, "also covered by" panel
    ├── ui/screens/SearchScreenTest.kt                # Local search, trending topics, recent searches
    ├── ui/screens/BookmarkScreenTest.kt              # Collections grid/list, create collection, article detail
    ├── ui/screens/settings/SettingsScreensTest.kt   # Hub + all 7 settings sub-screens
    └── ui/components/SharedComponentsTest.kt         # Bookmark-via-Save flow, TTS mini-player
(legacy) androidTest/java/uk/sume/news/
    └── ExampleInstrumentedTest.kt                    # Android context & package verification
```

**Housekeeping note:** existing legacy test files stay under package `uk.sume.news` (left as-is, out of scope to rename); all new tests added in this pass use the correct `uk.sume.streamfolio` package, per the recommendation this doc made previously.

### 1a. What changed in this pass, and two pre-existing bugs it uncovered

Implementing the P0 tests required two small, test-only/test-adjacent fixes — both are pre-existing defects that predate this pass, not introduced by it:

1. **`org.json` was unusable in JVM unit tests.** The Android unit-test stub jar throws `RuntimeException: Method put in org.json.JSONObject not mocked` for *every* `org.json.*` call (constructor, `put`, `get*`, `opt*` — all of it), because there was no real JVM implementation of `org.json` on the test classpath (only `xmlpull`/`xpp3` had been added for `RssParser`'s `XmlPullParser` needs, not the `org.json` equivalent). This meant **the pre-existing `BackupHelperTest.testSerializationDeserialization` was actually failing before this pass** — its "Confirms state mutation and subsequent restoration via `applyPreferences`" claim in this doc's prior revision was not verified against a real test run. Fixed by adding `testImplementation("org.json:json:20231013")` to [app/build.gradle.kts](file:///d:/News/app/build.gradle.kts) (mirrors the existing `xmlpull`/`xpp3` pattern).
2. **`BackupHelperTest`'s own `MockEditor.apply()`/`commit()` were no-ops.** Once (1) was fixed, the test still failed — `prefs.isAiEnabled = true` followed by two more `prefs.someField = x` assignments (each its own `edit()...apply()` pair) silently lost everything but the *last* write, because `apply()` did nothing and each new `edit()` call reset the pending editor state from the (never-updated) backing map. Fixed in [BackupHelperTest.kt](file:///d:/News/app/src/test/java/uk/sume/news/BackupHelperTest.kt) so `apply()`/`commit()` actually write through, matching real `SharedPreferences` semantics. Test-only change; no production code affected.
3. **`ArticleGrouping.kt` extraction (production code, behavior-preserving).** `NewsRepository.assignGroupIds()`'s Jaccard-clustering math (`cleanTitle`, `calculateJaccardSimilarityFromSets`, and the grouping loop) was private and only reachable through `NewsRepository`, whose constructor unconditionally builds a real Room `AppDatabase` — making the clustering algorithm itself untestable without Robolectric + a real DB just to test string/set math. Extracted verbatim (same regexes, same stopword/suffix lists, same `0.35f` threshold) into a new pure object, [ArticleGrouping.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/data/network/ArticleGrouping.kt); `NewsRepository.assignGroupIds()` now just fetches `existingArticles` from the DAO and delegates to `ArticleGrouping.assignGroupIds()`. No behavior change — see [ArticleGroupingTest.kt](file:///d:/News/app/src/test/java/uk/sume/streamfolio/data/network/ArticleGroupingTest.kt) for the resulting unit tests. `docs/01-architecture.md`'s "Story Grouping" references are unaffected (still accurate — `assignGroupIds()` is still where `NewsRepository` computes/persists `groupId`, it just now delegates the math).

### 1b. Fixed: `UrlSecurityValidator` was not enforcing its documented scheme allowlist

Found while writing P0 tests, then fixed at the user's request (not left as an open item): `UrlSecurityValidator.normalizeUrl()` only recognized literal `"http://"`/`"https://"` prefixes — any other string (e.g. `"ftp://example.com/feed.xml"`, `"javascript://alert(1)"`, `"file:///etc/passwd"`, `"mailto:someone@example.com"`) got `"https://"` prepended rather than rejected. Two distinct ways this bypassed the scheme allowlist, both now covered by regression tests:

1. **Authority form** (`scheme://...`): `"ftp://example.com/feed.xml"` became `"https://ftp://example.com/feed.xml"`, which OkHttp's `HttpUrl` parsed leniently as host `"ftp"` with the rest folded into the path.
2. **Opaque form** (`scheme:...`, no `//`): `"mailto:someone@example.com"` became `"https://mailto:someone@example.com"`, which OkHttp parsed as userinfo `"mailto:someone"` at a *fully legitimate* host `example.com` — arguably worse than (1), since no lenient/invalid parsing was even required.

Either way `isUrlSafe()` reported these as **safe**, contradicting its own KDoc ("Only http/https schemes are allowed"), because by the time the `ALLOWED_SCHEMES` check ran the URL had already been mangled into something with scheme `"https"`.

**Fix** (in [UrlSecurityValidator.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/util/UrlSecurityValidator.kt)): `normalizeUrl()` now detects a leading `scheme:` token (RFC 3986 syntax) and rejects it outright instead of coercing it — *unless* what follows the colon is purely digits terminating at `/`/end-of-string, which is a `host:port` a user legitimately typed for a custom feed (e.g. `"example.com:8080/feed"`), not a scheme. Regression coverage in [UrlSecurityValidatorTest.kt](file:///d:/News/app/src/test/java/uk/sume/streamfolio/util/UrlSecurityValidatorTest.kt): `` `rejects urls with an explicit non-http scheme instead of coercing them to https` ``, `` `rejects single-colon schemes with no authority slashes` ``, plus `` `bare hostnames without any scheme still get https prepended` `` and `` `bare host colon port is not mistaken for a scheme` `` confirming the legitimate cases still work. Verified no other call site in the app relies on the old lenient behavior — every caller (`BackupHelper`, `NewsRepository`, `NewsViewModel`, `RssParser`, custom-feed/OPML screens) uses this purely as a hardening gate on URLs already expected to be http(s).

### 1c. P1: Robolectric added — Room migrations, PreferencesHelper, and TtsPlaybackManager now tested

Three build-config problems had to be solved before any of this would run, all specific to this repo's setup:

1. **`compileSdk = 36` (Android 16 preview) vs. Robolectric support.** Unverified going in. `org.robolectric:robolectric:4.14.1` was added, and every Robolectric test class pins `@Config(sdk = [34])` rather than assuming compileSdk parity — 34 is a long-stable, well-supported Robolectric target. This worked without issue; nothing suggests 34 needs revisiting, but no test here exercises anything 36-specific.
2. **Room's `MigrationTestHelper` couldn't find the exported schema JSONs.** `MigrationTestHelper.loadSchema()` calls real `Context.assets.open(...)` — under Robolectric that only resolves if `testOptions.unitTests.isIncludeAndroidResources = true` is set (off by default; without it AGP never merges *any* resources/assets for the `test` source set, confirmed by the complete absence of a `mergeDebugUnitTestAssets`-style task before this change). Once that's on, the merged-assets path Robolectric reads from is the app's own **debug variant** assets (`android_merged_assets` in the generated `test_config.properties` points at `mergeDebugAssets`, not a separate "test assets" concept) — so the schema files had to be added via `sourceSets.getByName("debug").assets.srcDirs("$projectDir/schemas")` in [app/build.gradle.kts](file:///d:/News/app/build.gradle.kts), not `getByName("test")`. This means the schema JSONs ship inside debug APKs (never release, since `isMinifyEnabled`/shrinking only apply to `release` and this is a `debug`-only sourceSet addition) — a small, accepted trade-off; there was no narrower option available for local (non-instrumented) unit tests.
3. **Robolectric's default SQLite shadow doesn't support real multi-threaded access.** `TtsPlaybackManagerTest`'s restore-from-DB test failed with `IllegalStateException: Illegal connection pointer` because `TtsPlaybackManager.restorePlaylist()` reads via Room's IO executor on a real background thread, and Robolectric's legacy SQLite shadow ties connections to the thread that opened them. Fixed with `@SQLiteMode(SQLiteMode.Mode.NATIVE)` on that test class, which uses a real multi-threading-capable SQLite implementation instead.

**What's covered:**
- [MigrationsTest.kt](file:///d:/News/app/src/test/java/uk/sume/streamfolio/data/local/MigrationsTest.kt): each of `MIGRATION_5_6` → `MIGRATION_8_9` individually (new table/column/index creation, existing-row preservation) plus a full 5→9 chain, using real SQLite via `MigrationTestHelper` rather than mocks.
- [PreferencesHelperTest.kt](file:///d:/News/app/src/test/java/uk/sume/streamfolio/data/local/PreferencesHelperTest.kt): every stored preference's default value and round-trip through a fresh `PreferencesHelper` instance over the same backing store (simulating re-reading after process death, not just re-reading the same live object), plus the `recentSearches` 10-entry cap and `categoryOrder`'s whitespace/empty-segment handling.
- [TtsPlaybackManagerTest.kt](file:///d:/News/app/src/test/java/uk/sume/streamfolio/playback/TtsPlaybackManagerTest.kt): `addToTtsPlaylist`/`playNextInTtsPlaylist`/`removeFromTtsPlaylist`/`moveTtsPlaylistItem`/`clearTtsPlaylist` index bookkeeping, plus a genuine restore-across-process-death test (persist a `TtsPlaylistState` + `Article` rows directly via the DAOs, construct a fresh manager, poll for the async `Dispatchers.IO`-based restore to complete).

**Deliberately out of scope for this pass** (same file): `playTtsPlaylist()`, `resumePlayback()`, and `setSleepTimer()` all call `ensureServiceRunning()` (starts the real `TtsPlaybackService`) and/or fetch article bodies via `NewsRepository` (real `OkHttpClient`/network). §1d introduced an interface seam for `NewsViewModel`'s AI helpers, but **not** for `NewsRepository` itself — testing these `TtsPlaybackManager` methods would need that too (`NewsRepository` has no interface/fake seam of any kind), which is a larger, not-yet-scoped production-code change. `TtsPlaybackManager`'s singleton (`companion object`'s `@Volatile private var instance`) also has no public reset hook; the test resets it via reflection each run (the field turned out to be a static field on the *outer* class, not the `Companion` class — Kotlin does this specifically for `@Volatile` companion properties; verified with `javap -p` on the compiled class rather than assumed).

### 1d. P1 completed: NewsViewModel — an interface seam plus four Robolectric gotchas

`NewsViewModel`'s own `init{}` block unconditionally calls `checkGeminiSupport()`, which constructs the real ML Kit GenAI-backed `AiSummaryHelper` and calls `checkFeatureStatus()` on it — a Play-Services-adjacent SDK Robolectric doesn't shadow, risking a hang on a callback that never fires. `AiHelperFactory` already returned the existing `IAiSummaryHelper`/`IAiTranslateHelper` interfaces (a seam that predates this session), so the fix was adding two optional constructor parameters rather than inventing new interfaces:

```kotlin
class NewsViewModel @JvmOverloads constructor(
    application: Application,
    private val aiSummaryHelperOverride: IAiSummaryHelper? = null,
    private val aiTranslateHelperOverride: IAiTranslateHelper? = null
) : AndroidViewModel(application)
```

`@JvmOverloads` is required, not cosmetic: without it, adding these parameters compiles to a single Java constructor requiring all three arguments, which breaks `ViewModelProvider`'s default `AndroidViewModelFactory` reflection lookup for a single-`Application`-arg constructor — exactly how `MainActivity` constructs it (`ViewModelProvider(this)[NewsViewModel::class.java]`). `@JvmOverloads` generates the trailing-parameter overloads needed, including that exact single-arg one. A `TtsHelper` interface was considered but turned out unnecessary: `NewsViewModel` never calls `TtsHelper` directly (only via `TtsPlaybackManager`'s already-real, already-Robolectric-tested singleton), so there was nothing there to fake.

Four Robolectric-specific behaviors had to be understood by trial and error while writing [NewsViewModelTest.kt](file:///d:/News/app/src/test/java/uk/sume/streamfolio/ui/viewmodel/NewsViewModelTest.kt) — worth knowing before extending this file or writing similar ViewModel tests elsewhere in this repo:

1. **`SharingStarted.Lazily`/`WhileSubscribed` StateFlows never start from reading `.value` alone.** `categoryArticlesState` is `stateIn(viewModelScope, SharingStarted.Lazily, ...)` — the upstream Room-query/debounce/grouping pipeline only starts once something actually **collects** it. Polling `.value` in a loop hoping it updates does nothing; the test attaches a throwaway `launch { flow.collect {} }` first.
2. **Robolectric's main `Looper` defaults to `PAUSED`.** Anything dispatched via `Dispatchers.Main`/`Dispatchers.Main.immediate` — including `viewModelScope` itself and the coroutine `SharingStarted.Lazily` uses to detect "first subscriber attached" — is queued but never runs until the looper is explicitly idled. `ShadowLooper.idleMainLooper()` inside the test's polling loop is what actually lets those queued coroutines execute; without it, `categoryArticlesState.value` stays at its initial empty default forever regardless of how long a plain `Thread.sleep()` loop waits.
3. **`@Volatile` singleton statics leak across test classes, not just test methods, in the same Gradle JVM fork.** Gradle reuses one forked JVM across multiple test classes by default. `TtsPlaybackManager.instance` and `AppDatabase.INSTANCE` are plain JVM statics (§1c), so a `NewsViewModel` constructed in this test class could silently reuse a connection left behind by `TtsPlaybackManagerTest` (or a prior `NewsViewModelTest` method), pointed at that other test's already-torn-down Robolectric sandbox. `NewsViewModelTest.resetSingletons()` nulls both in `@Before`/`@After`, and `RoomDatabase.clearAllTables()` must run on a background dispatcher (`assertNotMainThread()` — even under Robolectric).
4. **`NewsViewModel.init{}` runs a real, destructive one-time migration on every fresh instance.** `if (!prefs.hasClearedOldTags) { repository.clearAllTags(); prefs.hasClearedOldTags = true }` runs unconditionally for a fresh `PreferencesHelper` (default `false`), and `clearAllTags()` sets **every** article's `tags` column to `NULL` — silently wiping tag fixtures a test had just inserted, if the ViewModel is constructed afterward. Discovered by literally reading the value back as `null` after insert with no exception anywhere; the fix is pre-setting `prefs.hasClearedOldTags = true` before constructing the ViewModel in tests, so this irrelevant legacy cleanup path is skipped.

**What's covered:** `selectCategory()` updating `selectedCategory` synchronously; `categoryArticlesState` reactively reflecting locally-tagged articles for a hashtag ("#tag") category — chosen specifically because `refreshCurrentFeed()`'s `currentCat.startsWith("#")` branch and hashtag `categoryArticlesState` both read from local Room data only, letting this be tested without a `NewsRepository` fake (out of scope, see above); `refreshCurrentFeed()` leaving `isRefreshing` false (not stuck true) for both `silent = true` and `silent = false`; `loadSimilarArticles()`'s indexed-`groupId` lookup and its live-Jaccard-scan fallback for articles predating `groupId` assignment.

### 1e. P2 completed: on-device Compose UI tests — device/SDK setup and six gotchas

P2 targets a **real physical device**, not an emulator (`adb devices` was invoked against a real Oppo/OnePlus-family device, package `uk.sume.streamfolio`, device model `CPH2651`, ColorOS). Two setup facts worth recording since they weren't obvious going in:

- **`adb` is not on `ANDROID_HOME`/`PATH` in this environment** — the actual SDK location is whatever [local.properties](file:///d:/News/local.properties)'s `sdk.dir` says (a machine-local, gitignored file), not a global environment variable. `adb.exe` lives under `<sdk.dir>/platform-tools/`.
- **`connectedDebugAndroidTest` has no `--tests` flag** (that's a `testDebugUnitTest`-only Gradle test filter). To target a single class or the whole package on-device, use `-Pandroid.testInstrumentationRunnerArguments.class=<FQCN>` or `.package=uk.sume.streamfolio` instead, e.g.:
  ```powershell
  ./gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.package=uk.sume.streamfolio --console=plain
  ```

**Launch pattern:** every P2 test extends [BaseAndroidUiTest.kt](file:///d:/News/app/src/androidTest/java/uk/sume/streamfolio/testutil/BaseAndroidUiTest.kt), which uses `createEmptyComposeRule()` plus a manual `ActivityScenario.launch(MainActivity::class.java)` called from inside the test body — **not** `createAndroidComposeRule<MainActivity>()`. The reason: `MainActivity`/`AppNavigation` reads `viewModel.prefs.isCompletedOnboarding` once, synchronously, to pick `NavHost`'s `startDestination`, and `createAndroidComposeRule`'s rule launches the activity as part of its own `apply()` — before any `@Before` method (and therefore before [AndroidTestSupport.resetAppState()](file:///d:/News/app/src/androidTest/java/uk/sume/streamfolio/testutil/AndroidTestSupport.kt)) has run. Deferring the launch to inside the test body, after `resetAppState()`, avoids racing against the app's own real, non-test-aware startup logic.

**Fixture/state isolation gotchas found (all in [AndroidTestSupport.kt](file:///d:/News/app/src/androidTest/java/uk/sume/streamfolio/testutil/AndroidTestSupport.kt) unless noted):**

1. **Instrumented tests share one real app process/SharedPreferences file across every test method in a run** — unlike Robolectric's per-test sandbox. `resetAppState()` fully clears the `"news_prefs"` file (not just individual keys) before re-setting only what's needed; a `recentSearches` entry left over from an earlier test method produced a real, reproducible spurious duplicate-node match in a later, unrelated test before this was fixed.
2. **`isDefaultFeedsEnabled = false` is required to kill real background network activity.** With it on, `NewsViewModel.init{}` fires `refreshCurrentFeed()` + `syncAllCategoriesInBackground()` for all 8 default categories on every single app launch — pure timing-flakiness noise for tests that seed their own fixtures directly into Room. Category tabs a test needs still appear via `customFeeds.map { it.category }`, independent of this flag.
3. **A fixture article's `sourceUrl` must belong to a registered `CustomFeed` row to appear in a real category.** `HomeScreen`'s category filter is `article.customFeedId != null || enabledUrls.contains(article.sourceUrl)`; a fixture URL like `"https://example.com/feed.xml"` matches neither, so `seedCustomFeedArticle()` registers a real `CustomFeed` row and sets `customFeedId` on the fixture, mirroring how a user's own added RSS feed behaves.
4. **`HomeScreen` renders an article via `TrendingCard`, not `ArticleListItem`, for the first 3 articles in a category.** The same `testTag("article_item_${article.link}")` convention had to be applied to both composables (see [HomeScreen.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/ui/screens/HomeScreen.kt)) — a test tagging only `ArticleListItem` would silently fail to find a single seeded article.
5. **`CustomFeedDao.getAllFeeds()` orders by `id DESC`**, not insertion order — a reorder test seeding "Alpha" then "Beta" sees `[Beta, Alpha]` as the actual initial on-screen order (Beta = higher id = sorts first), not `[Alpha, Beta]`. Confirmed via `printToLog` diagnostics after an initially-backwards assertion failed.
6. **`AppDatabase.clearAllTables()` doesn't restore the "there's always a Saved list" invariant** — that's normally seeded idempotently by `AppDatabase`'s `onOpen` callback, which only fires when the connection is first opened, not on `clearAllTables()`. `resetAppState()` re-inserts the default `BookmarkList(name = "Saved", isDefault = true)` row by hand after clearing.

**Compose-test-API gotchas found (all confirmed via direct experimentation against the real device):**

- **`hasText`/`onNodeWithText` default to an *exact* string match, not substring.** A toast/label asserted with the visible substring (e.g. `"Saved to"` when the real text is `Saved to "Saved"`) silently fails to match until `substring = true` is passed explicitly.
- **Material3's `SwipeToDismissBox` (used inline in `HomeScreen.ArticleListItem` for swipe-to-bookmark/share) is not reliably triggered by `performTouchInput`'s synthetic gestures** when nested inside a vertically-scrolling `LazyColumn`, even with an explicit multi-step `swipe()` past its `positionalThreshold = 0.75f`. [SharedComponentsTest.kt](file:///d:/News/app/src/androidTest/java/uk/sume/streamfolio/ui/components/SharedComponentsTest.kt) documents this as a known environment limitation and instead exercises `DetailScreen`'s plain "Save" `IconButton`, which calls the identical downstream `NewsViewModel.toggleBookmark()` flow via a simple, reliable click.
- **`SaveToListBottomSheet`'s list-selection state initializes asynchronously.** Its `selectedListIds` populates via a `LaunchedEffect(assignedListIds, allLists)`; clicking "Done" before that effect resolves leaves `selectedListIds` empty, which `NewsViewModel.saveArticleToLists()` silently no-ops on (`if (targetListIds.isNotEmpty())`) — no exception, no toast, no DB write. Fixed by waiting for `isOn()` on the pre-selected checkbox before clicking "Done".
- **`ui/components/SwipeableCard.kt` is dead code** — defined but never referenced from anywhere in the app (confirmed by search, not assumption). It is intentionally *not* tested in [SharedComponentsTest.kt](file:///d:/News/app/src/androidTest/java/uk/sume/streamfolio/ui/components/SharedComponentsTest.kt) despite being named in this doc's original P2 backlog; the real swipe-to-bookmark/share mechanism is `SwipeToDismissBox` as described above. `GlassmorphicNavBar` also has no dedicated test file — every P2 screen test already navigates through it by tapping its tab labels, equivalent coverage of its actual behavior (tab-selection routing) without a redundant separate test.
- **One-off `"No compose hierarchies found in the app"` failures were traced to real OEM background-process killing** (ColorOS aggressively kills backgrounded app processes; confirmed via `adb logcat` showing `System.exit called, status: 0` / `PACKAGE_REMOVED` entries at the failure timestamps), not a test or app bug — non-reproducible on immediate rerun of the same test.
- Compose's `waitUntilAtLeastOneExists` requires `-opt-in=androidx.compose.ui.test.ExperimentalTestApi`; added once, globally, via `kotlinOptions.freeCompilerArgs` in [app/build.gradle.kts](file:///d:/News/app/build.gradle.kts), rather than a per-file `@OptIn` on every test class.

**Diagnostic technique used throughout:** `composeRule.onRoot().printToLog("TAG")` combined with `adb logcat -c` (clear) then `adb logcat -d | grep <TAG>` (dump + filter) — this is how gotchas 1, 4, 5, and the search-screen duplicate-node issue below were actually root-caused, rather than guessed at.

**Scope note — settings sub-screens:** [SettingsScreensTest.kt](file:///d:/News/app/src/androidTest/java/uk/sume/streamfolio/ui/screens/settings/SettingsScreensTest.kt) covers one or two representative interactions per sub-screen (a toggle round-trip, a header/navigation check, one add-item flow) rather than exhaustively covering every control on all 7 screens — see the P2 backlog table below for exactly what each row covers.

### Why this matters concretely

`docs/07-frontend-or-client-app.md` documents two UI regressions that already shipped and were caught only by manual visual inspection on a device: Material3's `darkColorScheme()`/`lightColorScheme()` silently defaulting unset container/outline slots to the stock purple palette, and `CollectionGridCard`'s empty-state gradient washing out against the light-theme background. Both are exactly the class of bug that a Compose screenshot test would catch automatically and cheaply, before merge. That gap motivates the screenshot-testing recommendation in §4.

---

## 2. Architectural Constraints on Testability

Before proposing new tests, note what the current architecture allows without changes:

- **No DI framework.** `NewsViewModel(application: Application) : AndroidViewModel(application)` ([NewsViewModel.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/ui/viewmodel/NewsViewModel.kt)) and `NewsRepository(private val context: Context)` ([NewsRepository.kt](file:///d:/News/app/src/main/java/uk/sume/streamfolio/data/network/NewsRepository.kt)) both require a real or Robolectric-simulated `Context`/`Application` to construct — they cannot be unit-tested with injected fakes today. Adding a thin interface seam (e.g. an `AiSummaryHelper`/`AiTranslateHelper`/`TtsHelper` interface the ViewModel depends on) would unlock true unit tests of ViewModel state transitions, but that is a small refactor, not a test-only change — tracked as a backlog item in §4, not performed by this doc.
- **Pure-logic classes, already unit-testable as-is (no Android dependency beyond `XmlPullParser`, which the project already stubs for JVM via `xmlpull`/`xpp3` test deps):** `RssParser` (class), `UrlSecurityValidator` (object), `BackupHelper` (object), `OpmlHelper` (object), `DefaultFeedsConfig` (object).
- **Android-framework-coupled, need Robolectric or an instrumented device:** `PreferencesHelper(context: Context)`, `Migrations.kt` (Room `Migration` objects — need `androidx.room:room-testing` + `MigrationTestHelper`), `TtsPlaybackManager` (singleton with a `private constructor`).
- **Testable today with no Context and no real sockets:** `SsrfProtectionInterceptor` (plain OkHttp `Interceptor`) — since its branching depends only on `InetAddress.getByName(host)` resolution of `chain.request().url`, a hand-rolled fake `Interceptor.Chain` returning a canned in-memory `Response` from `proceed()` is enough (see `SsrfProtectionInterceptorTest`); IP-literal hosts (`8.8.8.8`, `127.0.0.1`, etc.) resolve without any network call at all.
- **Cannot run in CI or most emulators at all:** on-device Gemini Nano (`AiSummaryHelper`) and AICore, and platform TTS engines (`TtsHelper`) depend on device-specific ML runtimes/voices. Any ViewModel test that touches these paths needs a fake/interface, not a real instance.

---

## 3. Recommended Testing Pyramid

| Layer | Where it runs | Speed | What it covers |
| :--- | :--- | :--- | :--- |
| **(a) JVM unit tests** | `app/src/test/`, plain JUnit | Fastest (seconds, no device) | Pure logic: parsers, validators, helpers, clustering algorithms |
| **(b) Robolectric JVM tests** | `app/src/test/`, Robolectric-backed | Fast (no emulator boot) | Android-framework-coupled classes: `PreferencesHelper`, Room migrations, (post-refactor) `NewsViewModel`/`NewsRepository` |
| **(c) Instrumented Compose UI tests** | `app/src/androidTest/`, real device/emulator | Slow (emulator boot + install) | Per-screen/component rendering and interaction |
| **(d) Critical end-to-end journeys** | `app/src/androidTest/`, real device/emulator | Slowest | A handful of full user flows spanning multiple screens |

(a) and (b) should be the CI-gating fast path (run on every PR); (c) and (d) are emulator-bound and slower, so they are recommended as a separate, less frequent CI job (see §6).

---

## 4. Tooling Additions

**Applied for P0** (`gradle/libs.versions.toml` + `app/build.gradle.kts`, `testImplementation`):
- `io.mockk:mockk` — used to stub `android.util.Log.w()` (`BackupHelperMalformedInputTest`) and `android.util.Xml.newPullParser()` (`OpmlHelperTest`, delegated to a real `XmlPullParserFactory`-created parser) so these Android-framework calls don't hit the unit-test stub jar's "not mocked" throw.
- `org.json:json:20231013` — a real JVM `org.json` implementation; see §1a finding #1. Without this, **no** `org.json.*` call works in a JVM unit test (not just `optInt` — `put`, `get*`, the constructor, all of it).

P0 was achievable without Robolectric or MockWebServer by (a) hand-rolling a minimal fake `Interceptor.Chain` for `SsrfProtectionInterceptorTest` (fully in-memory, no real sockets, more deterministic than spinning up MockWebServer for logic that only branches on DNS/IP-literal resolution) and (b) extracting `NewsRepository`'s clustering math into the pure `ArticleGrouping.kt` object (§1a, item 3) instead of standing up Robolectric + an in-memory Room DB just to unit-test string/set math.

**Applied for P1** (see §1c for the setup problems each of these needed solving):
- `org.robolectric:robolectric:4.14.1` — runs Android-framework-coupled code (`PreferencesHelper`, Room migrations, `TtsPlaybackManager`) as a JVM unit test. Every Robolectric test class pins `@Config(sdk = [34])` rather than assuming `compileSdk = 36` parity (unverified).
- `androidx.test:core:1.6.1` and `androidx.test.ext:junit` (already present for `androidTest`, now also `testImplementation`) — `ApplicationProvider.getApplicationContext()` for Robolectric tests.
- `androidx.room:room-testing` — `MigrationTestHelper` for `MIGRATION_5_6` through `MIGRATION_8_9`.
- `testOptions.unitTests.isIncludeAndroidResources = true` and a `debug`-sourceSet `assets.srcDirs("$projectDir/schemas")` in [app/build.gradle.kts](file:///d:/News/app/build.gradle.kts) — required for `MigrationTestHelper` to find the exported schema JSONs under Robolectric (§1c item 2).
- `@SQLiteMode(SQLiteMode.Mode.NATIVE)` (Robolectric annotation, no extra dependency) on `TtsPlaybackManagerTest` — needed for real multi-threaded DB access from Room's IO executor (§1c item 3).

**Still recommended, not yet applied** (needed once a `NewsRepository` fake/interface seam exists — see §1c/§1d scope notes on `TtsPlaybackManager`/`NewsViewModel`):
- `com.squareup.okhttp3:mockwebserver` — deterministic HTTP fixtures for `NewsRepository` network-flow tests, and for `playTtsPlaylist()`/`resumePlayback()`/`setSleepTimer()`/`refreshCurrentFeed()` on a real (non-hashtag) category.
- `org.jetbrains.kotlinx:kotlinx-coroutines-test` — `runTest`/`TestDispatcher` would replace the `ShadowLooper.idleMainLooper()` + `Thread.sleep()` polling pattern §1d had to use with deterministic virtual-time control.
- `app.cash.turbine:turbine` — assert `StateFlow`/`Flow` emission *sequences* (not just final values) from `NewsViewModel`; not needed for what §1d covers, since polling `.value` after idling the looper was sufficient for those specific assertions.
- `androidx.arch.core:core-testing` — `InstantTaskExecutorRule` for synchronous LiveData/architecture-components execution (not currently needed — this app uses `StateFlow` throughout, not `LiveData`).

**Optional, specifically to prevent regressions like the ones in §1 (original color-palette bugs):**
- `app.cash.paparazzi` — JVM-only Compose screenshot tests (no emulator required) for `ui/theme/Theme.kt` / `Color.kt` across light, dark, and dynamic-color variants.

No production code dependencies changed beyond the test-only additions above, and no DI framework was introduced — kept deliberately minimal in line with this repo's "prefer small, precise edits" convention.

---

## 5. Prioritized Test Backlog

### P0 — High-risk logic, currently zero coverage — ✅ Implemented

| Target | Behavior Covered | Test Type | Test File |
| :--- | :--- | :--- | :--- |
| [`UrlSecurityValidator.kt`](file:///d:/News/app/src/main/java/uk/sume/streamfolio/util/UrlSecurityValidator.kt) | SSRF blocking for private IP ranges, link-local, loopback, cloud metadata hostnames, scheme/blank/malformed handling, `normalizeUrl`/`sanitizeUrl`/`normalizeToHttps`/`isResolvedAddressSafe`. Also found and fixed a real scheme-allowlist bypass — see §1b. | Unit | [UrlSecurityValidatorTest.kt](file:///d:/News/app/src/test/java/uk/sume/streamfolio/util/UrlSecurityValidatorTest.kt) |
| [`SsrfProtectionInterceptor.kt`](file:///d:/News/app/src/main/java/uk/sume/streamfolio/data/network/SsrfProtectionInterceptor.kt) | Blocks loopback/`localhost`/RFC1918/link-local resolved addresses; allows a public IP literal through to `proceed()` | Unit (fake `Interceptor.Chain`, no MockWebServer needed — see §4) | [SsrfProtectionInterceptorTest.kt](file:///d:/News/app/src/test/java/uk/sume/streamfolio/data/network/SsrfProtectionInterceptorTest.kt) |
| [`RssParser.kt`](file:///d:/News/app/src/main/java/uk/sume/streamfolio/data/network/RssParser.kt) | RSS 2.0 and Atom parsing, link-required article dropping, malformed-XML resilience, DOCDECL/XXE entity suppression, `isOpml`/`parseOpmlUrls` | Unit, inline XML fixtures | [RssParserTest.kt](file:///d:/News/app/src/test/java/uk/sume/streamfolio/data/network/RssParserTest.kt) |
| [`BackupHelper.kt`](file:///d:/News/app/src/main/java/uk/sume/streamfolio/util/BackupHelper.kt) | Blank/garbage/truncated JSON throws `JSONException`; `version < 1` throws `IllegalArgumentException`; missing optional sections degrade gracefully; unsafe custom-feed/article URLs are skipped (not restored); oversized string fields are clamped; out-of-range preference values are ignored, not applied | Unit (MockK stubs `Log.w`) | [BackupHelperMalformedInputTest.kt](file:///d:/News/app/src/test/java/uk/sume/streamfolio/util/BackupHelperMalformedInputTest.kt) |
| [`OpmlHelper.kt`](file:///d:/News/app/src/main/java/uk/sume/streamfolio/util/OpmlHelper.kt) | Nested-folder OPML parsing, default `"Imported"` category, folder-only outlines produce no feed, malformed OPML **propagates an exception** (no internal try/catch, unlike `RssParser`), XML-escaping on export, export→import round-trip | Unit (MockK delegates `Xml.newPullParser()` to a real parser) | [OpmlHelperTest.kt](file:///d:/News/app/src/test/java/uk/sume/streamfolio/util/OpmlHelperTest.kt) |
| [`ArticleGrouping.kt`](file:///d:/News/app/src/main/java/uk/sume/streamfolio/data/network/ArticleGrouping.kt) (extracted from `NewsRepository.assignGroupIds()`, see §1a) | Near-duplicate titles from different sources join the same group; unrelated titles don't; every article gets a non-null `groupId`; new articles join an existing prior-ingest group; an article with a pre-set `groupId` keeps it; `cleanTitle`/`jaccardSimilarity` edge cases | Unit | [ArticleGroupingTest.kt](file:///d:/News/app/src/test/java/uk/sume/streamfolio/data/network/ArticleGroupingTest.kt) |

### P1 — Android-coupled logic — ✅ Implemented

| Target | Behavior Covered | Test Type | Test File |
| :--- | :--- | :--- | :--- |
| [`Migrations.kt`](file:///d:/News/app/src/main/java/uk/sume/streamfolio/data/local/Migrations.kt) | Schema/data integrity across `MIGRATION_5_6` → `MIGRATION_8_9`, individually and as a full 5→9 chain | Robolectric, `MigrationTestHelper` (see §1c) | [MigrationsTest.kt](file:///d:/News/app/src/test/java/uk/sume/streamfolio/data/local/MigrationsTest.kt) |
| [`PreferencesHelper.kt`](file:///d:/News/app/src/main/java/uk/sume/streamfolio/data/local/PreferencesHelper.kt) | Every stored preference key's default value and round-trip, `recentSearches` 10-entry cap, `categoryOrder` whitespace/empty-segment handling | Robolectric | [PreferencesHelperTest.kt](file:///d:/News/app/src/test/java/uk/sume/streamfolio/data/local/PreferencesHelperTest.kt) |
| [`TtsPlaybackManager.kt`](file:///d:/News/app/src/main/java/uk/sume/streamfolio/playback/TtsPlaybackManager.kt) | Playlist add/next/remove/move index bookkeeping; restoration of playlist + active index from a persisted `TtsPlaylistState` across a fresh instance (process-death simulation) | Robolectric (see §1c for `@SQLiteMode(NATIVE)` requirement) | [TtsPlaybackManagerTest.kt](file:///d:/News/app/src/test/java/uk/sume/streamfolio/playback/TtsPlaybackManagerTest.kt) — **scope note:** `playTtsPlaylist()`/`resumePlayback()`/`setSleepTimer()` are not covered (they start the real `TtsPlaybackService` and hit `NewsRepository`'s network client — would need a `NewsRepository` fake/interface seam, which does not exist and was not introduced this pass) |
| [`NewsViewModel.kt`](file:///d:/News/app/src/main/java/uk/sume/streamfolio/ui/viewmodel/NewsViewModel.kt) | `selectCategory()`'s immediate `StateFlow` update; `categoryArticlesState` reactively reflecting Room data for a hashtag category; `refreshCurrentFeed()` leaving `isRefreshing` false (silent and non-silent); `loadSimilarArticles()`'s indexed-`groupId` lookup and live-Jaccard-scan fallback | Robolectric, after adding an `IAiSummaryHelper`/`IAiTranslateHelper` constructor-injection seam (`@JvmOverloads`, see §1d) | [NewsViewModelTest.kt](file:///d:/News/app/src/test/java/uk/sume/streamfolio/ui/viewmodel/NewsViewModelTest.kt) — **scope note:** category-selection/refresh coverage deliberately uses a hashtag category to stay network-free, since a `NewsRepository` fake was not introduced this pass (same limitation as `TtsPlaybackManager` above); Turbine was not needed, `.value` polling with `ShadowLooper.idleMainLooper()` sufficed (§1d) |

### P2 — Compose UI tests (per-screen/component, real device via `connectedDebugAndroidTest`) — ✅ Implemented

`Modifier.testTag(...)` was added to key elements as each test needed it (no `testTag` usage existed anywhere in the codebase before this pass) — see §1e for the full list of production files touched. Coverage below is one-or-a-few representative interactions per screen (the behaviors that most directly exercise real state/persistence), not an exhaustive enumeration of every control — see §1e for gotchas and known gaps.

| Target | Behavior Covered | Test File |
| :--- | :--- | :--- |
| [`HomeScreen.kt`](file:///d:/News/app/src/main/java/uk/sume/streamfolio/ui/screens/HomeScreen.kt) | Article list rendering (both `ArticleListItem` and `TrendingCard` paths), tap-through to `DetailScreen`, category tab switching, pull-to-refresh | [HomeScreenTest.kt](file:///d:/News/app/src/androidTest/java/uk/sume/streamfolio/ui/screens/HomeScreenTest.kt) |
| [`DetailScreen.kt`](file:///d:/News/app/src/main/java/uk/sume/streamfolio/ui/screens/DetailScreen.kt) | Reader/web-mode toggle, back navigation, "also covered by" related-articles panel | [DetailScreenTest.kt](file:///d:/News/app/src/androidTest/java/uk/sume/streamfolio/ui/screens/DetailScreenTest.kt) |
| [`SearchScreen.kt`](file:///d:/News/app/src/main/java/uk/sume/streamfolio/ui/screens/SearchScreen.kt) | Local search-result rendering, trending-topic tap, recent-search persistence | [SearchScreenTest.kt](file:///d:/News/app/src/androidTest/java/uk/sume/streamfolio/ui/screens/SearchScreenTest.kt) |
| [`BookmarkScreen.kt`](file:///d:/News/app/src/main/java/uk/sume/streamfolio/ui/screens/BookmarkScreen.kt) | Collections grid/list toggle, create-collection flow, tap-through into a collection's articles | [BookmarkScreenTest.kt](file:///d:/News/app/src/androidTest/java/uk/sume/streamfolio/ui/screens/BookmarkScreenTest.kt) |
| `ui/screens/settings/*` (hub + 7 sub-screens) | Hub menu-section rendering; `SettingsPreferencesScreen`'s "Group Similar Stories" toggle round-trip; `SettingsGesturesScreen` header; `SettingsAiScreen` AI master-switch toggle round-trip; `SettingsFeedsScreen` navigation + custom-feed add flow; `SettingsProvidersScreen`/`SettingsReorderScreen` header + arrow-based reorder (`categoryOrder` preference update); `SettingsBackupScreen` header | [SettingsScreensTest.kt](file:///d:/News/app/src/androidTest/java/uk/sume/streamfolio/ui/screens/settings/SettingsScreensTest.kt) |
| `ui/components/TtsMiniPlayer.kt`, `SaveToListBottomSheet` (bookmark-via-Save flow) | Save-to-collection flow from `DetailScreen` bookmarks the article in Room; queuing an article for playback shows the mini-player on `HomeScreen` | [SharedComponentsTest.kt](file:///d:/News/app/src/androidTest/java/uk/sume/streamfolio/ui/components/SharedComponentsTest.kt) |
| `ui/components/SwipeableCard.kt` | **Not tested — confirmed dead code** (defined, never referenced anywhere in the app); see §1e | — |
| `ui/components/GlassmorphicNavBar.kt` | **No dedicated test** — every screen test above already exercises its tab-selection routing by tapping its labels; see §1e | — |

**Not covered by this pass** (narrower than the original backlog scoping above, per findings in §1e): `HomeScreen`'s swipe-to-bookmark/share/mark-read gesture itself (Material3 `SwipeToDismissBox` proved unreliable to simulate — the identical downstream logic is instead covered via `DetailScreen`'s "Save" button in `SharedComponentsTest`), `HomeScreen`'s offline banner and empty state, `DetailScreen`'s AI summary sheet states and translation badge (would need a fake `AiSummaryHelper`/`AiTranslateHelper` injected at the UI-test level — the §1d seam is constructor-level and exercised by `NewsViewModelTest`, not wired into an instrumented-test fake yet), `BookmarkCollectionDetailScreen` rename/delete and reassignment-on-delete, `SettingsFeedsScreen`'s OPML import/export, `SettingsProvidersScreen`'s per-publisher enable/disable, `SettingsBackupScreen`'s actual export/restore action, `SettingsGesturesScreen`'s swipe-action customization controls, and `TtsLyricsVisualizer`/`BookmarkToastBanner` undo. These remain open backlog items, same status as before this pass.

### P3 — Regression prevention & end-to-end

| Target | Test Type | Rationale |
| :--- | :--- | :--- |
| `ui/theme/Theme.kt` / `Color.kt` — light/dark/dynamic-color container & outline slots | Paparazzi screenshot test | Directly targets the purple-palette and washed-out-card bug class from §1 |
| ~5 critical journeys: onboarding→home; pull-to-refresh→open article (shared-element transition doesn't crash); bookmark→appears in Saved; search→open result; disable AI→Detail hides AI affordances | Instrumented E2E | Broadest real-user-flow confidence without enumerating every screen combination |

---

## 6. Test Execution & CI (recommendation, not current state)

`docs/10-ci-cd-and-release.md` confirms **no `.github/workflows/` exists today** — builds and tests are run locally via Gradle. The commands below already work locally; the CI pipeline is a recommendation layered on top of the one already sketched in that doc.

### Running tests today (from project root)

```powershell
# Full unit test suite
./gradlew testDebugUnitTest

# Single suite
./gradlew testDebugUnitTest --tests "uk.sume.news.BackupHelperTest"

# Live feed validation (requires internet; hits real third-party servers — see caveat below)
./gradlew testDebugUnitTest --tests "uk.sume.news.DefaultFeedsTest"

# Instrumented tests (requires attached device/emulator)
./gradlew connectedDebugAndroidTest

# Lint
./gradlew lintDebug
```

**Caveat on `DefaultFeedsTest`:** it makes live HTTP calls to all curated feeds across 14 regions. It already tolerates *known* flaky publishers via a `knownFlakyFeeds` allowlist (`isKnownFlaky()` in [DefaultFeedsTest.kt](file:///d:/News/app/src/test/java/uk/sume/news/DefaultFeedsTest.kt)), but any *newly* flaky or temporarily-down feed not yet on that list still fails the run — so it remains inherently less deterministic than a fixture-based test if bundled into a gating CI job. Recommendation: once MockWebServer-based parsing tests exist (§4/§5) to cover parsing correctness deterministically, keep `DefaultFeedsTest` itself as a separately-invoked, non-gating "live reachability smoke" task (e.g. a distinct Gradle test task or a scheduled job) rather than part of the default `testDebugUnitTest` run that PRs depend on.

### Recommended CI shape (extends the pipeline in `docs/10-ci-cd-and-release.md`)

- **Fast job, gates every PR (`ubuntu-latest`, no emulator):** `lintDebug` + `testDebugUnitTest` (including new Robolectric-backed tests once added).
- **Slower job, nightly/manual (emulator required, e.g. `reactivecircus/android-emulator-runner`):** `connectedDebugAndroidTest` for the Compose UI and E2E tests in §5, given per-push emulator cost/time.

---

## 7. Test Fixtures & Fakes

- **Preference/Context fakes:** [FakeAndroidPrefs.kt](file:///d:/News/app/src/test/java/uk/sume/streamfolio/testutil/FakeAndroidPrefs.kt) (`FakeSharedPreferences`/`FakeEditor`/`FakeContext`) is the shared, reusable version of the pattern originally declared privately inside [BackupHelperTest.kt](file:///d:/News/app/src/test/java/uk/sume/news/BackupHelperTest.kt) — new `uk.sume.streamfolio`-package tests needing a `PreferencesHelper` should use this rather than re-declaring the boilerplate. Note it fixes a bug the original private copy had (see §1a item 2): `apply()`/`commit()` write through immediately instead of requiring a manual `.save()` call.
- **XML/OPML fixtures:** P0 used inline triple-quoted XML strings in the test files themselves (`RssParserTest.kt`, `OpmlHelperTest.kt`) rather than external files under `app/src/test/resources/sample_xml/` — simpler to review alongside the assertions for the current handful of cases. Move to external fixture files if/when the number of XML variants grows large enough that inlining hurts readability.
- **Network fixtures:** `SsrfProtectionInterceptorTest` uses a hand-rolled fake `Interceptor.Chain` instead of MockWebServer (§4) — no MockWebServer dependency was needed for P0. `MockWebServer` remains recommended for future `NewsRepository`-level network-flow tests.
- **`android.util.Log`/`android.util.Xml` stubs:** MockK's `mockkStatic(...)` is used to make these real Android calls behave in a plain JVM test — `Log.w()` is stubbed to a no-op return value (`BackupHelperMalformedInputTest`), `Xml.newPullParser()` is stubbed to delegate to a real `XmlPullParserFactory`-created parser (`OpmlHelperTest`, reusing the `xmlpull`/`xpp3` jars already present for `RssParser`).
- **AI/TTS fakes:** ML Kit GenAI/AICore and platform TTS voices are not reliably available in CI or most emulators. `NewsViewModel` tests that exercise AI-summary or TTS state transitions need a fake/interface substitute for `AiSummaryHelper`/`AiTranslateHelper`/`TtsHelper` — introducing that seam is a small prerequisite refactor tracked in §5's P1 row, not performed by this document.
- **On-device UI fixtures/state reset:** [AndroidTestSupport.kt](file:///d:/News/app/src/androidTest/java/uk/sume/streamfolio/testutil/AndroidTestSupport.kt) is the P2 equivalent of `FakeAndroidPrefs` — `resetAppState()` for full prefs/DB isolation between test methods sharing one real app process, plus `seedArticle`/`seedCustomFeed`/`seedCustomFeedArticle` fixture builders. See §1e for why each exists. [BaseAndroidUiTest.kt](file:///d:/News/app/src/androidTest/java/uk/sume/streamfolio/testutil/BaseAndroidUiTest.kt) is the shared `@Before`/launch boilerplate every P2 test class extends.

---

## 8. Open Items Requiring Confirmation

- Whether to actually introduce the `AiSummaryHelper`/`AiTranslateHelper`/`TtsHelper` interface seams needed for true `NewsViewModel` unit tests is a product/architecture decision beyond this doc's scope — `Unknown / Requires Confirmation`.
- Whether CI should run on GitHub Actions specifically (as sketched in `docs/10-ci-cd-and-release.md`) or another provider is `Unknown / Requires Confirmation` — no CI vendor decision exists in the repo today.
- Robolectric `4.14.1` pinned to `@Config(sdk = [34])` is now verified working (4 test classes, 26 tests, stable across repeated runs) — its support for `compileSdk = 36` (Android 16 preview) itself remains unverified and untested, since every test class pins to 34 rather than 36.
- P2 was run against one specific physical device (Oppo/OnePlus-family, ColorOS, `CPH2651`) — behavior on other OEM skins, screen sizes, or an emulator is `Unknown / Requires Confirmation`. The transient "No compose hierarchies found" failures traced to this device's aggressive background-process killing (§1e) may not reproduce, or may manifest differently, elsewhere.
- Whether to close the "Not covered by this pass" gaps listed under the P2 backlog table (§5) — AI summary sheet states, OPML import/export, per-publisher enable/disable, backup export/restore, gesture customization, swipe-gesture simulation — in a future pass is a scoping decision for the user, not assumed here.
