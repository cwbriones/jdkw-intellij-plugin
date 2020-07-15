package com.github.cwbriones.jdkw.services

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
        name = "JdkWrapper",
        storages = [Storage(StoragePathMacros.WORKSPACE_FILE)]
)
class JdkwPreferencesService : PersistentStateComponent<JdkwPreferencesService> {
    /**
     * When true, the plugin should suggest an import if one is available.
     */
    var suggestAvailableImport: Boolean = true

    override fun getState(): JdkwPreferencesService = this

    override fun loadState(state: JdkwPreferencesService) {
        XmlSerializerUtil.copyBean(state, this)
    }
}