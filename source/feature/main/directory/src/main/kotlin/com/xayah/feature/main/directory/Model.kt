package com.xayah.feature.main.directory

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.SdCard
import com.xayah.core.model.StorageType
import com.xayah.core.model.database.DirectoryEntity
import com.xayah.core.model.util.formatSize
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.util.fromVector

data class SegmentProgress(
    val used: Long,
    val total: Long,
) {
    val progress: Float
        get() = used.toFloat() / total

    val usedFormat: String
        get() = used.toDouble().formatSize()

    val totalFormat: String
        get() = total.toDouble().formatSize()
}

fun DirectoryEntity.icon() = when (storageType) {
    StorageType.INTERNAL -> ImageVectorToken.fromVector(Icons.Rounded.PhoneAndroid)
    StorageType.EXTERNAL -> ImageVectorToken.fromVector(Icons.Rounded.SdCard)
    StorageType.CUSTOM -> ImageVectorToken.fromVector(Icons.Rounded.Palette)
}

fun DirectoryEntity.pathDisplay() = when (storageType) {
    StorageType.EXTERNAL -> if (type.isEmpty()) path else "$path ($type)"
    else -> path
}
