import SwiftUI
import StreamFolioCore
import StreamFolioTokens
import StreamFolioUI

/// An article: clean Reader text by default, or the publisher's page (Web View). Link-only
/// publishers open straight in the Web View with no switcher, as agreed with the publisher.
struct ArticleDetailScreen: View {
    @Environment(AppEnvironment.self) private var env
    @State private var reader: ReaderStore
    @State private var showsTypography = false

    init(reader: ReaderStore) {
        _reader = State(initialValue: reader)
    }

    var body: some View {
        let article = reader.article
        Group {
            switch reader.mode {
            case .reader: readerPage
            case .web: ArticleWebView(link: article.link)
            }
        }
        .background { SFBackdrop() }
        .navigationBarTitleDisplayMode(.inline)
        .toolbarVisibility(.hidden, for: .tabBar)
        .navigationTitle(article.sourceName)
        // Not in the top bar (it collides with the three action buttons) and not in a system bottom
        // toolbar (its picker ignored taps on the iPhone Duo simulator).
        .safeAreaInset(edge: .bottom, spacing: 0) {
            if reader.capabilities.canShowReader {
                SFSegmentedControl(
                    options: [.init(ReaderStore.Mode.reader, "Reader"), .init(.web, "Web View")],
                    selection: $reader.mode
                )
                .padding(.bottom, SFSpacing.s4)
            }
        }
        .toolbar {
            ToolbarItemGroup(placement: .topBarTrailing) {
                if reader.mode == .reader {
                    Button { showsTypography = true } label: { Label("Text settings", systemImage: "textformat.size") }
                        .popover(isPresented: $showsTypography) {
                            SFTypographyPanel(
                                family: binding(Prefs.readerFontFamily),
                                size: binding(Prefs.readerFontSize),
                                lineSpacing: binding(Prefs.readerLineSpacing)
                            )
                            .presentationCompactAdaptation(.popover)
                        }
                }
                shareButton
                Button { Task { await reader.toggleSaved() } } label: {
                    Label(article.isBookmarked ? "Unsave" : "Save", systemImage: article.isBookmarked ? "bookmark.fill" : "bookmark")
                }
                .contextMenu {
                    Button { env.interactions.manageCollections(of: article) } label: { Label("Save to Collection…", systemImage: "folder.badge.plus") }
                }
            }
        }
        .task { await reader.open() }
    }

    // MARK: Reader

    private var readerPage: some View {
        let style = SFReaderStyle(env.preferences)
        return ScrollView {
            VStack(alignment: .leading, spacing: SFSpacing.s10) {
                SFReaderHeader(article: reader.article, readingTime: reader.readingTime, style: style)
                switch reader.content {
                case .loading:
                    SFTextSkeleton()
                case .article(let paragraphs):
                    SFReaderBody(paragraphs: paragraphs, style: style)
                case .excerpt(let paragraphs):
                    SFInfoBanner("Full-text parsing is restricted by this publisher. Tap 'Web View' above to read the complete article.")
                    SFReaderBody(paragraphs: paragraphs, style: style)
                case .webOnly:
                    SFInfoBanner("This article is best read on the publisher's page. Tap 'Web View' above.")
                }
            }
            .padding(.horizontal, SFSpacing.gutter)
            .padding(.top, SFSpacing.s8)
            .padding(.bottom, SFSpacing.s8)
        }
        .scrollIndicators(.hidden)
    }

    // MARK: Helpers

    @ViewBuilder
    private var shareButton: some View {
        let item = env.actions.shareItem(for: reader.article)
        if let link = item.url {
            ShareLink(item: link, subject: Text(reader.article.title), message: Text(item.text))
        } else {
            ShareLink(item: item.text)
        }
    }

    private func binding<Value>(_ key: Preference<Value>) -> Binding<Value> {
        Binding(get: { env.preferences[key] }, set: { env.preferences[key] = $0 })
    }
}
