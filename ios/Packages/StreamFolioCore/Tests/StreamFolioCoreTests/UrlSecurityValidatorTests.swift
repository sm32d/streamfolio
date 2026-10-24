import Testing
@testable import StreamFolioCore

@Suite("UrlSecurityValidator")
struct UrlSecurityValidatorTests {

    @Test("Accepts ordinary public feed URLs", arguments: [
        "https://feeds.bbci.co.uk/news/rss.xml",
        "https://techcrunch.com/feed/",
        "http://www.scmp.com/rss/91/feed",
        "example.com/rss",                       // scheme added
    ])
    func acceptsPublicUrls(url: String) {
        #expect(UrlSecurityValidator.isUrlSafe(url))
    }

    @Test("Rejects loopback, private, link-local and reserved IPv4 literals", arguments: [
        "http://127.0.0.1/", "http://127.255.255.254/", "https://10.0.0.1/", "https://172.16.0.1/",
        "https://172.31.255.255/", "https://192.168.1.1/", "http://169.254.169.254/latest/meta-data",
        "https://100.64.0.1/", "https://0.0.0.0/", "https://192.0.0.8/", "https://192.0.2.1/",
        "https://198.18.0.1/", "https://198.51.100.7/", "https://203.0.113.9/", "https://240.0.0.1/",
        "https://255.255.255.255/",
    ])
    func rejectsPrivateIPv4(url: String) {
        #expect(!UrlSecurityValidator.isUrlSafe(url))
    }

    @Test("Boundary of RFC1918 172.16/12 and CGNAT 100.64/10")
    func boundaries() {
        #expect(UrlSecurityValidator.isUrlSafe("https://172.15.0.1/"))
        #expect(!UrlSecurityValidator.isUrlSafe("https://172.16.0.1/"))
        #expect(!UrlSecurityValidator.isUrlSafe("https://172.31.0.1/"))
        #expect(UrlSecurityValidator.isUrlSafe("https://172.32.0.1/"))
        #expect(UrlSecurityValidator.isUrlSafe("https://100.63.0.1/"))
        #expect(!UrlSecurityValidator.isUrlSafe("https://100.127.0.1/"))
        #expect(UrlSecurityValidator.isUrlSafe("https://100.128.0.1/"))
    }

    @Test("Rejects blocked and local hostnames", arguments: [
        "http://localhost/", "https://LOCALHOST/", "https://localhost./", "https://app.localhost/",
        "https://metadata.google.internal/", "https://metadata.azure.internal/",
        "https://metadata.aws.internal/", "https://instance-data/",
    ])
    func rejectsBlockedHosts(url: String) {
        #expect(!UrlSecurityValidator.isUrlSafe(url))
    }

    @Test("Rejects ambiguous numeric hosts the OS resolver would turn into an IP", arguments: [
        "http://2130706433/", "http://127.1/", "http://0x7f.0.0.1/", "http://0177.0.0.1/", "http://1.2.3/",
    ])
    func rejectsAmbiguousNumericHosts(url: String) {
        #expect(!UrlSecurityValidator.isUrlSafe(url))
    }

    @Test("IPv6 literals", arguments: [
        ("http://[::1]/", false), ("http://[::]/", false), ("http://[fe80::1]/", false),
        ("http://[fc00::1]/", false), ("http://[fd12:3456::1]/", false), ("http://[ff02::1]/", false),
        ("http://[2001:db8::1]/", false), ("http://[::ffff:127.0.0.1]/", false),
        ("http://[::ffff:10.0.0.1]/", false), ("http://[64:ff9b::7f00:1]/", false),
        ("http://[2002:7f00:1::]/", false),
        ("http://[2606:4700:4700::1111]/", true),    // public (Cloudflare DNS)
        ("http://[::ffff:8.8.8.8]/", true),
    ])
    func ipv6(url: String, safe: Bool) {
        #expect(UrlSecurityValidator.isUrlSafe(url) == safe)
    }

    @Test("Only http(s) schemes")
    func schemes() {
        #expect(!UrlSecurityValidator.isUrlSafe("ftp://example.com/feed"))
        #expect(!UrlSecurityValidator.isUrlSafe("file:///etc/passwd"))
        #expect(!UrlSecurityValidator.isUrlSafe("javascript:alert(1)"))
    }

    @Test("requireHttps rejects http and normalizeToHttps upgrades it")
    func httpsRules() {
        #expect(!UrlSecurityValidator.isUrlSafe("http://example.com/feed", requireHttps: true))
        #expect(UrlSecurityValidator.isUrlSafe("https://example.com/feed", requireHttps: true))
        #expect(UrlSecurityValidator.normalizeToHttps("http://example.com/a?b=1") == "https://example.com/a?b=1")
        #expect(UrlSecurityValidator.normalizeToHttps("HTTP://Example.com/x") == "https://Example.com/x")
        #expect(UrlSecurityValidator.normalizeToHttps("http://127.0.0.1/x") == nil)
        #expect(UrlSecurityValidator.normalizeToHttps("   ") == nil)
    }

    @Test("normalize / sanitize")
    func normalize() {
        #expect(UrlSecurityValidator.normalizeUrl("  example.com/feed  ") == "https://example.com/feed")
        #expect(UrlSecurityValidator.normalizeUrl("") == nil)
        #expect(UrlSecurityValidator.sanitizeUrl("example.com/feed") == "https://example.com/feed")
        #expect(UrlSecurityValidator.sanitizeUrl("http://localhost/") == nil)
        #expect(UrlSecurityValidator.isHttpsUrl(" HTTPS://a.b "))
        #expect(!UrlSecurityValidator.isHttpsUrl("http://a.b"))
    }

    @Test("Resolved-address check uses the same policy as literals")
    func resolvedAddresses() {
        #expect(!UrlSecurityValidator.isResolvedAddressSafe([127, 0, 0, 1]))
        #expect(!UrlSecurityValidator.isResolvedAddressSafe([169, 254, 169, 254]))
        #expect(UrlSecurityValidator.isResolvedAddressSafe([8, 8, 8, 8]))
        #expect(!UrlSecurityValidator.isResolvedAddressSafe([UInt8](repeating: 0, count: 15) + [1]))   // ::1
        #expect(!UrlSecurityValidator.isResolvedAddressSafe([1, 2, 3]))                                // malformed
    }
}
