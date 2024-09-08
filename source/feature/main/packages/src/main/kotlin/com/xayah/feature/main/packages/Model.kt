package com.xayah.feature.main.packages

import android.content.Context
import com.xayah.core.model.DataState
import com.xayah.core.model.DataType
import com.xayah.core.model.database.PackageEntity

fun PackageEntity.reversePermission() = if (permissionSelected)
    copy(dataStates = dataStates.copy(permissionState = DataState.NotSelected))
else
    copy(dataStates = dataStates.copy(permissionState = DataState.Selected))

fun PackageEntity.reverseSsaid() = if (ssaidSelected)
    copy(dataStates = dataStates.copy(ssaidState = DataState.NotSelected))
else
    copy(dataStates = dataStates.copy(ssaidState = DataState.Selected))

fun PackageEntity.reversedPackage(dataType: DataType): PackageEntity = when (dataType) {
    DataType.PACKAGE_APK -> if (apkSelected)
        copy(dataStates = dataStates.copy(apkState = DataState.NotSelected))
    else
        copy(dataStates = dataStates.copy(apkState = DataState.Selected))

    DataType.PACKAGE_USER -> if (userSelected)
        copy(dataStates = dataStates.copy(userState = DataState.NotSelected))
    else
        copy(dataStates = dataStates.copy(userState = DataState.Selected))

    DataType.PACKAGE_USER_DE -> if (userDeSelected)
        copy(dataStates = dataStates.copy(userDeState = DataState.NotSelected))
    else
        copy(dataStates = dataStates.copy(userDeState = DataState.Selected))

    DataType.PACKAGE_DATA -> if (dataSelected)
        copy(dataStates = dataStates.copy(dataState = DataState.NotSelected))
    else
        copy(dataStates = dataStates.copy(dataState = DataState.Selected))

    DataType.PACKAGE_OBB -> if (obbSelected)
        copy(dataStates = dataStates.copy(obbState = DataState.NotSelected))
    else
        copy(dataStates = dataStates.copy(obbState = DataState.Selected))

    DataType.PACKAGE_MEDIA -> if (mediaSelected)
        copy(dataStates = dataStates.copy(mediaState = DataState.NotSelected))
    else
        copy(dataStates = dataStates.copy(mediaState = DataState.Selected))

    else -> this
}

fun PackageEntity.selectApkOnly(): PackageEntity = copy(
    dataStates = dataStates.copy(
        apkState = DataState.Selected,
        userState = DataState.NotSelected,
        userDeState = DataState.NotSelected,
        dataState = DataState.NotSelected,
        obbState = DataState.NotSelected,
        mediaState = DataState.NotSelected,
    )
)

fun PackageEntity.selectDataOnly(): PackageEntity = copy(
    dataStates = dataStates.copy(
        apkState = DataState.NotSelected,
        userState = DataState.Selected,
        userDeState = DataState.Selected,
        dataState = DataState.Selected,
        obbState = DataState.Selected,
        mediaState = DataState.Selected,
    )
)

fun PackageEntity.selectAll(): PackageEntity = copy(
    dataStates = dataStates.copy(
        apkState = DataState.Selected,
        userState = DataState.Selected,
        userDeState = DataState.Selected,
        dataState = DataState.Selected,
        obbState = DataState.Selected,
        mediaState = DataState.Selected,
    )
)

fun PackageEntity.selectNone(): PackageEntity = copy(
    dataStates = dataStates.copy(
        apkState = DataState.NotSelected,
        userState = DataState.NotSelected,
        userDeState = DataState.NotSelected,
        dataState = DataState.NotSelected,
        obbState = DataState.NotSelected,
        mediaState = DataState.NotSelected,
    )
)


fun countItems(context: Context, count: Int) = run {
    String.format(context.resources.getQuantityString(R.plurals.items, count), count)
}
