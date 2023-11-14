package com.xayah.databackup.ui.activity.main.page.settings

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.xayah.databackup.BuildConfig
import com.xayah.databackup.R
import com.xayah.databackup.util.ConstantUtil
import com.xayah.core.util.DateUtil
import com.xayah.databackup.util.readLatestVersionName
import com.xayah.databackup.util.readUpdateCheckTime
import com.xayah.databackup.util.saveLatestVersionLink
import com.xayah.databackup.util.saveLatestVersionName
import com.xayah.databackup.util.saveUpdateCheckTime
import com.xayah.core.rootservice.util.withIOContext
import kotlinx.coroutines.launch

private fun compareVersionName(context: Context, viewModel: SettingsViewModel) {
    val latestVersionName = context.readLatestVersionName()
    if (latestVersionName != BuildConfig.VERSION_NAME) {
        val tmp = viewModel.uiState.value.infoCardItems.toMutableList()
        tmp[0] = tmp[0].copy(
            title = "${context.getString(R.string.version)} (${context.getString(R.string.update_available)})",
            onWarning = true,
        )
        viewModel.setInfoCardItems(tmp.toList())
    }
}

private fun checkUpdate(context: Context, viewModel: SettingsViewModel) {
    viewModel.viewModelScope.launch {
        withIOContext {
            val updateCheckTime = context.readUpdateCheckTime()
            val now = DateUtil.getTimestamp()
            val hasPassedOneHour = DateUtil.getNumberOfHoursPassed(updateCheckTime, now) >= 1
            if (hasPassedOneHour) {
                context.saveUpdateCheckTime(now)
                viewModel.serverUtil.getReleases(
                    onSucceed = { releases ->
                        val appReleases = releases.filter { it.name.contains(ConstantUtil.AppReleasePrefix) }
                        if (appReleases.isNotEmpty()) {
                            val appRelease = appReleases.first()
                            context.saveLatestVersionName(appRelease.name.replace(ConstantUtil.AppReleasePrefix, ""))
                            context.saveLatestVersionLink(appRelease.url)
                            compareVersionName(context, viewModel)
                        }
                    },
                    onFailed = {}
                )
            } else {
                compareVersionName(context, viewModel)
            }
        }
    }
}

fun initialize(context: Context, viewModel: SettingsViewModel) {
    checkUpdate(context, viewModel)
}
