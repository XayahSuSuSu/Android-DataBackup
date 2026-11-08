package com.xayah.databackup.service.archive

import com.xayah.databackup.util.PathHelper
import com.xayah.databackup.util.PathHelper.APP_USER_PREFIX
import com.xayah.databackup.util.PathHelper.APP_USER_SEPARATOR

/**
 * Defines user-specific paths for Archive records and MMS attachments.
 */
internal object ArchiveRecordPaths {
    private const val RECORDS_DIR = "records"
    private const val JSON_SUFFIX = ".json"
    private const val NETWORKS_NAME = "networks"
    private const val CONTACTS_NAME = "contacts"
    private const val CALL_LOGS_NAME = "call_logs"
    private const val SMS_NAME = "sms"
    private const val MMS_NAME = "mms"

    private val mRecordNameByMetadataPath = linkedMapOf(
        PathHelper.getBackupNetworksConfigFileRelativePath() to NETWORKS_NAME,
        PathHelper.getBackupContactsConfigFileRelativePath() to CONTACTS_NAME,
        PathHelper.getBackupCallLogsConfigFileRelativePath() to CALL_LOGS_NAME,
        PathHelper.getBackupMessagesSmsConfigFileRelativePath() to SMS_NAME,
        PathHelper.getBackupMessagesMmsConfigFileRelativePath() to MMS_NAME,
    )

    fun getRelativePaths(userId: Int): Map<String, String> = mRecordNameByMetadataPath.mapValues { (_, name) ->
        "${getRecordRelativePath(name, userId)}$JSON_SUFFIX"
    }

    fun getFilePath(parent: String, metadataPath: String, userId: Int): String =
        "$parent/${getRecordRelativePath(mRecordNameByMetadataPath.getValue(metadataPath), userId)}$JSON_SUFFIX"

    fun getMmsAttachmentsDir(parent: String, userId: Int): String = "$parent/${getRecordRelativePath(MMS_NAME, userId)}"

    private fun getRecordRelativePath(name: String, userId: Int): String {
        require(userId >= 0) { "Invalid archive record user" }
        return "$RECORDS_DIR/$name${APP_USER_SEPARATOR}${APP_USER_PREFIX}$userId"
    }
}
