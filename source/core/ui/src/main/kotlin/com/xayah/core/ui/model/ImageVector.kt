package com.xayah.core.ui.model

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.vector.ImageVector

sealed class ImageVectorToken {
    data class DrawableToken(@DrawableRes val resID: Int) : ImageVectorToken()
    data class VectorToken(val imageVector: ImageVector) : ImageVectorToken()

    companion object
}
