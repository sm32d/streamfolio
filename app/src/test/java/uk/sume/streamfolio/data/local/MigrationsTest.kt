package uk.sume.streamfolio.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifies each Room migration (5->6->7->8->9) preserves existing data and produces the schema
 * Room expects, using real SQLite via Robolectric rather than mocks. compileSdk is 36 (Android
 * 16 preview); pinned to sdk = 34 since Robolectric's support for 36 has not been verified.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationsTest {

    private val testDbName = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    private fun insertV5Article(db: SupportSQLiteDatabase, link: String, isBookmarked: Boolean) {
        db.execSQL(
            """
            INSERT INTO articles
                (link, title, description, pubDate, sourceName, sourceUrl, category, isBookmarked, isRead)
            VALUES (?, 'Title', 'Description', '2024-01-01', 'Source', 'https://example.com/feed.xml', 'General', ?, 0)
            """.trimIndent(),
            arrayOf(link, if (isBookmarked) 1 else 0)
        )
    }

    @Test
    fun `migrate5To6 creates tts_playlist_state table without touching existing articles`() {
        var db = helper.createDatabase(testDbName, 5)
        insertV5Article(db, "https://example.com/a", isBookmarked = false)
        db.close()

        db = helper.runMigrationsAndValidate(testDbName, 6, true, MIGRATION_5_6)

        db.query("SELECT COUNT(*) FROM tts_playlist_state").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
        db.query("SELECT link FROM articles").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("https://example.com/a", cursor.getString(0))
        }
    }

    @Test
    fun `migrate6To7 adds nullable groupId column defaulting to null`() {
        var db = helper.createDatabase(testDbName, 6)
        insertV5Article(db, "https://example.com/a", isBookmarked = false)
        db.close()

        db = helper.runMigrationsAndValidate(testDbName, 7, true, MIGRATION_6_7)

        db.query("SELECT groupId FROM articles WHERE link = 'https://example.com/a'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertTrue(cursor.isNull(0))
        }
    }

    @Test
    fun `migrate7To8 seeds default Saved list and migrates existing bookmarks into it`() {
        var db = helper.createDatabase(testDbName, 6)
        insertV5Article(db, "https://example.com/bookmarked", isBookmarked = true)
        insertV5Article(db, "https://example.com/unbookmarked", isBookmarked = false)
        db.close()

        db = helper.runMigrationsAndValidate(testDbName, 7, true, MIGRATION_6_7)
        db.close()

        db = helper.runMigrationsAndValidate(testDbName, 8, true, MIGRATION_7_8)

        db.query("SELECT id, name, isDefault FROM bookmark_lists").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1L, cursor.getLong(0))
            assertEquals("Saved", cursor.getString(1))
            assertEquals(1, cursor.getInt(2))
            assertEquals("expected exactly one seeded list", false, cursor.moveToNext())
        }

        db.query("SELECT articleLink, listId FROM article_bookmark_lists").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("https://example.com/bookmarked", cursor.getString(0))
            assertEquals(1L, cursor.getLong(1))
            assertEquals(
                "only the previously-bookmarked article should be linked to the Saved list",
                false,
                cursor.moveToNext()
            )
        }
    }

    @Test
    fun `migrate8To9 creates a queryable index on articles groupId`() {
        var db = helper.createDatabase(testDbName, 7)
        insertV5Article(db, "https://example.com/a", isBookmarked = false)
        db.close()

        db = helper.runMigrationsAndValidate(testDbName, 8, true, MIGRATION_7_8)
        db.close()

        db = helper.runMigrationsAndValidate(testDbName, 9, true, MIGRATION_8_9)

        db.query("SELECT name FROM sqlite_master WHERE type = 'index' AND name = 'index_articles_groupId'").use { cursor ->
            assertTrue("expected index_articles_groupId to exist after MIGRATION_8_9", cursor.moveToFirst())
        }
    }

    @Test
    fun `full chain migration 5 to 9 preserves article data end to end`() {
        var db = helper.createDatabase(testDbName, 5)
        insertV5Article(db, "https://example.com/bookmarked", isBookmarked = true)
        db.close()

        db = helper.runMigrationsAndValidate(testDbName, 9, true, *ALL_MIGRATIONS)

        db.query("SELECT link, title, isBookmarked FROM articles").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("https://example.com/bookmarked", cursor.getString(0))
            assertEquals("Title", cursor.getString(1))
            assertEquals(1, cursor.getInt(2))
        }
        db.query("SELECT COUNT(*) FROM article_bookmark_lists WHERE articleLink = 'https://example.com/bookmarked'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
    }
}
