package com.xayah.core.data.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.ManageAccounts
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QuestionMark
import com.xayah.core.data.R
import com.xayah.core.model.DataType
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.util.fromDrawable
import com.xayah.core.ui.util.fromString
import com.xayah.core.ui.util.fromVector
import com.xayah.core.util.PathUtil

fun DataType.srcDir(userId: Int): String = when (this) {
    DataType.PACKAGE_USER -> PathUtil.getPackageUserDir(userId)
    DataType.PACKAGE_USER_DE -> PathUtil.getPackageUserDeDir(userId)
    DataType.PACKAGE_DATA -> PathUtil.getPackageDataDir(userId)
    DataType.PACKAGE_OBB -> PathUtil.getPackageObbDir(userId)
    DataType.PACKAGE_MEDIA -> PathUtil.getPackageMediaDir(userId)
    else -> ""
}

val DataType.typeNameToken: StringResourceToken
    get() = when (this) {
        DataType.PACKAGE_APK -> StringResourceToken.fromString(DataType.PACKAGE_APK.type.uppercase())
        DataType.PACKAGE_USER -> StringResourceToken.fromString(DataType.PACKAGE_USER.type.uppercase())
        DataType.PACKAGE_USER_DE -> StringResourceToken.fromString(DataType.PACKAGE_USER_DE.type.uppercase())
        DataType.PACKAGE_DATA -> StringResourceToken.fromString(DataType.PACKAGE_DATA.type.uppercase())
        DataType.PACKAGE_OBB -> StringResourceToken.fromString(DataType.PACKAGE_OBB.type.uppercase())
        DataType.PACKAGE_MEDIA -> StringResourceToken.fromString(DataType.PACKAGE_MEDIA.type.uppercase())
        else -> StringResourceToken.fromString("")
    }

val DataType.typeIconToken: ImageVectorToken
    get() = when (this) {
        DataType.PACKAGE_APK -> ImageVectorToken.fromVector(Icons.Rounded.Android)
        DataType.PACKAGE_USER -> ImageVectorToken.fromVector(Icons.Rounded.Person)
        DataType.PACKAGE_USER_DE -> ImageVectorToken.fromVector(Icons.Rounded.ManageAccounts)
        DataType.PACKAGE_DATA -> ImageVectorToken.fromDrawable(R.drawable.ic_rounded_database)
        DataType.PACKAGE_OBB -> ImageVectorToken.fromDrawable(R.drawable.ic_rounded_stadia_controller)
        DataType.PACKAGE_MEDIA -> ImageVectorToken.fromVector(Icons.Rounded.Image)
        else -> ImageVectorToken.fromVector(Icons.Rounded.QuestionMark)
    }