package com.xayah.databackup.service.restore.rustic

import android.content.Context
import androidx.annotation.WorkerThread
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.xayah.databackup.entity.backup.BackupSourceCategory
import com.xayah.databackup.entity.restore.RestoreProgressCallback
import com.xayah.databackup.entity.rustic.RusticBackupManifest
import com.xayah.databackup.entity.rustic.requireFullSnapshotId
import com.xayah.databackup.service.restore.MessageRestorePreparer
import com.xayah.databackup.service.restore.RestoreMessagesHelper
import com.xayah.databackup.util.PathHelper
import com.xayah.libnative.RusticWrapper
import java.io.File

/**
 * Restores selected SMS and MMS records from a Rustic snapshot through Telephony Provider.
 * Must run in the root process with the caller's Binder identity cleared.
 * Validates metadata with [MessageRestorePreparer] before writing and restores only manifest-listed attachments.
 * Creates destination message records, resolves conversation threads and writes attachments through provider URIs.
 * Skips duplicates and records identified as unrestorable, returning their inventory keys on success.
 * On failure, attempts to remove the current partial MMS; previously restored messages remain intact.
 *
 * @see [TelephonyBackupAgent.java](https://cs.android.com/android/platform/superproject/+/android-17.0.0_r1:packages/providers/TelephonyProvider/src/com/android/providers/telephony/TelephonyBackupAgent.java)
 * @see [PduPersister.java](https://cs.android.com/android/platform/superproject/+/android-17.0.0_r1:frameworks/base/telephony/common/com/google/android/mms/pdu/PduPersister.java)
 */
internal class RusticRestoreMessagesHelper(private val mContext: Context, private val mCacheDir: File) {
    @WorkerThread
    fun restore(
        repositoryPath: String,
        password: String,
        snapshotId: String,
        messageIds: List<String>,
        callback: RestoreProgressCallback,
    ): List<String> {
        requireFullSnapshotId(snapshotId)
        require(messageIds.isNotEmpty()) { "No messages selected" }
        fun getMetadataPath(relative: String) = PathHelper.getRusticSnapshotMetadataFilePath(relative)
        val smsPath = getMetadataPath(PathHelper.getBackupMessagesSmsConfigFileRelativePath())
        val mmsPath = getMetadataPath(PathHelper.getBackupMessagesMmsConfigFileRelativePath())
        val manifestPath = getMetadataPath(PathHelper.getRusticManifestFileRelativePath())
        val paths = buildList {
            if (messageIds.any { it.startsWith("sms:") }) add(smsPath)
            if (messageIds.any { it.startsWith("mms:") }) { add(mmsPath); add(manifestPath) }
        }
        val moshi = Moshi.Builder().build()
        val files = requireNotNull(moshi.adapter<Map<String, String>>().fromJson(
            RusticWrapper.readSnapshotTextFiles(repositoryPath, password, snapshotId, paths)
        ))
        // Validate all selected metadata before any provider writes.
        val prepared = MessageRestorePreparer.prepare(files[smsPath], files[mmsPath], messageIds)
        val includedAttachments = if (messageIds.any { it.startsWith("mms:") }) {
            val manifest = requireNotNull(moshi.adapter<RusticBackupManifest>().fromJson(requireNotNull(files[manifestPath])))
            require(manifest.schemaVersion == RusticBackupManifest.CURRENT_SCHEMA_VERSION) { "Unsupported backup schema" }
            manifest.included.filter { it.category == BackupSourceCategory.MmsAttachment }.map { it.path }.toSet()
        } else emptySet()
        return RestoreMessagesHelper(mContext, mCacheDir).restore(prepared, messageIds, includedAttachments, callback) { path, file ->
            RusticWrapper.restoreSnapshot(
                repositoryPath = repositoryPath,
                password = password,
                snapshotId = "$snapshotId:$path",
                destinationPath = file.path,
                options = RusticWrapper.RestoreOptions(noOwnership = true),
            )
        }
    }
}
