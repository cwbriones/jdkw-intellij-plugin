package io.briones.jdkw

import com.intellij.openapi.diagnostic.Logger

inline fun <reified T> getLogger(): Logger = Logger.getInstance(T::class.java)

inline fun <reified T> T?.unwrap(msg: String): T = this ?: throw NullPointerException(msg)
