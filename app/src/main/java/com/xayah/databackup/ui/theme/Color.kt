package com.xayah.databackup.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.xayah.databackup.R

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

object ColorScheme {
        @Composable
        fun surface() = MaterialTheme.colorScheme.surface

        @Composable
        fun onSurfaceVariant() = MaterialTheme.colorScheme.onSurfaceVariant

        @Composable
        fun surfaceVariant() = MaterialTheme.colorScheme.surfaceVariant

        @Composable
        fun error() = MaterialTheme.colorScheme.error

        @Composable
        fun errorContainer() = MaterialTheme.colorScheme.errorContainer

        @Composable
        fun green() = colorResource(id = R.color.green)

        @Composable
        fun greenContainer() = colorResource(id = R.color.greenContainer)

        @Composable
        fun onPrimary() = MaterialTheme.colorScheme.onPrimary

        @Composable
        fun primaryContainer() = MaterialTheme.colorScheme.primaryContainer

        @Composable
        fun onTertiary() = MaterialTheme.colorScheme.onTertiary

        @Composable
        fun tertiaryContainer() = MaterialTheme.colorScheme.tertiaryContainer

        @Composable
        fun inverseOnSurface() = MaterialTheme.colorScheme.inverseOnSurface
}