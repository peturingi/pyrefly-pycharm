package dk.ingi.pyreflypycharm.server

import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.SystemInfo
import com.jetbrains.python.sdk.PythonSdkUtil
import dk.ingi.pyreflypycharm.settings.PyreflyPyCharmSettings
import java.io.File

/**
 * Finds the Python interpreter of a project and the `pyrefly` binary to run.
 *
 * The binary is chosen in this order, mirroring the official VS Code extension:
 *  1. the path configured in pyrefly-pycharm's settings;
 *  2. `pyrefly` next to the project interpreter (a virtualenv's `bin/` or `Scripts\`),
 *     so the editor and the command line run the same Pyrefly version;
 *  3. `pyrefly` on `PATH`;
 *  4. a few common install locations (pipx/uv tools, Homebrew, Cargo).
 */
object PyreflyLocator {

    private val binaryName = if (SystemInfo.isWindows) "pyrefly.exe" else "pyrefly"

    data class Resolution(val binary: File?, val source: String, val pythonPath: String?)

    fun resolve(project: Project): Resolution {
        val python = findPythonSdk(project)?.takeUnless { PythonSdkUtil.isRemote(it) }?.homePath
        val configured = PyreflyPyCharmSettings.getInstance(project).state.executablePath?.trim().orEmpty()
        if (configured.isNotEmpty()) {
            val file = File(expandHome(configured))
            return Resolution(file.takeIf { it.canExecute() }, "settings ($configured)", python)
        }
        if (python != null) {
            val nextToInterpreter = File(python).parentFile?.resolve(binaryName)
            if (nextToInterpreter != null && nextToInterpreter.canExecute()) {
                return Resolution(nextToInterpreter, "project interpreter", python)
            }
        }
        PathEnvironmentVariableUtil.findInPath(binaryName)?.takeIf { it.canExecute() }?.let {
            return Resolution(it, "PATH", python)
        }
        val home = System.getProperty("user.home")
        listOf(
            "$home/.local/bin/$binaryName",
            "$home/.cargo/bin/$binaryName",
            "/opt/homebrew/bin/$binaryName",
            "/usr/local/bin/$binaryName",
        ).map(::File).firstOrNull { it.canExecute() }?.let {
            return Resolution(it, "common install location", python)
        }
        return Resolution(null, "not found", python)
    }

    /** The project SDK if it is a Python SDK, otherwise the first module's Python SDK. */
    fun findPythonSdk(project: Project): Sdk? = ReadAction.computeBlocking<Sdk?, RuntimeException> {
        if (project.isDisposed) return@computeBlocking null
        ProjectRootManager.getInstance(project).projectSdk?.takeIf { PythonSdkUtil.isPythonSdk(it) }
            ?: ModuleManager.getInstance(project).modules.firstNotNullOfOrNull { PythonSdkUtil.findPythonSdk(it) }
    }

    private fun expandHome(path: String): String =
        if (path == "~" || path.startsWith("~/")) System.getProperty("user.home") + path.substring(1) else path
}
