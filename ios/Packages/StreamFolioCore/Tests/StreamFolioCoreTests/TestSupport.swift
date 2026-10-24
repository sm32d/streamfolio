import Foundation
@testable import StreamFolioCore

/// Canned HTTP for offline tests. Records every request URL.
final class StubHTTP: HTTPFetching, @unchecked Sendable {
    struct Reply {
        var status = 200
        var body = Data()
        var headers: [String: String] = [:]
        var error: Error?
    }

    private let lock = NSLock()
    private var handlers: [(prefix: String, reply: @Sendable (URLRequest) -> Reply)] = []
    private var _requests: [URLRequest] = []

    var requests: [URLRequest] { lock.withLock { _requests } }
    func requestCount(prefix: String) -> Int { requests.filter { $0.url!.absoluteString.hasPrefix(prefix) }.count }

    func on(_ prefix: String, _ reply: @escaping @Sendable (URLRequest) -> Reply) {
        lock.withLock { handlers.insert((prefix, reply), at: 0) }   // newest registration wins
    }

    func on(_ prefix: String, status: Int = 200, body: String = "", headers: [String: String] = [:]) {
        on(prefix) { _ in Reply(status: status, body: Data(body.utf8), headers: headers) }
    }

    func fetch(_ request: URLRequest) async throws -> HTTPResult {
        let handler: (@Sendable (URLRequest) -> Reply)? = lock.withLock {
            _requests.append(request)
            return handlers.first { request.url!.absoluteString.hasPrefix($0.prefix) }?.reply
        }
        guard let reply = handler?(request) else { throw URLError(.cannotFindHost) }
        if let error = reply.error { throw error }
        let response = HTTPURLResponse(url: request.url!, statusCode: reply.status, httpVersion: "HTTP/1.1", headerFields: reply.headers)!
        return HTTPResult(data: reply.body, response: response)
    }
}

/// A controllable clock (milliseconds).
final class TestClock: @unchecked Sendable {
    private let lock = NSLock()
    private var value: Int64
    init(_ start: Int64 = 1_800_000_000_000) { value = start }
    var now: Int64 { lock.withLock { value } }
    func advance(minutes: Int64) { lock.withLock { value += minutes * 60_000 } }
    func advance(seconds: Int64) { lock.withLock { value += seconds * 1000 } }
}

func makeArticle(
    _ link: String, title: String = "Title", category: String = "World", pubDate: String = "2026-10-24T09:00:00.000Z",
    sourceUrl: String = "https://example.com/feed", groupId: String? = nil
) -> Article {
    Article(link: link, title: title, description: "d", pubDate: pubDate, sourceName: "Example", sourceUrl: sourceUrl, category: category, groupId: groupId)
}

func isolatedDefaults() -> UserDefaults {
    let name = "test-\(UUID().uuidString)"
    let defaults = UserDefaults(suiteName: name)!
    defaults.removePersistentDomain(forName: name)
    return defaults
}
