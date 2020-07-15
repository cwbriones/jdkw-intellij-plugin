package io.github.cwbriones.jdkw

import com.intellij.openapi.diagnostic.Logger

inline fun <reified T> getLogger(): Logger = Logger.getInstance(T::class.java)

/**
 * Convenience utility for asserting non-nullity with a message.
 */
inline fun <reified T> T?.unwrap(msg: String): T = this ?: throw NullPointerException(msg)
