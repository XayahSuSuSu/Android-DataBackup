package com.xayah.databackup.service.restore.archive

import android.content.Context
import android.system.Os
import android.system.OsConstants
import com.xayah.databackup.entity.restore.RestoreProgressCallback
import com.xayah.databackup.service.restore.MessageRestorePreparer
import com.xayah.databackup.service.restore.RestoreMessagesHelper
import com.xayah.databackup.util.PathHelper
import java.io.File

internal class ArchiveRestoreMessagesHelper(private val mContext: Context, private val mCacheDir: File) {
    fun restore(archivePath: String, messageIds: List<String>, callback: RestoreProgressCallback): List<String> {
        require(messageIds.isNotEmpty()) { "No messages selected" }
        val sms = if (messageIds.any { it.startsWith("sms:") }) File(PathHelper.getBackupMessagesSmsConfigFilePath(archivePath)).readText() else null
        val mms = if (messageIds.any { it.startsWith("mms:") }) File(PathHelper.getBackupMessagesMmsConfigFilePath(archivePath)).readText() else null
        val prepared = MessageRestorePreparer.prepare(sms, mms, messageIds)
        val parts = File(PathHelper.getBackupAppPartsDir(PathHelper.getBackupMessagesDir(archivePath)))
        val attachments = prepared.mms.values.flatMap { it.parts }.mapNotNull { it.attachmentPath }.distinct().associateWith { path ->
            val name = PathHelper.getChildPath(path)
            require(name.isNotBlank() && name != "." && name != "..") { "Invalid MMS attachment name" }
            File(parts, name)
        }
        val available = attachments.filterValues { file ->
            file.exists() && file.canonicalFile.parentFile == parts.canonicalFile && OsConstants.S_ISREG(Os.lstat(file.path).st_mode)
        }
        return RestoreMessagesHelper(mContext, mCacheDir).restore(prepared, messageIds, available.keys, callback) { path, target ->
            available.getValue(path).copyTo(target)
        }
    }
}
