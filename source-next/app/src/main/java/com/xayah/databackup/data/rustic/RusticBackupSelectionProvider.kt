package com.xayah.databackup.data.rustic

import com.xayah.databackup.data.AppRepository
import com.xayah.databackup.data.BackupConfigRepository
import com.xayah.databackup.data.CallLogRepository
import com.xayah.databackup.data.ContactRepository
import com.xayah.databackup.data.FileRepository
import com.xayah.databackup.data.MessageRepository
import com.xayah.databackup.data.NetworkRepository
import kotlinx.coroutines.flow.first

class RusticBackupSelectionProvider(
    private val mBackupConfigRepo: BackupConfigRepository,
    private val mAppRepo: AppRepository,
    private val mFileRepo: FileRepository,
    private val mNetworkRepo: NetworkRepository,
    private val mContactRepo: ContactRepository,
    private val mCallLogRepo: CallLogRepository,
    private val mMessageRepo: MessageRepository,
) {
    suspend fun getSelection(): RusticBackupSelection {
        return RusticBackupSelection(
            config = mBackupConfigRepo.getCurrentConfig(),
            apps = if (mAppRepo.isBackupAppsSelected.first()) mAppRepo.appsFilteredAndSelected.first() else emptyList(),
            files = if (mFileRepo.isBackupFilesSelected.first()) mFileRepo.filesSelected.first() else emptyList(),
            networks = if (mNetworkRepo.isBackupNetworksSelected.first()) mNetworkRepo.networksSelected.first() else null,
            contacts = if (mContactRepo.isBackupMessagesSelected.first()) mContactRepo.contactsSelected.first() else null,
            callLogs = if (mCallLogRepo.isBackupCallLogsSelected.first()) mCallLogRepo.callLogsSelected.first() else null,
            sms = if (mMessageRepo.isBackupContactsSelected.first()) mMessageRepo.smsListSelected.first() else null,
            mms = if (mMessageRepo.isBackupContactsSelected.first()) mMessageRepo.mmsListSelected.first() else null,
        )
    }
}
