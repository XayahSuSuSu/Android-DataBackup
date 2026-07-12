package com.xayah.databackup.data.rustic

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.xayah.databackup.adapter.WifiConfigurationAdapter
import com.xayah.databackup.database.entity.CallLog
import com.xayah.databackup.database.entity.Contact
import com.xayah.databackup.database.entity.Mms
import com.xayah.databackup.database.entity.Network
import com.xayah.databackup.database.entity.Sms
import com.xayah.databackup.util.PathHelper

/** Serializes selected structured backup data and the Rustic snapshot manifest as JSON. */
class RusticStructuredDataSerializer {
    private val mMoshi = Moshi.Builder()
        .add(WifiConfigurationAdapter())
        .build()

    fun serialize(selection: RusticBackupSelection): List<RusticStagedFile> {
        return buildList {
            selection.networks?.let {
                add(
                    RusticStagedFile(
                        PathHelper.getBackupNetworksConfigFileRelativePath(),
                        mMoshi.adapter<List<Network>>().toJson(it)
                    )
                )
            }
            selection.contacts?.let {
                add(
                    RusticStagedFile(
                        PathHelper.getBackupContactsConfigFileRelativePath(),
                        mMoshi.adapter<List<Contact>>().toJson(it)
                    )
                )
            }
            selection.callLogs?.let {
                add(
                    RusticStagedFile(
                        PathHelper.getBackupCallLogsConfigFileRelativePath(),
                        mMoshi.adapter<List<CallLog>>().toJson(it)
                    )
                )
            }
            selection.sms?.let {
                add(
                    RusticStagedFile(
                        PathHelper.getBackupMessagesSmsConfigFileRelativePath(),
                        mMoshi.adapter<List<Sms>>().toJson(it)
                    )
                )
            }
            selection.mms?.let {
                add(
                    RusticStagedFile(
                        PathHelper.getBackupMessagesMmsConfigFileRelativePath(),
                        mMoshi.adapter<List<Mms>>().toJson(it)
                    )
                )
            }
        }
    }

    fun serializeManifest(manifest: RusticBackupManifest): String {
        return mMoshi.adapter<RusticBackupManifest>().toJson(manifest)
    }
}
