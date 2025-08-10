package com.xayah.feature.main.settings

import android.content.Intent
import android.os.Build
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.datastore.KeyAutoScreenOff
import com.xayah.core.datastore.KeyMonet
import com.xayah.core.ui.component.Clickable
import com.xayah.core.ui.component.InnerBottomSpacer
import com.xayah.core.ui.component.Switchable
import com.xayah.core.ui.component.Title
import com.xayah.core.ui.route.MainRoutes
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.LocalNavController
import com.xayah.core.util.LanguageUtil
import com.xayah.core.util.getActivity
import com.xayah.core.util.navigateSingle
import com.xayah.core.util.readMappedLanguage
import com.xayah.feature.setup.MainActivity as SetupActivity

@ExperimentalLayoutApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun PageSettings() {
    val context = LocalContext.current
    val navController = LocalNavController.current!!
    val viewModel = hiltViewModel<IndexViewModel>()
    val directoryState by viewModel.directoryState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    SettingsScaffold(
        scrollBehavior = scrollBehavior,
        title = stringResource(id = R.string.settings),
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
                    icon = ImageVector.vectorResource(id = R.drawable.ic_rounded_acute),
                    title = stringResource(id = R.string.backup_settings),
                ) {
                    navController.navigateSingle(MainRoutes.BackupSettings.route)
                }
                Clickable(
                    icon = ImageVector.vectorResource(id = R.drawable.ic_rounded_history),
                    title = stringResource(id = R.string.restore_settings),
                ) {
                    navController.navigateSingle(MainRoutes.RestoreSettings.route)
                }
                Clickable(
                    title = stringResource(id = R.string.setup),
                    value = stringResource(id = R.string.enter_the_setup_page_again),
                ) {
                    context.getActivity().finish()
                    context.startActivity(Intent(context, SetupActivity::class.java))
                }
            }
            Title(title = stringResource(id = R.string.appearance)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Switchable(
                        key = KeyMonet,
                        title = stringResource(id = R.string.monet),
                        checkedText = stringResource(id = R.string.monet_desc),
                    )
                }
                DarkThemeSelectable()

                val locale by context.readMappedLanguage().collectAsStateWithLifecycle(initialValue = LanguageUtil.getSystemLocale(context))
                Clickable(
                    title = stringResource(id = R.string.language),
                    value = locale.getDisplayName(locale)
                ) {
                    navController.navigateSingle(MainRoutes.LanguageSettings.route)
                }
            }
            Title(title = stringResource(id = R.string.manage_backups)) {
                Clickable(
                    icon = Icons.Outlined.Block,
                    title = stringResource(id = R.string.blacklist),
                    value = stringResource(id = R.string.blacklist_desc),
                ) {
                    navController.navigateSingle(MainRoutes.BlackList.route)
                }
                Clickable(
                    icon = ImageVector.vectorResource(id = R.drawable.ic_rounded_folder_open),
                    title = stringResource(id = R.string.backup_dir),
                    value = if (directoryState == null) null else stringResource(id = directoryState!!.titleResId),
                ) {
                    navController.navigateSingle(MainRoutes.Directory.route)
                }
            }
            Title(title = stringResource(id = R.string.advanced)) {
                Switchable(
                    key = KeyAutoScreenOff,
                    defValue = false,
                    title = stringResource(id = R.string.auto_screen_off),
                    checkedText = stringResource(id = R.string.auto_screen_off_desc),
                )
                Clickable(
                    title = stringResource(id = R.string.configurations),
                    value = stringResource(id = R.string.configurations_desc),
                ) {
                    navController.navigateSingle(MainRoutes.Configurations.route)
                }
                Clickable(
                    title = stringResource(id = R.string.about),
                    value = stringResource(id = R.string.about_app),
                ) {
                    navController.navigateSingle(MainRoutes.About.route)
                }
            }
            InnerBottomSpacer(innerPadding = it)
        }
    }
}
