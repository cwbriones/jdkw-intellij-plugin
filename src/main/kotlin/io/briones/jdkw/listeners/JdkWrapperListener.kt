package io.briones.jdkw.listeners

import io.briones.jdkw.Bundle
import io.briones.jdkw.Notifier
import io.briones.jdkw.actions.ImportJdk
import io.briones.jdkw.actions.getAction
import io.briones.jdkw.getLogger
import io.briones.jdkw.services.JdkWrapperConfig
import io.briones.jdkw.services.JdkWrapperService
import io.briones.jdkw.services.JdkWrapperPreferencesService
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.startup.StartupActivity

class JdkWrapperListener : StartupActivity {
    companion object {
        private val logger = getLogger<JdkWrapperListener>()
    }

    init {
        logger.debug("initialized")
    }

    override fun runActivity(project: Project) {
        logger.debug("Received project open event: ${project.name}")
        if (!project.service<JdkWrapperPreferencesService>().suggestAvailableImport) {
            return
        }
        project.service<JdkWrapperService>().inferWrapperConfig(project.guessProjectDir()!!, fun(it: JdkWrapperConfig) {
                val notifier = Notifier(project)
                val sdk = project.service<JdkWrapperService>().findExistingJdk(it.javaHome)
                if (sdk == null) {
                    notifier.info {
                        setTitle("JDK Wrapper Detected")
                        setContent("Would you like to import the JDK from your wrapper configuration?")
                        addAction(NotificationAction.create(Bundle.message("autodetect.import")) { e: AnActionEvent, notification: Notification ->
                            getAction<ImportJdk>().actionPerformed(e)
                            notification.expire()
                        })
                        addAction(NotificationAction.create(Bundle.message("autodetect.dismiss")) { e: AnActionEvent, notification: Notification ->
                            e.project!!.service<JdkWrapperPreferencesService>().suggestAvailableImport = false
                            notification.expire()
                        })
                    }
                    return
                }
        })
    }
}
