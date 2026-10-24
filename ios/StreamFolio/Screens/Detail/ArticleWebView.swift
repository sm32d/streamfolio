import SwiftUI
import WebKit
import StreamFolioCore
import StreamFolioTokens
import StreamFolioUI

/// The publisher's own page. Shows load progress and, if the page cannot be loaded, a friendly
/// failure state with Try again (the raw WebKit error text is never shown).
struct ArticleWebView: View {
    let link: String

    @State private var page = WebPage()
    @State private var loadFailed = false
    @State private var reloadCount = 0

    private var url: URL? {
        UrlSecurityValidator.normalizeToHttps(link).flatMap(URL.init(string:))
    }

    var body: some View {
        VStack(spacing: 0) {
            if page.isLoading {
                ProgressView(value: page.estimatedProgress).tint(SFColor.primary)
            }
            ZStack {
                WebView(page)
                if loadFailed { failure }
            }
        }
        .task(id: reloadCount) { await load() }
    }

    private var failure: some View {
        ZStack {
            SFBackdrop()
            SFEmptyState(
                illustration: Image("OtterNoSearch"),
                title: "Couldn't load this page",
                message: "Check your connection and try again."
            ) {
                SFPrimaryButton("Try again", systemImage: "arrow.clockwise") { reloadCount += 1 }
                    .padding(.top, SFSpacing.s6)
            }
        }
    }

    private func load() async {
        loadFailed = false
        guard let url else { loadFailed = true; return }
        do {
            for try await _ in page.load(url) {}
        } catch is CancellationError {
            // leaving the screen
        } catch {
            loadFailed = true
        }
    }
}
