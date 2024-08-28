package com.xayah.core.model.util

import android.os.Build
import com.xayah.core.model.CompressionType
import com.xayah.core.model.DataType
import com.xayah.core.model.KillAppOption
import com.xayah.core.model.LZ4_SUFFIX
import com.xayah.core.model.OpType
import com.xayah.core.model.OperationState
import com.xayah.core.model.SFTPAuthMode
import com.xayah.core.model.SelectionType
import com.xayah.core.model.SmbAuthMode
import com.xayah.core.model.SortType
import com.xayah.core.model.TAR_SUFFIX
import com.xayah.core.model.ThemeType
import com.xayah.core.model.ZSTD_SUFFIX
import com.xayah.core.model.database.Info
import com.xayah.core.model.database.MediaEntity
import com.xayah.core.model.database.PackageEntity
import com.xayah.core.model.database.ProcessingInfoEntity
import com.xayah.core.model.database.TaskDetailMediaEntity
import com.xayah.core.model.database.TaskDetailPackageEntity
import com.xayah.core.model.database.TaskEntity
import java.text.DecimalFormat
import kotlin.math.pow

fun Double.formatSize(unitValue: Int = 1024): String = run {
    var unit = "Bytes"
    var size = this
    val gb = unitValue.toDouble().pow(3)
    val mb = unitValue.toDouble().pow(2)
    val kb = unitValue.toDouble()
    if (this > gb) {
        size = this / gb
        unit = "GB"
    } else if (this > mb) {
        size = this / mb
        unit = "MB"
    } else if (this > kb) {
        size = this / kb
        unit = "KB"
    }
    if (size == 0.0) "0.00 $unit" else "${DecimalFormat("#.00").format(size)} $unit"
}

fun CompressionType.Companion.of(name: String?): CompressionType =
    runCatching { CompressionType.valueOf(name!!.uppercase()) }.getOrDefault(CompressionType.ZSTD)

fun OpType.Companion.of(name: String?): OpType =
    runCatching { OpType.valueOf(name!!.uppercase()) }.getOrDefault(OpType.BACKUP)

fun SortType.Companion.of(name: String?): SortType =
    runCatching { SortType.valueOf(name!!.uppercase()) }.getOrDefault(SortType.ASCENDING)

fun CompressionType.Companion.suffixOf(suffix: String): CompressionType? = when (suffix) {
    TAR_SUFFIX -> CompressionType.TAR
    ZSTD_SUFFIX -> CompressionType.ZSTD
    LZ4_SUFFIX -> CompressionType.LZ4
    else -> null
}

fun SelectionType.Companion.of(name: String?): SelectionType =
    runCatching { SelectionType.valueOf(name!!.uppercase()) }.getOrDefault(SelectionType.DEFAULT)

fun ThemeType.Companion.of(name: String?): ThemeType =
    runCatching { ThemeType.valueOf(name!!.uppercase()) }.getOrDefault(ThemeType.AUTO)

fun SmbAuthMode.Companion.indexOf(index: Int): SmbAuthMode = when (index) {
    1 -> SmbAuthMode.GUEST
    2 -> SmbAuthMode.ANONYMOUS
    else -> SmbAuthMode.PASSWORD
}

fun SFTPAuthMode.Companion.indexOf(index: Int): SFTPAuthMode = when (index) {
    1 -> SFTPAuthMode.PUBLIC_KEY
    else -> SFTPAuthMode.PASSWORD
}

fun KillAppOption.Companion.indexOf(index: Int): KillAppOption = when (index) {
    1 -> KillAppOption.OPTION_I
    2 -> KillAppOption.OPTION_II
    else -> KillAppOption.DISABLED
}

fun KillAppOption.Companion.of(name: String?): KillAppOption =
    runCatching { KillAppOption.valueOf(name!!.uppercase()) }.getOrDefault(KillAppOption.OPTION_II)

fun Info.set(
    bytes: Long? = null,
    log: String? = null,
    content: String? = null,
    progress: Float? = null,
    state: OperationState? = null,
) {
    if (state != null) this.state = state
    if (bytes != null) this.bytes = bytes
    if (log != null) this.log = log
    if (content != null) this.content = content
    if (progress != null) this.progress = progress
}

