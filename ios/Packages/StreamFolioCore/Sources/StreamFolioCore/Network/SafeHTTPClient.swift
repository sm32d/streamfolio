import Foundation

/// Fetches a request and returns the body, or throws. The seam between the repository and the network.
public protocol HTTPFetching: Sendable {
    func fetch(_ request: URLRequest) async throws -> HTTPResult
}

public struct HTTPResult: Sendable {
    public let data: Data
    public let response: HTTPURLResponse

    /// 2xx. Other statuses are returned (not thrown) so callers can read headers such as `Retry-After`.
    public var isSuccessful: Bool { (200..<300).contains(response.statusCode) }

    public init(data: Data, response: HTTPURLResponse) {
        self.data = data
        self.response = response
    }
}

public enum HTTPFetchError: Error, Equatable {
    case unsafeURL
    case blockedAddress
    case tooLarge
    case notHTTP
}

/// Resolves a hostname to raw address bytes (4 or 16 each).
public protocol HostResolving: Sendable {
    func addresses(for host: String) async -> [[UInt8]]
}

public struct SystemHostResolver: HostResolving {
    public init() {}

    public func addresses(for host: String) async -> [[UInt8]] {
        await Task.detached(priority: .utility) {
            var hints = addrinfo(ai_flags: 0, ai_family: AF_UNSPEC, ai_socktype: SOCK_STREAM, ai_protocol: 0,
                                 ai_addrlen: 0, ai_canonname: nil, ai_addr: nil, ai_next: nil)
            var result: UnsafeMutablePointer<addrinfo>?
            guard getaddrinfo(host, nil, &hints, &result) == 0, let first = result else { return [] }
            defer { freeaddrinfo(result) }
            var found: [[UInt8]] = []
            var cursor: UnsafeMutablePointer<addrinfo>? = first
            while let info = cursor?.pointee {
                if let sockaddr = info.ai_addr {
                    switch Int32(sockaddr.pointee.sa_family) {
                    case AF_INET:
                        sockaddr.withMemoryRebound(to: sockaddr_in.self, capacity: 1) { pointer in
                            found.append(withUnsafeBytes(of: pointer.pointee.sin_addr) { Array($0) })
                        }
                    case AF_INET6:
                        sockaddr.withMemoryRebound(to: sockaddr_in6.self, capacity: 1) { pointer in
                            found.append(withUnsafeBytes(of: pointer.pointee.sin6_addr) { Array($0) })
                        }
                    default: break
                    }
                }
                cursor = info.ai_next
            }
            return found
        }.value
    }
}

/// `URLSession` client with the SSRF protections Android implements in `SsrfProtectionInterceptor`:
///
/// 1. the URL must pass `UrlSecurityValidator` (scheme, blocked hosts, private IP literals);
/// 2. the hostname is resolved first and refused if *any* address is non-public;
/// 3. every redirect target is validated the same way before it is followed;
/// 4. after the connection, the address actually connected to (`URLSessionTaskMetrics`) is checked
///    and the response is discarded if it was non-public.
///
/// Residual gap vs Android: `URLSession` resolves the name again when it connects, so a DNS server
/// that answers differently between step 2 and the connection (rebinding) can still make one request
/// leave the device; step 4 stops the response from being used. Documented in `docs/06`.
public final class SafeHTTPClient: HTTPFetching {
    private let session: URLSession
    private let resolver: any HostResolving
    private let maxBytes: Int

    public init(
        resolver: any HostResolving = SystemHostResolver(),
        timeout: TimeInterval = 10,
        maxBytes: Int = 20_000_000,
        configuration: URLSessionConfiguration = .ephemeral
    ) {
        configuration.timeoutIntervalForRequest = timeout
        configuration.timeoutIntervalForResource = timeout * 3
        configuration.httpCookieStorage = nil
        configuration.httpShouldSetCookies = false
        configuration.waitsForConnectivity = false
        self.session = URLSession(configuration: configuration)
        self.resolver = resolver
        self.maxBytes = maxBytes
    }

    public func fetch(_ request: URLRequest) async throws -> HTTPResult {
        guard let url = request.url, try await isSafe(url) else { throw HTTPFetchError.unsafeURL }

        let guardDelegate = ConnectionGuard(resolver: resolver)
        let (data, response) = try await session.data(for: request, delegate: guardDelegate)

        if guardDelegate.connectedToNonPublicAddress { throw HTTPFetchError.blockedAddress }
        guard let http = response as? HTTPURLResponse else { throw HTTPFetchError.notHTTP }
        guard data.count <= maxBytes else { throw HTTPFetchError.tooLarge }
        return HTTPResult(data: data, response: http)
    }

    private func isSafe(_ url: URL) async throws -> Bool {
        try await Self.isSafe(url, resolver: resolver)
    }

    fileprivate static func isSafe(_ url: URL, resolver: any HostResolving) async throws -> Bool {
        guard UrlSecurityValidator.isUrlSafe(url.absoluteString), let host = url.host else { return false }
        // IP literals were fully judged by the validator.
        if host.contains(":") || host.allSatisfy({ $0.isNumber || $0 == "." }) { return true }
        let addresses = await resolver.addresses(for: host)
        guard !addresses.isEmpty else { return true }   // let URLSession report the DNS failure
        return addresses.allSatisfy { UrlSecurityValidator.isResolvedAddressSafe($0) }
    }
}

/// Per-request delegate: validates redirects and inspects the connected address.
private final class ConnectionGuard: NSObject, URLSessionTaskDelegate, @unchecked Sendable {
    private let resolver: any HostResolving
    private let lock = NSLock()
    private var violation = false

    init(resolver: any HostResolving) { self.resolver = resolver }

    var connectedToNonPublicAddress: Bool { lock.withLock { violation } }

    func urlSession(
        _ session: URLSession, task: URLSessionTask,
        willPerformHTTPRedirection response: HTTPURLResponse, newRequest request: URLRequest
    ) async -> URLRequest? {
        guard let url = request.url, (try? await SafeHTTPClient.isSafe(url, resolver: resolver)) == true else { return nil }
        return request
    }

    func urlSession(_ session: URLSession, task: URLSessionTask, didFinishCollecting metrics: URLSessionTaskMetrics) {
        for transaction in metrics.transactionMetrics {
            guard let remote = transaction.remoteAddress, let bytes = Self.bytes(of: remote) else { continue }
            if !UrlSecurityValidator.isResolvedAddressSafe(bytes) {
                lock.withLock { violation = true }
            }
        }
    }

    /// `remoteAddress` is a textual IPv4/IPv6 address.
    private static func bytes(of text: String) -> [UInt8]? {
        let address = text.split(separator: "%", maxSplits: 1).first.map(String.init) ?? text
        var v4 = in_addr()
        if inet_pton(AF_INET, address, &v4) == 1 { return withUnsafeBytes(of: &v4) { Array($0) } }
        var v6 = in6_addr()
        if inet_pton(AF_INET6, address, &v6) == 1 { return withUnsafeBytes(of: &v6) { Array($0) } }
        return nil
    }
}
