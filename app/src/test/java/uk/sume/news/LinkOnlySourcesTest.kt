package uk.sume.news

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.sume.streamfolio.data.model.Article
import uk.sume.streamfolio.data.network.LinkOnlyFetchPolicy
import uk.sume.streamfolio.data.network.LinkOnlySources
import uk.sume.streamfolio.data.network.RssParser
import uk.sume.streamfolio.data.network.SourceConfig

class LinkOnlySourcesTest {

    private val feedUrl = LinkOnlySources.JOHO_TODAI_EN_FEED

    // A feed shaped like the publisher's: description (plain text), content:encoded (full HTML body
    // with an image), media:content and an enclosure.
    private val johoLikeFeed = """
        <?xml version="1.0" encoding="UTF-8"?>
        <rss version="2.0" xmlns:content="http://purl.org/rss/1.0/modules/content/" xmlns:media="http://search.yahoo.com/mrss/">
        <channel><title>Joho no Todai (English)</title><link>https://joho-todai.com/</link>
        <ttl>30</ttl>
        <item>
          <title>Stolen RTX 5090 Listed Online</title>
          <link>https://joho-todai.com/en/rtx-5090-stolen/</link>
          <pubDate>Fri, 23 Oct 2026 11:10:46 GMT</pubDate>
          <description><![CDATA[<img src="https://joho-todai.com/img/a.jpg"><p>Short description.</p>]]></description>
          <media:content url="https://joho-todai.com/img/b.jpg" medium="image"/>
          <enclosure url="https://joho-todai.com/img/c.jpg" type="image/jpeg"/>
          <content:encoded><![CDATA[<p><img src="https://joho-todai.com/img/d.jpg">FULL BODY TEXT</p>]]></content:encoded>
        </item>
        </channel></rss>
    """.trimIndent()

    @Test
    fun forUrl_matchesHostAndSubdomainsOnly() {
        assertNotNull(LinkOnlySources.forUrl("https://joho-todai.com/en/rss/"))
        assertNotNull(LinkOnlySources.forUrl("https://www.joho-todai.com/en/x/"))
        assertNull(LinkOnlySources.forUrl("https://example.com/joho-todai.com"))
        assertNull(LinkOnlySources.forUrl("https://notjoho-todai.com/"))
        assertNull(LinkOnlySources.forUrl(null))
        assertNull(LinkOnlySources.forUrl("not a url"))
    }

    @Test
    fun isLinkOnlyArticle_checksLinkAndFeedUrl() {
        val base = Article(
            link = "https://example.com/a", title = "t", description = "d", pubDate = "",
            sourceName = "Example", sourceUrl = "https://example.com/feed", category = "Technology"
        )
        assertFalse(LinkOnlySources.isLinkOnlyArticle(base))
        assertTrue(LinkOnlySources.isLinkOnlyArticle(base.copy(link = "https://joho-todai.com/en/a/")))
        assertTrue(LinkOnlySources.isLinkOnlyArticle(base.copy(sourceUrl = feedUrl)))
    }

    @Test
    fun parser_linkOnlyFeed_keepsOnlyHeadlineLinkDateAndShortDescription() {
        val articles = RssParser().parse(johoLikeFeed, "Technology", feedUrl = feedUrl)
        assertEquals(1, articles.size)
        val a = articles[0]
        assertEquals("Stolen RTX 5090 Listed Online", a.title)
        assertEquals("https://joho-todai.com/en/rtx-5090-stolen/", a.link)
        assertEquals("Short description.", a.description)
        assertNull("no image may be kept", a.thumbnailUrl)
        assertNull(a.fullText)
        assertFalse(a.description.contains("FULL BODY"))
        assertEquals("Joho no Todai", a.sourceName)
        assertTrue(a.pubDate.startsWith("2026-10-23"))
    }

    @Test
    fun parser_otherFeeds_stillReadImages() {
        val otherFeed = johoLikeFeed.replace("joho-todai.com", "example.com")
        val articles = RssParser().parse(otherFeed, "Technology", feedUrl = "https://example.com/feed/")
        assertEquals(1, articles.size)
        assertNotNull(articles[0].thumbnailUrl)
    }

