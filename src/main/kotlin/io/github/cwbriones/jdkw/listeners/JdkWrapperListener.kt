package io.github.cwbriones.jdkw.listeners

import com.intellij.notification.NotificationAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.startup.StartupActivity
import io.github.cwbriones.jdkw.Bundle
import io.github.cwbriones.jdkw.Notifier
import io.github.cwbriones.jdkw.actions.ImportJdk
import io.github.cwbriones.jdkw.actions.getAction
import io.github.cwbriones.jdkw.getLogger
import io.github.cwbriones.jdkw.services.JdkWrapperPreferencesService
import io.github.cwbriones.jdkw.services.JdkWrapperService

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
        val projectDir = project.guessProjectDir() ?: throw IllegalStateException("Could not infer project dir.")
        project.service<JdkWrapperService>().inferWrapperConfig(projectDir) {
            val notifier = Notifier(project)
            val sdk = project.service<JdkWrapperService>().findExistingJdk(it.javaHome)
            if (sdk == null) {
                notifier.info {
                    setTitle("JDK Wrapper Detected")
                    setContent("Would you like to import the JDK from your wrapper configuration?")
                    addAction(
                        NotificationAction.create(Bundle.message("autodetect.import")) { e, notification ->
                            getAction<ImportJdk>().actionPerformed(e)
                            notification.expire()
                        }
                    )
                    addAction(
                        NotificationAction.create(Bundle.message("autodetect.dismiss")) { _, notification ->
                            project.service<JdkWrapperPreferencesService>().suggestAvailableImport = false
                            notification.expire()
                        }
                    )
                }
            }
        }
    }
}
