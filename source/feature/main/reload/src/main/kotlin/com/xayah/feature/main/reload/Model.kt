package com.xayah.feature.main.reload

import com.xayah.core.database.model.MediaRestoreEntity
import com.xayah.core.database.model.PackageRestoreEntire
import com.xayah.core.rootservice.parcelables.PathParcelable

internal data class TypedTimestamp(
    val timestamp: Long,
    val archivePathList: MutableList<PathParcelable>,
)

internal data class TypedPath(
    val name: String,
    val typedTimestampList: MutableList<TypedTimestamp>,
)

data class MediumReloadingState(
    var isFinished: Boolean = false,
    var current: Int = 0,
    var total: Int = 0,
    val medium: MutableList<MediaRestoreEntity> = mutableListOf(),
)

data class PackagesReloadingState(
    var isFinished: Boolean = false,
    var current: Int = 0,
    var total: Int = 0,
    val packages: MutableList<PackageRestoreEntire> = mutableListOf(),
)

internal const val Migration1 = "1.0.0sp1"
internal const val Migration2 = "1.1.0-alpha01"
internal const val Migration3 = "1.1.0-alpha05"