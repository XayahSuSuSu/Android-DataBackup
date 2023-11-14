package com.xayah.databackup.util.command

import androidx.compose.runtime.MutableState
import com.xayah.core.model.DataType
import com.xayah.core.rootservice.parcelables.PathParcelable
import javax.inject.Inject

fun AdditionUtil.backupItselfExtension(targetPath: String) {}
private suspend fun PackagesBackupUtil.setUploadingState(type: DataType) {}
fun PackagesBackupUtil.mkdirsExtension() {}
fun PackagesBackupUtil.getSavePath()=""
fun PackagesBackupUtil.backupArchiveExtension(targetPath: String, type: DataType, isSuccess: MutableState<Boolean>, entityLog: MutableList<String>) {}
fun PackagesBackupUtil.backupArchiveExtension(targetPath: String) {}
fun PackagesBackupAfterwardsUtil.backupArchiveExtension(targetPath: String) {}
fun PackagesBackupAfterwardsUtil.clearUpExtension() {}
class ExtensionUtil @Inject constructor(
) {
    suspend fun deleteTmp() {
    }

    suspend fun fetchTmp(src: String): String {
        return ""
    }

    suspend fun walkFileTree(src: String): List<PathParcelable> {
        return listOf()
    }

    suspend fun getSavePath(): String {
        return ""
    }
}