package uk.sume.streamfolio.testutil

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import uk.sume.streamfolio.data.local.AppDatabase
import uk.sume.streamfolio.data.local.PreferencesHelper
import uk.sume.streamfolio.data.model.Article
import uk.sume.streamfolio.data.model.BookmarkList
import uk.sume.streamfolio.data.model.CustomFeed

/**
 * Shared helpers for on-device Compose UI tests (P2). Instrumented tests all run inside the
 * real app process, so AppDatabase.getDatabase()/TtsPlaybackManager singletons persist across
 * test methods within one `connectedDebugAndroidTest` invocation - resetAppState() is what keeps
 * each test starting from a clean, deterministic baseline instead of relying on that.
 *
 * MainActivity/AppNavigation reads `viewModel.prefs.isCompletedOnboarding` once, synchronously,
 * to pick NavHost's startDestination - it must already be set correctly *before* MainActivity is
 * launched. `createAndroidComposeRule<MainActivity>()` launches the activity as part of the
 * rule's own `apply()`, before any `@Before` method runs, which is too late. Tests here use
 * `createEmptyComposeRule()` plus a manual `ActivityScenario.launch(MainActivity::class.java)`
 * inside the test body, after `resetAppState()` has already run.
 */
object AndroidTestSupport {

    fun targetContext() = InstrumentationRegistry.getInstrumentation().targetContext

    fun prefs() = PreferencesHelper(targetContext())

    fun db() = AppDatabase.getDatabase(targetContext())

    /** Baseline for most screen tests: onboarding done, no first-run banners, DB wiped. */
    fun resetAppState() {
        runBlocking {
            db().clearAllTables()
            // AppDatabase's onOpen callback normally seeds this idempotently, but that only
            // fires when the connection is first opened - clearAllTables() doesn't reopen it,
            // so the app's real "there's always a Saved list" invariant needs restoring by hand.
            db().bookmarkListDao().insertList(BookmarkList(name = "Saved", isDefault = true))
        }
        // Every instrumented test method runs inside the same real app process/SharedPreferences
        // file - clearing Room tables alone isn't a full reset. Observed concretely: a topic
        // clicked in one test method (adding it to prefs.recentSearches, e.g. "Markets") was
        // still there as a leftover "recent search" row in a *different* test method that never
        // touched search itself, producing a spurious duplicate-node match. Wiping the whole
        // prefs file first, then setting only what's needed below, avoids this class of leak for
        // any preference a test doesn't already know to reset by hand.
        targetContext().getSharedPreferences("news_prefs", android.content.Context.MODE_PRIVATE)
            .edit().clear().commit()
        prefs().apply {
            isCompletedOnboarding = true
            hasClearedOldTags = true
            hasSeenAiSpotlight = true
            hasSeenSwipeHint = true
            isAiEnabled = false
            // NewsViewModel's init{} always fires refreshCurrentFeed() + syncAllCategoriesInBackground()
            // once onboarding is complete, which - with default feeds on - means real network
            // fetches for all 8 default categories on every single test launch. That background
            // activity was a source of test timing flakiness with no test-value (P2's fixtures are
            // seeded directly into Room). Disabling it here leaves only fetchCustomFeeds() for a
            // test's own fixture feed, which fails fast against its fake URL. Category tabs a test
            // needs still show up via AndroidTestSupport.seedCustomFeedArticle()'s CustomFeed rows,
            // since HomeScreen's category list always includes customFeeds.map { it.category }
            // regardless of this flag.
            isDefaultFeedsEnabled = false
        }
    }

    fun seedArticle(article: Article) {
        runBlocking { db().articleDao().insertArticle(article) }
    }

    fun seedArticles(vararg articles: Article) {
        runBlocking { articles.forEach { db().articleDao().insertArticle(it) } }
    }

    fun seedBookmarkList(list: BookmarkList) {
        runBlocking { db().bookmarkListDao().insertList(list) }
    }

    /**
     * HomeScreen's category feed filters articles to `article.customFeedId != null ||
     * enabledUrls.contains(article.sourceUrl)`, where enabledUrls comes from
     * DefaultFeedsConfig's real curated-feed list for the device's region - a fixture
     * `sourceUrl` like "https://example.com/feed.xml" never matches that and gets silently
     * filtered out of any *real* (non-hashtag) category. Registering the article under a real
     * CustomFeed row instead (customFeedId set) is the supported way to inject deterministic
     * fixture content into a real category's feed, mirroring how a user's own added RSS feed
     * behaves in production.
     */
    /**
     * Registers a CustomFeed row with no article. With isDefaultFeedsEnabled = false (the P2
     * baseline, see resetAppState()), category-list screens (HomeScreen's tabs,
     * SettingsReorderScreen's reorder list) derive their categories from
     * `customFeeds.map { it.category }` - this is enough to make a category exist/appear on
     * those screens without needing seedCustomFeedArticle()'s full article fixture too.
     */
    fun seedCustomFeed(category: String, feedUrl: String = "https://example.com/custom-feed/${category.hashCode()}.xml") {
        runBlocking { db().customFeedDao().insertFeed(CustomFeed(title = "Test Feed", url = feedUrl, category = category)) }
    }

    fun seedCustomFeedArticle(
        link: String,
        title: String,
        category: String = "Top Stories",
        groupId: String? = null,
        feedUrl: String = "https://example.com/custom-feed/${link.hashCode()}.xml"
    ): Article {
        return runBlocking {
            val feedDao = db().customFeedDao()
            feedDao.insertFeed(CustomFeed(title = "Test Feed", url = feedUrl, category = category))
            val feedId = feedDao.getAllFeedsSync().first { it.url == feedUrl }.id
            val fixture = article(link = link, title = title, category = category, groupId = groupId).copy(customFeedId = feedId)
            db().articleDao().insertArticle(fixture)
            fixture
        }
    }

    /** A minimal, valid Article fixture - override only the fields a given test cares about. */
    fun article(
        link: String,
        title: String,
        category: String = "Top Stories",
        sourceName: String = "Test Source",
        tags: String? = null,
        groupId: String? = null,
        isBookmarked: Boolean = false,
        isRead: Boolean = false
    ) = Article(
        link = link,
        title = title,
        description = "Test description for $title",
        pubDate = "2026-01-01T00:00:00.000Z",
        sourceName = sourceName,
        sourceUrl = "https://example.com/feed.xml",
        category = category,
        tags = tags,
        groupId = groupId,
        isBookmarked = isBookmarked,
        isRead = isRead
    )
}
