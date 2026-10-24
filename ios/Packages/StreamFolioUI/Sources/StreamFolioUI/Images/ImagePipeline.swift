#if canImport(UIKit)
import SwiftUI
import UIKit
import ImageIO
import StreamFolioCore

/// Loads, caches and downsamples remote images (the Coil replacement).
///
/// - Goes through `SafeHTTPClient`, so images get the same SSRF protection as feeds.
/// - Sends the same browser User-Agent as feeds (some publishers refuse the default agent).
/// - 25 MB memory + 100 MB disk HTTP cache, plus a decoded-image `NSCache` keyed by size.
/// - Never requests anything from a link-only publisher (publisher terms: no images). This is the one
///   place that rule is enforced for images, so no view has to remember it.
public actor ImagePipeline {
    public static let shared = ImagePipeline()

    private let client: SafeHTTPClient
    private let cache: NSCache<NSString, UIImage> = {
        let cache = NSCache<NSString, UIImage>()
        cache.totalCostLimit = 64 * 1024 * 1024
        return cache
    }()
    private var inFlight: [String: Task<UIImage?, Never>] = [:]

    public init() {
        let configuration = URLSessionConfiguration.ephemeral
        configuration.urlCache = URLCache(memoryCapacity: 25 * 1024 * 1024, diskCapacity: 100 * 1024 * 1024)
        configuration.requestCachePolicy = .returnCacheDataElseLoad
        client = SafeHTTPClient(timeout: 15, maxBytes: 15_000_000, configuration: configuration)
    }

    /// The image at `url`, decoded no larger than `maxPixelSize` on its longest side.
    public func image(for url: URL, maxPixelSize: CGFloat) async -> UIImage? {
        guard !LinkOnlySources.isLinkOnly(url: url.absoluteString) else { return nil }
        let key = "\(url.absoluteString)#\(Int(maxPixelSize))"
        if let hit = cache.object(forKey: key as NSString) { return hit }
        if let running = inFlight[key] { return await running.value }

        let client = self.client
        let task = Task.detached(priority: .userInitiated) { () -> UIImage? in
            var request = URLRequest(url: url)
            request.setValue(FeedDownloader.browserUserAgent, forHTTPHeaderField: "User-Agent")
            request.setValue("image/webp,image/apng,image/*,*/*;q=0.8", forHTTPHeaderField: "Accept")
            guard let result = try? await client.fetch(request), result.isSuccessful else { return nil }
            return Self.downsample(result.data, maxPixelSize: maxPixelSize)
        }
        inFlight[key] = task
        let image = await task.value
        inFlight[key] = nil
        if let image { cache.setObject(image, forKey: key as NSString, cost: Int(image.size.width * image.size.height * 4 * image.scale * image.scale)) }
        return image
    }

    private static func downsample(_ data: Data, maxPixelSize: CGFloat) -> UIImage? {
        let options: [CFString: Any] = [kCGImageSourceShouldCache: false]
        guard let source = CGImageSourceCreateWithData(data as CFData, options as CFDictionary) else { return nil }
        let thumbnailOptions: [CFString: Any] = [
            kCGImageSourceCreateThumbnailFromImageAlways: true,
            kCGImageSourceShouldCacheImmediately: true,
            kCGImageSourceCreateThumbnailWithTransform: true,
            kCGImageSourceThumbnailMaxPixelSize: maxPixelSize,
        ]
        guard let cgImage = CGImageSourceCreateThumbnailAtIndex(source, 0, thumbnailOptions as CFDictionary) else { return nil }
        return UIImage(cgImage: cgImage)
    }
}

/// Remote image that fades in over a brand-gradient placeholder.
public struct SFRemoteImage: View {
    private let url: URL?
    private let maxPixelSize: CGFloat
    @State private var image: UIImage?
    @Environment(\.displayScale) private var displayScale

    /// - Parameter maxPixelSize: longest side in *points*; scaled by the display scale when decoding.
    public init(url: URL?, maxPointSize: CGFloat = 400) {
        self.url = url
        self.maxPixelSize = maxPointSize
    }

    public var body: some View {
        // The gradient takes whatever size the parent proposes; the photo is an overlay so its own
        // pixel size can never inflate the layout (a `scaledToFill` image in a ZStack would).
        SFGradient.thumbnailFallback
            .overlay {
                if let image {
                    Image(uiImage: image)
                        .resizable()
                        .scaledToFill()
                        .transition(.opacity)
                }
            }
            .animation(.easeOut(duration: 0.25), value: image != nil)
            .clipped()
        .task(id: url) {
            guard let url else { image = nil; return }
            image = await ImagePipeline.shared.image(for: url, maxPixelSize: maxPixelSize * displayScale)
        }
    }
}
#endif
