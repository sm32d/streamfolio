import SwiftUI
import UniformTypeIdentifiers
import StreamFolioCore
import StreamFolioTokens
import StreamFolioUI

/// Full-settings backup and OPML import/export (Android `SettingsBackupScreen`). Files go through the
/// system Files picker, so nothing is shared with any app until the user chooses where it goes.
struct SettingsBackupScreen: View {
    @Environment(AppEnvironment.self) private var env

    @State private var importKind: ImportKind?
    @State private var export: ExportRequest?
    @State private var pendingRestore: Data?
    @State private var pendingOpml: [OpmlFeed]?
    @State private var notice: Notice?

    private enum ImportKind { case backup, opml }

    var body: some View {
        SFSettingsPage {
            SFHeroBanner("Backup & Restore Hub", subtitle: "Export/Import preferences, custom feeds, and OPML data", systemImage: "externaldrive.fill")

            SFSettingsSection("Full Settings Backup") {
                SFSettingsCard {
                    action(systemImage: "arrow.down.doc.fill", tint: SFColor.primary, title: "Import Settings",
                           subtitle: "Restore app preferences, feeds & gestures from JSON") { importKind = .backup }
                    action(systemImage: "square.and.arrow.up.fill", tint: SFColor.secondary, title: "Export Settings",
                           subtitle: "Backup preferences & feeds to standard JSON file", perform: exportBackup)
                }
            }

            SFSettingsSection("OPML Subscription Outline") {
                SFSettingsCard {
                    action(systemImage: "icloud.and.arrow.up.fill", tint: SFColor.tertiary, title: "Import OPML",
                           subtitle: "Upload RSS subscription list outline file") { importKind = .opml }
                    action(systemImage: "icloud.and.arrow.down.fill", tint: SFColor.primary, title: "Export OPML",
                           subtitle: "Save RSS subscription list outline file", perform: exportOpml)
                }
            }

            SFInfoBanner("Full Settings Backups generate a standard text JSON file that securely restores feeds, category orders, UI gestures, and translation choices.")
        }
        .navigationTitle("Backup & Restore")
        .navigationBarTitleDisplayMode(.inline)
        .fileImporter(
            isPresented: Binding(get: { importKind != nil }, set: { if !$0 { importKind = nil } }),
            allowedContentTypes: [.data]
        ) { result in
            guard let kind = importKind else { return }
            importKind = nil
            Task { await handleImport(result, kind: kind) }
        }
        .fileExporter(
            isPresented: Binding(get: { export != nil }, set: { if !$0 { export = nil } }),
            document: export?.document, contentType: export?.type ?? .json, defaultFilename: export?.filename
        ) { result in
            if case .failure(let error) = result { notice = Notice("Export failed", error.localizedDescription) }
        }
        .confirmationDialog(
            "Restore Settings", isPresented: Binding(get: { pendingRestore != nil }, set: { if !$0 { pendingRestore = nil } }),
            titleVisibility: .visible, presenting: pendingRestore
        ) { data in
            Button("Restore Settings", role: .destructive) { Task { await restore(data) } }
        } message: { _ in
            Text("This will overwrite all of your current custom RSS feeds, swipe action choices, categories, and translation preferences. Are you sure you want to proceed?")
        }
        .sheet(isPresented: Binding(get: { pendingOpml != nil }, set: { if !$0 { pendingOpml = nil } })) {
            if let feeds = pendingOpml {
                OpmlImportSheet(feeds: feeds) { mapping, selected in
                    Task { await importOpml(feeds, mapping: mapping, selected: selected) }
                }
            }
        }
        .alert(item: $notice) { Alert(title: Text($0.title), message: Text($0.message)) }
        .sensoryFeedback(.success, trigger: notice?.id)
    }

    private func action(systemImage: String, tint: Color, title: LocalizedStringKey, subtitle: String, perform: @escaping () -> Void) -> some View {
        Button(action: perform) {
            SFSettingsRow(systemImage: systemImage, tint: tint, title: title, subtitle: subtitle)
        }
        .buttonStyle(.plain)
    }

    // MARK: Export

    private func exportBackup() {
        Task {
            do {
                export = ExportRequest(document: SettingsFile(data: try await env.backup.export()), type: .json, filename: "streamfolio_backup")
            } catch {
                notice = Notice("Failed to create backup", error.localizedDescription)
            }
        }
    }

    private func exportOpml() {
        Task {
            let feeds = (try? await env.customFeeds.all()) ?? []
            guard !feeds.isEmpty else {
                notice = Notice("Nothing to export", "You have no custom feeds to export.")
                return
            }
            export = ExportRequest(document: SettingsFile(data: Data(Opml.export(feeds).utf8)), type: .opml, filename: "streamfolio_subscriptions")
        }
    }

    // MARK: Import

    private func handleImport(_ result: Result<URL, Error>, kind: ImportKind) async {
        let limit = kind == .backup ? BackupService.maxBytes : Opml.maxBytes
        let data: Data
        do {
            data = try Self.read(try result.get(), limit: limit)
        } catch {
            notice = Notice(kind == .backup ? "Failed to read backup" : "Failed to read OPML", error.localizedDescription)
            return
        }

        switch kind {
        case .backup:
            if BackupService.looksLikeBackup(data) { pendingRestore = data } else { notice = Notice("Invalid backup file", "This file is not a StreamFolio backup.") }
        case .opml:
            let parsed = Opml.parse(data)
            let existing = (try? await env.customFeeds.all()) ?? []
            if parsed.isEmpty {
                notice = Notice("Nothing to import", "No RSS feeds found in this OPML file.")
            } else if Opml.importable(parsed, existing: existing).isEmpty {
                notice = Notice("Nothing new", "All feeds in this OPML are already imported!")
            } else {
                pendingOpml = Opml.importable(parsed, existing: existing)
            }
        }
    }

