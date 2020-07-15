package io.briones.jdkw.actions

import io.briones.jdkw.Bundle
import io.briones.jdkw.Notifier
import io.briones.jdkw.services.JdkWrapperConfig
import io.briones.jdkw.services.JdkWrapperService
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.components.service
import com.intellij.openapi.project.guessProjectDir

/**
 * Convenience function to locate an action using its class name.
 *
 * You must ensure that the registered action id matches the class name.
 *
 * @see ActionManager.getAction
 */
inline fun <reified T : AnAction> getAction(): AnAction
        = ActionManager.getInstance().getAction(T::class.java.name)

class ImportJdk : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project!!
        val notifier = Notifier(project)
        project.service<JdkWrapperService>().inferWrapperConfig(project.guessProjectDir()!!, fun(it: JdkWrapperConfig) {
            val service = project.service<JdkWrapperService>()
            notifier.info {
                setContent("JDK import from home directory: ${it.javaHome}")
                setImportant(false)
            }
            val sdk = service.findExistingJdk(it.javaHome) ?: service.importJdk(it.javaHome)
            service.configureJdkForProject(sdk)
            notifier.info {
                setContent(Bundle.getMessage("autodetect.import.completed"))
            }
        })
    }
}
