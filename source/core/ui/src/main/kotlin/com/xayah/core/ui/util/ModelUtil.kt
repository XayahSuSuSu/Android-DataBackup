package com.xayah.core.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Web
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.xayah.core.model.CloudType
import com.xayah.core.model.DataType
import com.xayah.core.ui.R

val CloudType.icon: ImageVector
    get() {
        return when (this) {
            CloudType.FTP -> Icons.Rounded.Web
            CloudType.WEBDAV -> Icons.Rounded.Language
            CloudType.SMB -> Icons.Rounded.Storage
            CloudType.SFTP -> Icons.Rounded.Lan
        }
    }


val DataType.icon: ImageVector
    @Composable
    get() = when (this) {
        DataType.PACKAGE_USER -> ImageVector.vectorResource(id = R.drawable.ic_rounded_person)
        DataType.PACKAGE_USER_DE -> ImageVector.vectorResource(id = R.drawable.ic_rounded_manage_accounts)
        DataType.PACKAGE_DATA -> ImageVector.vectorResource(id = R.drawable.ic_rounded_database)
        DataType.PACKAGE_OBB -> ImageVector.vectorResource(id = R.drawable.ic_rounded_stadia_controller)
        DataType.PACKAGE_MEDIA -> ImageVector.vectorResource(id = R.drawable.ic_rounded_image)
        else -> ImageVector.vectorResource(id = R.drawable.ic_rounded_android)
    }
