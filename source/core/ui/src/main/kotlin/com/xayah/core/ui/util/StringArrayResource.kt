package com.xayah.core.ui.util

import androidx.annotation.ArrayRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringArrayResource
import com.xayah.core.ui.model.StringArrayResourceToken

fun StringArrayResourceToken.Companion.fromStringArrayId(@ArrayRes id: Int): StringArrayResourceToken {
    return StringArrayResourceToken.StringArrayIdToken(id = id)
}

fun StringArrayResourceToken.Companion.fromStringArray(value: List<String>): StringArrayResourceToken {
    return StringArrayResourceToken.StringArrayToken(value = value)
}

val StringArrayResourceToken.value: List<String>
    @Composable
    get() = when (this) {
        is StringArrayResourceToken.StringArrayIdToken -> {
            stringArrayResource(id = id).toList()
        }

        is StringArrayResourceToken.StringArrayToken -> {
            value
        }
    }
