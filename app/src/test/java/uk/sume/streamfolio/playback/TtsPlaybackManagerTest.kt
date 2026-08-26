package uk.sume.streamfolio.playback

import androidx.test.core.app.ApplicationProvider
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
import org.robolectric.shadows.ShadowLooper
import uk.sume.streamfolio.data.local.AppDatabase
import uk.sume.streamfolio.data.model.Article
import uk.sume.streamfolio.data.model.TtsPlaylistState

/**
 * Covers the P1 backlog item "Playlist queue restoration/index tracking across process death".
 *
 * Scope deliberately excludes playTtsPlaylist()/resumePlayback()/setSleepTimer(): those call
 * ensureServiceRunning() (starts the real TtsPlaybackService) and fetch article bodies via
 * NewsRepository (real network client) - faking those needs the interface-seam refactor already
 * tracked for NewsViewModel in docs/09-testing-and-quality.md, not performed here. Every test
 * below only exercises playlist/index bookkeeping methods that mutate StateFlow state
 * synchronously and never reach that code path (verified by inspecting TtsPlaybackManager.kt).
 *
 * compileSdk is 36 (Android 16 preview); pinned to sdk = 34 since Robolectric's support for 36
 * has not been verified against this repo.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
// Robolectric's default legacy SQLite shadow ties each connection to the thread that opened it;
// Room's IO executor runs restorePlaylist()'s DB reads on a different (real) background thread,
// which the legacy shadow rejects ("Illegal connection pointer"). NATIVE mode uses a real
// multi-threading-capable SQLite implementation instead, matching actual device behavior.
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class TtsPlaybackManagerTest {

    private fun article(link: String) = Article(
        link = link,
        title = "Title for $link",
        description = "",
        pubDate = "",
        sourceName = "Source",
        sourceUrl = "",
        category = "General"
    )

    private fun resetSingleton() {
        // TtsPlaybackManager has no public reset hook for its static singleton; reflection keeps
        // each test isolated from whichever instance a prior test method left behind. The
        // @Volatile companion property's backing field is compiled as a private static field
        // directly on the outer class (not on Companion) - Kotlin does this specifically for
        // @Volatile/@JvmStatic companion properties, verified via `javap -p` on the compiled
        // TtsPlaybackManager.class.
        val instanceField = TtsPlaybackManager::class.java.getDeclaredField("instance")
        instanceField.isAccessible = true
        instanceField.set(null, null)
    }

    @Before
    fun setUp() {
        resetSingleton()
    }

    @After
    fun tearDown() {
        resetSingleton()
    }

    private fun newManager(): TtsPlaybackManager =
        TtsPlaybackManager.getInstance(ApplicationProvider.getApplicationContext())

    @Test
    fun `addToTtsPlaylist sets active index to 0 for the first article only`() {
        val manager = newManager()
        val a = article("https://example.com/a")
        val b = article("https://example.com/b")

        manager.addToTtsPlaylist(a)
        assertEquals(listOf(a), manager.ttsPlaylist.value)
        assertEquals(0, manager.currentTtsArticleIndex.value)

        manager.addToTtsPlaylist(b)
        assertEquals(listOf(a, b), manager.ttsPlaylist.value)
        assertEquals("adding a second article must not move the active index", 0, manager.currentTtsArticleIndex.value)
    }

    @Test
    fun `addToTtsPlaylist ignores a duplicate link already in the queue`() {
        val manager = newManager()
        val a = article("https://example.com/a")

        manager.addToTtsPlaylist(a)
        manager.addToTtsPlaylist(article("https://example.com/a"))

        assertEquals(1, manager.ttsPlaylist.value.size)
    }

    @Test
    fun `playNextInTtsPlaylist inserts immediately after the active article`() {
        val manager = newManager()
        val a = article("https://example.com/a")
        val b = article("https://example.com/b")
        val c = article("https://example.com/c")
        manager.addToTtsPlaylist(a)
        manager.addToTtsPlaylist(b)
        // active index is 0 (article a)

        manager.playNextInTtsPlaylist(c)

        assertEquals(listOf(a, c, b), manager.ttsPlaylist.value)
        assertEquals("inserting next must not change which article is active", 0, manager.currentTtsArticleIndex.value)
    }

    @Test
    fun `playNextInTtsPlaylist moving an already-queued later article shifts it next and adjusts index`() {
        val manager = newManager()
        val a = article("https://example.com/a")
        val b = article("https://example.com/b")
        val c = article("https://example.com/c")
        manager.addToTtsPlaylist(a)
        manager.addToTtsPlaylist(b)
        manager.addToTtsPlaylist(c)
        // playlist [a, b, c], active index 0

        manager.playNextInTtsPlaylist(c)

        assertEquals(listOf(a, c, b), manager.ttsPlaylist.value)
        assertEquals(0, manager.currentTtsArticleIndex.value)
    }

    @Test
    fun `playNextInTtsPlaylist on the currently active article is a no-op`() {
        val manager = newManager()
        val a = article("https://example.com/a")
        manager.addToTtsPlaylist(a)

        manager.playNextInTtsPlaylist(a)

        assertEquals(listOf(a), manager.ttsPlaylist.value)
        assertEquals(0, manager.currentTtsArticleIndex.value)
    }

    @Test
    fun `removeFromTtsPlaylist before the active index shifts the active index down`() {
        val manager = newManager()
        val x = article("https://example.com/x")
        val y = article("https://example.com/y")
        val z = article("https://example.com/z")
        manager.addToTtsPlaylist(x)
        manager.addToTtsPlaylist(y)
        manager.addToTtsPlaylist(z)
        // moveTtsPlaylistItem is pure reindexing (no network/service calls), so use it to make z
        // (originally index 2) the active article without going through playTtsPlaylist().
        manager.moveTtsPlaylistItem(fromIndex = 0, toIndex = 2) // active index 0 (x) moves to slot 2
        assertEquals(listOf(y, z, x), manager.ttsPlaylist.value)
        assertEquals(2, manager.currentTtsArticleIndex.value)

        manager.removeFromTtsPlaylist(y) // removing index 0, which is before active index 2

        assertEquals(listOf(z, x), manager.ttsPlaylist.value)
        assertEquals(1, manager.currentTtsArticleIndex.value)
    }

    @Test
    fun `removeFromTtsPlaylist emptying the queue stops playback and resets the active index`() {
        val manager = newManager()
        val a = article("https://example.com/a")
        manager.addToTtsPlaylist(a)

        manager.removeFromTtsPlaylist(a)

        assertTrue(manager.ttsPlaylist.value.isEmpty())
        assertEquals(-1, manager.currentTtsArticleIndex.value)
    }

    @Test
    fun `removeFromTtsPlaylist for a link not in the queue is a no-op`() {
        val manager = newManager()
        val a = article("https://example.com/a")
        manager.addToTtsPlaylist(a)

        manager.removeFromTtsPlaylist(article("https://example.com/not-queued"))

        assertEquals(listOf(a), manager.ttsPlaylist.value)
        assertEquals(0, manager.currentTtsArticleIndex.value)
    }

    @Test
    fun `moveTtsPlaylistItem keeps the active index pointed at the same article`() {
        val manager = newManager()
        val a = article("https://example.com/a")
        val b = article("https://example.com/b")
        val c = article("https://example.com/c")
        manager.addToTtsPlaylist(a)
        manager.addToTtsPlaylist(b)
        manager.addToTtsPlaylist(c)
        // active index 0 (a)

        manager.moveTtsPlaylistItem(fromIndex = 2, toIndex = 0)

        assertEquals(listOf(c, a, b), manager.ttsPlaylist.value)
        assertEquals("active article a moved from slot 0 to slot 1", 1, manager.currentTtsArticleIndex.value)
    }

    @Test
    fun `moveTtsPlaylistItem with an out-of-range index is a no-op`() {
        val manager = newManager()
        val a = article("https://example.com/a")
        manager.addToTtsPlaylist(a)

        manager.moveTtsPlaylistItem(fromIndex = 0, toIndex = 5)

        assertEquals(listOf(a), manager.ttsPlaylist.value)
    }

    @Test
    fun `clearTtsPlaylist empties the queue and resets the active index`() {
        val manager = newManager()
        manager.addToTtsPlaylist(article("https://example.com/a"))
        manager.addToTtsPlaylist(article("https://example.com/b"))

        manager.clearTtsPlaylist()

        assertTrue(manager.ttsPlaylist.value.isEmpty())
        assertEquals(-1, manager.currentTtsArticleIndex.value)
    }

    @Test
    fun `a fresh instance restores the playlist and active index persisted before process death`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = AppDatabase.getDatabase(context)
        val articleA = article("https://example.com/restored-a")
        val articleB = article("https://example.com/restored-b")
        runBlocking {
            db.articleDao().insertArticle(articleA)
            db.articleDao().insertArticle(articleB)
            db.ttsPlaylistStateDao().saveState(
                TtsPlaylistState(
                    id = 1,
                    currentIndex = 1,
                    linksJson = """["${articleA.link}","${articleB.link}"]"""
                )
            )
        }

        val manager = newManager()

        // restorePlaylist() runs on Dispatchers.IO from init{}, a real background thread not
        // driven by Robolectric's main-looper scheduler - poll with a bounded timeout instead of
        // assuming it has completed synchronously.
        val deadline = System.currentTimeMillis() + 5_000
        while (manager.ttsPlaylist.value.isEmpty() && System.currentTimeMillis() < deadline) {
            ShadowLooper.idleMainLooper()
            Thread.sleep(20)
        }

        assertEquals(listOf(articleA, articleB), manager.ttsPlaylist.value)
        assertEquals(1, manager.currentTtsArticleIndex.value)
    }
}
