package com.xayah.core.ui.util

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.xayah.core.ui.model.StringResourceToken

fun StringResourceToken.Companion.fromStringId(@StringRes id: Int): StringResourceToken {
    return StringResourceToken.StringIdToken(id = id)
}

fun StringResourceToken.Companion.fromString(value: String): StringResourceToken {
    return StringResourceToken.StringToken(value = value)
}

val StringResourceToken.value: String
    @Composable
    get() = when (this) {
        is StringResourceToken.StringIdToken -> {
            stringResource(id = id)
        }

        is StringResourceToken.StringToken -> {
            value
        }
    }
