package uk.sume.streamfolio.util

import android.util.Xml
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.xmlpull.v1.XmlPullParserFactory
import uk.sume.streamfolio.data.model.CustomFeed
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

class OpmlHelperTest {

    @Before
    fun stubAndroidXml() {
        // android.util.Xml.newPullParser() is a real Android framework call the unit-test stub
        // jar throws "not mocked" for; delegate it to a real XmlPullParser implementation (the
        // xmlpull/xpp3 jars already on the test classpath for RssParser's own JVM tests) so
        // OpmlHelper.parseOpml() can run unmodified in a plain JVM unit test.
        mockkStatic(Xml::class)
        every { Xml.newPullParser() } answers { XmlPullParserFactory.newInstance().newPullParser() }
    }

    @After
    fun unstubAndroidXml() {
        unmockkStatic(Xml::class)
    }

    private fun stream(xml: String) = ByteArrayInputStream(xml.toByteArray(StandardCharsets.UTF_8))

    @Test
    fun `parses nested category folders into flat feed list`() {
        // language=XML
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <opml version="2.0">
              <body>
                <outline text="Tech">
                  <outline type="rss" text="Feed A" xmlUrl="https://a.example.com/rss.xml"/>
                  <outline type="rss" text="Feed B" xmlUrl="https://b.example.com/rss.xml"/>
                </outline>
                <outline text="Sports">
                  <outline type="rss" text="Feed C" xmlUrl="https://c.example.com/rss.xml"/>
                </outline>
              </body>
            </opml>
        """.trimIndent()

        val feeds = OpmlHelper.parseOpml(stream(xml))

        assertEquals(3, feeds.size)
        assertEquals(OpmlFeed("Feed A", "https://a.example.com/rss.xml", "Tech"), feeds[0])
        assertEquals(OpmlFeed("Feed B", "https://b.example.com/rss.xml", "Tech"), feeds[1])
        assertEquals(OpmlFeed("Feed C", "https://c.example.com/rss.xml", "Sports"), feeds[2])
    }

    @Test
    fun `feed outlines without a category folder default to Imported`() {
        // language=XML
        val xml = """
            <opml version="2.0">
              <body>
                <outline type="rss" text="Standalone Feed" xmlUrl="https://standalone.example.com/rss.xml"/>
              </body>
            </opml>
        """.trimIndent()

        val feeds = OpmlHelper.parseOpml(stream(xml))

        assertEquals(1, feeds.size)
        assertEquals("Imported", feeds[0].category)
    }

    @Test
    fun `outlines without an xmlUrl are treated as folders, not feeds`() {
        // language=XML
        val xml = """
            <opml version="2.0">
              <body>
                <outline text="Just a note, no feed"/>
              </body>
            </opml>
        """.trimIndent()

        val feeds = OpmlHelper.parseOpml(stream(xml))
        assertTrue(feeds.isEmpty())
    }

    @Test
    fun `malformed opml propagates a parser exception rather than silently returning partial data`() {
        // OpmlHelper.parseOpml() has no internal try/catch (unlike RssParser.parse()), so
        // truncated/invalid XML surfaces as an exception to the caller.
        val truncated = "<opml version=\"2.0\"><body><outline text=\"Unterminated"
        assertThrows(Exception::class.java) {
            OpmlHelper.parseOpml(stream(truncated))
        }
    }

    @Test
    fun `exportFeedsToOpml escapes special xml characters and groups by category`() {
        val feeds = listOf(
            CustomFeed(title = "Tom & Jerry's \"Blog\"", url = "https://example.com/feed?a=1&b=2", category = "Fun <Stuff>"),
            CustomFeed(title = "Second Feed", url = "https://example.com/second.xml", category = "Fun <Stuff>")
        )

        val opml = OpmlHelper.exportFeedsToOpml(feeds)

        assertTrue(opml.contains("Tom &amp; Jerry&apos;s &quot;Blog&quot;"))
        assertTrue(opml.contains("https://example.com/feed?a=1&amp;b=2"))
        assertTrue(opml.contains("Fun &lt;Stuff&gt;"))
        assertTrue("raw unescaped ampersand must not appear in the exported XML", !opml.contains(" & "))
    }

    @Test
    fun `export then re-import round-trips titles urls and categories`() {
        val feeds = listOf(
            CustomFeed(title = "Feed A", url = "https://a.example.com/rss.xml", category = "Tech"),
            CustomFeed(title = "Feed B", url = "https://b.example.com/rss.xml", category = "Tech"),
            CustomFeed(title = "Feed C", url = "https://c.example.com/rss.xml", category = "Sports")
        )

        val opml = OpmlHelper.exportFeedsToOpml(feeds)
        val reimported = OpmlHelper.parseOpml(stream(opml))

        assertEquals(3, reimported.size)
        assertEquals(
            feeds.map { it.title to it.url to it.category }.toSet(),
            reimported.map { it.title to it.xmlUrl to it.category }.toSet()
        )
    }
}
