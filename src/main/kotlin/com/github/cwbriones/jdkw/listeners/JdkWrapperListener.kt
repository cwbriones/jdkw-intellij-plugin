package com.github.cwbriones.jdkw.listeners

import com.github.cwbriones.jdkw.ext.getLogger
import com.github.cwbriones.jdkw.services.JdkWrapperService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class JdkWrapperListener : StartupActivity {
    companion object {
        private val logger = getLogger<JdkWrapperListener>()
    }

    init {
        println("Listener started.")
        logger.debug("initialized")
    }

    override fun runActivity(project: Project) {
        println("Project opened: ${project.name}, ${project.baseDir}")
        logger.info("Project opened: ${project.name}, ${project.baseDir}")
        project.service<JdkWrapperService>().onOpen()
    }
}
