package com.xayah.core.ui.util

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.xayah.core.ui.model.ImageVectorToken

fun ImageVectorToken.Companion.fromDrawable(@DrawableRes resID: Int): ImageVectorToken {
    return ImageVectorToken.DrawableToken(resID = resID)
}

fun ImageVectorToken.Companion.fromVector(imageVector: ImageVector): ImageVectorToken {
    return ImageVectorToken.VectorToken(imageVector = imageVector)
}

val ImageVectorToken.value: ImageVector
    @Composable
    get() = when (this) {
        is ImageVectorToken.DrawableToken -> {
            ImageVector.vectorResource(id = resID)
        }

        is ImageVectorToken.VectorToken -> {
            imageVector
        }
    }
