package com.xayah.databackup.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

@Composable
fun LaunchedEffect(context: CoroutineContext, vararg keys: Any?, block: suspend CoroutineScope.() -> Unit) {
    LaunchedEffect(*keys) {
        withContext(context) {
            block.invoke(this)
        }
    }
}
