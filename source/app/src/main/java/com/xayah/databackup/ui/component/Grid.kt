package com.xayah.databackup.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlin.math.ceil

@Composable
fun VerticalGrid(
    modifier: Modifier = Modifier,
    columns: Int,
    count: Int,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable() (Int) -> Unit
) {
    Column(modifier = modifier, verticalArrangement = verticalArrangement) {
        val rows = ceil(count.toFloat() / columns).toInt()
        for (row in 0 until rows) {
            val header = row * columns
            Row(horizontalArrangement = horizontalArrangement) {
                for (column in 0 until columns) {
                    val index = header + column
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        if (index < count) {
                            content(index)
                        }
                    }
                }
            }
        }
    }
}
