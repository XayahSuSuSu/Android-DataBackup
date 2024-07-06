package com.xayah.core.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Web
import com.xayah.core.model.CloudType
import com.xayah.core.model.DataType
import com.xayah.core.ui.R
import com.xayah.core.ui.model.ImageVectorToken

val CloudType.icon: ImageVectorToken
    get() {
        return when (this) {
            CloudType.FTP -> ImageVectorToken.fromVector(Icons.Rounded.Web)
            CloudType.WEBDAV -> ImageVectorToken.fromVector(Icons.Rounded.Language)
            CloudType.SMB -> ImageVectorToken.fromVector(Icons.Rounded.Storage)
            CloudType.SFTP -> ImageVectorToken.fromVector(Icons.Rounded.Lan)
        }
    }


val DataType.icon: ImageVectorToken
    get() = when (this) {
        DataType.PACKAGE_USER -> ImageVectorToken.fromDrawable(R.drawable.ic_rounded_person)
        DataType.PACKAGE_USER_DE -> ImageVectorToken.fromDrawable(R.drawable.ic_rounded_manage_accounts)
        DataType.PACKAGE_DATA -> ImageVectorToken.fromDrawable(R.drawable.ic_rounded_database)
        DataType.PACKAGE_OBB -> ImageVectorToken.fromDrawable(R.drawable.ic_rounded_stadia_controller)
        DataType.PACKAGE_MEDIA -> ImageVectorToken.fromDrawable(R.drawable.ic_rounded_image)
        else -> ImageVectorToken.fromDrawable(R.drawable.ic_rounded_android)
    }
