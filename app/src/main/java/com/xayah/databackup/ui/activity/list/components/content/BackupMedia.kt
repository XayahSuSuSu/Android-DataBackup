package com.xayah.databackup.ui.activity.list.components.content

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewModelScope
import com.xayah.databackup.R
import com.xayah.databackup.data.MediaInfoBackup
import com.xayah.databackup.data.TypeActivityTag
import com.xayah.databackup.data.TypeBackupMedia
import com.xayah.databackup.data.ofBackupStrategy
import com.xayah.databackup.ui.activity.list.ListViewModel
import com.xayah.databackup.ui.activity.list.components.ManifestDescItem
import com.xayah.databackup.ui.activity.list.components.MediaBackupItem
import com.xayah.databackup.ui.activity.list.components.SearchBar
import com.xayah.databackup.ui.activity.processing.ProcessingActivity
import com.xayah.databackup.util.*
import com.xayah.materialyoufileexplorer.MaterialYouFileExplorer
import kotlinx.coroutines.launch

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

fun onMediaBackupAdd(
    viewModel: ListViewModel,
    context: Context,
    explorer: MaterialYouFileExplorer
) {
    explorer.apply {
        isFile = false
        toExplorer(context) { path, _ ->
            viewModel.viewModelScope.launch {
                // 判断是否为备份目录
                if (path == context.readBackupSavePath()) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.backup_dir_as_media_error),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }
                var name = path.split("/").last()
                for (i in viewModel.mediaBackupList.value) {
                    if (path == i.path) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.repeat_to_add),
                            Toast.LENGTH_SHORT
                        ).show()
                        return@launch
                    }
                    if (name == i.name) {
                        // 重名媒体资料
                        val nameList = name.split("_").toMutableList()
                        val index = nameList.last().toIntOrNull()
                        if (index == null) {
                            nameList.add("0")
                        } else {
                            nameList[nameList.lastIndex] = (index + 1).toString()
                        }
                        name = nameList.joinToString(separator = "_")
                    }
                }
                val mediaInfo = MediaInfoBackup().apply {
                    this.name = name
                    this.path = path
                    this.backupDetail.apply {
                        this.data = true
                        this.size = ""
                        this.date = ""
                    }
                }
                viewModel.mediaBackupList.value.add(mediaInfo)
                GlobalObject.getInstance().mediaInfoBackupMap.value[mediaInfo.name] = mediaInfo
                GsonUtil.saveMediaInfoBackupMapToFile(GlobalObject.getInstance().mediaInfoBackupMap.value)
            }
        }
    }
}