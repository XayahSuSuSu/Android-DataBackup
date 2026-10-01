package com.xayah.databackup.entity.backup

import androidx.annotation.FloatRange
import arrow.optics.optics
import com.xayah.databackup.App.Companion.application
import com.xayah.databackup.R

@optics
data class ProcessItem(
    val isLoading: Boolean = true,
    val isSelected: Boolean = false,
    val currentIndex: Int = 0,
    val totalCount: Int = 0,
    val msg: String = application.getString(R.string.idle),
    @FloatRange(0.0, 1.0) val progress: Float = 0f
) {
    companion object
}

@optics
data class ProcessAppDataDetailItem(
    val bytes: Long = 0L,
    val speed: Long = 0L,
    val status: Int = 0,
    val info: String = "",
) {
    companion object
}

@optics
data class ProcessAppDataItem(
    val enabled: Boolean = true,
    val title: String = "",
    val subtitle: String = application.getString(R.string.idle),
    val msg: String = application.getString(R.string.idle),
    val details: List<ProcessAppDataDetailItem> = listOf(),
) {
    companion object
}

@optics
data class ProcessAppItem(
    val label: String = "",
    val packageName: String = "",
    val userId: Int = 0,
    val apkItem: ProcessAppDataItem = ProcessAppDataItem(
        title = application.getString(R.string.apk),
        details = listOf(
            ProcessAppDataDetailItem(),
        )
    ),
    val intDataItem: ProcessAppDataItem = ProcessAppDataItem(
        title = application.getString(R.string.internal_data),
        details = listOf(
            ProcessAppDataDetailItem(),
            ProcessAppDataDetailItem(),
        )
    ),
    val extDataItem: ProcessAppDataItem = ProcessAppDataItem(
        title = application.getString(R.string.external_data),
        details = listOf(
            ProcessAppDataDetailItem(),
        )
    ),
    val addlDataItem: ProcessAppDataItem = ProcessAppDataItem(
        title = application.getString(R.string.additional_data),
        details = listOf(
            ProcessAppDataDetailItem(),
            ProcessAppDataDetailItem(),
        )
    ),
    @FloatRange(0.0, 1.0) val progress: Float = 0f
) {
    companion object
}
