package com.xayah.core.data.util

import com.xayah.core.model.DataType
import com.xayah.core.util.PathUtil

fun DataType.srcDir(userId: Int): String = when (this) {
    DataType.PACKAGE_USER -> PathUtil.getPackageUserDir(userId)
    DataType.PACKAGE_USER_DE -> PathUtil.getPackageUserDeDir(userId)
    DataType.PACKAGE_DATA -> PathUtil.getPackageDataDir(userId)
    DataType.PACKAGE_OBB -> PathUtil.getPackageObbDir(userId)
    DataType.PACKAGE_MEDIA -> PathUtil.getPackageMediaDir(userId)
    else -> ""
}
