package com.xayah.core.ui.model

import androidx.annotation.ArrayRes

sealed class StringArrayResourceToken {
    data class StringArrayIdToken(@ArrayRes val id: Int) : StringArrayResourceToken()
    data class StringArrayToken(val value: List<String>) : StringArrayResourceToken()

    companion object
}
