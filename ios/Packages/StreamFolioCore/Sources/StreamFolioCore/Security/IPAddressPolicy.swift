/// Classifies IP addresses as publicly routable or not. Shared by the URL validator (literal hosts)
/// and the fetch layer (addresses a hostname actually resolved to), so there is one definition of
/// "private".
///
/// IPv4 ranges are those blocked by Android's `UrlSecurityValidator`. IPv6 handling is an iOS-side
/// hardening: Android only checks IPv4 literals and `InetAddress` loopback/link-local/site-local
/// after resolution.
public enum IPAddressPolicy {

    /// `true` when the 4 (IPv4) or 16 (IPv6) address bytes are not publicly routable.
    public static func isNonPublic(_ bytes: [UInt8]) -> Bool {
        switch bytes.count {
        case 4: return isNonPublicIPv4(bytes)
        case 16: return isNonPublicIPv6(bytes)
        default: return true   // malformed → treat as unsafe
        }
    }

    public static func isNonPublicIPv4(_ b: [UInt8]) -> Bool {
        guard b.count == 4 else { return true }
        let (a, b1, c) = (Int(b[0]), Int(b[1]), Int(b[2]))
        switch (a, b1, c) {
        case (127, _, _): return true                 // loopback
        case (10, _, _): return true                  // RFC1918
        case (172, 16...31, _): return true           // RFC1918
        case (192, 168, _): return true               // RFC1918
        case (169, 254, _): return true               // link-local / cloud metadata
        case (100, 64...127, _): return true          // carrier-grade NAT
        case (0, _, _): return true                   // "this network"
        case (192, 0, 0): return true                 // IETF protocol assignments
        case (192, 0, 2): return true                 // TEST-NET-1
        case (198, 18...19, _): return true           // benchmarking
        case (198, 51, 100): return true              // TEST-NET-2
        case (203, 0, 113): return true               // TEST-NET-3
        case (240...255, _, _): return true           // reserved + broadcast
        default: return false
        }
    }

    public static func isNonPublicIPv6(_ b: [UInt8]) -> Bool {
        guard b.count == 16 else { return true }

        // ::  and ::1
        if b[0..<15].allSatisfy({ $0 == 0 }) && (b[15] == 0 || b[15] == 1) { return true }

        // ::ffff:a.b.c.d (IPv4-mapped) — judge the embedded IPv4
        if b[0..<10].allSatisfy({ $0 == 0 }) && b[10] == 0xFF && b[11] == 0xFF {
            return isNonPublicIPv4(Array(b[12..<16]))
        }
        // 64:ff9b::/96 (NAT64) — judge the embedded IPv4
        if b[0] == 0x00, b[1] == 0x64, b[2] == 0xFF, b[3] == 0x9B, b[4..<12].allSatisfy({ $0 == 0 }) {
            return isNonPublicIPv4(Array(b[12..<16]))
        }
        // 2002::/16 (6to4) — judge the embedded IPv4
        if b[0] == 0x20, b[1] == 0x02 {
            return isNonPublicIPv4(Array(b[2..<6]))
        }

        if b[0] == 0xFE, (b[1] & 0xC0) == 0x80 { return true }   // fe80::/10 link-local
        if (b[0] & 0xFE) == 0xFC { return true }                  // fc00::/7 unique local
        if b[0] == 0xFF { return true }                           // ff00::/8 multicast
        if b[0] == 0x20, b[1] == 0x01, b[2] == 0x0D, b[3] == 0xB8 { return true } // 2001:db8::/32 documentation
        return false
    }
}
