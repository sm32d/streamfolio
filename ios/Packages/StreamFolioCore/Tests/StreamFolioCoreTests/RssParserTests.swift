import Foundation
import Testing
@testable import StreamFolioCore

@Suite("RssParser")
struct RssParserTests {
    let parser = RssParser()

    // MARK: Real-world sample feeds (sample_xml/)

    @Test("Parses every sample feed with the expected item count", arguments: [
        ("sample_xml/cbssports.xml", 36, "https://www.cbssports.com/rss/headlines/"),
        ("sample_xml/cna_rss.xml", 20, "https://www.channelnewsasia.com/api/v1/rss-outbound-feed?_format=xml&category=10416"),
        ("sample_xml/hollywoodreporter.xml", 10, "https://www.hollywoodreporter.com/feed/"),
        ("sample_xml/ht_sports.xml", 64, "https://www.hindustantimes.com/feeds/rss/sports/rssfeed.xml"),
        ("sample_xml/livemint_markets.xml", 35, "https://www.livemint.com/rss/markets"),
        ("sample_xml/moneycontrol.xml", 15, "https://www.moneycontrol.com/rss/latestnews.xml"),
        ("sample_xml/variety.xml", 10, "https://variety.com/feed/"),
        ("sample_xml/yahoo_sports.xml", 50, "https://sports.yahoo.com/rss/"),
    ])
    func sampleFeeds(path: String, expectedItems: Int, feedUrl: String) throws {
        let articles = parser.parse(try RepoFiles.data(path), category: "Sports", feedUrl: feedUrl)
        #expect(articles.count == expectedItems, "\(path)")
        for article in articles {
            #expect(!article.link.isEmpty)
            #expect(!article.title.isEmpty)
            #expect(!article.title.contains("<"), "markup left in title: \(article.title)")
            #expect(!article.description.contains("<"), "markup left in description")
            #expect(article.category == "Sports")
            #expect(article.sourceUrl == feedUrl)
            #expect(article.sourceName != CuratedFeeds.unknownSource)
        }
    }

    @Test("Dates are normalized to sortable UTC ISO-8601")
    func datesNormalized() throws {
        let articles = parser.parse(try RepoFiles.data("sample_xml/cna_rss.xml"), category: "World")
        let dated = articles.filter { !$0.pubDate.isEmpty }
        #expect(!dated.isEmpty)
        for article in dated {
            #expect(article.publishedAt != nil, "unparsed date: \(article.pubDate)")
            #expect(article.pubDate.hasSuffix("Z"))
        }
    }

    @Test("Curated publisher names are resolved from the feed URL")
    func publisherResolution() throws {
        let cbs = parser.parse(try RepoFiles.data("sample_xml/cbssports.xml"), category: "Sports", feedUrl: "https://www.cbssports.com/rss/headlines/")
        #expect(cbs.allSatisfy { $0.sourceName == "CBS Sports" })
    }

    // MARK: Hand-written cases

    @Test("RSS item: fields, CDATA, entities, image from media:thumbnail")
    func rssItem() {
        let xml = """
        <?xml version="1.0"?>
        <rss version="2.0" xmlns:media="http://search.yahoo.com/mrss/"><channel><title>Example Daily</title>
        <item>
          <title><![CDATA[Fish &amp; Chips <b>rule</b>]]></title>
          <link>https://example.com/a</link>
          <description><![CDATA[<p>Hello <i>world</i> &amp; friends</p>]]></description>
          <pubDate>Sat, 24 Oct 2026 09:23:00 GMT</pubDate>
          <media:thumbnail url="https://example.com/t.jpg"/>
        </item></channel></rss>
        """
        let articles = parser.parse(xml, category: "World", feedUrl: "https://example.com/feed")
        #expect(articles.count == 1)
        let article = articles[0]
        #expect(article.link == "https://example.com/a")
        #expect(article.title == "Fish & Chips rule")
        #expect(article.description == "Hello world & friends")
        #expect(article.thumbnailUrl == "https://example.com/t.jpg")
        #expect(article.pubDate == "2026-10-24T09:23:00.000Z")
        #expect(article.sourceName == "Example Daily")
    }

    @Test("Atom entry: href link, summary, published")
    func atomEntry() {
        let xml = """
        <feed xmlns="http://www.w3.org/2005/Atom"><title>Atom Feed</title>
        <entry><title>Entry One</title><link href="https://example.com/e1"/><summary>Short</summary>
        <published>2026-10-24T09:23:00Z</published></entry></feed>
        """
        let article = parser.parse(xml, category: "Technology")[0]
        #expect(article.link == "https://example.com/e1")
        #expect(article.title == "Entry One")
        #expect(article.description == "Short")
        #expect(article.pubDate == "2026-10-24T09:23:00.000Z")
    }

