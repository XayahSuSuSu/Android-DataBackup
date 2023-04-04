package com.xayah.databackup.ui.activity.list.common.components.item

import com.xayah.databackup.data.AppInfoRestore
import com.xayah.databackup.data.MediaInfoRestore
import com.xayah.databackup.librootservice.RootService
import com.xayah.databackup.util.GlobalObject
import com.xayah.databackup.util.Path

fun deleteAppInfoRestoreItem(
    appInfoRestore: AppInfoRestore,
    onSuccess: () -> Unit
): Boolean {
    val path = "${Path.getBackupDataSavePath()}/${appInfoRestore.detailBase.packageName}/${appInfoRestore.date}"
    return if (RootService.getInstance().deleteRecursively(path)) {
        appInfoRestore.detailRestoreList.removeAt(appInfoRestore.restoreIndex)
        appInfoRestore.restoreIndex = appInfoRestore.detailRestoreList.size - 1
        if (appInfoRestore.detailRestoreList.isEmpty()) {
            GlobalObject.getInstance().appInfoRestoreMap.value.remove(appInfoRestore.detailBase.packageName)
        }
        onSuccess()
        true
    } else {
        false
    }
}

fun deleteMediaInfoRestoreItem(
    mediaInfoRestore: MediaInfoRestore,
    onSuccess: () -> Unit
): Boolean {
    val path = "${Path.getBackupMediaSavePath()}/${mediaInfoRestore.name}/${mediaInfoRestore.date}"
    return if (RootService.getInstance().deleteRecursively(path)) {
        mediaInfoRestore.detailRestoreList.removeAt(mediaInfoRestore.restoreIndex)
        mediaInfoRestore.restoreIndex = mediaInfoRestore.detailRestoreList.size - 1
        if (mediaInfoRestore.detailRestoreList.isEmpty()) {
            GlobalObject.getInstance().mediaInfoRestoreMap.value.remove(
                mediaInfoRestore.name
            )
        }
        onSuccess()
        true
    } else {
        false
    }
}
