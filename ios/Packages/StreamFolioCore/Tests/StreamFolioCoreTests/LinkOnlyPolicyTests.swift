import Foundation
import Testing
@testable import StreamFolioCore

/// Port of Android `LinkOnlySourcesTest` (fetch policy + source config). These are terms agreed with
/// the publisher, so they are identical on every platform.
@Suite("Link-only fetch policy (publisher terms)")
struct LinkOnlyPolicyTests {
    let johoLikeFeed = """
        <rss version="2.0"><channel><title>Joho no Todai (English)</title><ttl>30</ttl><item><title>T</title></item></channel></rss>
        """

    @Test("Server instructions are parsed")
    func parsesServerInstructions() {
        #expect(LinkOnlyFetchPolicy.parseMaxAgeMs("public, max-age=600") == 600_000)
        #expect(LinkOnlyFetchPolicy.parseMaxAgeMs("no-cache") == 0)
        #expect(LinkOnlyFetchPolicy.parseMaxAgeMs(nil) == 0)
        #expect(LinkOnlyFetchPolicy.parseTtlMs(johoLikeFeed) == 30 * 60_000)
        #expect(LinkOnlyFetchPolicy.parseRetryAfterMs("120", now: 0) == 120_000)
        #expect(LinkOnlyFetchPolicy.parseRetryAfterMs("garbage", now: 0) == 0)
        #expect(LinkOnlyFetchPolicy.parseRetryAfterMs(nil, now: 0) == 0)
    }

    @Test("Never more often than every thirty minutes")
    func thirtyMinuteFloor() {
        let now: Int64 = 1_000_000
        let floor = now + LinkOnlyFetchPolicy.minInterval
        #expect(LinkOnlyFetchPolicy.nextFetchAfterSuccess(now: now, cacheControl: nil, ttlMs: 0) == floor)
        #expect(LinkOnlyFetchPolicy.nextFetchAfterSuccess(now: now, cacheControl: "max-age=60", ttlMs: 0) == floor)
        #expect(LinkOnlyFetchPolicy.nextFetchAfterNotModified(now: now, cacheControl: nil, storedTtlMs: 0) == floor)
        #expect(LinkOnlyFetchPolicy.nextFetchAfterFailure(now: now) == floor)
    }

    @Test("Longer server instructions are obeyed")
    func obeysLongerInstructions() {
        let now: Int64 = 1_000_000
        #expect(LinkOnlyFetchPolicy.nextFetchAfterSuccess(now: now, cacheControl: "max-age=7200", ttlMs: 0) == now + 7_200_000)
        #expect(LinkOnlyFetchPolicy.nextFetchAfterSuccess(now: now, cacheControl: nil, ttlMs: LinkOnlyFetchPolicy.parseTtlMs("<rss><channel><ttl>120</ttl>")) == now + 7_200_000)
        #expect(LinkOnlyFetchPolicy.nextFetchAfterBackoff(now: now, retryAfter: "10800") == now + 10_800_000)
        #expect(LinkOnlyFetchPolicy.nextFetchAfterBackoff(now: now, retryAfter: nil) == now + LinkOnlyFetchPolicy.defaultBackoff)
        #expect(LinkOnlyFetchPolicy.nextFetchAfterGone(now: now) == now + LinkOnlyFetchPolicy.goneBackoff)
    }

    @Test("No 24-hour ceiling on server instructions")
    func noDayCeiling() {
        let twoDays: Int64 = 48 * 60 * 60 * 1000
        #expect(LinkOnlyFetchPolicy.nextFetchAfterSuccess(now: 0, cacheControl: "max-age=172800", ttlMs: 0) == twoDays)
        #expect(LinkOnlyFetchPolicy.nextFetchAfterSuccess(now: 0, cacheControl: nil, ttlMs: twoDays) == twoDays)
        #expect(LinkOnlyFetchPolicy.nextFetchAfterBackoff(now: 0, retryAfter: "259200") == 3 * 24 * 60 * 60 * 1000)
        #expect(LinkOnlyFetchPolicy.parseRetryAfterMs("172800", now: 0) == twoDays)
    }

    @Test("A 304 still honours the stored ttl")
    func notModifiedKeepsTtl() {
        let twoHours: Int64 = 2 * 60 * 60 * 1000
        #expect(LinkOnlyFetchPolicy.nextFetchAfterNotModified(now: 1000, cacheControl: "max-age=300", storedTtlMs: twoHours) == 1000 + twoHours)
    }

    @Test("Only a one-year sanity bound against overflow")
    func overflowBound() {
        #expect(LinkOnlyFetchPolicy.nextFetchAfterSuccess(now: 0, cacheControl: "max-age=99999999999", ttlMs: 0) == LinkOnlyFetchPolicy.maxWait)
        #expect(LinkOnlyFetchPolicy.parseMaxAgeMs("max-age=9223372036854775807") == LinkOnlyFetchPolicy.maxWait)
    }

    @Test("Retry-After accepts an HTTP date")
    func retryAfterDate() {
        let now: Int64 = 0   // 1970-01-01
        #expect(LinkOnlyFetchPolicy.parseRetryAfterMs("Thu, 01 Jan 1970 01:00:00 GMT", now: now) == 3_600_000)
    }

    @Test("Source list: missing or disabled means off; unreadable is not validated")
    func sourceConfig() {
        #expect(SourceConfig.parseEnabledIds(#"{"version":1,"sources":{"joho-todai-en":{"enabled":true}}}"#) == ["joho-todai-en"])
        #expect(SourceConfig.parseEnabledIds(#"{"version":1,"sources":{"joho-todai-en":{"enabled":false}}}"#) == [])
        #expect(SourceConfig.parseEnabledIds(#"{"version":1,"sources":{}}"#) == [])
        #expect(SourceConfig.parseEnabledIds(#"{"version":1,"sources":{"joho-todai-en":{}}}"#) == [])
        #expect(SourceConfig.parseEnabledIds("not json") == nil)
        #expect(SourceConfig.parseEnabledIds(#"{"version":1}"#) == nil)
        #expect(SourceConfig.parseEnabledIds("") == nil)
    }

    @Test("The repository's real source list parses")
    func realSourceList() throws {
        let ids = SourceConfig.parseEnabledIds(try RepoFiles.data("source-config/sources.json"))
        #expect(ids != nil)
    }
}
