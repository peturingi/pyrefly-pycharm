package dk.ingi.pyreflypycharm.server

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener

/**
 * Interpreter changes and excluded-folder changes both arrive as root changes. Re-evaluate the
 * configuration off the EDT; PyreflyPyCharmService only restarts or notifies Pyrefly when something it
 * cares about actually changed.
 */
class PyreflyPyCharmRootsListener(private val project: Project) : ModuleRootListener {
    override fun rootsChanged(event: ModuleRootEvent) {
        ApplicationManager.getApplication().executeOnPooledThread {
            if (!project.isDisposed) PyreflyPyCharmService.getInstance(project).configurationChanged()
        }
    }
}
