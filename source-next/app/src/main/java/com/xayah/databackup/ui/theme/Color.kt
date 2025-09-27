package com.xayah.databackup.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.xayah.databackup.ui.theme.color.palettes.TonalPalette

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

val GreenSource = Color(0xFF266A4A)

fun Color.tone(tone: Int): Color {
    return Color(TonalPalette.fromInt(this.toArgb()).tone(tone))
}
