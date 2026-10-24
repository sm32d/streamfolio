import Foundation

/// Pure rules deciding how soon a link-only feed may be fetched again (port of `LinkOnlyFetchPolicy`).
///
/// The device never fetches a link-only feed more than once per `minInterval`, and obeys any longer
/// instruction from the publisher's server (Cache-Control max-age, Retry-After, feed `<ttl>`) in
/// full. There is deliberately no 24-hour ceiling: valid server intervals are honoured as sent.
/// `maxWait` (one year) exists only to stop arithmetic overflow and obviously corrupt values.
/// All times are milliseconds. These are terms agreed with the publisher; see ADR-006.
public enum LinkOnlyFetchPolicy {
    public static let minInterval: Int64 = 30 * 60 * 1000
    /// Back-off when the server sends 429/503 without a usable Retry-After.
    public static let defaultBackoff: Int64 = 60 * 60 * 1000
    /// Back-off after 404/410: the feed has moved or been withdrawn.
    public static let goneBackoff: Int64 = 24 * 60 * 60 * 1000
    /// Sanity bound (one year) against overflow and corrupt stored values, not a policy limit.
    public static let maxWait: Int64 = 365 * 24 * 60 * 60 * 1000

    private static func secondsToMs(_ seconds: Int64) -> Int64 {
        min(max(seconds, 0), maxWait / 1000) * 1000
    }

    public static func parseMaxAgeMs(_ cacheControl: String?) -> Int64 {
        guard let cacheControl,
              let match = cacheControl.firstMatch(of: /(?i)max-age\s*=\s*(\d+)/),
              let seconds = Int64(match.output.1) else { return 0 }
        return secondsToMs(seconds)
    }

    /// `<ttl>` is in minutes, read from the first 4 KB of the feed.
    public static func parseTtlMs(_ xml: String) -> Int64 {
        let head = xml.prefix(4096)
        guard let match = head.firstMatch(of: /(?i)<ttl>\s*(\d+)\s*<\/ttl>/),
              let minutes = Int64(match.output.1) else { return 0 }
        return secondsToMs(min(minutes, Int64.max / 60) * 60)
    }

    /// Retry-After is either delta-seconds or an HTTP-date. 0 when absent or unparsable.
    public static func parseRetryAfterMs(_ retryAfter: String?, now: Int64) -> Int64 {
        let value = retryAfter?.trimmed ?? ""
        if value.isEmpty { return 0 }
        if let seconds = Int64(value) { return secondsToMs(seconds) }
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = TimeZone(identifier: "GMT")
        formatter.dateFormat = "EEE, dd MMM yyyy HH:mm:ss zzz"
        guard let date = formatter.date(from: value) else { return 0 }
        let target = Int64(date.timeIntervalSince1970 * 1000)
        return min(max(target - now, 0), maxWait)
    }

    /// After a 200 response. `ttlMs` is the feed's `<ttl>` from this response (0 if none).
    public static func nextFetchAfterSuccess(now: Int64, cacheControl: String?, ttlMs: Int64) -> Int64 {
        now + max(minInterval, parseMaxAgeMs(cacheControl), min(max(ttlMs, 0), maxWait))
    }

    /// After a 304. No body, so the `<ttl>` from the last 200 response must still be honoured.
    public static func nextFetchAfterNotModified(now: Int64, cacheControl: String?, storedTtlMs: Int64) -> Int64 {
        now + max(minInterval, parseMaxAgeMs(cacheControl), min(max(storedTtlMs, 0), maxWait))
    }

    public static func nextFetchAfterBackoff(now: Int64, retryAfter: String?) -> Int64 {
        now + max(defaultBackoff, parseRetryAfterMs(retryAfter, now: now))
    }

    public static func nextFetchAfterGone(now: Int64) -> Int64 { now + goneBackoff }

    /// Generic failure (5xx): wait the normal minimum before trying again.
    public static func nextFetchAfterFailure(now: Int64) -> Int64 { now + minInterval }
}
