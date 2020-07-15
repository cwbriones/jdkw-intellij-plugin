package io.briones.jdkw.services

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.ProcessOutputType
import com.intellij.history.core.Paths
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.SdkType
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil
import com.intellij.openapi.roots.LanguageLevelProjectExtension
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.java.LanguageLevel
import io.briones.jdkw.getLogger
import java.io.File
import java.io.FileNotFoundException

data class JdkWrapperConfig(val javaHome: String)

class JdkWrapperService(private val project: Project) {
    companion object {
        val logger = getLogger<JdkWrapperService>()
    }

    fun configureJdkForProject(sdk: Sdk) {
        SdkConfigurationUtil.setDirectoryProjectSdk(project, sdk)
        val languageLevelExt = LanguageLevelProjectExtension.getInstance(project)
        val languageLevel = languageLevelFromSdk(sdk)
        if (languageLevel != null) {
            languageLevelExt.languageLevel = languageLevel
        }
    }

    private fun languageLevelFromSdk(sdk: Sdk): LanguageLevel? =
        JavaSdk.getInstance().getVersion(sdk)?.maxLanguageLevel

    fun inferWrapperConfig(contentRoot: VirtualFile, callback: (JdkWrapperConfig) -> Unit) {
        logger.info("looking for jdk-wrapper.sh")
        val jdkWrapper = contentRoot.findChild("jdk-wrapper.sh")
        if (jdkWrapper != null && jdkWrapper.exists()) {
            logger.info("found - executing")

            val commandLine = GeneralCommandLine("./jdk-wrapper.sh")
                .withWorkDirectory(jdkWrapper.parent.canonicalPath)
                .withParameters("bash", "-c", "echo \$JAVA_HOME")

            val handler = OSProcessHandler(commandLine)

            TextConsoleBuilderFactory
                .getInstance()
                .createBuilder(project)
                .console
                .attachToProcess(handler)

            handler.addProcessListener(OutputCapturingListener(callback))
            handler.startNotify()
            return
        }
    }

    fun findExistingJdk(javaHomePath: String): Sdk? =
        service<ProjectJdkTable>().allJdks.find {
            it.homePath == javaHomePath
        }

    fun importJdk(javaHomePath: String): Sdk {
        val javaHome = writeAction {
            LocalFileSystem.getInstance().refreshAndFindFileByIoFile(File(javaHomePath))
        } ?: throw FileNotFoundException("Unable to locate java home directory")

        val javaSdkType = JavaSdk.getInstance()
        val sdk = SdkConfigurationUtil.setupSdk(
            arrayOf(),
            javaHome,
            javaSdkType,
            true,
            null,
            sdkName(javaSdkType, javaHome.path)
        ) ?: throw IllegalStateException("Unable to setup JDK")
        SdkConfigurationUtil.addSdk(sdk)
        return sdk
    }

    @Suppress("ReturnCount")
    private fun sdkName(sdkType: SdkType, sdkHome: String): String {
        val suggestedName = sdkType.suggestSdkName(null, sdkHome)
        val components = Paths.split(sdkHome).toMutableList()
        if (components.pop() != "Home") {
            return suggestedName
        }
        if (components.pop() != "Contents") {
            return suggestedName
        }
        return components.last()?.let { "$suggestedName (${extractDist(it)})" } ?: suggestedName
    }

    private fun extractDist(distId: String): String {
        val (dist, _, build) = distId.split("_")
        return "$dist, $build"
    }

    private fun <T, L : MutableList<T>> L.pop(): T? {
        return if (isEmpty()) null else removeAt(size - 1)
    }

    private fun <T> writeAction(f: () -> T): T = ApplicationManager.getApplication().runWriteAction<T>(f)
}

class OutputCapturingListener(private val onTerminate: (JdkWrapperConfig) -> Unit) : ProcessListener {
    private val allText: MutableList<String> = mutableListOf()

    override fun onTextAvailable(event: ProcessEvent, key: Key<*>) {
        if (ProcessOutputType.isStdout(key)) {
            allText.add(event.text)
        }
    }

    override fun processTerminated(event: ProcessEvent) {
        if (event.exitCode == 0) {
            ApplicationManager.getApplication().invokeLater {
                val config = JdkWrapperConfig(
                    javaHome = allText.joinToString(transform = String::trim)
                )
                onTerminate(config)
            }
        }
    }

    override fun processWillTerminate(event: ProcessEvent, willBeDestroyed: Boolean) { /* unused */ }

    override fun startNotified(event: ProcessEvent) { /* unused */ }
}
