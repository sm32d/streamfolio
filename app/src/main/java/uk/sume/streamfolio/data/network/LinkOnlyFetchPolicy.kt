package uk.sume.streamfolio.data.network

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Pure helpers deciding how soon a link-only feed may be fetched again.
 *
 * The device never fetches a link-only feed more than once per [MIN_INTERVAL_MS], and obeys any
 * longer instruction from the publisher's server (Cache-Control max-age, Retry-After, feed `ttl`)
 * in full. There is deliberately no 24-hour ceiling: valid server intervals are honoured as sent.
 * [MAX_WAIT_MS] (one year) exists only to stop arithmetic overflow and obviously corrupt values.
 */
object LinkOnlyFetchPolicy {
    const val MIN_INTERVAL_MS = 30L * 60L * 1000L
    /** Back-off when the server sends 429/503 without a usable Retry-After. */
    const val DEFAULT_BACKOFF_MS = 60L * 60L * 1000L
    /** Back-off after 404/410: the feed has moved or been withdrawn. */
    const val GONE_BACKOFF_MS = 24L * 60L * 60L * 1000L
    /** Sanity bound (one year) against overflow and corrupt stored values, not a policy limit. */
    const val MAX_WAIT_MS = 365L * 24L * 60L * 60L * 1000L

    private val maxAgeRegex = Regex("max-age\\s*=\\s*(\\d+)", RegexOption.IGNORE_CASE)
    private val ttlRegex = Regex("<ttl>\\s*(\\d+)\\s*</ttl>", RegexOption.IGNORE_CASE)

    private fun secondsToMs(seconds: Long): Long = seconds.coerceIn(0L, MAX_WAIT_MS / 1000L) * 1000L

    fun parseMaxAgeMs(cacheControl: String?): Long {
        val seconds = cacheControl?.let { maxAgeRegex.find(it)?.groupValues?.get(1)?.toLongOrNull() } ?: return 0L
        return secondsToMs(seconds)
    }

    fun parseTtlMs(xml: String): Long {
        val head = if (xml.length > 4096) xml.substring(0, 4096) else xml
        val minutes = ttlRegex.find(head)?.groupValues?.get(1)?.toLongOrNull() ?: return 0L
        return secondsToMs(minutes.coerceAtMost(Long.MAX_VALUE / 60L) * 60L)
    }

    /** Retry-After is either delta-seconds or an HTTP-date. Returns 0 when absent or unparsable. */
    fun parseRetryAfterMs(retryAfter: String?, nowMs: Long): Long {
        val value = retryAfter?.trim().orEmpty()
        if (value.isEmpty()) return 0L
        value.toLongOrNull()?.let { return secondsToMs(it) }
        return try {
            val fmt = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("GMT")
            }
            val target = fmt.parse(value)?.time ?: return 0L
            (target - nowMs).coerceIn(0L, MAX_WAIT_MS)
        } catch (_: Exception) {
            0L
        }
    }

    /**
     * After a 200 response. [ttlMs] is the feed's `<ttl>` from this response (0 if none).
     */
    fun nextFetchAfterSuccess(nowMs: Long, cacheControl: String?, ttlMs: Long): Long {
        val wait = maxOf(MIN_INTERVAL_MS, parseMaxAgeMs(cacheControl), ttlMs.coerceIn(0L, MAX_WAIT_MS))
        return nowMs + wait
    }

    /**
     * After a 304. The response has no body, so the feed's `<ttl>` from the last 200 response
     * ([storedTtlMs]) must still be honoured.
     */
    fun nextFetchAfterNotModified(nowMs: Long, cacheControl: String?, storedTtlMs: Long): Long {
        val wait = maxOf(MIN_INTERVAL_MS, parseMaxAgeMs(cacheControl), storedTtlMs.coerceIn(0L, MAX_WAIT_MS))
        return nowMs + wait
    }

    fun nextFetchAfterBackoff(nowMs: Long, retryAfter: String?): Long {
        val wait = maxOf(DEFAULT_BACKOFF_MS, parseRetryAfterMs(retryAfter, nowMs))
        return nowMs + wait
    }

    fun nextFetchAfterGone(nowMs: Long): Long = nowMs + GONE_BACKOFF_MS

    /** Generic failure (5xx, timeouts): wait the normal minimum before trying again. */
    fun nextFetchAfterFailure(nowMs: Long): Long = nowMs + MIN_INTERVAL_MS
}
