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
import com.xayah.databackup.compose.ui.activity.list.components.MediaRestoreItem
import com.xayah.databackup.compose.ui.activity.list.components.SearchBar
import com.xayah.databackup.compose.ui.activity.processing.ProcessingActivity
import com.xayah.databackup.data.MediaInfoRestore
import com.xayah.databackup.data.TypeActivityTag
import com.xayah.databackup.data.TypeRestoreMedia
import com.xayah.databackup.data.ofBackupStrategy
import com.xayah.databackup.util.*
import java.text.Collator
import java.util.*

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
fun LazyListScope.contentMediaRestore(list: List<MediaInfoRestore>, onSearch: (String) -> Unit) {
    item {
        SearchBar(onSearch)
    }
    items(
        count = list.size,
    ) { index ->
        MediaRestoreItem(
            modifier = Modifier.animateItemPlacement(),
            mediaInfoRestore = list[index]
        )
    }
}

suspend fun onMediaRestoreInitialize(viewModel: ListViewModel) {
    if (GlobalObject.getInstance().mediaInfoRestoreMap.value.isEmpty()) {
        GlobalObject.getInstance().mediaInfoRestoreMap.emit(Command.getMediaInfoRestoreMap())
    }
    viewModel.mediaRestoreList.value.addAll(
        GlobalObject.getInstance().mediaInfoRestoreMap.value.values.toList()
    )
    viewModel.mediaRestoreList.value.sortWith { mediaInfo1, mediaInfo2 ->
        if (mediaInfo1 == null && mediaInfo2 == null) {
            0
        } else if (mediaInfo1 == null) {
            -1
        } else if (mediaInfo2 == null) {
            1
        } else {
            val collator = Collator.getInstance(Locale.CHINA)
            collator.getCollationKey(mediaInfo1.name)
                .compareTo(collator.getCollationKey(mediaInfo2.name))
        }
    }

    viewModel.isInitialized.value = true
}

@ExperimentalMaterial3Api
fun LazyListScope.onMediaRestoreManifest(viewModel: ListViewModel, context: Context) {
    val list = listOf(
        ManifestDescItem(
            title = context.getString(R.string.selected_data),
            subtitle = run {
                var size = 0
                for (i in viewModel.mediaRestoreList.value) {
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
fun LazyListScope.onMediaRestoreContent(viewModel: ListViewModel) {
    contentMediaRestore(list = viewModel.mediaRestoreList.value) { value ->
        viewModel.mediaRestoreList.value.apply {
            viewModel.isInitialized.value = false
            clear()
            addAll(
                GlobalObject.getInstance().mediaInfoRestoreMap.value.values.toList()
                    .filter {
                        it.name.lowercase()
                            .contains(value.lowercase()) ||
                                it.path.lowercase()
                                    .contains(value.lowercase())
                    }
            )
            viewModel.isInitialized.value = true
        }
    }
}

@ExperimentalMaterial3Api
fun toMediaRestoreProcessing(context: Context) {
    context.startActivity(Intent(context, ProcessingActivity::class.java).apply {
        putExtra(TypeActivityTag, TypeRestoreMedia)
    })
}

suspend fun onMediaRestoreMapSave() {
    GsonUtil.saveMediaInfoRestoreMapToFile(GlobalObject.getInstance().mediaInfoRestoreMap.value)
}
