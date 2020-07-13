package com.github.cwbriones.jdkw.services

import com.github.cwbriones.jdkw.MyBundle
import com.github.cwbriones.jdkw.ext.getLogger
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil
import com.intellij.openapi.roots.LanguageLevelProjectExtension
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.java.LanguageLevel
import java.io.File
import java.io.FileNotFoundException

class JdkWrapperService(private val project: Project) {
    companion object {
        val logger = getLogger<JdkWrapperService>()
    }

    init {
        println(MyBundle.message("projectService", project.name))
    }

    fun onOpen() {
        print("Project @ ${project.guessProjectDir()}")
        inferProjectJdk(project.guessProjectDir()!!) {
            val sdk = importJdk(it)
            SdkConfigurationUtil.setDirectoryProjectSdk(project, sdk)
            val languageLevelExt = LanguageLevelProjectExtension.getInstance(project)
            val languageLevel = languageLevelFromSdk(sdk)
            if (languageLevel != null) {
                languageLevelExt.languageLevel = languageLevel
            }
        }
    }

    private fun languageLevelFromSdk(sdk: Sdk): LanguageLevel? =
            JavaSdk.getInstance().getVersion(sdk)?.maxLanguageLevel

    private fun inferProjectJdk(contentRoot: VirtualFile, callback: (String) -> Unit) {
        println("looking for jdk-wrapper.sh")
        logger.info("looking for jdk-wrapper.sh")
        val jdkWrapper = contentRoot.findChild("jdk-wrapper.sh")
        if (jdkWrapper != null && jdkWrapper.exists()) {
            logger.info("found - executing")

            val commandLine = GeneralCommandLine("./jdk-wrapper.sh")
                    .withWorkDirectory(jdkWrapper.parent.canonicalPath)
                    .withParameters("bash", "-c", "echo \$JAVA_HOME")

            val handler = OSProcessHandler(commandLine)

            val console =
                    TextConsoleBuilderFactory
                            .getInstance()
                            .createBuilder(project)
                            .console
                            .attachToProcess(handler)

            val allText = mutableListOf<String>()
            handler.startNotify()
//            handler.waitFor(5000)
            handler.addProcessListener(object : ProcessListener {
                override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                    allText.add(event.text)
                }

                override fun processTerminated(event: ProcessEvent) {
                    if (event.exitCode == 0) {
                        ApplicationManager.getApplication().invokeLater {
                            callback(allText.joinToString(transform = String::trim))
                        }
                        // Success!
                    }
                }

                override fun processWillTerminate(event: ProcessEvent, willBeDestroyed: Boolean) {}
                override fun startNotified(event: ProcessEvent) {}
            })
            return
        }
        println("nada")
    }

    private fun importJdk(javaHomePath: String): Sdk {
        val jdkService = service<ProjectJdkTable>()
        val matchingJdk = jdkService.allJdks.find {
            it.homePath == javaHomePath
        }
        if (matchingJdk != null) {
            return matchingJdk
        }
        println("No matching JDK found at path: ${javaHomePath}, importing.")
        val javaHomeFile = File(javaHomePath)

        if (!javaHomeFile.exists()) {
            throw IllegalStateException("Why does the path not exist")
        }

        val javaHome = writeAction {
            LocalFileSystem.getInstance().refreshAndFindFileByIoFile(javaHomeFile)
        } ?: throw FileNotFoundException("Nada")

        val javaSdkType = JavaSdk.getInstance()
        val sdk = SdkConfigurationUtil.setupSdk(
                arrayOf(),
                javaHome,
                javaSdkType,
                true,
                null,
                javaSdkType.suggestSdkName(null, javaHome.canonicalPath)
        ) ?: throw IllegalStateException("Unable to setup JDK")
        SdkConfigurationUtil.addSdk(sdk)
        println("added")
        return sdk
    }

    private fun <T> writeAction(f: () -> T): T = ApplicationManager.getApplication().runWriteAction<T>(f)
}