    private func restore(_ data: Data) async {
        do {
            let summary = try await env.backup.restore(data)
            env.feed.settingsDidChange(regionChanged: true)
            notice = Notice("Backup restored", "Restored \(summary.feeds) feeds, \(summary.articles) saved or read articles and \(summary.lists) collections.")
        } catch {
            notice = Notice("Restore failed", error.localizedDescription)
        }
    }

    private func importOpml(_ feeds: [OpmlFeed], mapping: [String: String], selected: Set<String>) async {
        do {
            let added = try await env.customFeeds.importFeeds(feeds, categoryMapping: mapping, selectedCategories: selected)
            env.feed.settingsDidChange()
            notice = Notice("Feeds imported", "Added \(added) feed\(added == 1 ? "" : "s").")
        } catch {
            notice = Notice("Import failed", error.localizedDescription)
        }
    }

    /// Reads a file the user picked, refusing anything over `limit` bytes without loading it.
    private static func read(_ url: URL, limit: Int) throws -> Data {
        let scoped = url.startAccessingSecurityScopedResource()
        defer { if scoped { url.stopAccessingSecurityScopedResource() } }
        if let size = try url.resourceValues(forKeys: [.fileSizeKey]).fileSize, size > limit {
            throw BackupService.BackupError.tooLarge
        }
        return try Data(contentsOf: url)
    }
}

// MARK: - Supporting types

private struct Notice: Identifiable {
    let id = UUID()
    let title: String
    let message: String
    init(_ title: String, _ message: String) { (self.title, self.message) = (title, message) }
}

private struct ExportRequest {
    let document: SettingsFile
    let type: UTType
    let filename: String
}

private extension UTType {
    static let opml = UTType(filenameExtension: "opml", conformingTo: .xml) ?? .xml
}

/// A file the user saves or opens: the backup JSON or an OPML list.
struct SettingsFile: FileDocument {
    static let readableContentTypes: [UTType] = [.json, .xml, .data]

    var data: Data

    init(data: Data) { self.data = data }

    init(configuration: ReadConfiguration) throws {
        data = configuration.file.regularFileContents ?? Data()
    }

    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper {
        FileWrapper(regularFileWithContents: data)
    }
}

// MARK: - OPML import sheet

/// Choose which OPML folders to import and what to call their tabs.
private struct OpmlImportSheet: View {
    let feeds: [OpmlFeed]
    let onImport: ([String: String], Set<String>) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var selected: Set<String>
    @State private var names: [String: String]

    init(feeds: [OpmlFeed], onImport: @escaping ([String: String], Set<String>) -> Void) {
        self.feeds = feeds
        self.onImport = onImport
        let folders = Self.folders(of: feeds)
        _selected = State(initialValue: Set(folders))
        _names = State(initialValue: Dictionary(uniqueKeysWithValues: folders.map { ($0, $0) }))
    }

    private static func folders(of feeds: [OpmlFeed]) -> [String] {
        var seen = Set<String>()
        return feeds.map(\.category).filter { seen.insert($0).inserted }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: SFSpacing.s8) {
            Text("Import Feeds (\(feeds.count) found)").font(SFFont.sheetTitle).foregroundStyle(SFColor.onSurface)
            Text("Map OPML folders to StreamFolio tabs, or uncheck categories you don't want to import.")
                .font(SFFont.bodySmall).foregroundStyle(SFColor.textSecondary)

            ScrollView {
                VStack(spacing: SFSpacing.s6) {
                    ForEach(Self.folders(of: feeds), id: \.self) { folder in folderCard(folder) }
                }
            }
            .scrollIndicators(.hidden)

            HStack(spacing: SFSpacing.s6) {
                SFSecondaryButton("Cancel") { dismiss() }
                SFPrimaryButton("Import Selected") {
                    onImport(names, selected)
                    dismiss()
                }
                .disabled(selected.isEmpty)
            }
        }
        .padding(SFSpacing.gutter)
        .sfSheetBackground()
        .presentationDetents([.large])
    }

    private func folderCard(_ folder: String) -> some View {
        let inFolder = feeds.filter { $0.category == folder }
        let isOn = selected.contains(folder)
        return VStack(alignment: .leading, spacing: SFSpacing.s4) {
            Toggle(isOn: Binding(get: { isOn }, set: { if $0 { selected.insert(folder) } else { selected.remove(folder) } })) {
                HStack {
                    Text(folder).font(SFFont.bodySmall.weight(.bold)).foregroundStyle(SFColor.onSurface)
                    Spacer()
                    Text("\(inFolder.count) feed\(inFolder.count == 1 ? "" : "s")").font(SFFont.caption).foregroundStyle(SFColor.primary)
                }
            }
            .tint(SFColor.primary)

            if isOn {
                VStack(alignment: .leading, spacing: SFSpacing.s2) {
                    Text("Feeds to import:").font(SFFont.caption.weight(.semibold)).foregroundStyle(SFColor.textMuted)
                    ForEach(inFolder, id: \.xmlUrl) { feed in
                        HStack(spacing: SFSpacing.s4) {
                            Circle().fill(SFColor.primary).frame(width: 5, height: 5).accessibilityHidden(true)
                            Text(feed.title).font(SFFont.meta).foregroundStyle(SFColor.textBody).lineLimit(1)
                        }
                    }
                }
                SFTextField("StreamFolio Category Tab Name", text: Binding(get: { names[folder] ?? folder }, set: { names[folder] = $0 }))
            }
        }
        .padding(SFSpacing.s6)
        .background(SFColor.surfaceVariant.opacity(0.3), in: .rect(cornerRadius: SFRadius.r14))
        .animation(.smooth, value: isOn)
    }
}
