package io.github.cwbriones.jdkw.services

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.ProcessOutputType
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil
import com.intellij.openapi.roots.LanguageLevelProjectExtension
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.java.LanguageLevel
import io.github.cwbriones.jdkw.getLogger
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Paths
import java.security.MessageDigest

data class JdkWrapperConfig(val javaHome: String)

class JdkWrapperService(private val project: Project) : Disposable {
    companion object {
        val logger = getLogger<JdkWrapperService>()
    }

    fun usesWrapper(contentRoot: VirtualFile) = contentRoot.findChild("jdk-wrapper.sh")?.exists() ?: false

    fun jdkwId(contentRoot: VirtualFile): String {
        val jdkw = contentRoot.findChild(".jdkw") ?: return ""
        val contents = ApplicationManager
            .getApplication()
            .runReadAction(Computable<ByteArray>(jdkw::contentsToByteArray))
        val shaDigest = MessageDigest.getInstance("SHA-256")
        return shaDigest.digest(contents).joinToString("") {
            String.format("%02x", it)
        }
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

    /**
     * Determine the configured JDK by running the jdk-wrapper and examining the results.
     *
     * Since this executes the wrapper, it could potentially trigger a network fetch of the
     * distribution if it's not already on the system.
     */
    fun inferWrapperConfig(contentRoot: VirtualFile, callback: (JdkWrapperConfig) -> Unit) {
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

            console.attachToProcess(handler)
            Disposer.register(this, console)

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
        val suggestedName = javaSdkType.suggestSdkName(null, javaHomePath)
        val distSuffix = getDistSuffix(javaHomePath) ?: ""
        val sdk = SdkConfigurationUtil.setupSdk(
            arrayOf(),
            javaHome,
            javaSdkType,
            true,
            null,
            sequenceOf(suggestedName, distSuffix).joinToString(" ")
        ) ?: throw IllegalStateException("Unable to setup JDK")
        SdkConfigurationUtil.addSdk(sdk)
        return sdk
    }

    override fun dispose() { /* unused */ }

    private fun getDistSuffix(sdkHome: String): String? {
        var sdkHomePath = Paths.get(sdkHome)
        if (!sdkHomePath.endsWith(Paths.get("Contents", "Home"))) {
            return null
        }
        sdkHomePath = sdkHomePath.parent.parent
        val (dist, _, build) = sdkHomePath.fileName.toString().split("_")
        return "($dist, $build)"
    }
}

private fun <T> writeAction(f: () -> T): T = ApplicationManager.getApplication().runWriteAction<T>(f)

private fun <T, L : MutableList<T>> L.pop(): T? {
    return if (isEmpty()) null else removeAt(size - 1)
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
