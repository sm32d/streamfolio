// swift-tools-version: 6.2
import PackageDescription

let package = Package(
    name: "StreamFolioCore",
    platforms: [.iOS("26.0"), .macOS("26.0")],
    products: [
        .library(name: "StreamFolioCore", targets: ["StreamFolioCore"]),
    ],
    dependencies: [
        .package(url: "https://github.com/groue/GRDB.swift", exact: "7.11.1"),
        .package(url: "https://github.com/scinfu/SwiftSoup", exact: "2.14.3"),
    ],
    targets: [
        // Models, persistence, parsing, networking policy and backup. No UI imports, so everything
        // here runs under `swift test` on macOS.
        .target(
            name: "StreamFolioCore",
            dependencies: [
                .product(name: "GRDB", package: "GRDB.swift"),
                .product(name: "SwiftSoup", package: "SwiftSoup"),
            ],
            resources: [.process("Resources")]
        ),
        .testTarget(
            name: "StreamFolioCoreTests",
            dependencies: ["StreamFolioCore"]
        ),
    ]
)
