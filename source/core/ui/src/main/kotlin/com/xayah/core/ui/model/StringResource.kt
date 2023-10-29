package com.xayah.core.ui.model

import androidx.annotation.StringRes

sealed class StringResourceToken {
    data class StringIdToken(@StringRes val id: Int) : StringResourceToken()
    data class StringToken(val value: String) : StringResourceToken()

    companion object
}
