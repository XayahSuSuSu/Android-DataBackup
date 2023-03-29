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
    val path = "${Path.getBackupDataSavePath()}/${appInfoRestore.detailBase.packageName}/${appInfoRestore.detailRestoreList[appInfoRestore.restoreIndex].date}"
    return if (RootService.getInstance().deleteRecursively(path)) {
        appInfoRestore.detailRestoreList.remove(
            appInfoRestore.detailRestoreList[appInfoRestore.restoreIndex]
        )
        appInfoRestore.restoreIndex--
        if (appInfoRestore.detailRestoreList.isEmpty()) {
            GlobalObject.getInstance().appInfoRestoreMap.value.remove(
                appInfoRestore.detailBase.packageName
            )
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
    val path = "${Path.getBackupMediaSavePath()}/${mediaInfoRestore.name}/${mediaInfoRestore.detailRestoreList[mediaInfoRestore.restoreIndex].date}"
    return if (RootService.getInstance().deleteRecursively(path)) {
        mediaInfoRestore.detailRestoreList.remove(
            mediaInfoRestore.detailRestoreList[mediaInfoRestore.restoreIndex]
        )
        mediaInfoRestore.restoreIndex--
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
