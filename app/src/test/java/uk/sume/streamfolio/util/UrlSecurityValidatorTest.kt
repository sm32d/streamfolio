package uk.sume.streamfolio.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlSecurityValidatorTest {

    @Test
    fun `rejects loopback and private IPv4 literals`() {
        val blocked = listOf(
            "http://127.0.0.1/feed.xml",
            "http://10.0.0.1/feed.xml",
            "http://172.16.0.1/feed.xml",
            "http://172.31.255.255/feed.xml",
            "http://192.168.1.1/feed.xml",
            "http://169.254.169.254/feed.xml",
            "http://100.64.0.1/feed.xml",
            "http://0.0.0.0/feed.xml"
        )
        for (url in blocked) {
            assertFalse("expected $url to be blocked", UrlSecurityValidator.isUrlSafe(url))
        }
    }

    @Test
    fun `rejects public-range-adjacent IPv4 not actually reserved`() {
        // 172.15.x.x and 172.32.x.x are outside the reserved 172.16-31 block.
        assertTrue(UrlSecurityValidator.isUrlSafe("http://172.15.0.1/feed.xml"))
        assertTrue(UrlSecurityValidator.isUrlSafe("http://172.32.0.1/feed.xml"))
    }

    @Test
    fun `rejects known cloud metadata hostnames`() {
        val blocked = listOf(
            "http://metadata.google.internal/computeMetadata/v1/",
            "http://metadata.azure.internal/",
            "http://metadata.aws.internal/",
            "https://instance-data/latest/meta-data/",
            "http://localhost/feed.xml",
            "http://LOCALHOST/feed.xml"
        )
        for (url in blocked) {
            assertFalse("expected $url to be blocked", UrlSecurityValidator.isUrlSafe(url))
        }
    }

    @Test
    fun `allows ordinary public https urls`() {
        assertTrue(UrlSecurityValidator.isUrlSafe("https://example.com/rss.xml"))
        assertTrue(UrlSecurityValidator.isUrlSafe("https://news.example.co.uk/feed"))
    }

    /**
     * Regression test for a fixed bug: normalizeUrl() used to prepend "https://" to *any*
     * string not already starting with "http://"/"https://", including strings that already
     * declared a different scheme. "ftp://example.com/feed.xml" became
     * "https://ftp://example.com/feed.xml", which OkHttp's HttpUrl parser parsed leniently as
     * host "ftp" with the rest folded into the path - so isUrlSafe() reported these as *safe*
     * despite the class's own "only http/https schemes are allowed" contract. Fixed by having
     * normalizeUrl() detect and reject any other explicit "scheme://" prefix instead of
     * coercing it.
     */
    @Test
    fun `rejects urls with an explicit non-http scheme instead of coercing them to https`() {
        val rejected = listOf(
            "ftp://example.com/feed.xml",
            "javascript://alert(1)",
            "file:///etc/passwd",
            "file://etc/passwd",
            "gopher://example.com/feed"
        )
        for (url in rejected) {
            assertFalse("expected $url to be rejected, not coerced", UrlSecurityValidator.isUrlSafe(url))
            assertNull("normalizeUrl must not coerce $url into an https:// URL", UrlSecurityValidator.normalizeUrl(url))
        }
    }

    @Test
    fun `bare hostnames without any scheme still get https prepended`() {
        // These must keep working: normalizeUrl's job is still to accept a user-typed hostname
        // like "example.com/feed" for a custom feed, distinct from a string with its own scheme.
        assertTrue(UrlSecurityValidator.isUrlSafe("example.com/feed.xml"))
        assertEquals("https://example.com/feed.xml", UrlSecurityValidator.normalizeUrl("example.com/feed.xml"))
    }

    @Test
    fun `rejects single-colon schemes with no authority slashes`() {
        // "mailto:someone@example.com" -> naive https:// prepend gives
        // "https://mailto:someone@example.com", which OkHttp parses as userinfo
        // "mailto:someone" at a perfectly legitimate host "example.com" - a *more* dangerous
        // variant of the ftp:// bug since it doesn't even need the parser to be lenient about
        // an invalid port, it produces a fully valid URL pointing somewhere unrelated to the
        // scheme the caller actually specified. Must be rejected before that mangling happens.
        assertFalse(UrlSecurityValidator.isUrlSafe("mailto:someone@example.com"))
        assertFalse(UrlSecurityValidator.isUrlSafe("data:text/html,<script>alert(1)</script>"))
        assertNull(UrlSecurityValidator.normalizeUrl("mailto:someone@example.com"))
    }

    @Test
    fun `bare host colon port is not mistaken for a scheme`() {
        assertTrue(UrlSecurityValidator.isUrlSafe("example.com:8080/feed.xml"))
        assertEquals("https://example.com:8080/feed.xml", UrlSecurityValidator.normalizeUrl("example.com:8080/feed.xml"))
        // localhost:8080 is still correctly blocked, but for being localhost, not for its port.
        assertFalse(UrlSecurityValidator.isUrlSafe("localhost:8080/feed.xml"))
    }

    @Test
    fun `rejects blank or malformed urls`() {
        assertFalse(UrlSecurityValidator.isUrlSafe(""))
        assertFalse(UrlSecurityValidator.isUrlSafe("   "))
        assertFalse(UrlSecurityValidator.isUrlSafe("not a url"))
    }

    @Test
    fun `requireHttps rejects plain http`() {
        assertFalse(UrlSecurityValidator.isUrlSafe("http://example.com/feed.xml", requireHttps = true))
        assertTrue(UrlSecurityValidator.isUrlSafe("https://example.com/feed.xml", requireHttps = true))
    }

    @Test
    fun `normalizeUrl prepends https when scheme missing`() {
        assertEquals("https://example.com/feed", UrlSecurityValidator.normalizeUrl("example.com/feed"))
        assertEquals("http://example.com/feed", UrlSecurityValidator.normalizeUrl("http://example.com/feed"))
        assertNull(UrlSecurityValidator.normalizeUrl("   "))
        assertNull(UrlSecurityValidator.normalizeUrl(""))
    }

    @Test
    fun `sanitizeUrl returns null for unsafe urls and normalized value for safe ones`() {
        assertNull(UrlSecurityValidator.sanitizeUrl("http://169.254.169.254/"))
        assertEquals("https://example.com/feed", UrlSecurityValidator.sanitizeUrl("example.com/feed"))
    }

    @Test
    fun `normalizeToHttps upgrades scheme and validates result`() {
        assertEquals("https://example.com/a", UrlSecurityValidator.normalizeToHttps("http://example.com/a"))
        assertEquals("https://example.com/a", UrlSecurityValidator.normalizeToHttps("https://example.com/a"))
        // Upgrading a loopback URL to https must still be rejected.
        assertNull(UrlSecurityValidator.normalizeToHttps("http://127.0.0.1/a"))
    }

    @Test
    fun `isHttpsUrl checks scheme prefix only`() {
        assertTrue(UrlSecurityValidator.isHttpsUrl("https://example.com"))
        assertFalse(UrlSecurityValidator.isHttpsUrl("http://example.com"))
        assertFalse(UrlSecurityValidator.isHttpsUrl("example.com"))
    }

    @Test
    fun `isResolvedAddressSafe rejects loopback link-local and site-local addresses`() {
        val loopback = java.net.InetAddress.getByName("127.0.0.1")
        val linkLocal = java.net.InetAddress.getByName("169.254.1.1")
        val siteLocal = java.net.InetAddress.getByName("192.168.1.1")
        val public = java.net.InetAddress.getByName("8.8.8.8")

        assertFalse(UrlSecurityValidator.isResolvedAddressSafe(loopback))
        assertFalse(UrlSecurityValidator.isResolvedAddressSafe(linkLocal))
        assertFalse(UrlSecurityValidator.isResolvedAddressSafe(siteLocal))
        assertTrue(UrlSecurityValidator.isResolvedAddressSafe(public))
    }
}
