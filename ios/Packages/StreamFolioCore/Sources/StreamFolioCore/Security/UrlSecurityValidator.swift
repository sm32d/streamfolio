import Foundation

/// Validates URLs to prevent SSRF and malicious schemes. Port of Android's `UrlSecurityValidator`.
///
/// - Only http/https schemes are allowed.
/// - Loopback, link-local, site-local and reserved IP ranges are rejected (IPv4 and, on iOS, IPv6).
/// - Well-known metadata/internal hostnames are rejected.
/// - When `requireHttps` is true, http is rejected (used for custom feeds).
///
/// Hardening over Android (documented in `docs/06`): trailing-dot and `*.localhost` names,
/// non-canonical numeric hosts (`2130706433`, `127.1`, `0x7f.1`) which `getaddrinfo` would accept
/// as an IP, and IPv6 literals.
public enum UrlSecurityValidator {

    private static let allowedSchemes: Set<String> = ["http", "https"]

    private static let blockedHosts: Set<String> = [
        "localhost",
        "metadata.google.internal",
        "metadata.azure.internal",
        "metadata.aws.internal",
        "instance-data",
        "169.254.169.254",
    ]

    /// `true` if `url` is safe to fetch. If `requireHttps` is true, only https is accepted.
    public static func isUrlSafe(_ url: String, requireHttps: Bool = false) -> Bool {
        guard let normalized = normalizeUrl(url),
              let components = URLComponents(string: normalized),
              let scheme = components.scheme?.lowercased(),
              let rawHost = components.host, !rawHost.trimmingCharacters(in: .whitespaces).isEmpty
        else { return false }

        if requireHttps && scheme != "https" { return false }
        guard allowedSchemes.contains(scheme) else { return false }

        let host = canonicalHost(rawHost)
        if host.isEmpty || blockedHosts.contains(host) { return false }
        if host == "localhost" || host.hasSuffix(".localhost") { return false }
        if isNonPublicLiteral(host) { return false }
        return true
    }

    /// Normalizes and validates a URL, returning the normalized URL or nil if unsafe.
    public static func sanitizeUrl(_ url: String, requireHttps: Bool = false) -> String? {
        guard let normalized = normalizeUrl(url) else { return nil }
        return isUrlSafe(normalized, requireHttps: requireHttps) ? normalized : nil
    }

    /// Prepends `https://` if the URL has no scheme. Returns nil for blank input and for URLs that
    /// carry an explicit non-http(s) scheme (`ftp://…`, `file://…`), which Android would have turned
    /// into a bogus `https://ftp//…` request.
    public static func normalizeUrl(_ url: String) -> String? {
        let trimmed = url.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty { return nil }
        let lower = trimmed.lowercased()
        if lower.hasPrefix("http://") || lower.hasPrefix("https://") { return trimmed }
        if lower.range(of: #"^[a-z][a-z0-9+.\-]*://"#, options: .regularExpression) != nil { return nil }
        return "https://\(trimmed)"
    }

    public static func isHttpsUrl(_ url: String) -> Bool {
        url.trimmingCharacters(in: .whitespacesAndNewlines).lowercased().hasPrefix("https://")
    }

    /// Upgrades an http URL to https and validates it. Returns nil if unsafe. Article links may
    /// historically be http but are fetched over HTTPS.
    public static func normalizeToHttps(_ url: String) -> String? {
        guard let normalized = normalizeUrl(url) else { return nil }
        let upgraded = normalized.lowercased().hasPrefix("http://")
            ? "https://" + normalized.dropFirst("http://".count)
            : normalized
        return isUrlSafe(upgraded, requireHttps: true) ? upgraded : nil
    }

    /// `true` if an address a hostname resolved to (4 or 16 bytes) is publicly routable.
    public static func isResolvedAddressSafe(_ bytes: [UInt8]) -> Bool {
        !IPAddressPolicy.isNonPublic(bytes)
    }

    // MARK: Host classification

    /// Lowercase, brackets and trailing dots removed.
    private static func canonicalHost(_ raw: String) -> String {
        var host = raw.lowercased().trimmingCharacters(in: .whitespaces)
        if host.hasPrefix("[") && host.hasSuffix("]") { host = String(host.dropFirst().dropLast()) }
        while host.hasSuffix(".") { host.removeLast() }
        return host
    }

    private static func isNonPublicLiteral(_ host: String) -> Bool {
        if host.contains(":") {
            guard let bytes = parseIPv6(host) else { return true }   // unparsable IPv6 literal
            return IPAddressPolicy.isNonPublicIPv6(bytes)
        }
        if let octets = canonicalIPv4(host) {
            return IPAddressPolicy.isNonPublicIPv4(octets)
        }
        // Digits/dots only, or a 0x component, but not a canonical dotted quad: ambiguous
        // encodings that the OS resolver would turn into an IP address. Reject outright.
        if isAmbiguousNumericHost(host) { return true }
        return false
    }

    /// Strict dotted-quad: four decimal octets 0...255 with no leading zeros beyond a single "0".
    private static func canonicalIPv4(_ host: String) -> [UInt8]? {
        let parts = host.split(separator: ".", omittingEmptySubsequences: false)
        guard parts.count == 4 else { return nil }
        var octets: [UInt8] = []
        for part in parts {
            guard !part.isEmpty, part.allSatisfy({ $0.isASCII && $0.isNumber }),
                  part.count == 1 || part.first != "0",
                  let value = UInt8(part) else { return nil }
            octets.append(value)
        }
        return octets
    }

    private static func isAmbiguousNumericHost(_ host: String) -> Bool {
        if host.allSatisfy({ $0.isASCII && ($0.isNumber || $0 == ".") }) { return true }
        return host.split(separator: ".").contains { $0.hasPrefix("0x") }
    }

    private static func parseIPv6(_ host: String) -> [UInt8]? {
        // Drop a zone id (fe80::1%en0).
        let address = host.split(separator: "%", maxSplits: 1).first.map(String.init) ?? host
        var storage = in6_addr()
        guard inet_pton(AF_INET6, address, &storage) == 1 else { return nil }
        return withUnsafeBytes(of: &storage) { Array($0) }
    }
}