    @Test("First <img> in the description becomes the thumbnail, relative paths resolved against the article host")
    func imageFromDescription() {
        let xml = """
        <rss><channel><title>T</title>
        <item><title>A</title><link>https://example.com/a</link>
        <description><![CDATA[<img src="/img/x.png"> text]]></description></item>
        <item><title>B</title><link>https://example.com/b</link>
        <description><![CDATA[<img src="http://cdn.example.com/y.png">]]></description></item>
        </channel></rss>
        """
        let articles = parser.parse(xml, category: "World")
        #expect(articles[0].thumbnailUrl == "https://example.com/img/x.png")
        #expect(articles[1].thumbnailUrl == "https://cdn.example.com/y.png")   // upgraded to https
    }

    @Test("A trailing ' - Publisher' is split off the headline")
    func titleSuffix() {
        let xml = """
        <rss><channel><title>Aggregator</title>
        <item><title>Big news today - Daily Planet</title><link>https://x.example/a</link></item>
        </channel></rss>
        """
        let article = parser.parse(xml, category: "World", feedUrl: "https://x.example/feed")[0]
        #expect(article.title == "Big news today")
        #expect(article.sourceName == "Daily Planet")
    }

    @Test("Items without a link are skipped; malformed tail keeps earlier articles")
    func robustness() {
        let xml = """
        <rss><channel><title>T</title>
        <item><title>No link</title></item>
        <item><title>Good</title><link>https://example.com/good</link></item>
        <item><title>Broken</title><link>https://example.com/broken
        """
        let articles = parser.parse(xml, category: "World")
        #expect(articles.map(\.link) == ["https://example.com/good"])
    }

    @Test("DOCTYPE with an external entity is inert (XXE)")
    func xxe() {
        let xml = """
        <?xml version="1.0"?>
        <!DOCTYPE rss [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
        <rss><channel><title>T</title>
        <item><title>&xxe;</title><link>https://example.com/x</link></item></channel></rss>
        """
        let articles = parser.parse(xml, category: "World")
        for article in articles { #expect(!article.title.contains("root:")) }
    }

    // MARK: Link-only (publisher terms)

    private let linkOnlyXml = """
    <rss version="2.0" xmlns:media="http://search.yahoo.com/mrss/" xmlns:content="http://purl.org/rss/1.0/modules/content/">
    <channel><title>Joho no Todai</title>
    <item>
      <title>Headline</title>
      <link>https://joho-todai.com/en/a/</link>
      <pubDate>Sat, 24 Oct 2026 09:23:00 +0900</pubDate>
      <description><![CDATA[<img src="https://joho-todai.com/i.jpg"> \(String(repeating: "x", count: 700))]]></description>
      <content:encoded><![CDATA[<p>FULL BODY</p>]]></content:encoded>
      <media:content url="https://joho-todai.com/m.jpg" type="image/jpeg"/>
      <media:thumbnail url="https://joho-todai.com/t.jpg"/>
      <enclosure url="https://joho-todai.com/e.jpg" type="image/jpeg"/>
    </item></channel></rss>
    """

    @Test("Link-only feeds keep only title, link, date and a short plain-text description")
    func linkOnlyFeed() {
        let article = parser.parse(linkOnlyXml, category: "Technology", feedUrl: LinkOnlySources.johoTodaiEnFeed)[0]
        #expect(article.title == "Headline")
        #expect(article.link == "https://joho-todai.com/en/a/")
        #expect(article.pubDate == "2026-10-24T00:23:00.000Z")
        #expect(article.thumbnailUrl == nil)
        #expect(!article.description.contains("FULL BODY"))
        #expect(!article.description.contains("<"))
        #expect(article.description.count <= LinkOnlySources.maxDescriptionChars)
    }

    @Test("The same markup from a non-link-only feed still yields images and the body")
    func sameMarkupNormalFeed() {
        let article = parser.parse(linkOnlyXml, category: "Technology", feedUrl: "https://other.example/feed")[0]
        #expect(article.thumbnailUrl != nil)
        #expect(article.description.contains("FULL BODY"))
    }

    // MARK: OPML

    @Test("OPML detection and nested outline URLs")
    func opml() {
        let opml = Data("""
        <?xml version="1.0"?><opml version="2.0"><body>
        <outline text="Group"><outline text="A" xmlUrl="https://a.example/feed"/><outline text="B" xmlUrl="https://b.example/rss"/></outline>
        <outline text="No url"/></body></opml>
        """.utf8)
        #expect(parser.isOpml(opml))
        #expect(parser.parseOpmlUrls(opml) == ["https://a.example/feed", "https://b.example/rss"])
        #expect(!parser.isOpml(Data("<rss><channel/></rss>".utf8)))
    }
}
