package uk.sume.streamfolio.ui.viewmodel

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode
import uk.sume.streamfolio.data.local.AppDatabase
import uk.sume.streamfolio.data.local.PreferencesHelper
import uk.sume.streamfolio.data.model.Article
import uk.sume.streamfolio.data.network.IAiSummaryHelper
import uk.sume.streamfolio.data.network.IAiTranslateHelper
import uk.sume.streamfolio.playback.TtsPlaybackManager

private class FakeAiSummaryHelper : IAiSummaryHelper {
    override suspend fun checkFeatureStatus(): Int = 1 // any non-zero: checkGeminiSupport() only tests != 0
    override suspend fun downloadFeature() {}
    override suspend fun summarizeText(text: String): String = "fake summary"
}

private class FakeAiTranslateHelper : IAiTranslateHelper {
    override suspend fun identifyLanguage(text: String): String = "en"
    override suspend fun translateText(text: String, sourceLangCode: String, targetLangCode: String): String = text
}

/**
 * P1 coverage for the StateFlow behaviors named in docs/09-testing-and-quality.md's backlog:
 * category selection, pull-to-refresh, and loadSimilarArticles' groupId-vs-fallback lookup.
 *
 * Requires the IAiSummaryHelper/IAiTranslateHelper constructor-injection seam added to
 * NewsViewModel for this pass: without it, checkGeminiSupport() in init{} unconditionally
 * constructs the real ML Kit GenAI helper and calls checkFeatureStatus() on it, which risks
 * hanging under Robolectric (that SDK isn't AOSP framework, so Robolectric doesn't shadow it).
 *
 * Every test below selects a hashtag-prefixed "category" (e.g. "#tag") rather than a real one:
 * refreshCurrentFeed()'s `currentCat.startsWith("#")` branch returns immediately without any
 * network fetch, and categoryArticlesState's flatMapLatest sources hashtag categories from a
 * local Room query (getAllArticlesUnlimited() filtered by tag) rather than RSS feeds - this lets
 * category-selection/refresh StateFlow behavior be tested without a NewsRepository fake, which
 * was out of scope for this pass (only the AI/TTS seam was introduced).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class NewsViewModelTest {

    // Both TtsPlaybackManager.instance and AppDatabase.INSTANCE are plain JVM statics (see
    // TtsPlaybackManagerTest for why @Volatile companion properties end up on the outer class,
    // not Companion). Gradle reuses one forked JVM across multiple test classes by default, so
    // without resetting these, a NewsViewModel constructed here could silently reuse a
    // TtsPlaybackManager/AppDatabase connection left behind by a *different* test class in the
    // same JVM fork, pointed at that other test's now-torn-down Robolectric sandbox. Observed
    // concretely: inserted articles' `tags` column read back null after such reuse.
    private fun resetJvmStaticSingleton(owner: Class<*>, fieldName: String) {
        val field = owner.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(null, null)
    }

    private fun resetSingletons() {
        resetJvmStaticSingleton(TtsPlaybackManager::class.java, "instance")
        resetJvmStaticSingleton(AppDatabase::class.java, "INSTANCE")
    }

    @Before
    fun setUp() {
        resetSingletons()
    }

    @After
    fun tearDown() {
        resetSingletons()
    }

    private fun article(link: String, title: String, tags: String? = null, groupId: String? = null) = Article(
        link = link,
        title = title,
        description = "",
        pubDate = "",
        sourceName = "Source",
        sourceUrl = "https://example.com/feed.xml",
        category = "#tag",
        tags = tags,
        groupId = groupId
    )

    private fun newViewModel(): NewsViewModel {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        // NewsViewModel's init{} unconditionally runs a one-time legacy-tag cleanup
        // (repository.clearAllTags(), wiping every article's `tags` column) whenever
        // prefs.hasClearedOldTags is false - which it always is for a fresh PreferencesHelper.
        // Pre-seeding this flag skips that irrelevant migration step instead of letting it wipe
        // tags fixtures a test just inserted.
        PreferencesHelper(context).hasClearedOldTags = true
        return NewsViewModel(context, FakeAiSummaryHelper(), FakeAiTranslateHelper())
    }

    private fun <T> pollUntil(timeoutMillis: Long = 3_000, condition: () -> T?): T? {
        val deadline = System.currentTimeMillis() + timeoutMillis
        var last: T? = null
        while (System.currentTimeMillis() < deadline) {
            // Robolectric's main Looper defaults to PAUSED: anything dispatched via
            // Dispatchers.Main/Main.immediate (viewModelScope, and stateIn's "start on first
            // subscriber" coroutine for SharingStarted.Lazily) is queued but never actually runs
            // until the looper is explicitly idled - without this, categoryArticlesState's
            // upstream flow never starts at all, regardless of how long Thread.sleep() waits.
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            last = condition()
            if (last != null) return last
            Thread.sleep(20)
        }
        return last
    }

    @Test
    fun `selectCategory updates selectedCategory immediately`() {
        val viewModel = newViewModel()

        viewModel.selectCategory("#technology")

        assertEquals("#technology", viewModel.selectedCategory.value)
    }

    @Test
    fun `categoryArticlesState reactively reflects locally tagged articles for a hashtag category`() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val db = AppDatabase.getDatabase(context)
        runBlocking {
            // Tags are stored with a leading "#" (see NewsRepository.extractTagsFromContent's
            // keywordToTagMap, e.g. "#AI", "#Sports") - categoryArticlesState matches a hashtag
            // "category" against this raw tags string, so the fixture must match that shape.
            db.articleDao().insertArticle(article("https://example.com/match", "Matching Article", tags = "#technology"))
            db.articleDao().insertArticle(article("https://example.com/nomatch", "Unrelated Article", tags = "#sports"))
        }

        val viewModel = newViewModel()
        viewModel.selectCategory("#technology")

        // categoryArticlesState is stateIn(..., SharingStarted.Lazily, ...): the upstream
        // Room-query/debounce/grouping chain only starts once something actually collects it -
        // reading .value alone never triggers that, it would just stay at the initial empty
        // CategoryArticlesState() forever.
        val collectorJob = CoroutineScope(Dispatchers.Main.immediate).launch {
            viewModel.categoryArticlesState.collect {}
        }
        val groups = try {
            pollUntil { viewModel.categoryArticlesState.value.groups.takeIf { it.isNotEmpty() } }
        } finally {
            collectorJob.cancel()
        }

        assertTrue("expected the tag-matching article to appear", groups.orEmpty().any { it.primary.link == "https://example.com/match" })
        assertTrue(
            "expected the non-matching article to be filtered out",
            groups.orEmpty().none { it.primary.link == "https://example.com/nomatch" }
        )
    }

    @Test
    fun `refreshCurrentFeed with silent false ends with isRefreshing false, not stuck true`() {
        // The hashtag "#" branch returns immediately with no network fetch, so this can't
        // directly observe the fleeting true state without Turbine (not yet added - see
        // docs/09-testing-and-quality.md section 4); it does guard the real regression class
        // "isRefreshing left stuck true because an early-return branch forgot to reset it".
        val viewModel = newViewModel()
        viewModel.selectCategory("#technology")
        assertEquals(false, viewModel.isRefreshing.value)

        viewModel.refreshCurrentFeed(silent = false)
        pollUntil { Unit.takeIf { !viewModel.isRefreshing.value } }

        assertEquals(false, viewModel.isRefreshing.value)
    }

    @Test
    fun `refreshCurrentFeed with silent true never flips isRefreshing`() {
        val viewModel = newViewModel()
        viewModel.selectCategory("#technology")

        viewModel.refreshCurrentFeed(silent = true)

        assertEquals(false, viewModel.isRefreshing.value)
    }

    @Test
    fun `loadSimilarArticles uses the indexed groupId lookup when groupId is present`() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val db = AppDatabase.getDatabase(context)
        val leader = article("https://example.com/leader", "Leader Story", groupId = "https://example.com/leader")
        val follower = article("https://example.com/follower", "Follower Story", groupId = "https://example.com/leader")
        val unrelated = article("https://example.com/unrelated", "Totally Different", groupId = "https://example.com/unrelated")
        runBlocking {
            db.articleDao().insertArticle(leader)
            db.articleDao().insertArticle(follower)
            db.articleDao().insertArticle(unrelated)
        }

        val viewModel = newViewModel()
        viewModel.selectArticleForDetail(leader)

        runBlocking { viewModel.loadSimilarArticles(leader.link) }

        val similarLinks = viewModel.similarArticlesForDetail.value.map { it.link }
        assertEquals(listOf(follower.link), similarLinks)
    }

    @Test
    fun `loadSimilarArticles falls back to a live title scan when groupId is null`() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val db = AppDatabase.getDatabase(context)
        val main = article("https://example.com/a", "Prime Minister Announces New Policy On Housing")
        val similar = article("https://example.com/b", "PM announces new policy on housing")
        val unrelated = article("https://example.com/c", "Completely Unrelated Story About Sports")
        runBlocking {
            db.articleDao().insertArticle(main)
            db.articleDao().insertArticle(similar)
            db.articleDao().insertArticle(unrelated)
        }

        val viewModel = newViewModel()
        viewModel.selectArticleForDetail(main)

        runBlocking { viewModel.loadSimilarArticles(main.link) }

        val similarLinks = viewModel.similarArticlesForDetail.value.map { it.link }
        assertEquals(listOf(similar.link), similarLinks)
    }
}
