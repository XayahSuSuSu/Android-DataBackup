package com.xayah.databackup.ui.activity.list.common.components.item

import com.xayah.databackup.data.AppInfoRestore
import com.xayah.databackup.data.MediaInfoRestore
import com.xayah.databackup.util.GlobalObject
import com.xayah.databackup.util.Path
import com.xayah.databackup.util.command.Command

suspend fun deleteAppInfoRestoreItem(
    appInfoRestore: AppInfoRestore,
    onSuccess: () -> Unit
): Boolean {
    Command.rm("${Path.getBackupDataSavePath()}/${appInfoRestore.detailBase.packageName}/${appInfoRestore.detailRestoreList[appInfoRestore.restoreIndex].date}")
        .apply {
            return if (this) {
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
}

suspend fun deleteMediaInfoRestoreItem(
    mediaInfoRestore: MediaInfoRestore,
    onSuccess: () -> Unit
): Boolean {
    Command.rm("${Path.getBackupMediaSavePath()}/${mediaInfoRestore.name}/${mediaInfoRestore.detailRestoreList[mediaInfoRestore.restoreIndex].date}")
        .apply {
            return if (this) {
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
}
