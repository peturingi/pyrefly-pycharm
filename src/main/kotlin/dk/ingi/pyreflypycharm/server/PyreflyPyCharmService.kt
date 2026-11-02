package dk.ingi.pyreflypycharm.server

import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.redhat.devtools.lsp4ij.LanguageServerManager
import dk.ingi.pyreflypycharm.settings.PyreflyPyCharmConfigurable
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Project-level state shared by the pyrefly-pycharm pieces: the resolved Pyrefly binary, the live language
 * client, and the last configuration sent to Pyrefly.
 *
 * Nothing in here talks to Pyrefly while holding an IDE lock. Reads of project structure are short
 * read actions; all LSP traffic goes through LSP4IJ's asynchronous client.
 */
@Service(Service.Level.PROJECT)
class PyreflyPyCharmService(private val project: Project) {

    @Volatile
    private var cachedResolution: PyreflyLocator.Resolution? = null

    @Volatile
    private var clientRef: WeakReference<PyreflyPyCharmLanguageClient>? = null

    @Volatile
    private var lastSentConfig: String? = null

    private val missingBinaryNotified = AtomicBoolean(false)

    fun resolution(): PyreflyLocator.Resolution =
        cachedResolution ?: PyreflyLocator.resolve(project).also {
            cachedResolution = it
            LOG.info("Pyrefly binary: ${it.binary ?: "<none>"} (${it.source}); python: ${it.pythonPath ?: "<none>"}")
        }

    fun currentConfig() = PyreflyPyCharmConfig.build(project, resolution().pythonPath)

    internal fun clientStarted(client: PyreflyPyCharmLanguageClient) {
        clientRef = WeakReference(client)
        lastSentConfig = currentConfig().toString()
    }

    /**
     * Called when settings or project structure (interpreter, excluded folders) may have changed.
     * Restarts Pyrefly if the binary changed; otherwise pushes the new configuration if it differs.
     */
    fun configurationChanged() {
        val previous = cachedResolution
        cachedResolution = null
        // Nothing resolved yet means Pyrefly was never started here; LSP4IJ will start it with
        // fresh settings when a Python file is opened.
        if (previous == null) return
        val now = resolution()
        if (now.binary != previous.binary) {
            restart()
            return
        }
        val config = currentConfig().toString()
        if (config != lastSentConfig) {
            lastSentConfig = config
            clientRef?.get()?.triggerChangeConfiguration()
        }
    }

    fun restart() {
        cachedResolution = null
        missingBinaryNotified.set(false)
        LanguageServerManager.getInstance(project)
            .start(SERVER_ID, LanguageServerManager.StartOptions().setForceRestart(true))
    }

    fun notifyMissingBinaryOnce() {
        if (!missingBinaryNotified.compareAndSet(false, true)) return
        NotificationGroupManager.getInstance().getNotificationGroup(NOTIFICATION_GROUP)
            .createNotification(
                "Pyrefly not found",
                "pyrefly-pycharm could not find a <code>pyrefly</code> binary next to the project interpreter, on PATH, " +
                    "or in common install locations. Install it (<code>pip install pyrefly</code>) or set its path.",
                NotificationType.WARNING,
            )
            .addAction(NotificationAction.createSimple("Configure…") {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, PyreflyPyCharmConfigurable::class.java)
            })
            .notify(project)
    }

    companion object {
        const val SERVER_ID = "pyrefly-pycharm"
        const val NOTIFICATION_GROUP = "pyrefly-pycharm"
        private val LOG = logger<PyreflyPyCharmService>()

        fun getInstance(project: Project): PyreflyPyCharmService = project.service()
    }
}
