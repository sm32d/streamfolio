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

    /**
     * Prepends https:// if the URL has no scheme. Returns null for blank input.
     */
    fun normalizeUrl(url: String): String? {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return null
        return if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            trimmed
        } else {
            "https://$trimmed"
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
