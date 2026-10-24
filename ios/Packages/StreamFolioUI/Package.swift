// swift-tools-version: 6.2
import PackageDescription

let package = Package(
    name: "StreamFolioUI",
    platforms: [.iOS("26.0"), .macOS("26.0")],
    products: [
        .library(name: "StreamFolioTokens", targets: ["StreamFolioTokens"]),
        .library(name: "StreamFolioUI", targets: ["StreamFolioUI"]),
    ],
    dependencies: [
        .package(path: "../StreamFolioCore"),
    ],
    targets: [
        // Pure Swift design tokens (no SwiftUI) so parity tests run fast on macOS via `swift test`.
        .target(name: "StreamFolioTokens"),
        .target(
            name: "StreamFolioUI",
            dependencies: ["StreamFolioTokens", .product(name: "StreamFolioCore", package: "StreamFolioCore")]
        ),
        .testTarget(name: "StreamFolioTokensTests", dependencies: ["StreamFolioTokens"]),
    ]
)
