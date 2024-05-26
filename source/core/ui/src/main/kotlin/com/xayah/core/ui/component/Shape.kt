package com.xayah.core.ui.component

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

val ClippedCircleShape = ClippedCircleShapeClass()

class ClippedCircleShapeClass : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val bkg = Path().apply {
            addRoundRect(
                roundRect = RoundRect(
                    rect = size.toRect(),
                    topLeft = CornerRadius(CornerSize(50).toPx(size, density)),
                    topRight = CornerRadius(CornerSize(50).toPx(size, density)),
                    bottomRight = CornerRadius(CornerSize(50).toPx(size, density)),
                    bottomLeft = CornerRadius(CornerSize(50).toPx(size, density)),
                )
            )
        }
        val clip = Path().apply {
            val rect = Rect(
                offset = Offset(
                    x = size.width * 0.69f,
                    y = Offset.Zero.y
                ),
                size = Size(
                    width = Offset.Zero.x + size.width,
                    height = Offset.Zero.y + size.height
                )
            )
            addRoundRect(
                roundRect = RoundRect(
                    rect = rect,
                    topLeft = CornerRadius(CornerSize(50).toPx(size, density)),
                    topRight = CornerRadius(CornerSize(50).toPx(size, density)),
                    bottomRight = CornerRadius(CornerSize(50).toPx(size, density)),
                    bottomLeft = CornerRadius(CornerSize(50).toPx(size, density)),
                )
            )
        }

        return Outline.Generic(
            Path.combine(path1 = bkg, path2 = clip, operation = PathOperation.Difference)
        )
    }
}
