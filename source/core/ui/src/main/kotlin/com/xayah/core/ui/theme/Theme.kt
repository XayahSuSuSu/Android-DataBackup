package com.xayah.core.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.xayah.core.datastore.readMonet
import com.xayah.core.datastore.readThemeType
import com.xayah.core.model.ThemeType
import com.xayah.core.ui.component.LocalSlotScope
import com.xayah.core.ui.component.rememberSlotScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@Composable
fun darkTheme() = run {
    val context = LocalContext.current
    val themeType by context.readThemeType().collectImmediatelyAsState()
    when (themeType) {
        ThemeType.AUTO -> isSystemInDarkTheme()
        else -> remember(themeType) { themeType != ThemeType.LIGHT_THEME }
    }
}

@Composable
fun DataBackupTheme(
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val darkTheme = darkTheme()
    // Dynamic color is available on Android 12+
    val dynamicColor by context.readMonet().collectImmediatelyAsState()
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    // Transparent system bars
    val systemUiController = rememberSystemUiController()
    SideEffect {
        systemUiController.setStatusBarColor(
            color = Color.Transparent,
            darkIcons = !darkTheme
        )
        systemUiController.setNavigationBarColor(
            color = Color.Transparent,
            darkIcons = !darkTheme,
            navigationBarContrastEnforced = false
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = {
            val slotScope = rememberSlotScope()
            CompositionLocalProvider(LocalSlotScope provides slotScope, content = content)
        }
    )
}

@Composable
private fun <T> Flow<T>.collectImmediatelyAsState(): State<T> = produceState(
    initialValue = runBlocking { first() }, // DataStore shouldn't be really heavy
    key1 = this,
) {
    drop(1).collect { value = it }
}
