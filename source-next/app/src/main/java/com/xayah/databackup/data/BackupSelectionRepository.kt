package com.xayah.databackup.data

import com.xayah.databackup.database.entity.App
import com.xayah.databackup.database.entity.CallLog
import com.xayah.databackup.database.entity.Contact
import com.xayah.databackup.database.entity.File
import com.xayah.databackup.database.entity.Mms
import com.xayah.databackup.database.entity.Network
import com.xayah.databackup.database.entity.Sms
import com.xayah.databackup.entity.backup.BackupSelection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class BackupSelectionRepository(
    private val mBackupConfigRepo: BackupConfigRepository,
    private val mAppRepo: AppRepository,
    private val mFileRepo: FileRepository,
    private val mNetworkRepo: NetworkRepository,
    private val mContactRepo: ContactRepository,
    private val mCallLogRepo: CallLogRepository,
    private val mMessageRepo: MessageRepository,
) {
    val isBackupAppsSelected: Flow<Boolean> = mAppRepo.isBackupAppsSelected
    val appsFilteredAndSelected: Flow<List<App>> = mAppRepo.appsFilteredAndSelected
    val isBackupFilesSelected: Flow<Boolean> = mFileRepo.isBackupFilesSelected
    val filesSelected: Flow<List<File>> = mFileRepo.filesSelected
    val isBackupNetworksSelected: Flow<Boolean> = mNetworkRepo.isBackupNetworksSelected
    val networksSelected: Flow<List<Network>> = mNetworkRepo.networksSelected
    val isBackupContactsSelected: Flow<Boolean> = mContactRepo.isBackupContactsSelected
    val contactsSelected: Flow<List<Contact>> = mContactRepo.contactsSelected
    val isBackupCallLogsSelected: Flow<Boolean> = mCallLogRepo.isBackupCallLogsSelected
    val callLogsSelected: Flow<List<CallLog>> = mCallLogRepo.callLogsSelected
    val isBackupMessagesSelected: Flow<Boolean> = mMessageRepo.isBackupMessagesSelected
    val smsListSelected: Flow<List<Sms>> = mMessageRepo.smsListSelected
    val mmsListSelected: Flow<List<Mms>> = mMessageRepo.mmsListSelected

    suspend fun getSelection(): BackupSelection {
        return BackupSelection(
            config = mBackupConfigRepo.getCurrentConfig(),
            apps = if (isBackupAppsSelected.first()) appsFilteredAndSelected.first() else emptyList(),
            files = if (isBackupFilesSelected.first()) filesSelected.first() else emptyList(),
            networks = if (isBackupNetworksSelected.first()) networksSelected.first() else null,
            contacts = if (isBackupContactsSelected.first()) contactsSelected.first() else null,
            callLogs = if (isBackupCallLogsSelected.first()) callLogsSelected.first() else null,
            sms = if (isBackupMessagesSelected.first()) smsListSelected.first() else null,
            mms = if (isBackupMessagesSelected.first()) mmsListSelected.first() else null,
        )
    }
}
