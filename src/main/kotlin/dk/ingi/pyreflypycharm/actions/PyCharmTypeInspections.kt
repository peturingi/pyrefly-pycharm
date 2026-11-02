package dk.ingi.pyreflypycharm.actions

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInspection.ex.modifyAndCommitProjectProfile
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import dk.ingi.pyreflypycharm.server.PyreflyPyCharmService
import dk.ingi.pyreflypycharm.settings.PyreflyPyCharmSettings

/**
 * PyCharm's own inspections that depend on its type engine. With Pyrefly reporting type errors they
 * only add noise (and disagree with Pyrefly), so pyrefly-pycharm offers to turn them off in the current
 * project's inspection profile. Turning them back on restores PyCharm's behaviour.
 */
object PyCharmTypeInspections {

    val SHORT_NAMES = listOf(
        "PyTypeCheckerInspection",
        "PyUnresolvedReferencesInspection",
        "PyArgumentListInspection",
        "PyCallingNonCallableInspection",
        "PyTypeHintsInspection",
        "PyAbstractClassInspection",
        "PyNoneFunctionAssignmentInspection",
        "PyFinalInspection",
        "PyProtocolInspection",
        "PyTypedDictInspection",
        "PyNewTypeInspection",
        "PyOverloadsInspection",
        "PyInvalidCastInspection",
        "PyDataclassInspection",
        "PyEnumInspection",
        "PyTypeAliasRedeclarationInspection",
        "PyAsyncCallInspection",
    )

    fun setEnabled(project: Project, enabled: Boolean) {
        modifyAndCommitProjectProfile(project) { model ->
            for (name in SHORT_NAMES) {
                if (model.getToolsOrNull(name, project) != null) model.setToolEnabled(name, enabled, project)
            }
        }
        DaemonCodeAnalyzer.getInstance(project).restart("pyrefly-pycharm toggled PyCharm type inspections")
        notify(
            project,
            if (enabled) "PyCharm's type inspections are back on." else "PyCharm's type inspections are off. Pyrefly now reports type errors.",
        )
    }

    /** After Pyrefly first starts in a project, offer (once) to turn PyCharm's type inspections off. */
    fun offerToDisableOnce(project: Project) {
        val settings = PyreflyPyCharmSettings.getInstance(project).state
        if (settings.inspectionPromptShown) return
        settings.inspectionPromptShown = true
        ApplicationManager.getApplication().invokeLater {
            if (project.isDisposed) return@invokeLater
            NotificationGroupManager.getInstance().getNotificationGroup(PyreflyPyCharmService.NOTIFICATION_GROUP)
                .createNotification(
                    "Pyrefly is running",
                    "PyCharm's own type inspections are still on and will disagree with Pyrefly. Turn them off for this project?",
                    NotificationType.INFORMATION,
                )
                .addAction(NotificationAction.createSimpleExpiring("Turn off") { setEnabled(project, false) })
                .addAction(NotificationAction.createSimpleExpiring("Keep both") {})
                .notify(project)
        }
    }

    private fun notify(project: Project, text: String) {
        NotificationGroupManager.getInstance().getNotificationGroup(PyreflyPyCharmService.NOTIFICATION_GROUP)
            .createNotification(text, NotificationType.INFORMATION)
            .notify(project)
    }
}
