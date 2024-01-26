package com.xayah.feature.main.packages

import android.content.Context
import com.xayah.core.model.DataState
import com.xayah.core.model.DataType
import com.xayah.core.model.database.PackageEntity

enum class SelectionState {
    All,
    PART,
    None,
}

val List<PackageDataChipItem>.dataBytes
    get() = run {
        var bytes: Long = 0
        runCatching {
            for (i in this) {
                val dataBytes = i.dataBytes!!
                if (i.selected) bytes += dataBytes
            }
            bytes
        }.getOrNull()
    }

fun List<PackageDataChipItem>.countItems(context: Context) = run {
    val count = sumOf { if (it.selected) 1L else 0 }
    countItems(context, count.toInt())
}

fun countItems(context: Context, count: Int) = run {
    String.format(context.resources.getQuantityString(R.plurals.items, count), count)
}

val List<PackageDataChipItem>.selectionState
    get() = run {
        when (this.sumOf { if (it.selected) 1L else 0 }) {
            this.size.toLong() -> SelectionState.All
            0L -> SelectionState.None
            else -> SelectionState.PART
        }
    }

fun List<PackageDataChipItem>.dataReversedPackage(packageEntity: PackageEntity) = run {
    when (selectionState) {
        SelectionState.All -> packageEntity.copy(
            dataStates = packageEntity.dataStates.copy(
                userState = DataState.NotSelected,
                userDeState = DataState.NotSelected,
                dataState = DataState.NotSelected,
                obbState = DataState.NotSelected,
                mediaState = DataState.NotSelected,
            )
        )

        SelectionState.None, SelectionState.PART -> packageEntity.copy(
            dataStates = packageEntity.dataStates.copy(
                userState = DataState.Selected,
                userDeState = DataState.Selected,
                dataState = DataState.Selected,
                obbState = DataState.Selected,
                mediaState = DataState.Selected,
            )
        )
    }
}

fun PackageEntity.reversePermission() = if (permissionSelected)
    copy(dataStates = dataStates.copy(permissionState = DataState.NotSelected))
else
    copy(dataStates = dataStates.copy(permissionState = DataState.Selected))

fun PackageEntity.reverseSsaid() = if (ssaidSelected)
    copy(dataStates = dataStates.copy(ssaidState = DataState.NotSelected))
else
    copy(dataStates = dataStates.copy(ssaidState = DataState.Selected))

data class PackageDataChipItem(
    val dataType: DataType,
    val dataBytes: Long?,
    val selected: Boolean,
) {
    fun reversedPackage(packageEntity: PackageEntity): PackageEntity = when (dataType) {
        DataType.PACKAGE_APK -> if (packageEntity.apkSelected)
            packageEntity.copy(dataStates = packageEntity.dataStates.copy(apkState = DataState.NotSelected))
        else
            packageEntity.copy(dataStates = packageEntity.dataStates.copy(apkState = DataState.Selected))

        DataType.PACKAGE_USER -> if (packageEntity.userSelected)
            packageEntity.copy(dataStates = packageEntity.dataStates.copy(userState = DataState.NotSelected))
        else
            packageEntity.copy(dataStates = packageEntity.dataStates.copy(userState = DataState.Selected))

        DataType.PACKAGE_USER_DE -> if (packageEntity.userDeSelected)
            packageEntity.copy(dataStates = packageEntity.dataStates.copy(userDeState = DataState.NotSelected))
        else
            packageEntity.copy(dataStates = packageEntity.dataStates.copy(userDeState = DataState.Selected))

        DataType.PACKAGE_DATA -> if (packageEntity.dataSelected)
            packageEntity.copy(dataStates = packageEntity.dataStates.copy(dataState = DataState.NotSelected))
        else
            packageEntity.copy(dataStates = packageEntity.dataStates.copy(dataState = DataState.Selected))

        DataType.PACKAGE_OBB -> if (packageEntity.obbSelected)
            packageEntity.copy(dataStates = packageEntity.dataStates.copy(obbState = DataState.NotSelected))
        else
            packageEntity.copy(dataStates = packageEntity.dataStates.copy(obbState = DataState.Selected))

        DataType.PACKAGE_MEDIA -> if (packageEntity.mediaSelected)
            packageEntity.copy(dataStates = packageEntity.dataStates.copy(mediaState = DataState.NotSelected))
        else
            packageEntity.copy(dataStates = packageEntity.dataStates.copy(mediaState = DataState.Selected))

        else -> packageEntity
    }
}
