package com.github.cwbriones.jdkw.listeners

import com.github.cwbriones.jdkw.Notifier
import com.github.cwbriones.jdkw.actions.ImportJdk
import com.github.cwbriones.jdkw.actions.getAction
import com.github.cwbriones.jdkw.ext.getLogger
import com.github.cwbriones.jdkw.services.JdkWrapperService
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
        project.service<JdkWrapperService>().inferJdk(project.guessProjectDir()!!, fun(it: String) {
                val notifier = Notifier(project)
                val sdk = project.service<JdkWrapperService>().findExistingJdk(it)
                if (sdk == null) {
                    notifier.info {
                        setTitle("JDK Wrapper Detected")
                        setContent("Would you like to import the JDK from your wrapper configuration?")
                        addAction(getAction<ImportJdk>())
                    }
                    return
                }
        })
    }
}
