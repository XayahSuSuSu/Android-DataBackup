package com.xayah.core.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

suspend fun <T> withMainContext(block: suspend CoroutineScope.() -> T): T = withContext(Dispatchers.Main, block)

suspend fun <T> withIOContext(block: suspend CoroutineScope.() -> T): T = withContext(Dispatchers.IO, block)

fun CoroutineScope.launchOnDefault(block: suspend CoroutineScope.() -> Unit) = launch(context = Dispatchers.Default, block = block)

fun CoroutineScope.launchOnMain(block: suspend CoroutineScope.() -> Unit) = launch(context = Dispatchers.Main, block = block)
