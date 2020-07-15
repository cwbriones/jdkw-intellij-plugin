package io.briones.jdkw

import com.intellij.openapi.diagnostic.Logger

inline fun <reified T> getLogger(): Logger = Logger.getInstance(T::class.java)
