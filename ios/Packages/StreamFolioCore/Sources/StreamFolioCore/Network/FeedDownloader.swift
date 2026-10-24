import Foundation
import os

/// Downloads feed documents. The single place that decides whether a feed may be fetched at all:
/// ordinary feeds are fetched straight away, while link-only feeds follow the publisher's agreed
/// terms (remote source list, 30-minute floor, conditional requests, server back-off instructions).
public actor FeedDownloader {
    /// Chrome-on-Android is what Android sends; publishers commonly block unknown agents.
    public static let browserUserAgent =
        "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1"

    private let http: any HTTPFetching
    private let state: LinkOnlyStateStore
    private let store: ArticleStore
    private let now: @Sendable () -> Int64
    private let linkOnlyUserAgent: String
    private let log = Logger(subsystem: "uk.sume.streamfolio", category: "FeedDownloader")

    private var sourceListRefresh: Task<Void, Never>?

    public init(
        http: any HTTPFetching,
        state: LinkOnlyStateStore,
        store: ArticleStore,
        appVersion: String,
        now: @escaping @Sendable () -> Int64 = { Int64(Date().timeIntervalSince1970 * 1000) }
    ) {
        self.http = http
        self.state = state
        self.store = store
        self.now = now
        // Identifies StreamFolio to link-only publishers so they can recognise (or block) the traffic.
        self.linkOnlyUserAgent = "StreamFolio/\(appVersion) (iOS feed reader; headline-and-link only)"
    }

    // MARK: Source list

    /// Downloads the remote source list and applies it: link-only sources that are missing or
    /// disabled are no longer fetched and everything cached from them is deleted. Called at the start
    /// of every refresh, before any link-only feed is requested. A download validated within
    /// `SourceConfig.freshnessMs` is reused. If the file cannot be fetched or parsed it is not
    /// validated, so link-only fetching stays suspended (`LinkOnlyStateStore.mayFetch`).
    /// Concurrent callers share one download.
    public func refreshSourceListIfDue(force: Bool = false) async {
        if let inFlight = sourceListRefresh {
            await inFlight.value
            return
        }
        let current = now()
        let last = state.configCheckedAt
        if !force && last != 0 && current >= last && current - last < SourceConfig.freshnessMs { return }

        let task = Task { await self.downloadSourceList(at: current) }
        sourceListRefresh = task
        await task.value
        sourceListRefresh = nil
    }

    private func downloadSourceList(at timestamp: Int64) async {
        var request = URLRequest(url: SourceConfig.url)
        request.setValue(linkOnlyUserAgent, forHTTPHeaderField: "User-Agent")
        request.setValue("no-cache", forHTTPHeaderField: "Cache-Control")
        request.cachePolicy = .reloadIgnoringLocalCacheData
        do {
            let result = try await http.fetch(request)
            guard result.isSuccessful else {
                log.warning("Source list unavailable (HTTP \(result.response.statusCode)); link-only fetching suspended until it is validated")
                return
            }
            guard let enabled = SourceConfig.parseEnabledIds(result.data) else {
                log.warning("Source list unreadable; link-only fetching suspended until it is validated")
                return
            }
            state.enabledIds = enabled
            state.configCheckedAt = timestamp
            for source in LinkOnlySources.all where !enabled.contains(source.id) {
                await purge(source)
            }
        } catch {
            log.warning("Could not refresh source list; link-only fetching suspended until it is validated")
        }
    }

    /// Stops fetching `source` and removes everything cached from it on this device.
    private func purge(_ source: LinkOnlySource) async {
        for pattern in LinkOnlySources.likePatterns(for: source) {
            try? await store.deleteMatching(pattern: pattern)
        }
        state.clearFeedState()
    }

    // MARK: Feeds

    /// The feed document, or nil when it must not be (re)fetched now, has not changed, or failed.
    public func feedData(for urlString: String) async -> Data? {
        guard let url = URL(string: urlString) else { return nil }
        if LinkOnlySources.isLinkOnly(url: urlString) { return await linkOnlyFeedData(url) }

        var request = URLRequest(url: url)
        request.setValue(Self.browserUserAgent, forHTTPHeaderField: "User-Agent")
        request.setValue("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8", forHTTPHeaderField: "Accept")
        request.setValue("en-US,en;q=0.9", forHTTPHeaderField: "Accept-Language")
        do {
            let result = try await http.fetch(request)
            return result.isSuccessful ? result.data : nil
        } catch {
            log.error("Feed fetch failed for \(url.host() ?? "[invalid]", privacy: .public): \(String(describing: error), privacy: .public)")
            return nil
        }
    }

    private func linkOnlyFeedData(_ url: URL) async -> Data? {
        let key = url.absoluteString
        let current = now()
        // Only with a source list validated just now that enables this source.
        guard state.mayFetch(key, now: current) else { return nil }
        let nextAllowed = state.nextFetchAt(key)
        if current < nextAllowed && nextAllowed - current <= LinkOnlyFetchPolicy.maxWait { return nil }

        // Reserve the slot *before* suspending: actors are re-entrant, and two refreshes must never
        // both pass the 30-minute check. If the request never produces a response, the old value is
        // restored (Android likewise does not throttle on a transport error).
        state.setNextFetchAt(LinkOnlyFetchPolicy.nextFetchAfterFailure(now: current), feedUrl: key)

        var request = URLRequest(url: url)
        request.setValue(linkOnlyUserAgent, forHTTPHeaderField: "User-Agent")
        request.setValue("application/rss+xml, application/atom+xml, application/xml;q=0.9, text/xml;q=0.8", forHTTPHeaderField: "Accept")
        if let etag = state.etag(key) { request.setValue(etag, forHTTPHeaderField: "If-None-Match") }
        if let modified = state.lastModified(key) { request.setValue(modified, forHTTPHeaderField: "If-Modified-Since") }
        request.cachePolicy = .reloadIgnoringLocalCacheData   // conditional headers are ours, not URLCache's

        let result: HTTPResult
        do {
            result = try await http.fetch(request)
        } catch {
            state.setNextFetchAt(nextAllowed, feedUrl: key)   // no response: do not throttle
            return nil
        }

        let response = result.response
        let cacheControl = response.value(forHTTPHeaderField: "Cache-Control")
        switch response.statusCode {
        case 304:
            state.setNextFetchAt(
                LinkOnlyFetchPolicy.nextFetchAfterNotModified(now: current, cacheControl: cacheControl, storedTtlMs: state.ttlMs(key)),
                feedUrl: key
            )
            return nil
        case 429, 503:
            state.setNextFetchAt(LinkOnlyFetchPolicy.nextFetchAfterBackoff(now: current, retryAfter: response.value(forHTTPHeaderField: "Retry-After")), feedUrl: key)
            return nil
        case 404, 410:
            state.setNextFetchAt(LinkOnlyFetchPolicy.nextFetchAfterGone(now: current), feedUrl: key)
            return nil
        default:
            guard result.isSuccessful else {
                state.setNextFetchAt(LinkOnlyFetchPolicy.nextFetchAfterFailure(now: current), feedUrl: key)
                return nil
            }
        }
        state.saveValidators(etag: response.value(forHTTPHeaderField: "ETag"), lastModified: response.value(forHTTPHeaderField: "Last-Modified"), feedUrl: key)
        let ttl = LinkOnlyFetchPolicy.parseTtlMs(String(decoding: result.data, as: UTF8.self))
        state.setTtlMs(ttl, feedUrl: key)
        state.setNextFetchAt(LinkOnlyFetchPolicy.nextFetchAfterSuccess(now: current, cacheControl: cacheControl, ttlMs: ttl), feedUrl: key)
        return result.data
    }
}
