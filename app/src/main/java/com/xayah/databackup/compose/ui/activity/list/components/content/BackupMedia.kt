package com.xayah.databackup.compose.ui.activity.list.components.content

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Modifier
import com.xayah.databackup.R
import com.xayah.databackup.compose.ui.activity.list.ListViewModel
import com.xayah.databackup.compose.ui.activity.list.components.ManifestDescItem
import com.xayah.databackup.compose.ui.activity.list.components.MediaBackupItem
import com.xayah.databackup.compose.ui.activity.list.components.SearchBar
import com.xayah.databackup.compose.ui.activity.processing.ProcessingActivity
import com.xayah.databackup.data.MediaInfoBackup
import com.xayah.databackup.data.TypeActivityTag
import com.xayah.databackup.data.TypeBackupMedia
import com.xayah.databackup.data.ofBackupStrategy
import com.xayah.databackup.util.*

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
fun LazyListScope.contentRestoreBackup(list: List<MediaInfoBackup>, onSearch: (String) -> Unit) {
    item {
        SearchBar(onSearch)
    }
    items(
        count = list.size,
        key = {
            list[it].name
        }) { index ->
        MediaBackupItem(
            modifier = Modifier.animateItemPlacement(),
            mediaInfoBackup = list[index]
        )
    }
}

suspend fun onMediaBackupInitialize(viewModel: ListViewModel) {
    if (GlobalObject.getInstance().mediaInfoBackupMap.value.isEmpty()) {
        GlobalObject.getInstance().mediaInfoBackupMap.emit(Command.getMediaInfoBackupMap())
    }
    if (viewModel.mediaBackupList.value.isEmpty()) {
        viewModel.mediaBackupList.value.addAll(
            GlobalObject.getInstance().mediaInfoBackupMap.value.values.toList()
        )
        // 当基于基类成员变量排序时, 会导致LazyColumn key重复使用的bug
    }
    viewModel.isInitialized.targetState = true
}

@ExperimentalMaterial3Api
fun LazyListScope.onMediaBackupManifest(viewModel: ListViewModel, context: Context) {
    val list = listOf(
        ManifestDescItem(
            title = context.getString(R.string.selected_data),
            subtitle = run {
                var size = 0
                for (i in viewModel.mediaBackupList.value) {
                    if (i.selectData) size++
                }
                size.toString()
            },
            iconId = R.drawable.ic_round_database
        ),
        ManifestDescItem(
            title = context.getString(R.string.backup_user),
            subtitle = context.readBackupUser(),
            iconId = R.drawable.ic_round_person
        ),
        ManifestDescItem(
            title = context.getString(R.string.restore_user),
            subtitle = context.readRestoreUser(),
            iconId = R.drawable.ic_round_iphone
        ),
        ManifestDescItem(
            title = context.getString(R.string.compression_type),
            subtitle = "tar",
            iconId = R.drawable.ic_round_bolt
        ),
        ManifestDescItem(
            title = context.getString(R.string.backup_strategy),
            subtitle = ofBackupStrategy(context.readBackupStrategy()),
            iconId = -1,
            icon = Icons.Rounded.Place
        ),
        ManifestDescItem(
            title = context.getString(R.string.backup_dir),
            subtitle = context.readBackupSavePath(),
            iconId = R.drawable.ic_round_folder_open
        ),
    )
    contentManifest(list)
}

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
fun LazyListScope.onMediaBackupContent(viewModel: ListViewModel) {
    contentRestoreBackup(list = viewModel.mediaBackupList.value) { value ->
        viewModel.mediaBackupList.value.apply {
            viewModel.isInitialized.targetState = false
            clear()
            addAll(
                GlobalObject.getInstance().mediaInfoBackupMap.value.values.toList()
                    .filter {
                        it.name.lowercase()
                            .contains(value.lowercase()) ||
                                it.path.lowercase()
                                    .contains(value.lowercase())
                    }
            )
            viewModel.isInitialized.targetState = true
        }
    }
}

@ExperimentalMaterial3Api
fun toMediaBackupProcessing(context: Context) {
    context.startActivity(Intent(context, ProcessingActivity::class.java).apply {
        putExtra(TypeActivityTag, TypeBackupMedia)
    })
}

suspend fun onMediaBackupMapSave() {
    GsonUtil.saveMediaInfoBackupMapToFile(GlobalObject.getInstance().mediaInfoBackupMap.value)
}
