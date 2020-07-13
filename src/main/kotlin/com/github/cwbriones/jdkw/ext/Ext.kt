package com.github.cwbriones.jdkw.ext

import com.intellij.openapi.diagnostic.Logger

inline fun <reified T> getLogger(): Logger = Logger.getInstance(T::class.java)
