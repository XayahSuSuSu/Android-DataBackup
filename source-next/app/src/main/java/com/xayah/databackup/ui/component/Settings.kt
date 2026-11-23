package com.xayah.databackup.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.databackup.R
import com.xayah.databackup.entity.backup.ArchiveCompression
import com.xayah.databackup.util.ArchiveCompressionSetting
import com.xayah.databackup.util.AutoScreenOff
import com.xayah.databackup.util.MaxParallelAppBackups
import com.xayah.databackup.util.MinParallelAppBackups
import com.xayah.databackup.util.ParallelAppBackups
import com.xayah.databackup.util.ResetBackupList
import com.xayah.databackup.util.SkipAppCache
import com.xayah.databackup.util.readEnum
import com.xayah.databackup.util.readInt
import com.xayah.databackup.util.saveEnum
import com.xayah.databackup.util.saveInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun AutoScreenOffSwitch() {
    SwitchablePreference(
        icon = ImageVector.vectorResource(R.drawable.ic_eye_off),
        title = stringResource(R.string.auto_screen_off),
        subtitle = stringResource(R.string.auto_screen_off_desc),
        dataStorePair = AutoScreenOff
    )
}


@Composable
fun ResetBackupListSwitch() {
    SwitchablePreference(
        icon = ImageVector.vectorResource(R.drawable.ic_list_restart),
        title = stringResource(R.string.reset_backup_list),
        subtitle = stringResource(R.string.reset_backup_list_desc),
        dataStorePair = ResetBackupList
    )
}

@Composable
fun SkipAppCacheSwitch() {
    SwitchablePreference(
        icon = ImageVector.vectorResource(R.drawable.ic_brush_cleaning),
        title = stringResource(R.string.skip_app_cache),
        subtitle = stringResource(R.string.skip_app_cache_desc),
        dataStorePair = SkipAppCache
    )
}

/** Shows the Archive compression preset; each tap selects the next preset. */
@Composable
fun ArchiveCompressionPreference() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val compression by context.readEnum(ArchiveCompressionSetting)
        .collectAsStateWithLifecycle(initialValue = ArchiveCompressionSetting.second)
    val presetDescription = when (compression) {
        ArchiveCompression.Fast -> stringResource(R.string.archive_compression_fast)
        ArchiveCompression.Balanced -> stringResource(R.string.archive_compression_balanced)
        ArchiveCompression.Small -> stringResource(R.string.archive_compression_small)
    }
    Preference(
        icon = ImageVector.vectorResource(R.drawable.ic_archive),
        title = stringResource(R.string.archive_compression),
        subtitle = stringResource(R.string.tap_to_change, presetDescription),
        onClick = {
            val presets = ArchiveCompression.entries
            val nextPreset = presets[(compression.ordinal + 1) % presets.size]
            scope.launch(Dispatchers.Default) {
                context.saveEnum(ArchiveCompressionSetting.first, nextPreset)
            }
        }
    )
}

/** Shows how many apps an Archive backup processes at once; each tap selects the next count. */
@Composable
fun ParallelAppBackupsPreference() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val compression by context.readEnum(ArchiveCompressionSetting)
        .collectAsStateWithLifecycle(initialValue = ArchiveCompressionSetting.second)
    val appCount by context.readInt(ParallelAppBackups).collectAsStateWithLifecycle(initialValue = ParallelAppBackups.second)
    val canRunInParallel = compression.canRunInParallel
    Preference(
        enabled = canRunInParallel,
        icon = ImageVector.vectorResource(R.drawable.ic_cpu),
        title = stringResource(R.string.parallel_app_backups),
        subtitle = if (canRunInParallel) {
            stringResource(R.string.tap_to_change, stringResource(R.string.parallel_app_backups_desc, appCount))
        } else {
            stringResource(R.string.parallel_app_backups_unavailable)
        },
        onClick = {
            if (canRunInParallel) {
                val nextCount = if (appCount >= MaxParallelAppBackups) MinParallelAppBackups else appCount + 1
                scope.launch(Dispatchers.Default) {
                    context.saveInt(ParallelAppBackups.first, nextCount)
                }
            }
        }
    )
}