fun TaskDetailPackageEntity.set(
    state: OperationState? = null,
    processingIndex: Int? = null,
    packageEntity: PackageEntity? = null,
) {
    if (state != null) this.state = state
    if (processingIndex != null) this.processingIndex = processingIndex
    if (packageEntity != null) this.packageEntity = packageEntity
}

fun TaskDetailPackageEntity.set(
    dataType: DataType,
    bytes: Long? = null,
    log: String? = null,
    content: String? = null,
    progress: Float? = null,
    state: OperationState? = null,
) = run {
    when (dataType) {
        DataType.PACKAGE_APK -> {
            apkInfo.set(bytes, log, content, progress, state)
        }

        DataType.PACKAGE_USER -> {
            userInfo.set(bytes, log, content, progress, state)
        }

        DataType.PACKAGE_USER_DE -> {
            userDeInfo.set(bytes, log, content, progress, state)
        }

        DataType.PACKAGE_DATA -> {
            dataInfo.set(bytes, log, content, progress, state)
        }

        DataType.PACKAGE_OBB -> {
            obbInfo.set(bytes, log, content, progress, state)
        }

        DataType.PACKAGE_MEDIA -> {
            mediaInfo.set(bytes, log, content, progress, state)
        }

        else -> {}
    }
}

fun TaskDetailPackageEntity.get(
    dataType: DataType,
): Info = run {
    when (dataType) {
        DataType.PACKAGE_APK -> {
            apkInfo
        }

        DataType.PACKAGE_USER -> {
            userInfo
        }

        DataType.PACKAGE_USER_DE -> {
            userDeInfo
        }

        DataType.PACKAGE_DATA -> {
            dataInfo
        }

        DataType.PACKAGE_OBB -> {
            obbInfo
        }

        DataType.PACKAGE_MEDIA -> {
            mediaInfo
        }

        else -> apkInfo
    }
}

fun TaskDetailMediaEntity.set(
    state: OperationState? = null,
    processingIndex: Int? = null,
    mediaEntity: MediaEntity? = null,
) {
    if (state != null) this.state = state
    if (processingIndex != null) this.processingIndex = processingIndex
    if (mediaEntity != null) this.mediaEntity = mediaEntity
}

fun TaskDetailMediaEntity.set(
    bytes: Long? = null,
    log: String? = null,
    content: String? = null,
    progress: Float? = null,
    state: OperationState? = null,
) = run {
    mediaInfo.set(bytes, log, content, progress, state)
}

fun ProcessingInfoEntity.set(
    bytes: Long? = null,
    log: String? = null,
    title: String? = null,
    content: String? = null,
    progress: Float? = null,
    state: OperationState? = null,
) = run {
    if (bytes != null) this.bytes = bytes
    if (log != null) this.log = log
    if (title != null) this.title = title
    if (content != null) this.content = content
    if (progress != null) this.progress = progress
    if (state != null) this.state = state
}

fun TaskEntity.set(
    startTimestamp: Long? = null,
    endTimestamp: Long? = null,
    rawBytes: Double? = null,
    availableBytes: Double? = null,
    totalBytes: Double? = null,
    totalCount: Int? = null,
    successCount: Int? = null,
    failureCount: Int? = null,
    preprocessingIndex: Int? = null,
    processingIndex: Int? = null,
    postProcessingIndex: Int? = null,
    isProcessing: Boolean? = null,
    cloud: String? = null,
    backupDir: String? = null,
) = run {
    if (startTimestamp != null) this.startTimestamp = startTimestamp
    if (endTimestamp != null) this.endTimestamp = endTimestamp
    if (rawBytes != null) this.rawBytes = rawBytes
    if (availableBytes != null) this.availableBytes = availableBytes
    if (totalBytes != null) this.totalBytes = totalBytes
    if (totalCount != null) this.totalCount = totalCount
    if (successCount != null) this.successCount = successCount
    if (failureCount != null) this.failureCount = failureCount
    if (preprocessingIndex != null) this.preprocessingIndex = preprocessingIndex
    if (processingIndex != null) this.processingIndex = processingIndex
    if (postProcessingIndex != null) this.postProcessingIndex = postProcessingIndex
    if (isProcessing != null) this.isProcessing = isProcessing
    if (cloud != null) this.cloud = cloud
    if (backupDir != null) this.backupDir = backupDir
}
