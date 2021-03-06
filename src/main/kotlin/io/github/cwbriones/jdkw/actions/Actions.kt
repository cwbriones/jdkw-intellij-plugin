package io.github.cwbriones.jdkw.actions

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.guessProjectDir
import io.github.cwbriones.jdkw.Bundle
import io.github.cwbriones.jdkw.Notifier
import io.github.cwbriones.jdkw.services.JdkWrapperPreferencesService
import io.github.cwbriones.jdkw.services.JdkWrapperService
import io.github.cwbriones.jdkw.unwrap

/**
 * Convenience function to locate an action using its class name.
 *
 * You must ensure that the registered action id matches the class name.
 *
 * @see ActionManager.getAction
 */
inline fun <reified T : AnAction> getAction(): AnAction = ActionManager.getInstance().getAction(T::class.java.name)

class ImportJdk : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project.unwrap("Expected project on action")
        val notifier = Notifier(project)
        val projectDir = project.guessProjectDir().unwrap("Could not infer project dir.")
        project.service<JdkWrapperService>().inferWrapperConfig(projectDir) { config ->
            val service = project.service<JdkWrapperService>()
            notifier.info {
                setContent("JDK import from home directory: ${config.javaHome}")
                setImportant(false)
            }
            val sdk = service.findExistingJdk(config.javaHome) ?: service.importJdk(config.javaHome)
            service.configureJdkForProject(sdk)

            val jdkId = service.jdkwId(projectDir)

            val preferences = project.service<JdkWrapperPreferencesService>()
            preferences.lastImportedId = jdkId
            notifier.info {
                setContent(Bundle.getMessage("autodetect.import.completed"))
            }
        }
    }
}
