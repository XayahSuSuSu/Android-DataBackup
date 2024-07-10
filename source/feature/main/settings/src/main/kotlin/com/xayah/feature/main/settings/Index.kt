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
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.datastore.KeyAutoScreenOff
import com.xayah.core.datastore.KeyMonet
import com.xayah.core.datastore.readLanguage
import com.xayah.core.ui.component.Clickable
import com.xayah.core.ui.component.InnerBottomSpacer
import com.xayah.core.ui.component.Switchable
import com.xayah.core.ui.component.Title
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.route.MainRoutes
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.LocalNavController
import com.xayah.core.ui.util.fromDrawable
import com.xayah.core.ui.util.fromString
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.fromVector
import com.xayah.core.util.LanguageUtil
import com.xayah.core.util.LanguageUtil.toLocale
import com.xayah.core.util.getActivity
import kotlinx.coroutines.flow.map
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
        title = StringResourceToken.fromStringId(R.string.settings),
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
                    title = StringResourceToken.fromStringId(R.string.backup_settings),
                ) {
                    navController.navigate(MainRoutes.BackupSettings.route)
                }
                Clickable(
                    icon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_history),
                    title = StringResourceToken.fromStringId(R.string.restore_settings),
                ) {
                    navController.navigate(MainRoutes.RestoreSettings.route)
                }
                Clickable(
                    title = StringResourceToken.fromStringId(R.string.setup),
                    value = StringResourceToken.fromStringId(R.string.enter_the_setup_page_again),
                ) {
                    context.getActivity().finish()
                    context.startActivity(Intent(context, SetupActivity::class.java))
                }
            }
            Title(title = StringResourceToken.fromStringId(R.string.appearance)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Switchable(
                        key = KeyMonet,
                        title = StringResourceToken.fromStringId(R.string.monet),
                        checkedText = StringResourceToken.fromStringId(R.string.monet_desc),
                    )
                }
                DarkThemeSelectable()

                val locale by context.readLanguage().map { it.toLocale(context) }.collectAsStateWithLifecycle(initialValue = LanguageUtil.getSystemLocale(context))
                Clickable(
                    title = StringResourceToken.fromStringId(R.string.language),
                    value = StringResourceToken.fromString(locale.getDisplayName(locale))
                ) {
                    navController.navigate(MainRoutes.LanguageSettings.route)
                }
            }
            Title(title = StringResourceToken.fromStringId(R.string.manage_backups)) {
                Clickable(
                    icon = ImageVectorToken.fromVector(Icons.Outlined.Block),
                    title = StringResourceToken.fromStringId(R.string.blacklist),
                    value = StringResourceToken.fromStringId(R.string.blacklist_desc),
                ) {
                    navController.navigate(MainRoutes.BlackList.route)
                }
                Clickable(
                    icon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_folder_open),
                    title = StringResourceToken.fromStringId(R.string.backup_dir),
                    value = if (directoryState != null) StringResourceToken.fromString(directoryState!!.title) else null,
                ) {
                    navController.navigate(MainRoutes.Directory.route)
                }
            }
            Title(title = StringResourceToken.fromStringId(R.string.advanced)) {
                Switchable(
                    key = KeyAutoScreenOff,
                    defValue = false,
                    title = StringResourceToken.fromStringId(R.string.auto_screen_off),
                    checkedText = StringResourceToken.fromStringId(R.string.auto_screen_off_desc),
                )
                Clickable(
                    title = StringResourceToken.fromStringId(R.string.configurations),
                    value = StringResourceToken.fromStringId(R.string.configurations_desc),
                ) {
                    navController.navigate(MainRoutes.Configurations.route)
                }
                Clickable(
                    title = StringResourceToken.fromStringId(R.string.about),
                    value = StringResourceToken.fromStringId(R.string.about_app),
                ) {
                    navController.navigate(MainRoutes.About.route)
                }
            }
            InnerBottomSpacer(innerPadding = it)
        }
    }
}
