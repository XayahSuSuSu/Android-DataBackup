package com.xayah.feature.main.settings.redesigned

import android.content.Intent
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.xayah.core.datastore.KeyMonet
import com.xayah.core.ui.component.Clickable
import com.xayah.core.ui.component.Switchable
import com.xayah.core.ui.component.Title
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.route.MainRoutes
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.LocalNavController
import com.xayah.core.ui.util.fromDrawable
import com.xayah.core.ui.util.fromString
import com.xayah.feature.main.settings.R
import com.xayah.feature.setup.MainActivity as SetupActivity

@ExperimentalLayoutApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun PageSettings() {
    val context = LocalContext.current
    val navController = LocalNavController.current!!
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    SettingsScaffold(
        scrollBehavior = scrollBehavior,
        title = StringResourceToken.fromString("Settings"),
        actions = {}
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(SizeTokens.Level24)
        ) {
            Column {
                Clickable(
                    icon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_acute),
                    title = StringResourceToken.fromString("Backup settings"),
                    value = StringResourceToken.fromString("Manage backups, compression method, backup user, encryption method"),
                ) {
                    navController.navigate(MainRoutes.BackupSettings.route)
                }
                Clickable(
                    icon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_history),
                    title = StringResourceToken.fromString("Restore settings"),
                    value = StringResourceToken.fromString("Restore SSAID, target user, restore method"),
                ) {
                    navController.navigate(MainRoutes.RestoreSettings.route)
                }
                Clickable(
                    title = StringResourceToken.fromString("Setup"),
                    value = StringResourceToken.fromString("Enter the setup page again"),
                ) {
                    (context as ComponentActivity).finish()
                    context.startActivity(Intent(context, SetupActivity::class.java))
                }
            }
            Title(title = StringResourceToken.fromString("Manage backups")) {
                Clickable(
                    title = StringResourceToken.fromString("Backup directory"),
                    value = StringResourceToken.fromString("Internal storage (30 GB / 100 GB)"),
                ) {
                    navController.navigate(MainRoutes.Directory.route)
                }
                Clickable(
                    title = StringResourceToken.fromString("All backups"),
                    value = StringResourceToken.fromString("3 backups found (10 GB)"),
                )
            }
            Title(title = StringResourceToken.fromString("Appearance")) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Switchable(
                        key = KeyMonet,
                        title = StringResourceToken.fromString("Monet"),
                        checkedText = StringResourceToken.fromString("Generate colors from wallpaper"),
                    )
                }
                DarkThemeSelectable()
            }
        }
    }
}