    @Test
    fun parser_linkOnlyDescription_isTruncated() {
        val long = "x".repeat(LinkOnlySources.MAX_DESCRIPTION_CHARS + 200)
        val feed = johoLikeFeed.replace("<p>Short description.</p>", long)
        val a = RssParser().parse(feed, "Technology", feedUrl = feedUrl)[0]
        assertEquals(LinkOnlySources.MAX_DESCRIPTION_CHARS, a.description.length)
    }

    @Test
    fun fetchPolicy_parsesServerInstructions() {
        assertEquals(600_000L, LinkOnlyFetchPolicy.parseMaxAgeMs("public, max-age=600"))
        assertEquals(0L, LinkOnlyFetchPolicy.parseMaxAgeMs("no-cache"))
        assertEquals(0L, LinkOnlyFetchPolicy.parseMaxAgeMs(null))
        assertEquals(30L * 60_000L, LinkOnlyFetchPolicy.parseTtlMs(johoLikeFeed))
        assertEquals(120_000L, LinkOnlyFetchPolicy.parseRetryAfterMs("120", 0L))
        assertEquals(0L, LinkOnlyFetchPolicy.parseRetryAfterMs("garbage", 0L))
        assertEquals(0L, LinkOnlyFetchPolicy.parseRetryAfterMs(null, 0L))
    }

    @Test
    fun fetchPolicy_neverFetchesMoreOftenThanThirtyMinutes() {
        val now = 1_000_000L
        assertEquals(now + LinkOnlyFetchPolicy.MIN_INTERVAL_MS, LinkOnlyFetchPolicy.nextFetchAfterSuccess(now, null, null))
        assertEquals(now + LinkOnlyFetchPolicy.MIN_INTERVAL_MS, LinkOnlyFetchPolicy.nextFetchAfterSuccess(now, "max-age=60", null))
        assertEquals(now + LinkOnlyFetchPolicy.MIN_INTERVAL_MS, LinkOnlyFetchPolicy.nextFetchAfterNotModified(now, null))
        assertEquals(now + LinkOnlyFetchPolicy.MIN_INTERVAL_MS, LinkOnlyFetchPolicy.nextFetchAfterFailure(now))
    }

    @Test
    fun fetchPolicy_obeysLongerServerInstructions() {
        val now = 1_000_000L
        assertEquals(now + 7_200_000L, LinkOnlyFetchPolicy.nextFetchAfterSuccess(now, "max-age=7200", null))
        assertEquals(now + 7_200_000L, LinkOnlyFetchPolicy.nextFetchAfterSuccess(now, null, "<rss><channel><ttl>120</ttl>"))
        assertEquals(now + 10_800_000L, LinkOnlyFetchPolicy.nextFetchAfterBackoff(now, "10800"))
        assertEquals(now + LinkOnlyFetchPolicy.DEFAULT_BACKOFF_MS, LinkOnlyFetchPolicy.nextFetchAfterBackoff(now, null))
    }

    @Test
    fun fetchPolicy_capsWaitAtOneDay() {
        val now = 0L
        val farFuture = LinkOnlyFetchPolicy.nextFetchAfterSuccess(now, "max-age=99999999", null)
        assertEquals(LinkOnlyFetchPolicy.MAX_WAIT_MS, farFuture)
    }

    @Test
    fun sourceConfig_missingOrDisabledSourceIsNotEnabled() {
        assertEquals(
            setOf("joho-todai-en"),
            SourceConfig.parseEnabledIds("""{"version":1,"sources":{"joho-todai-en":{"enabled":true}}}""")
        )
        assertEquals(
            emptySet<String>(),
            SourceConfig.parseEnabledIds("""{"version":1,"sources":{"joho-todai-en":{"enabled":false}}}""")
        )
        assertEquals(emptySet<String>(), SourceConfig.parseEnabledIds("""{"version":1,"sources":{}}"""))
        assertEquals(
            emptySet<String>(),
            SourceConfig.parseEnabledIds("""{"version":1,"sources":{"joho-todai-en":{}}}""")
        )
    }

    @Test
    fun sourceConfig_unreadableFileMeansKeepLastKnownState() {
        assertNull(SourceConfig.parseEnabledIds("not json"))
        assertNull(SourceConfig.parseEnabledIds("""{"version":1}"""))
        assertNull(SourceConfig.parseEnabledIds(""))
    }
}
