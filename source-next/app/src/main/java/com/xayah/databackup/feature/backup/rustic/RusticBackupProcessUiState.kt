package com.xayah.databackup.feature.backup.rustic

import androidx.annotation.DrawableRes
import androidx.annotation.FloatRange
import androidx.annotation.StringRes
import arrow.optics.optics
import com.xayah.databackup.R
import com.xayah.databackup.util.formatToStorageSize
import com.xayah.databackup.util.formatToStorageSizePerSecond
import kotlin.math.roundToInt

data class RusticBackupProgressUiState(
    val bytesDone: Long = 0L,
    val speed: Long = 0L,
    @FloatRange(0.0, 1.0) val progress: Float = 0f,
) {
    val progressPercent: String
        get() = (progress.coerceIn(0f, 1f) * 100).roundToInt().toString()

    val bytesDoneText: String
        get() = bytesDone.formatToStorageSize

    val speedText: String
        get() = speed.formatToStorageSizePerSecond
}

data class RusticRepositoryStorageUiState(
    val isLoading: Boolean = true,
    val repositoryBytes: Long = 0L,
    val otherBytes: Long = 0L,
    val freeBytes: Long = 0L,
    val totalBytes: Long = 0L,
) {
    val isAvailable: Boolean
        get() = totalBytes > 0L

    val repositoryRatio: Float
        get() = ratioOf(repositoryBytes)

    val otherRatio: Float
        get() = ratioOf(otherBytes)

    val freeRatio: Float
        get() = ratioOf(freeBytes)

    private fun ratioOf(bytes: Long): Float {
        return if (totalBytes > 0L) {
            bytes.toFloat().div(totalBytes.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    }
}

data class RusticBackupSourceUiItem(
    @StringRes val titleRes: Int,
    @DrawableRes val iconRes: Int,
    val selectedCount: Int,
    val totalCount: Int,
) {
    val enabled: Boolean
        get() = selectedCount > 0

    val countText: String
        get() = "$selectedCount/$totalCount"
}

data class RusticBackupSourceSummary(
    val selectedCount: Int,
    val totalCount: Int,
)

fun List<RusticBackupSourceUiItem>.toSourceSummary(): RusticBackupSourceSummary {
    return RusticBackupSourceSummary(
        selectedCount = sumOf { it.selectedCount },
        totalCount = sumOf { it.totalCount },
    )
}

enum class RusticBackupStepStatus {
    Active,
    Pending,
}

enum class RusticBackupProcessStatus {
    Processing,
    Finished,
    Failed,
}

data class RusticBackupStepUiItem(
    @StringRes val titleRes: Int,
    @DrawableRes val iconRes: Int,
    val status: RusticBackupStepStatus = RusticBackupStepStatus.Pending,
)

@optics
data class RusticBackupProcessUiState(
    val isLoaded: Boolean = false,
    val status: RusticBackupProcessStatus = RusticBackupProcessStatus.Processing,
    val backupName: String = "",
    val repositoryPath: String = "",
    val isPasswordProtected: Boolean = false,
    val repositoryStorage: RusticRepositoryStorageUiState = RusticRepositoryStorageUiState(),
    val progress: RusticBackupProgressUiState = RusticBackupProgressUiState(),
    val steps: List<RusticBackupStepUiItem> = rusticBackupProcessSteps(),
    val snapshotId: String = "",
    val errorMessage: String = "",
) {
    companion object

    val isProcessing: Boolean = status == RusticBackupProcessStatus.Processing
    val isFinished: Boolean = status == RusticBackupProcessStatus.Finished
    val isFailed: Boolean = status == RusticBackupProcessStatus.Failed
    val isTerminal: Boolean = isFinished || isFailed

    val currentStep: RusticBackupStepUiItem?
        get() = if (isFinished) {
            steps.lastOrNull()
        } else {
            steps.firstOrNull { it.status == RusticBackupStepStatus.Active } ?: steps.firstOrNull()
        }

    val currentStepNumber: Int
        get() = currentStep?.let { steps.indexOf(it) + 1 } ?: 0
}

fun rusticBackupProcessSteps(): List<RusticBackupStepUiItem> {
    return listOf(
        RusticBackupStepUiItem(
            titleRes = R.string.prepare_repository,
            iconRes = R.drawable.ic_database,
            status = RusticBackupStepStatus.Active,
        ),
        RusticBackupStepUiItem(
            titleRes = R.string.collect_sources,
            iconRes = R.drawable.ic_list_checks,
        ),
        RusticBackupStepUiItem(
            titleRes = R.string.create_snapshot,
            iconRes = R.drawable.ic_chart_bar_stacked,
        ),
        RusticBackupStepUiItem(
            titleRes = R.string.finalize_snapshot,
            iconRes = R.drawable.ic_square_check_big,
        ),
    )
}
