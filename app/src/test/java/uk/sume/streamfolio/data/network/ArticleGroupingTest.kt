package uk.sume.streamfolio.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.sume.streamfolio.data.model.Article

class ArticleGroupingTest {

    private fun article(link: String, title: String, groupId: String? = null) = Article(
        link = link,
        title = title,
        description = "",
        pubDate = "",
        sourceName = "Source",
        sourceUrl = "",
        category = "General",
        groupId = groupId
    )

    @Test
    fun `near-duplicate titles from different sources are placed in the same group`() {
        val articles = listOf(
            article("https://a.example.com/1", "Prime Minister Announces New Policy On Housing"),
            article("https://b.example.com/1", "PM announces new policy on housing", ),
            article("https://c.example.com/1", "Completely Unrelated Story About Sports")
        )

        val grouped = ArticleGrouping.assignGroupIds(existingArticles = emptyList(), newArticles = articles)

        assertEquals(grouped[0].groupId, grouped[1].groupId)
        assertNotEquals(grouped[0].groupId, grouped[2].groupId)
    }

    @Test
    fun `every article receives a non-null groupId`() {
        val articles = listOf(
            article("https://a.example.com/1", "Story One"),
            article("https://a.example.com/2", "Story Two")
        )

        val grouped = ArticleGrouping.assignGroupIds(emptyList(), articles)

        grouped.forEach { assertTrue(it.groupId != null) }
    }

    @Test
    fun `an article with no matching group becomes its own group leader keyed by its link`() {
        val solo = article("https://a.example.com/solo", "A Totally Unique Headline About Nothing Else")
        val grouped = ArticleGrouping.assignGroupIds(emptyList(), listOf(solo))

        assertEquals(solo.link, grouped[0].groupId)
    }

    @Test
    fun `new article joins an existing group from a prior ingest rather than starting a new one`() {
        val existingLeader = article(
            "https://a.example.com/original",
            "Prime Minister Announces New Policy On Housing",
            groupId = "https://a.example.com/original"
        )
        val incoming = article("https://b.example.com/followup", "PM announces new policy on housing")

        val grouped = ArticleGrouping.assignGroupIds(existingArticles = listOf(existingLeader), newArticles = listOf(incoming))

        assertEquals("https://a.example.com/original", grouped[0].groupId)
    }

    @Test
    fun `article that already carries a groupId keeps it unchanged`() {
        val incoming = article("https://a.example.com/1", "Some Headline", groupId = "manually-set-group")
        val grouped = ArticleGrouping.assignGroupIds(emptyList(), listOf(incoming))

        assertEquals("manually-set-group", grouped[0].groupId)
    }

    @Test
    fun `empty input list returns empty output without error`() {
        assertTrue(ArticleGrouping.assignGroupIds(emptyList(), emptyList()).isEmpty())
    }

    @Test
    fun `cleanTitle strips punctuation, stopwords and known source suffixes`() {
        val cleaned = ArticleGrouping.cleanTitle("Breaking: The Economy Is Growing Fast! - BBC News")
        assertTrue(cleaned.contains("economy"))
        assertTrue(cleaned.contains("growing"))
        assertTrue("stopword 'the' must be removed", !cleaned.split(" ").contains("the"))
        assertTrue("trailing source suffix 'bbc news' must be stripped", !cleaned.contains("bbc"))
    }

    @Test
    fun `jaccardSimilarity of identical sets is 1 and disjoint sets is 0`() {
        val a = setOf("housing", "policy", "minister")
        assertEquals(1.0f, ArticleGrouping.jaccardSimilarity(a, a))
        assertEquals(0.0f, ArticleGrouping.jaccardSimilarity(a, setOf("sports", "football")))
    }

    @Test
    fun `jaccardSimilarity of two empty sets is defined as 1`() {
        assertEquals(1.0f, ArticleGrouping.jaccardSimilarity(emptySet(), emptySet()))
    }
}
