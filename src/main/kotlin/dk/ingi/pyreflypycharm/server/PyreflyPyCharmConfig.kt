package dk.ingi.pyreflypycharm.server

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import dk.ingi.pyreflypycharm.settings.PyreflyPyCharmSettings

/**
 * Builds the client configuration Pyrefly reads from LSP `initializationOptions` and from the
 * `python` section of `workspace/configuration`.
 *
 * Pyrefly applies the `workspace/configuration` answer on top of the initialization options, and a
 * `pyrefly` object that omits a key resets that key to its default. So both paths always send the
 * complete object produced here.
 *
 * Shape (see Pyrefly's `LspConfig` / `PyreflyClientConfig` in pyrefly/lib/lsp/non_wasm/workspace.rs):
 * ```
 * { "pythonPath": "...", "analysis": {...}, "pyrefly": {...} }
 * ```
 */
object PyreflyPyCharmConfig {

    /** Values Pyrefly accepts for `pyrefly.typeCheckingMode`. */
    val TYPE_CHECKING_MODES = listOf("auto", "off", "basic", "legacy", "default", "strict", "all")

    fun build(project: Project, pythonPath: String?): JsonObject {
        val s = PyreflyPyCharmSettings.getInstance(project).state
        val diagnosticMode = if (s.workspaceDiagnostics) "workspace" else "openFilesOnly"
        val navigationOff = !s.navigation

        val disabled = JsonObject().apply {
            addProperty("hover", !s.hover)
            addProperty("inlayHint", !s.inlayHints)
            addProperty("codeAction", !s.codeActions)
            addProperty("completion", !s.completion)
            addProperty("signatureHelp", !s.signatureHelp)
            addProperty("definition", navigationOff)
            addProperty("declaration", navigationOff)
            addProperty("typeDefinition", navigationOff)
            addProperty("implementation", navigationOff)
            addProperty("references", navigationOff)
            // PyCharm handles these natively; pyrefly-pycharm never asks Pyrefly for them.
            addProperty("rename", true)
            addProperty("documentHighlight", true)
            addProperty("documentSymbol", true)
            addProperty("codeLens", true)
            addProperty("semanticTokens", true)
        }

        val pyrefly = JsonObject().apply {
            addProperty("disableTypeErrors", !s.diagnostics)
            addProperty("diagnosticMode", diagnosticMode)
            addProperty("typeCheckingMode", s.typeCheckingMode?.takeIf { it in TYPE_CHECKING_MODES } ?: "auto")
            add("disabledLanguageServices", disabled)
            if (s.forwardExcludedFolders) {
                add("extraProjectExcludes", excludedFolderGlobs(project))
            }
        }

        return JsonObject().apply {
            if (pythonPath != null) addProperty("pythonPath", pythonPath)
            add("analysis", JsonObject().apply { addProperty("diagnosticMode", diagnosticMode) })
            add("pyrefly", pyrefly)
        }
    }

    /** PyCharm's "Excluded" folders as absolute globs (the folder itself and everything below it). */
    private fun excludedFolderGlobs(project: Project): JsonArray = ReadAction.computeBlocking<JsonArray, RuntimeException> {
        val globs = JsonArray()
        if (project.isDisposed) return@computeBlocking globs
        ModuleManager.getInstance(project).modules
            .flatMap { ModuleRootManager.getInstance(it).excludeRoots.toList() }
            .filter { it.isInLocalFileSystem }
            .map { it.path }
            .distinct()
            .sorted()
            .forEach {
                globs.add(it)
                globs.add("$it/**")
            }
        globs
    }
}
