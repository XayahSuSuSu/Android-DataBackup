package com.xayah.databackup.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.paddingStart(start: Dp) =
    this.padding(start, 0.dp, 0.dp, 0.dp)

fun Modifier.paddingTop(top: Dp) =
    this.padding(0.dp, top, 0.dp, 0.dp)

fun Modifier.paddingEnd(end: Dp) =
    this.padding(0.dp, 0.dp, end, 0.dp)

fun Modifier.paddingBottom(bottom: Dp) =
    this.padding(0.dp, 0.dp, 0.dp, bottom)

fun Modifier.paddingHorizontal(horizontal: Dp) =
    this.padding(horizontal, 0.dp)

fun Modifier.paddingVertical(vertical: Dp) =
    this.padding(0.dp, vertical)
