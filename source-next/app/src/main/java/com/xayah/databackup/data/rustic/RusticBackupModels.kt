package com.xayah.databackup.data.rustic

import com.squareup.moshi.JsonClass
import com.xayah.databackup.database.entity.App
import com.xayah.databackup.database.entity.CallLog
import com.xayah.databackup.database.entity.Contact
import com.xayah.databackup.database.entity.File
import com.xayah.databackup.database.entity.Info
import com.xayah.databackup.database.entity.Mms
import com.xayah.databackup.database.entity.Network
import com.xayah.databackup.database.entity.Option
import com.xayah.databackup.database.entity.Sms
import com.xayah.databackup.entity.BackupConfig

enum class RusticSourceCategory {
    Apk,
    InternalData,
    ExternalData,
    AdditionalData,
    File,
    MmsAttachment,
}

@JsonClass(generateAdapter = true)
data class RusticSourcePath(
    val path: String,
    val category: RusticSourceCategory,
)

@JsonClass(generateAdapter = true)
data class RusticSkippedSource(
    val path: String,
    val category: RusticSourceCategory,
    val reason: String,
) {
    companion object {
        const val REASON_NOT_FOUND = "not_found"
    }
}

data class RusticAppSourcePlan(
    val packageName: String,
    val userId: Int,
    val info: Info,
    val option: Option,
    val included: List<RusticSourcePath>,
    val skipped: List<RusticSkippedSource>,
)

data class RusticBackupSelection(
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

data class RusticStagedFile(
    val relativePath: String,
    val content: String,
)

data class RusticCollectedSources(
    val sourcePaths: List<String>,
    val stagingPath: String,
    val skippedSources: List<RusticSkippedSource>,
)

enum class RusticBackupStage {
    PrepareRepository,
    CollectSources,
    CreateSnapshot,
    FinalizeSnapshot,
}

sealed interface RusticBackupEvent {
    data class StageChanged(val stage: RusticBackupStage) : RusticBackupEvent
    data class Progress(val bytesDone: Long, val speed: Long, val progress: Float) : RusticBackupEvent
}

data class RusticBackupResult(
    val snapshotId: String,
    val includedCount: Int,
    val skippedSources: List<RusticSkippedSource>,
)
