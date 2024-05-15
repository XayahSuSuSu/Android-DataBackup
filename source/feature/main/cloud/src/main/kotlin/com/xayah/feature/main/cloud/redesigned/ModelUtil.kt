package com.xayah.feature.main.cloud.redesigned

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Web
import com.xayah.core.model.CloudType
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.util.fromVector

val CloudType.icon: ImageVectorToken
    get() {
        return when (this) {
            CloudType.FTP -> ImageVectorToken.fromVector(Icons.Rounded.Web)
            CloudType.WEBDAV -> ImageVectorToken.fromVector(Icons.Rounded.Language)
            CloudType.SMB -> ImageVectorToken.fromVector(Icons.Rounded.Storage)
        }
    }
