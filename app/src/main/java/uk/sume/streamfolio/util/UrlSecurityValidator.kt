package uk.sume.streamfolio.util

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.net.InetAddress

/**
 * Validates URLs to prevent SSRF and malicious schemes.
 *
 * - Only http/https schemes are allowed.
 * - Loopback, link-local, site-local and reserved IPv4 ranges are rejected.
 * - Well-known metadata/internal hostnames are rejected.
 * - When [requireHttps] is true, http is rejected (used for custom feeds).
 */
object UrlSecurityValidator {

    private val ALLOWED_SCHEMES = setOf("http", "https")

    private val BLOCKED_HOSTS = setOf(
        "localhost",
        "metadata.google.internal",
        "metadata.azure.internal",
        "metadata.aws.internal",
        "instance-data",
        "169.254.169.254"
    )

    /**
     * Returns true if [url] is safe to fetch. If [requireHttps] is true, only https is accepted.
     */
    fun isUrlSafe(url: String, requireHttps: Boolean = false): Boolean {
        val normalized = normalizeUrl(url) ?: return false
        val httpUrl = normalized.toHttpUrlOrNull() ?: return false

        if (httpUrl.host.isBlank()) return false
        if (requireHttps && httpUrl.scheme != "https") return false
        if (!ALLOWED_SCHEMES.contains(httpUrl.scheme)) return false

        val host = httpUrl.host.lowercase()
        if (BLOCKED_HOSTS.contains(host)) return false
        if (host == "localhost") return false

        if (isLiteralPrivateOrLoopbackIp(host)) return false

        return true
    }

    /**
     * Normalizes and validates a URL, returning the normalized URL or null if unsafe.
     * If [requireHttps] is true, http URLs are rejected.
     */
    fun sanitizeUrl(url: String, requireHttps: Boolean = false): String? {
        val normalized = normalizeUrl(url) ?: return null
        return if (isUrlSafe(normalized, requireHttps)) normalized else null
    }

    // Matches a leading "token:" (RFC 3986 scheme syntax: letter, then letters/digits/+/./-)
    // whether or not it's followed by "//", e.g. "ftp://", "javascript://", "file://",
    // "mailto:", "data:". Also matches a bare "host:port" like "example.com:8080" or
    // "localhost:8080" though, which must NOT be rejected - disambiguated in
    // hasExplicitNonHttpScheme() below by checking what follows the colon.
    private val LEADING_TOKEN_COLON = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:(.*)$", RegexOption.DOT_MATCHES_ALL)

    /**
     * True if [trimmed] starts with an explicit URI scheme other than http/https - as opposed
     * to a bare "host:port" the user typed for a custom feed (e.g. "example.com:8080/feed").
     * A URI scheme's colon is never immediately followed by a run of digits that ends at '/' or
     * end-of-string (that shape is specific to a port number); anything else after the colon -
     * an authority ("//..."), an opaque part ("someone@example.com"), digits with a non-digit
     * tail, etc. - means the leading token was an actual scheme, not a hostname label.
     */
    private fun hasExplicitNonHttpScheme(trimmed: String): Boolean {
        val rest = LEADING_TOKEN_COLON.find(trimmed)?.groupValues?.get(1) ?: return false
        val looksLikePort = rest.takeWhile { it != '/' }.let { it.isNotEmpty() && it.all(Char::isDigit) }
        return !looksLikePort
    }

    /**
     * Prepends https:// if the URL has no scheme. Returns null for blank input, and null for a
     * URL that already declares a non-http(s) scheme (e.g. "ftp://...", "javascript://...",
     * "file://...", "mailto:...") rather than treating it as a bare hostname.
     *
     * A prior version of this function prepended "https://" to *any* string not already
     * starting with "http://"/"https://", including strings with their own explicit scheme.
     * "ftp://example.com/feed.xml" became "https://ftp://example.com/feed.xml", which OkHttp's
     * HttpUrl parser then parsed leniently as host "ftp" with the rest folded into the path;
     * "mailto:someone@example.com" became "https://mailto:someone@example.com", parsed as
     * userinfo "mailto:someone" at a perfectly legitimate host "example.com". Either way
     * isUrlSafe() reported these as "safe" despite the ALLOWED_SCHEMES check below, since by
     * the time that check ran the URL had already been mangled into something with scheme
     * "https". Rejecting any other explicit scheme upfront closes that gap while still allowing
     * bare hostnames - with or without an explicit port - like "example.com/feed" or
     * "example.com:8080/feed" to fall through to the https:// prepend.
     */
    fun normalizeUrl(url: String): String? {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return null
        return when {
            trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            hasExplicitNonHttpScheme(trimmed) -> null
            else -> "https://$trimmed"
        }
    }

    /**
     * Returns true if the URL is https.
     */
    fun isHttpsUrl(url: String): Boolean {
        return url.trim().startsWith("https://", ignoreCase = true)
    }

    /**
     * Upgrades an http URL to https and validates it. Returns null if unsafe.
     * Useful for article links that may historically be http but should now be
     * fetched over HTTPS.
     */
    fun normalizeToHttps(url: String): String? {
        val normalized = normalizeUrl(url) ?: return null
        val upgraded = if (normalized.startsWith("http://", ignoreCase = true)) {
            "https://" + normalized.substring(7)
        } else {
            normalized
        }
        return if (isUrlSafe(upgraded, requireHttps = true)) upgraded else null
    }

    /**
     * Returns true if the resolved IP address is not loopback/link-local/site-local.
     */
    fun isResolvedAddressSafe(address: InetAddress): Boolean {
        return !(address.isLoopbackAddress || address.isLinkLocalAddress || address.isSiteLocalAddress)
    }

    private fun isLiteralPrivateOrLoopbackIp(host: String): Boolean {
        if (!host.matches(Regex("""^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})$"""))) return false

        val octets = host.split(".").map { it.toIntOrNull() ?: return false }
        if (octets.any { it !in 0..255 }) return false

        val (a, b, c, d) = octets
        return when {
            a == 127 -> true
            a == 10 -> true
            a == 172 && b in 16..31 -> true
            a == 192 && b == 168 -> true
            a == 169 && b == 254 -> true
            a == 100 && b in 64..127 -> true
            a == 0 -> true
            a == 192 && b == 0 && c == 0 -> true
            a == 192 && b == 0 && c == 2 -> true
            a == 198 && (b == 18 || b == 19) -> true
            a == 198 && b == 51 && c == 100 -> true
            a == 203 && b == 0 && c == 113 -> true
            a in 240..255 -> true
            else -> false
        }
    }
}
