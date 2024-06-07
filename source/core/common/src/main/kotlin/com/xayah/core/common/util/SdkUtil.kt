package com.xayah.core.common.util

import android.annotation.TargetApi
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.compose.runtime.Composable

val geSdk26 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
val ltSdk30 = Build.VERSION.SDK_INT < Build.VERSION_CODES.R
val geSdk31 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
val geSdk33 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
val leSdk33 = Build.VERSION.SDK_INT <= Build.VERSION_CODES.TIRAMISU

inline fun <reified T> valueGeSdk28(ge: () -> T, otherwise: () -> T) = if (geSdk26) @TargetApi(Build.VERSION_CODES.P) {
    ge()
} else {
    otherwise()
}

inline fun <reified T> valueGeSdk31(ge: () -> T, otherwise: () -> T) = if (geSdk31) @TargetApi(Build.VERSION_CODES.S) {
    ge()
} else {
    otherwise()
}

inline fun <reified T> valueGeSdk33(ge: () -> T, otherwise: () -> T) = if (geSdk33) @TargetApi(Build.VERSION_CODES.TIRAMISU) {
    ge()
} else {
    otherwise()
}

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S, lambda = 0)
fun ifGeSdk31(block: () -> Unit) {
    if (geSdk31) {
        block()
    }
}

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S, lambda = 0)
@Composable
fun IfGeSdk31(block: @Composable () -> Unit) {
    if (geSdk31) {
        block()
    }
}
