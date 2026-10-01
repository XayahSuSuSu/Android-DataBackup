package com.xayah.databackup.entity.backup

import com.xayah.databackup.database.entity.App
import com.xayah.databackup.database.entity.CallLog
import com.xayah.databackup.database.entity.Contact
import com.xayah.databackup.database.entity.File
import com.xayah.databackup.database.entity.Mms
import com.xayah.databackup.database.entity.Network
import com.xayah.databackup.database.entity.Sms
import com.xayah.databackup.entity.BackupConfig

data class BackupSelection(
    val config: BackupConfig,
    val apps: List<App> = emptyList(),
    val files: List<File> = emptyList(),
    // Null means the category was not selected; an empty list is a selected category with no items.
    val networks: List<Network>? = null,
    val contacts: List<Contact>? = null,
    val callLogs: List<CallLog>? = null,
    val sms: List<Sms>? = null,
    val mms: List<Mms>? = null,
)
