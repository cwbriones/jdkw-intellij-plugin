package io.github.cwbriones.jdkw.listeners

import com.intellij.notification.NotificationAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.vfs.VirtualFile
import io.github.cwbriones.jdkw.Bundle
import io.github.cwbriones.jdkw.Notifier
import io.github.cwbriones.jdkw.actions.ImportJdk
import io.github.cwbriones.jdkw.actions.getAction
import io.github.cwbriones.jdkw.getLogger
import io.github.cwbriones.jdkw.services.JdkWrapperPreferencesService
import io.github.cwbriones.jdkw.services.JdkWrapperService
import io.github.cwbriones.jdkw.unwrap

class JdkWrapperListener : StartupActivity {
    companion object {
        private val logger = getLogger<JdkWrapperListener>()
    }

    init {
        logger.debug("initialized")
    }

    override fun runActivity(project: Project) {
        logger.debug("Looking for jdk-wrapper for project: ${project.name}")
        val jdkwService = project.service<JdkWrapperService>()
        val projectDir = project.guessProjectDir().unwrap("Could not infer project dir.")
        if (!needsJdkImport(project, projectDir)) {
            logger.debug("Skipping JDK import.")
            return
        }
        jdkwService.inferWrapperConfig(projectDir) { config ->
            val notifier = Notifier(project)
            val sdk = project.service<JdkWrapperService>().findExistingJdk(config.javaHome)
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

    @Suppress("ReturnCount")
    private fun needsJdkImport(project: Project, contentRoot: VirtualFile): Boolean {
        val jdkwService = project.service<JdkWrapperService>()
        val preferences = project.service<JdkWrapperPreferencesService>()
        if (!preferences.suggestAvailableImport) {
            logger.debug("JDK import suggestions are disabled.")
            return false
        }
        if (!jdkwService.usesWrapper(contentRoot)) {
            logger.debug("Project does not use JDK Wrapper.")
            return false
        }
        val jdkId = jdkwService.jdkwId(contentRoot)
        if (jdkId == preferences.lastImportedId) {
            logger.debug("JDK ID has not changed since last import")
            return false
        }
        return true
    }
}
