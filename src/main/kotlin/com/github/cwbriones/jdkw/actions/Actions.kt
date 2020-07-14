package com.github.cwbriones.jdkw.actions

import com.github.cwbriones.jdkw.Notifier
import com.github.cwbriones.jdkw.services.JdkWrapperService
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.guessProjectDir

/**
 * Convenience function to locate an action using its class name.
 *
 * You must ensure that the action id matches the class name in plugin.xml.
 *
 * @see ActionManager.getAction
 */
inline fun <reified T : AnAction> getAction(): AnAction
        = ActionManager.getInstance().getAction(T::class.java.name)

class ImportJdk : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project!!
        val notifier = Notifier(project)
        project.service<JdkWrapperService>().inferJdk(project.guessProjectDir()!!, fun(it: String) {
            val service = project.service<JdkWrapperService>()
            val sdk = service.findExistingJdk(it) ?: service.importJdk(it)
            notifier.info {
                setContent("Import from home directory: ${sdk.homePath}")
                setImportant(false)
            }
            service.configureJdkForProject(sdk)
            notifier.info {
                setContent("JDK import completed.")
            }
        })
    }
}