package com.github.cwbriones.jdkw.listeners

import com.github.cwbriones.jdkw.Bundle
import com.github.cwbriones.jdkw.Notifier
import com.github.cwbriones.jdkw.actions.ImportJdk
import com.github.cwbriones.jdkw.actions.getAction
import com.github.cwbriones.jdkw.ext.getLogger
import com.github.cwbriones.jdkw.services.JdkWrapperConfig
import com.github.cwbriones.jdkw.services.JdkWrapperService
import com.github.cwbriones.jdkw.services.JdkwPreferencesService
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
        if (!project.service<JdkwPreferencesService>().suggestAvailableImport) {
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
                            e.project!!.service<JdkwPreferencesService>().suggestAvailableImport = false
                            notification.expire()
                        })
                    }
                    return
                }
        })
    }
}
