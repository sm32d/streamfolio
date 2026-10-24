import Foundation
import Testing
@testable import StreamFolioCore

@Suite("CuratedFeeds")
struct CuratedFeedsTests {
    let feeds = CuratedFeeds.shared

    @Test("Bundled JSON matches DefaultFeedsConfig.kt (parity)")
    func parityWithKotlin() throws {
        let kotlin = try RepoFiles.string("app/src/main/java/uk/sume/streamfolio/data/network/DefaultFeedsConfig.kt")
        let body = String(kotlin[kotlin.range(of: "private val FEEDS = mapOf(")!.lowerBound..<kotlin.range(of: "fun getFeedsFor")!.lowerBound])

        // Every quoted URL and constant in the Kotlin map, per region, in order.
        var expected: [String: [String]] = [:]
        var order: [String] = []
        var region = ""
        for line in body.split(separator: "\n") {
            let text = line.trimmed
            if let m = text.firstMatch(of: /^"([A-Z]{2})" to mapOf\($/) {
                region = String(m.output.1); order.append(region); expected[region] = []
            } else if let m = text.firstMatch(of: /^"(https?:\/\/[^"]+)",?$/) {
                expected[region]?.append(String(m.output.1))
            } else if text.hasPrefix("LinkOnlySources.JOHO_TODAI_EN_FEED") {
                expected[region]?.append(LinkOnlySources.johoTodaiEnFeed)
            }
        }
        #expect(order == feeds.regionCodes)
        let kotlinTotal = expected.values.reduce(0) { $0 + $1.count }
        let swiftTotal = feeds.allProviders().count
        #expect(kotlinTotal == swiftTotal, "Kotlin has \(kotlinTotal) feed entries, JSON has \(swiftTotal) — rerun ios/scripts/generate_curated_feeds.py")
        for code in order {
            let swiftUrls = feeds.allProviders().filter { $0.region == code }.map(\.url)
            #expect(swiftUrls == expected[code], "region \(code) differs from Kotlin")
        }
    }

    @Test("Publisher rule count matches Kotlin")
    func publisherRuleParity() throws {
        let kotlin = try RepoFiles.string("app/src/main/java/uk/sume/streamfolio/data/network/DefaultFeedsConfig.kt")
        let section = String(kotlin[kotlin.range(of: "fun getPublisherName")!.lowerBound..<kotlin.range(of: "fun getAllCuratedProviders")!.lowerBound])
        let kotlinRules = section.split(separator: "\n").filter { $0.contains("->") && $0.contains("url.contains") }.count
        let names = Set(feeds.allProviders().map(\.publisherName))
        #expect(kotlinRules == 62)
        #expect(!names.contains(CuratedFeeds.unknownSource), "a curated feed has no publisher rule: \(names)")
    }

    @Test("Lookup honours region, disabled feeds and cross-region opt-ins")
    func lookup() {
        let us = feeds.feeds(region: "US", category: "Top Stories")
        #expect(us.contains("https://feeds.npr.org/1001/rss.xml"))

        let disabled = feeds.feeds(region: "us", category: "Top Stories", disabledFeedUrls: ["US|Top Stories|https://feeds.npr.org/1001/rss.xml"])
        #expect(!disabled.contains("https://feeds.npr.org/1001/rss.xml"))

        let cross = feeds.feeds(region: "US", category: "Top Stories", enabledCrossRegionFeeds: ["GB|Top Stories|https://feeds.bbci.co.uk/news/rss.xml"])
        #expect(cross.contains("https://feeds.bbci.co.uk/news/rss.xml"))

        // Unknown region falls back to US; no duplicates (BBC world appears twice in US Top Stories sources).
        #expect(feeds.feeds(region: "ZZ", category: "Top Stories") == us)
        #expect(Set(us).count == us.count)
    }

    @Test("Publisher names, first rule wins")
    func publisherNames() {
        #expect(feeds.publisherName(for: "https://feeds.bbci.co.uk/news/rss.xml") == "BBC News")
        #expect(feeds.publisherName(for: "https://www.scmp.com/rss/91/feed") == "South China Morning Post")
        #expect(feeds.publisherName(for: "https://nowhere.example/feed") == CuratedFeeds.unknownSource)
    }

    @Test("Region catalog")
    func regions() {
        #expect(RegionCatalog.all.count == 14)
        #expect(RegionCatalog.all.map(\.name) == RegionCatalog.all.map(\.name).sorted())
        #expect(RegionCatalog.info(for: "xx").code == "US")
        #expect(Set(RegionCatalog.all.map(\.code)) == Set(feeds.regionCodes))
    }

    @Test("Link-only host matching")
    func linkOnlyHosts() {
        #expect(LinkOnlySources.isLinkOnly(url: "https://joho-todai.com/en/rss/"))
        #expect(LinkOnlySources.isLinkOnly(url: "https://www.joho-todai.com/x"))
        #expect(!LinkOnlySources.isLinkOnly(url: "https://notjoho-todai.com/x"))
        #expect(!LinkOnlySources.isLinkOnly(url: nil))
        #expect(!ArticleCapabilities(isLinkOnly: true).canListen)
        #expect(ArticleCapabilities(isLinkOnly: false).canSummarize)
    }
}
