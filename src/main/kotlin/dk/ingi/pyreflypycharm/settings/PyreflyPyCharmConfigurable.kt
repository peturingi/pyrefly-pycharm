package dk.ingi.pyreflypycharm.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import dk.ingi.pyreflypycharm.actions.PyCharmTypeInspections
import dk.ingi.pyreflypycharm.server.PyreflyPyCharmConfig
import dk.ingi.pyreflypycharm.server.PyreflyPyCharmService

/** Settings | Tools | Pyrefly. */
class PyreflyPyCharmConfigurable(private val project: Project) : BoundConfigurable("Pyrefly") {

    private val state get() = PyreflyPyCharmSettings.getInstance(project).state

    override fun createPanel(): DialogPanel {
        val detected = PyreflyPyCharmService.getInstance(project).resolution()
        return panel {
            group("Pyrefly Binary") {
                row("Path:") {
                    textFieldWithBrowseButton(
                        FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor().withTitle("Select Pyrefly Binary"),
                        project,
                    ).align(AlignX.FILL)
                        .bindText({ state.executablePath.orEmpty() }, { state.executablePath = it.trim() })
                        .comment(
                            "Leave empty to auto-detect. Currently using: " +
                                (detected.binary?.absolutePath?.let { "<code>$it</code> (${detected.source})" } ?: "<b>none found</b>"),
                        )
                }
                row("Extra arguments:") {
                    textField().align(AlignX.FILL)
                        .bindText({ state.extraArguments.orEmpty() }, { state.extraArguments = it.trim() })
                        .comment("Appended after <code>pyrefly lsp</code>.")
                }
            }
            group("Use Pyrefly For") {
                row { checkBox("Type errors and warnings").bindSelected({ state.diagnostics }, { state.diagnostics = it }) }
                indent {
                    row("Without a Pyrefly config use preset:") {
                        comboBox(PyreflyPyCharmConfig.TYPE_CHECKING_MODES)
                            .bindItem({ state.typeCheckingMode ?: "auto" }, { state.typeCheckingMode = it ?: "auto" })
                    }.comment(
                        "Applies to projects without <code>pyrefly.toml</code> or <code>[tool.pyrefly]</code>. " +
                            "<code>auto</code> migrates a mypy/pyright config if present, otherwise uses <code>basic</code>, " +
                            "which only reports high-confidence errors. Pick <code>default</code> to see what <code>pyrefly init</code> would.",
                    )
                    row {
                        checkBox("Check all project files, not only open ones")
                            .bindSelected({ state.workspaceDiagnostics }, { state.workspaceDiagnostics = it })
                    }
                }
                row { checkBox("Hover type information").bindSelected({ state.hover }, { state.hover = it }) }
                row { checkBox("Inlay hints (inferred types, parameter names)").bindSelected({ state.inlayHints }, { state.inlayHints = it }) }
                row { checkBox("Quick fixes (code actions)").bindSelected({ state.codeActions }, { state.codeActions = it }) }
                row {
                    checkBox("Code completion").bindSelected({ state.completion }, { state.completion = it })
                        .comment("Off by default: PyCharm's completion stays active and results would be duplicated.")
                }
                row { checkBox("Parameter info (signature help)").bindSelected({ state.signatureHelp }, { state.signatureHelp = it }) }
                row {
                    checkBox("Go to definition, find usages").bindSelected({ state.navigation }, { state.navigation = it })
                        .comment("Off by default: PyCharm's own navigation stays active.")
                }
            }
            group("Project") {
                row {
                    checkBox("Exclude PyCharm's \"Excluded\" folders from Pyrefly checking")
                        .bindSelected({ state.forwardExcludedFolders }, { state.forwardExcludedFolders = it })
                }
                row {
                    button("Turn Off PyCharm's Type Inspections") { PyCharmTypeInspections.setEnabled(project, false) }
                    button("Turn Them Back On") { PyCharmTypeInspections.setEnabled(project, true) }
                }.comment(
                    "Turns PyCharm's own type checking inspections (${PyCharmTypeInspections.SHORT_NAMES.size} of them) " +
                        "off or on in the current inspection profile, so only Pyrefly reports type errors.",
                )
            }
        }
    }

    override fun apply() {
        super.apply()
        PyreflyPyCharmService.getInstance(project).configurationChanged()
    }
}
