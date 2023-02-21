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
import com.xayah.databackup.compose.ui.activity.list.components.AppBackupItem
import com.xayah.databackup.compose.ui.activity.list.components.ManifestDescItem
import com.xayah.databackup.compose.ui.activity.list.components.SearchBar
import com.xayah.databackup.compose.ui.activity.processing.ProcessingActivity
import com.xayah.databackup.data.AppInfoBackup
import com.xayah.databackup.data.TypeActivityTag
import com.xayah.databackup.data.TypeBackupApp
import com.xayah.databackup.data.ofBackupStrategy
import com.xayah.databackup.util.*
import java.text.Collator
import java.util.*

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
fun LazyListScope.contentAppBackup(list: List<AppInfoBackup>, onSearch: (String) -> Unit) {
    item {
        SearchBar(onSearch)
    }
    items(
        count = list.size,
        key = {
            list[it].detailBase.packageName
        }) { index ->
        AppBackupItem(
            modifier = Modifier.animateItemPlacement(),
            appInfoBackup = list[index]
        )
    }
}

suspend fun onAppBackupInitialize(viewModel: ListViewModel) {
    if (GlobalObject.getInstance().appInfoBackupMap.value.isEmpty()) {
        GlobalObject.getInstance().appInfoBackupMap.emit(Command.getAppInfoBackupMap())
    }
    viewModel.appBackupList.value.addAll(
        GlobalObject.getInstance().appInfoBackupMap.value.values.toList()
            .filter { it.detailBase.isSystemApp.not() }
    )
    viewModel.appBackupList.value.sortWith { appInfo1, appInfo2 ->
        if (appInfo1 == null && appInfo2 == null) {
            0
        } else if (appInfo1 == null) {
            -1
        } else if (appInfo2 == null) {
            1
        } else {
            val collator = Collator.getInstance(Locale.CHINA)
            collator.getCollationKey(appInfo1.detailBase.appName)
                .compareTo(collator.getCollationKey(appInfo2.detailBase.appName))
        }
    }
    viewModel.isInitialized.value = true
}

@ExperimentalMaterial3Api
fun LazyListScope.onAppBackupManifest(viewModel: ListViewModel, context: Context) {
    val list = listOf(
        ManifestDescItem(
            title = context.getString(R.string.selected_app),
            subtitle = run {
                var size = 0
                for (i in viewModel.appBackupList.value) {
                    if (i.selectApp) size++
                }
                size.toString()
            },
            iconId = R.drawable.ic_round_apps
        ),
        ManifestDescItem(
            title = context.getString(R.string.selected_data),
            subtitle = run {
                var size = 0
                for (i in viewModel.appBackupList.value) {
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
            subtitle = context.readCompressionType(),
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
fun LazyListScope.onAppBackupContent(viewModel: ListViewModel) {
    contentAppBackup(list = viewModel.appBackupList.value) { value ->
        viewModel.appBackupList.value.apply {
            viewModel.isInitialized.value = false
            clear()
            addAll(
                GlobalObject.getInstance().appInfoBackupMap.value.values.toList()
                    .filter { it.detailBase.isSystemApp.not() }
                    .filter {
                        it.detailBase.appName.lowercase()
                            .contains(value.lowercase()) ||
                                it.detailBase.packageName.lowercase()
                                    .contains(value.lowercase())
                    }
            )
            viewModel.isInitialized.value = true
        }
    }
}

@ExperimentalMaterial3Api
fun toAppBackupProcessing(context: Context) {
    context.startActivity(Intent(context, ProcessingActivity::class.java).apply {
        putExtra(TypeActivityTag, TypeBackupApp)
    })
}

suspend fun onAppBackupMapSave() {
    GsonUtil.saveAppInfoBackupMapToFile(GlobalObject.getInstance().appInfoBackupMap.value)
}
