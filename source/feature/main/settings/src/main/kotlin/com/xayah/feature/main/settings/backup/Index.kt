package com.xayah.feature.main.settings.backup

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.datastore.KeyBackupItself
import com.xayah.core.datastore.KeyCheckKeystore
import com.xayah.core.datastore.KeyCompressionTest
import com.xayah.core.datastore.KeyFollowSymlinks
import com.xayah.core.datastore.KeyLoadSystemApps
import com.xayah.core.datastore.readKillAppOption
import com.xayah.core.datastore.saveKillAppOption
import com.xayah.core.model.KillAppOption
import com.xayah.core.model.util.indexOf
import com.xayah.core.ui.component.InnerBottomSpacer
import com.xayah.core.ui.component.LocalSlotScope
import com.xayah.core.ui.component.Selectable
import com.xayah.core.ui.component.Switchable
import com.xayah.core.ui.component.select
import com.xayah.core.ui.model.DialogRadioItem
import com.xayah.core.ui.token.SizeTokens
import com.xayah.feature.main.settings.R
import com.xayah.feature.main.settings.SettingsScaffold

@ExperimentalLayoutApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun PageBackupSettings() {
    val context = LocalContext.current
    val dialogState = LocalSlotScope.current!!.dialogSlot
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    SettingsScaffold(
        scrollBehavior = scrollBehavior,
        title = stringResource(id = R.string.backup_settings),
        actions = {}
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(SizeTokens.Level24)
        ) {
            Column {
                val items = stringArrayResource(id = R.array.kill_app_options)
                val dialogItems by remember(items) {
                    mutableStateOf(items.mapIndexed { index, s ->
                        DialogRadioItem(enum = KillAppOption.indexOf(index), title = s, desc = null)
                    })
                }
                val currentOption by context.readKillAppOption().collectAsStateWithLifecycle(initialValue = KillAppOption.OPTION_II)
                val currentIndex by remember(currentOption) { mutableIntStateOf(currentOption.ordinal) }
                Selectable(
                    title = stringResource(id = R.string.kill_app_options),
                    value = stringResource(id = R.string.kill_app_options_desc),
                    current = items[currentIndex]
                ) {
                    val (state, selectedIndex) = dialogState.select(
                        title = context.getString(R.string.kill_app_options),
                        defIndex = currentIndex,
                        items = dialogItems
                    )
                    if (state.isConfirm) {
                        context.saveKillAppOption(dialogItems[selectedIndex].enum!!)
                    }
                }

                Switchable(
                    key = KeyLoadSystemApps,
                    defValue = false,
                    title = stringResource(id = R.string.load_system_apps),
                    checkedText = stringResource(id = com.xayah.feature.setup.R.string.enabled),
                    notCheckedText = stringResource(id = com.xayah.feature.setup.R.string.not_enabled),
                )
                Switchable(
                    key = KeyCheckKeystore,
                    defValue = true,
                    title = stringResource(id = R.string.check_keystore),
                    checkedText = stringResource(id = R.string.check_keystore_desc),
                )
                Switchable(
                    key = KeyBackupItself,
                    defValue = true,
                    title = stringResource(id = R.string.backup_itself),
                    checkedText = stringResource(id = R.string.backup_itself_desc),
                )
                Switchable(
                    key = KeyCompressionTest,
                    defValue = true,
                    title = stringResource(id = R.string.compression_test),
                    checkedText = stringResource(id = R.string.compression_test_desc),
                )
                /**
                 * Switchable(
                 *     key = KeyCompatibleMode,
                 *     defValue = Build.VERSION.SDK_INT < Build.VERSION_CODES.P,
                 *     title = stringResource(id = R.string.compatible_mode),
                 *     checkedText = stringResource(id = R.string.compatible_mode_desc),
                 * )
                 */
                Switchable(
                    key = KeyFollowSymlinks,
                    defValue = false,
                    title = stringResource(id = R.string.follow_symlinks),
                    checkedText = stringResource(id = R.string.follow_symlinks_desc),
                )
            }
            InnerBottomSpacer(innerPadding = it)
        }
    }
}
