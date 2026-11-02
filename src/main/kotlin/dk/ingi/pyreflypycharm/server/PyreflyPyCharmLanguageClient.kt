package dk.ingi.pyreflypycharm.server

import com.google.gson.JsonObject
import com.intellij.openapi.project.Project
import com.redhat.devtools.lsp4ij.ServerStatus
import com.redhat.devtools.lsp4ij.client.LanguageClientImpl
import dk.ingi.pyreflypycharm.actions.PyCharmTypeInspections

/**
 * Answers Pyrefly's `workspace/configuration` requests (section `python`) and
 * `workspace/didChangeConfiguration` with the same object sent as initialization options.
 */
class PyreflyPyCharmLanguageClient(project: Project) : LanguageClientImpl(project) {

    override fun createSettings(): Any = JsonObject().apply {
        add("python", PyreflyPyCharmService.getInstance(project).currentConfig())
    }

    override fun handleServerStatusChanged(serverStatus: ServerStatus) {
        super.handleServerStatusChanged(serverStatus)
        if (serverStatus == ServerStatus.started) {
            PyreflyPyCharmService.getInstance(project).clientStarted(this)
            PyCharmTypeInspections.offerToDisableOnce(project)
        }
    }
}
