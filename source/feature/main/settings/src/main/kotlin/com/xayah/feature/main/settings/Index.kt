package com.xayah.feature.main.settings

import android.os.Build
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.datastore.readBackupItself
import com.xayah.core.datastore.readCheckKeystore
import com.xayah.core.datastore.readCleanRestoring
import com.xayah.core.datastore.readCompatibleMode
import com.xayah.core.datastore.readCompressionTest
import com.xayah.core.datastore.readFollowSymlinks
import com.xayah.core.datastore.readMonet
import com.xayah.core.datastore.saveBackupItself
import com.xayah.core.datastore.saveCheckKeystore
import com.xayah.core.datastore.saveCleanRestoring
import com.xayah.core.datastore.saveCompatibleMode
import com.xayah.core.datastore.saveCompressionTest
import com.xayah.core.datastore.saveFollowSymlinks
import com.xayah.core.datastore.saveMonet
import com.xayah.core.ui.component.VerticalGrid
import com.xayah.core.ui.component.paddingHorizontal
import com.xayah.core.ui.component.paddingVertical
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.token.PaddingTokens
import com.xayah.core.ui.util.fromDrawable
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.fromVector
import kotlinx.coroutines.runBlocking
import com.xayah.core.ui.R as UiR

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun PageSettings() {
    val viewModel = hiltViewModel<IndexViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Spacer(modifier = Modifier.paddingVertical(PaddingTokens.Level3))
        InfoCard(uiState.settingsInfoItems)
        Divider(modifier = Modifier.padding(PaddingTokens.Level3))
        ApplicationSettings()
        SettingsTitle(modifier = Modifier.paddingVertical(PaddingTokens.Level3), title = StringResourceToken.fromStringId(R.string.backup))
        BackupSettings()
        SettingsTitle(modifier = Modifier.paddingVertical(PaddingTokens.Level3), title = StringResourceToken.fromStringId(R.string.restore))
        RestoreSettings()
        Spacer(modifier = Modifier.paddingVertical(PaddingTokens.Level3))
    }
}

@Composable
private fun RestoreSettings() {
    val context = LocalContext.current
    val cleanRestoring by context.readCleanRestoring().collectAsState(initial = true)
    SettingsSwitch(
        icon = ImageVectorToken.fromDrawable(UiR.drawable.ic_rounded_mop),
        title = StringResourceToken.fromStringId(R.string.clean_restoring),
        content = StringResourceToken.fromStringId(R.string.clean_restoring_desc),
        checked = cleanRestoring,
    ) {
        runBlocking { context.saveCleanRestoring(cleanRestoring.not()) }
    }
}

@Composable
private fun BackupSettings() {
    val context = LocalContext.current
    val checkKeystore by context.readCheckKeystore().collectAsState(initial = true)
    SettingsSwitch(
        icon = ImageVectorToken.fromVector(Icons.Rounded.Code),
        title = StringResourceToken.fromStringId(R.string.check_keystore),
        content = StringResourceToken.fromStringId(R.string.check_keystore_desc),
        checked = checkKeystore,
    ) {
        runBlocking { context.saveCheckKeystore(checkKeystore.not()) }
    }
    val backupItself by context.readBackupItself().collectAsState(initial = true)
    SettingsSwitch(
        icon = ImageVectorToken.fromDrawable(UiR.drawable.ic_rounded_join_inner),
        title = StringResourceToken.fromStringId(R.string.backup_itself),
        content = StringResourceToken.fromStringId(R.string.backup_itself_desc),
        checked = backupItself,
    ) {
        runBlocking { context.saveBackupItself(backupItself.not()) }
    }
    val compressionTest by context.readCompressionTest().collectAsState(initial = true)
    SettingsSwitch(
        icon = ImageVectorToken.fromDrawable(UiR.drawable.ic_rounded_layers),
        title = StringResourceToken.fromStringId(R.string.compression_test),
        content = StringResourceToken.fromStringId(R.string.compression_test_desc),
        checked = compressionTest
    ) {
        runBlocking { context.saveCompressionTest(compressionTest.not()) }
    }
    val compatibleMode by context.readCompatibleMode().collectAsState(initial = true)
    SettingsSwitch(
        icon = ImageVectorToken.fromDrawable(UiR.drawable.ic_rounded_build),
        title = StringResourceToken.fromStringId(R.string.compatible_mode),
        content = StringResourceToken.fromStringId(R.string.compatible_mode_desc),
        checked = compatibleMode
    ) {
        runBlocking { context.saveCompatibleMode(compatibleMode.not()) }
    }
    val followSymlinks by context.readFollowSymlinks().collectAsState(initial = true)
    SettingsSwitch(
        icon = ImageVectorToken.fromVector(Icons.Rounded.Link),
        title = StringResourceToken.fromStringId(R.string.follow_symlinks),
        content = StringResourceToken.fromStringId(R.string.follow_symlinks_desc),
        checked = followSymlinks
    ) {
        runBlocking { context.saveFollowSymlinks(followSymlinks.not()) }
    }
}

@Composable
private fun ApplicationSettings() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        val monet by context.readMonet().collectAsState(initial = true)
        SettingsSwitch(
            icon = ImageVectorToken.fromDrawable(UiR.drawable.ic_round_auto_awesome),
            title = StringResourceToken.fromStringId(R.string.monet),
            checked = monet,
            content = StringResourceToken.fromStringId(R.string.monet_desc)
        ) {
            runBlocking { context.saveMonet(monet.not()) }
        }
    }
}

@Composable
private fun InfoCard(items: List<SettingsInfoItem>) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .paddingHorizontal(PaddingTokens.Level3)
    ) {
        VerticalGrid(columns = 2, count = items.size) { index ->
            SettingsInfo(
                modifier = Modifier
                    .padding(PaddingTokens.Level3)
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { if (items[index].onClick != null) items[index].onClick!!.invoke() },
                info = items[index]
            )
        }
    }
}
