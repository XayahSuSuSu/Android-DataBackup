package com.xayah.core.rootservice.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun <T> withMainContext(block: suspend CoroutineScope.() -> T): T = withContext(Dispatchers.Main, block)

suspend fun <T> withIOContext(block: suspend CoroutineScope.() -> T): T = withContext(Dispatchers.IO, block)
