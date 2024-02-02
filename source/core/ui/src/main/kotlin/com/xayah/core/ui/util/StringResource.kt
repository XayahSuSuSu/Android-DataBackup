package com.xayah.core.ui.util

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.xayah.core.common.util.toPureString
import com.xayah.core.ui.model.StringResourceToken

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

fun StringResourceToken.getValue(context: Context): String = when (this) {
    is StringResourceToken.StringIdToken -> {
        context.getString(id)
    }

    is StringResourceToken.StringToken -> {
        value
    }

    is StringResourceToken.StringArgsToken -> {
        args.map { it.getValue(context) }.toPureString()
    }
}
