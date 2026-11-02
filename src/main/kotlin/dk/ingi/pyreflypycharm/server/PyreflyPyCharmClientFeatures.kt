package dk.ingi.pyreflypycharm.server

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.redhat.devtools.lsp4ij.client.features.*
import dk.ingi.pyreflypycharm.inspection.PyreflyInspection
import dk.ingi.pyreflypycharm.settings.PyreflyPyCharmSettings
import org.eclipse.lsp4j.InitializeParams

/**
 * Decides which LSP features LSP4IJ uses for Pyrefly.
 *
 * Feature switches are read on every call, so toggling a setting takes effect on the client side
 * immediately; the matching `disabledLanguageServices` on the server side follow via
 * `workspace/didChangeConfiguration`.
 */
class PyreflyPyCharmClientFeatures : LSPClientFeatures() {

    init {
        setDiagnosticFeature(PyreflyPyCharmDiagnosticFeature())
        setHoverFeature(object : LSPHoverFeature() {
            override fun isEnabled(file: PsiFile) = settings().hover
        })
        setInlayHintFeature(object : LSPInlayHintFeature() {
            override fun isEnabled(file: PsiFile) = settings().inlayHints
        })
        setCodeActionFeature(object : LSPCodeActionFeature() {
            override fun isEnabled(file: PsiFile) = settings().codeActions
        })
        setCompletionFeature(object : LSPCompletionFeature() {
            override fun isEnabled(file: PsiFile) = settings().completion
        })
        setSignatureHelpFeature(object : LSPSignatureHelpFeature() {
            override fun isEnabled(file: PsiFile) = settings().signatureHelp
        })
        setDefinitionFeature(object : LSPDefinitionFeature() {
            override fun isEnabled(file: PsiFile) = settings().navigation
        })
        setDeclarationFeature(object : LSPDeclarationFeature() {
            override fun isEnabled(file: PsiFile) = settings().navigation
        })
        setTypeDefinitionFeature(object : LSPTypeDefinitionFeature() {
            override fun isEnabled(file: PsiFile) = settings().navigation
        })
        setImplementationFeature(object : LSPImplementationFeature() {
            override fun isEnabled(file: PsiFile) = settings().navigation
        })
        setReferencesFeature(object : LSPReferencesFeature() {
            override fun isEnabled(file: PsiFile) = settings().navigation
        })
        setUsageFeature(object : LSPUsageFeature() {
            override fun isEnabled(file: PsiFile) = settings().navigation
        })

        // PyCharm does these natively and better integrated with its own PSI; never route them to Pyrefly.
        setRenameFeature(object : LSPRenameFeature() { override fun isEnabled(file: PsiFile) = false })
        setFormattingFeature(object : LSPFormattingFeature() { override fun isEnabled(file: PsiFile) = false })
        setOnTypeFormattingFeature(object : LSPOnTypeFormattingFeature() { override fun isEnabled(file: PsiFile) = false })
        setFoldingRangeFeature(object : LSPFoldingRangeFeature() { override fun isEnabled(file: PsiFile) = false })
        setSelectionRangeFeature(object : LSPSelectionRangeFeature() { override fun isEnabled(file: PsiFile) = false })
        setSemanticTokensFeature(object : LSPSemanticTokensFeature() { override fun isEnabled(file: PsiFile) = false })
        setDocumentHighlightFeature(object : LSPDocumentHighlightFeature() { override fun isEnabled(file: PsiFile) = false })
        setDocumentSymbolFeature(object : LSPDocumentSymbolFeature() { override fun isEnabled(file: PsiFile) = false })
        setBreadcrumbsFeature(object : LSPBreadcrumbsFeature() { override fun isEnabled(file: PsiFile) = false })
        setDocumentLinkFeature(object : LSPDocumentLinkFeature() { override fun isEnabled(file: PsiFile) = false })
        setCodeLensFeature(object : LSPCodeLensFeature() { override fun isEnabled(file: PsiFile) = false })
        setCallHierarchyFeature(object : LSPCallHierarchyFeature() { override fun isEnabled(file: PsiFile) = false })
        setTypeHierarchyFeature(object : LSPTypeHierarchyFeature() { override fun isEnabled(file: PsiFile) = false })
        setDocumentColorFeature(object : LSPDocumentColorFeature() { override fun isEnabled(file: PsiFile) = false })
        setInlineCompletionFeature(object : LSPInlineCompletionFeature() { override fun isEnabled(file: PsiFile) = false })
    }

    private fun settings() = PyreflyPyCharmSettings.getInstance(project).state

    /** Don't start Pyrefly at all when there is no binary; tell the user once instead. */
    override fun isEnabled(file: VirtualFile): Boolean {
        val service = PyreflyPyCharmService.getInstance(project)
        if (service.resolution().binary == null) {
            service.notifyMissingBinaryOnce()
            return false
        }
        return true
    }

    override fun initializeParams(initializeParams: InitializeParams) {
        super.initializeParams(initializeParams)
        initializeParams.initializationOptions = PyreflyPyCharmService.getInstance(project).currentConfig()
    }

    /** Pyrefly can take a moment to start on big projects; keep it running between file switches. */
    override fun keepServerAlive() = true
}

/** Routes Pyrefly diagnostics to the "Pyrefly" inspection so they can be configured in the inspection profile. */
class PyreflyPyCharmDiagnosticFeature : LSPDiagnosticFeature() {
    override fun isEnabled(file: PsiFile) = PyreflyPyCharmSettings.getInstance(project).state.diagnostics

    override fun isInspectionApplicableFor(file: PsiFile, inspection: LocalInspectionTool) =
        inspection.id == PyreflyInspection.ID
}
