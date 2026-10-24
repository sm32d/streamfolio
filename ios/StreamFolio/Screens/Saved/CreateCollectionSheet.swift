import SwiftUI
import StreamFolioCore
import StreamFolioTokens
import StreamFolioUI

/// Name a new collection.
struct CreateCollectionSheet: View {
    @Environment(AppEnvironment.self) private var env
    @Environment(\.dismiss) private var dismiss
    @State private var name = ""
    @State private var nameTaken = false
    @FocusState private var focused: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: SFSpacing.s8) {
            Text("New Collection").font(SFFont.sheetTitle).foregroundStyle(SFColor.onSurface)
            TextField("Collection name", text: $name)
                .textFieldStyle(.roundedBorder)
                .focused($focused)
                .submitLabel(.done)
                .onSubmit(create)
            if nameTaken { Text("A collection with that name already exists.").font(SFFont.meta).foregroundStyle(SFColor.error) }
            SFPrimaryButton("Create", action: create).disabled(name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
        }
        .padding(SFSpacing.gutter)
        .sfSheetBackground()
        .presentationDetents([.height(280)])
        .onAppear { focused = true }
    }

    private func create() {
        guard !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }
        Task {
            if await env.saved.create(named: name) != nil { dismiss() } else { nameTaken = true }
        }
    }
}
