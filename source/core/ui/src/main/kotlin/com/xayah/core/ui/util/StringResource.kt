package com.xayah.core.ui.util

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.common.util.toPureString

fun StringResourceToken.Companion.fromStringId(@StringRes id: Int): StringResourceToken {
    return StringResourceToken.StringIdToken(id = id)
}

fun StringResourceToken.Companion.fromString(value: String): StringResourceToken {
    return StringResourceToken.StringToken(value = value)
}

fun StringResourceToken.Companion.fromStringArgs(vararg args: StringResourceToken): StringResourceToken {
    return StringResourceToken.StringArgsToken(args = args)
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

        is StringResourceToken.StringArgsToken -> {
            args.map { it.value }.toPureString()
        }
    }
