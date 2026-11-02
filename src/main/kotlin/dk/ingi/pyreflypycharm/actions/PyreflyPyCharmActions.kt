package dk.ingi.pyreflypycharm.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import dk.ingi.pyreflypycharm.server.PyreflyPyCharmService

abstract class PyreflyPyCharmProjectAction : AnAction(), DumbAware {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }
}

class RestartPyreflyAction : PyreflyPyCharmProjectAction() {
    override fun actionPerformed(e: AnActionEvent) {
        PyreflyPyCharmService.getInstance(e.project ?: return).restart()
    }
}

class DisablePyCharmTypeInspectionsAction : PyreflyPyCharmProjectAction() {
    override fun actionPerformed(e: AnActionEvent) {
        PyCharmTypeInspections.setEnabled(e.project ?: return, false)
    }
}

class EnablePyCharmTypeInspectionsAction : PyreflyPyCharmProjectAction() {
    override fun actionPerformed(e: AnActionEvent) {
        PyCharmTypeInspections.setEnabled(e.project ?: return, true)
    }
}
