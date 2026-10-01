package com.xayah.databackup.service.backup

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.xayah.databackup.adapter.WifiConfigurationAdapter
import com.xayah.databackup.database.entity.CallLog
import com.xayah.databackup.database.entity.Contact
import com.xayah.databackup.database.entity.Mms
import com.xayah.databackup.database.entity.Network
import com.xayah.databackup.database.entity.Sms
import com.xayah.databackup.entity.backup.BackupSelection
import com.xayah.databackup.entity.backup.BackupStagedFile
import com.xayah.databackup.util.PathHelper

/** Serializes selected structured backup data to the shared backup directory layout. */
class BackupSerializationHelper {
    private val mMoshi = Moshi.Builder()
        .add(WifiConfigurationAdapter())
        .build()

    fun serialize(selection: BackupSelection): List<BackupStagedFile> {
        return buildList {
            selection.networks?.let {
                add(
                    BackupStagedFile(
                        PathHelper.getBackupNetworksConfigFileRelativePath(),
                        mMoshi.adapter<List<Network>>().toJson(it)
                    )
                )
            }
            selection.contacts?.let {
                add(
                    BackupStagedFile(
                        PathHelper.getBackupContactsConfigFileRelativePath(),
                        mMoshi.adapter<List<Contact>>().toJson(it)
                    )
                )
            }
            selection.callLogs?.let {
                add(
                    BackupStagedFile(
                        PathHelper.getBackupCallLogsConfigFileRelativePath(),
                        mMoshi.adapter<List<CallLog>>().toJson(it)
                    )
                )
            }
            selection.sms?.let {
                add(
                    BackupStagedFile(
                        PathHelper.getBackupMessagesSmsConfigFileRelativePath(),
                        mMoshi.adapter<List<Sms>>().toJson(it)
                    )
                )
            }
            selection.mms?.let {
                add(
                    BackupStagedFile(
                        PathHelper.getBackupMessagesMmsConfigFileRelativePath(),
                        mMoshi.adapter<List<Mms>>().toJson(it)
                    )
                )
            }
        }
    }

}
