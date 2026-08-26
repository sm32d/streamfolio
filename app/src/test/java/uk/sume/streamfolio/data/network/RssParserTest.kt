package uk.sume.streamfolio.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RssParserTest {

    private val parser = RssParser()

    // language=XML
    private val rss2Sample = """
        <?xml version="1.0" encoding="UTF-8"?>
        <rss version="2.0">
          <channel>
            <title>Example Channel</title>
            <item>
              <title>First Headline</title>
              <link>https://example.com/articles/first</link>
              <description>&lt;p&gt;Some &lt;b&gt;bold&lt;/b&gt; summary text.&lt;/p&gt;</description>
              <pubDate>Mon, 01 Jan 2024 10:00:00 GMT</pubDate>
              <source url="https://example.com/rss.xml">Example Source</source>
            </item>
            <item>
              <title>Second Headline</title>
              <link>https://example.com/articles/second</link>
              <description>Plain text summary</description>
              <pubDate>Tue, 02 Jan 2024 11:30:00 GMT</pubDate>
            </item>
          </channel>
        </rss>
    """.trimIndent()

    // language=XML
    private val atomSample = """
        <?xml version="1.0" encoding="UTF-8"?>
        <feed xmlns="http://www.w3.org/2005/Atom">
          <title>Example Atom Feed</title>
          <entry>
            <title>Atom Headline</title>
            <link href="https://example.com/atom/one" />
            <summary>An atom entry summary.</summary>
            <published>2024-01-03T09:15:00Z</published>
          </entry>
        </feed>
    """.trimIndent()

    // language=XML
    private val opmlSample = """
        <?xml version="1.0" encoding="UTF-8"?>
        <opml version="2.0">
          <head><title>Subscriptions</title></head>
          <body>
            <outline text="Tech">
              <outline type="rss" text="Feed A" xmlUrl="https://a.example.com/rss.xml"/>
              <outline type="rss" text="Feed B" xmlUrl="https://b.example.com/rss.xml"/>
            </outline>
          </body>
        </opml>
    """.trimIndent()

    @Test
    fun `parses RSS 2 items into articles with title link description and pubDate`() {
        val articles = parser.parse(rss2Sample, category = "Tech")

        assertEquals(2, articles.size)
        val first = articles[0]
        assertEquals("First Headline", first.title)
        assertEquals("https://example.com/articles/first", first.link)
        // Description is sanitized (HTML stripped) via Jsoup.clean(..., Safelist.none()).
        assertEquals("Some bold summary text.", first.description)
        assertEquals("Tech", first.category)
        assertEquals("Example Source", first.sourceName)

        val second = articles[1]
        assertEquals("Second Headline", second.title)
        assertEquals("Plain text summary", second.description)
    }

    @Test
    fun `parses Atom entries into articles`() {
        val articles = parser.parse(atomSample, category = "General")

        assertEquals(1, articles.size)
        val entry = articles[0]
        assertEquals("Atom Headline", entry.title)
        assertEquals("https://example.com/atom/one", entry.link)
        assertEquals("An atom entry summary.", entry.description)
    }

    @Test
    fun `articles without a link are dropped`() {
        // language=XML
        val xml = """
            <rss version="2.0">
              <channel>
                <item>
                  <title>No link here</title>
                  <description>Missing link entirely</description>
                </item>
              </channel>
            </rss>
        """.trimIndent()

        val articles = parser.parse(xml, category = "General")
        assertTrue(articles.isEmpty())
    }

    @Test
    fun `malformed xml does not throw and returns empty or partial results`() {
        val malformed = "<rss version=\"2.0\"><channel><item><title>Unterminated"
        // RssParser.parse() catches all exceptions internally; this must not throw.
        val articles = parser.parse(malformed, category = "General")
        assertTrue(articles.isEmpty())
    }

    @Test
    fun `docdecl entity expansion is suppressed rather than inlined into parsed content`() {
        // A classic XXE-style payload: an internal DTD entity referencing a local file.
        // FEATURE_PROCESS_DOCDECL=false means the parser must not resolve &xxe; into file
        // contents anywhere in the output.
        // language=XML
        val xxePayload = """
            <?xml version="1.0"?>
            <!DOCTYPE rss [
              <!ENTITY xxe SYSTEM "file:///etc/passwd">
            ]>
            <rss version="2.0">
              <channel>
                <item>
                  <title>&xxe;Suspicious Title</title>
                  <link>https://example.com/xxe</link>
                  <description>test</description>
                </item>
              </channel>
            </rss>
        """.trimIndent()

        val articles = parser.parse(xxePayload, category = "General")

        // Whatever the parser does with the malformed entity reference, it must never leak
        // local file contents into a parsed field.
        for (article in articles) {
            assertFalse(article.title.contains("root:"))
            assertFalse(article.description.contains("root:"))
        }
    }

    @Test
    fun `isOpml detects opml documents and rejects rss and atom`() {
        assertTrue(parser.isOpml(opmlSample))
        assertFalse(parser.isOpml(rss2Sample))
        assertFalse(parser.isOpml(atomSample))
    }

    @Test
    fun `parseOpmlUrls extracts nested feed xmlUrls`() {
        val urls = parser.parseOpmlUrls(opmlSample)
        assertEquals(listOf("https://a.example.com/rss.xml", "https://b.example.com/rss.xml"), urls)
    }

    @Test
    fun `parseOpmlUrls returns empty list for malformed input without throwing`() {
        val urls = parser.parseOpmlUrls("not xml at all")
        assertTrue(urls.isEmpty())
    }
}
