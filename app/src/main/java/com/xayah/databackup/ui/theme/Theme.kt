package com.xayah.databackup.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.xayah.databackup.App
import com.xayah.databackup.BuildConfig
import com.xayah.databackup.R
import com.xayah.databackup.librootservice.RootService
import com.xayah.databackup.ui.activity.settings.components.initializeBackupDirectory
import com.xayah.databackup.ui.components.TextDialog
import com.xayah.databackup.util.Logcat
import kotlinx.coroutines.launch

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

suspend fun initializeRootService(isDialogOpen: MutableState<Boolean>, onRootServiceInitialized: (suspend () -> Unit)? = null) {
    if (onRootServiceInitialized != null) {
        val service = RootService.getInstance().bindService(BuildConfig.APPLICATION_ID) {
            isDialogOpen.value = true
        }
        if (service != null) {
            initializeBackupDirectory()
            Logcat.getInstance().init()
            onRootServiceInitialized()
        }
    }
}

@ExperimentalMaterial3Api
@Composable
fun DataBackupTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit, onRootServiceInitialized: (suspend () -> Unit)? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colorScheme = when {
        App.isDynamicColors.value && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

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
    val isDialogOpen = remember {
        mutableStateOf(false)
    }
    LaunchedEffect(null) {
        initializeRootService(isDialogOpen, onRootServiceInitialized)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = {
            TextDialog(
                isOpen = isDialogOpen,
                icon = Icons.Rounded.Warning,
                title = stringResource(id = R.string.error),
                content = stringResource(id = R.string.failed_to_connect_rootservice) + stringResource(id = R.string.symbol_exclamation),
                confirmText = stringResource(id = R.string.retry),
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                onConfirmClick = {
                    scope.launch {
                        isDialogOpen.value = false
                        initializeRootService(isDialogOpen, onRootServiceInitialized)
                    }
                },
                showDismissBtn = false
            )
            content()
        }
    )
}