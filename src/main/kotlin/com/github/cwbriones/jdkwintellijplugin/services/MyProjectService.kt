package com.github.cwbriones.jdkwintellijplugin.services

import com.intellij.openapi.project.Project
import com.github.cwbriones.jdkwintellijplugin.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
