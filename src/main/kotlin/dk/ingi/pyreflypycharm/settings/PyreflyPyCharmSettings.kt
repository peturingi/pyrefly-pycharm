package dk.ingi.pyreflypycharm.settings

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

/**
 * Per-project, per-user pyrefly-pycharm settings (stored in the workspace file, so they are not committed).
 *
 * The defaults split work between Pyrefly and PyCharm: Pyrefly is the source of truth for type errors,
 * hover types and inlay hints, while PyCharm keeps the features it already does natively (completion,
 * navigation, rename, formatting). Each Pyrefly feature can be switched on here.
 */
@Service(Service.Level.PROJECT)
@State(name = "PyreflyPyCharmSettings", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
class PyreflyPyCharmSettings : SimplePersistentStateComponent<PyreflyPyCharmSettings.State>(State()) {

    class State : BaseState() {
        /** Explicit path to the `pyrefly` binary. Empty means auto-detect. */
        var executablePath by string("")

        /** Extra arguments placed after `pyrefly lsp`. */
        var extraArguments by string("")

        var diagnostics by property(true)
        var hover by property(true)
        var inlayHints by property(true)
        var codeActions by property(true)
        var completion by property(false)
        var navigation by property(false)
        var signatureHelp by property(false)

        /**
         * Pyrefly preset for files not covered by a `pyrefly.toml` / `[tool.pyrefly]`:
         * auto, off, basic, legacy, default, strict or all. A project config always wins.
         */
        var typeCheckingMode by string("auto")

        /** Report errors for every file in the project, not just open files. */
        var workspaceDiagnostics by property(false)

        /** Push PyCharm's "Excluded" folders to Pyrefly as `extraProjectExcludes`. */
        var forwardExcludedFolders by property(true)

        /** Set once the "turn off PyCharm's type inspections?" prompt has been shown for this project. */
        var inspectionPromptShown by property(false)
    }

    companion object {
        fun getInstance(project: Project): PyreflyPyCharmSettings = project.service()
    }
}
