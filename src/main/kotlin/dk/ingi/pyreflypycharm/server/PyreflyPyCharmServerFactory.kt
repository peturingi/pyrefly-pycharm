package dk.ingi.pyreflypycharm.server

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.ParametersList
import com.intellij.openapi.project.Project
import com.redhat.devtools.lsp4ij.LanguageServerFactory
import com.redhat.devtools.lsp4ij.client.LanguageClientImpl
import com.redhat.devtools.lsp4ij.client.features.LSPClientFeatures
import com.redhat.devtools.lsp4ij.server.OSProcessStreamConnectionProvider
import com.redhat.devtools.lsp4ij.server.StreamConnectionProvider
import dk.ingi.pyreflypycharm.settings.PyreflyPyCharmSettings

/** Entry point registered with LSP4IJ's `server` extension point. */
class PyreflyPyCharmServerFactory : LanguageServerFactory {

    override fun createConnectionProvider(project: Project): StreamConnectionProvider {
        val service = PyreflyPyCharmService.getInstance(project)
        val resolution = service.resolution()
        // PyreflyPyCharmClientFeatures.isEnabled() keeps LSP4IJ from getting here without a binary;
        // fall back to plain "pyrefly" so the LSP console shows a clear "cannot start" error.
        val binary = resolution.binary?.absolutePath ?: "pyrefly"
        val extra = PyreflyPyCharmSettings.getInstance(project).state.extraArguments.orEmpty()
        val commandLine = GeneralCommandLine(binary, "lsp")
            .withParameters(ParametersList.parse(extra).toList())
            .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
        project.basePath?.let { commandLine.withWorkDirectory(it) }
        return OSProcessStreamConnectionProvider(commandLine)
    }

    override fun createLanguageClient(project: Project): LanguageClientImpl = PyreflyPyCharmLanguageClient(project)

    override fun createClientFeatures(): LSPClientFeatures = PyreflyPyCharmClientFeatures()
}
