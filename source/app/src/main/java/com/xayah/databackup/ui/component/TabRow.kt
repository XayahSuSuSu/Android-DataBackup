package com.xayah.databackup.ui.component

import androidx.annotation.FloatRange
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.xayah.databackup.ui.component.material3.TabPosition
import com.xayah.databackup.ui.component.material3.tabIndicatorOffset
import com.xayah.databackup.ui.theme.ColorScheme

/**
 * @see <a href="https://juejin.cn/post/7273680143658238004">PagerTabIndicator.kt</a>
 */
@Composable
fun RoundedCornerIndicator(
    tabPositions: List<TabPosition>,
    selectedTabIndex: Int,
    color: Color = ColorScheme.primary(),
    @FloatRange(from = 0.0, to = 1.0) percent: Float = 1f,
) {
    val currentTab = tabPositions[selectedTabIndex]

    Canvas(
        modifier = Modifier
            .tabIndicatorOffset(tabPositions[selectedTabIndex])
            .fillMaxSize(),
        onDraw = {
            val indicatorWidth = currentTab.width.toPx() * percent

            val canvasHeight = size.height * percent
            drawRoundRect(
                color = color,
                topLeft = Offset(currentTab.width.toPx() * (1 - percent) / 2, size.height * (1 - percent) / 2),
                size = Size(indicatorWidth, canvasHeight),
                cornerRadius = CornerRadius((canvasHeight / 2))
            )
        }
    )
}
