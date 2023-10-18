package com.xayah.databackup.util.command

import androidx.compose.runtime.MutableState
import com.xayah.databackup.util.DataType

fun AdditionUtil.backupItselfExtension(targetPath: String) {}
private suspend fun PackagesBackupUtil.setUploadingState(type: DataType) {}
fun PackagesBackupUtil.mkdirsExtension() {}
fun PackagesBackupUtil.backupArchiveExtension(targetPath: String, type: DataType, isSuccess: MutableState<Boolean>, entityLog: MutableList<String>) {}
fun PackagesBackupUtil.backupArchiveExtension(targetPath: String) {}
fun PackagesBackupAfterwardsUtil.backupArchiveExtension(targetPath: String) {}
