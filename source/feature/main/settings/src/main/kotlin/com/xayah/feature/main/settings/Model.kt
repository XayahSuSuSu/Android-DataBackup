package com.xayah.feature.main.settings

import android.os.Build
import com.xayah.core.common.util.BuildConfigUtil
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.util.fromDrawable
import com.xayah.core.ui.util.fromString
import com.xayah.core.ui.util.fromStringId

data class SettingsInfoItem(
    val icon: ImageVectorToken,
    val title: StringResourceToken,
    val content: StringResourceToken,
    val onWarning: Boolean = false,
    val onClick: (() -> Unit)? = null,
)

internal val infoList = listOf(
    SettingsInfoItem(
        icon = ImageVectorToken.fromDrawable(com.xayah.core.ui.R.drawable.ic_rounded_apps),
        title = StringResourceToken.fromStringId(R.string.version),
        content = StringResourceToken.fromString(BuildConfigUtil.VERSION_NAME),
        onClick = {
        }
    ),
    SettingsInfoItem(
        icon = ImageVectorToken.fromDrawable(com.xayah.core.ui.R.drawable.ic_rounded_star),
        title = StringResourceToken.fromStringId(R.string.feature),
        content = StringResourceToken.fromString(BuildConfigUtil.FLAVOR_feature),
    ),
    SettingsInfoItem(
        icon = ImageVectorToken.fromDrawable(com.xayah.core.ui.R.drawable.ic_rounded_app_registration),
        title = StringResourceToken.fromStringId(R.string.abi),
        content = StringResourceToken.fromString(BuildConfigUtil.FLAVOR_abi),
    ),
    SettingsInfoItem(
        icon = ImageVectorToken.fromDrawable(com.xayah.core.ui.R.drawable.ic_rounded_phone_android),
        title = StringResourceToken.fromStringId(R.string.architecture),
        content = StringResourceToken.fromString(Build.SUPPORTED_ABIS.firstOrNull() ?: "")
    ),
)
