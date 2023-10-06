package com.xayah.databackup.ui.activity.main.page.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xayah.databackup.BuildConfig
import com.xayah.databackup.R
import com.xayah.databackup.ui.component.SettingsGridItemConfig
import com.xayah.databackup.util.ConstantUtil
import com.xayah.databackup.util.DateUtil
import com.xayah.databackup.util.ServerUtil
import com.xayah.databackup.util.readLatestVersionLink
import com.xayah.databackup.util.readLatestVersionName
import com.xayah.databackup.util.readUpdateCheckTime
import com.xayah.databackup.util.saveLatestVersionLink
import com.xayah.databackup.util.saveLatestVersionName
import com.xayah.databackup.util.saveUpdateCheckTime
import com.xayah.librootservice.util.ExceptionUtil
import com.xayah.librootservice.util.withIOContext
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val serverUtil: ServerUtil,
    val infoCardItems: List<SettingsGridItemConfig>,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val serverUtil: ServerUtil,
) : ViewModel() {
    private val _uiState = mutableStateOf(
        SettingsUiState(
            serverUtil = serverUtil,
            infoCardItems = listOf(
                SettingsGridItemConfig(
                    icon = ImageVector.vectorResource(context.theme, context.resources, R.drawable.ic_rounded_apps),
                    title = context.getString(R.string.version),
                    content = BuildConfig.VERSION_NAME
                ),
                SettingsGridItemConfig(
                    icon = ImageVector.vectorResource(context.theme, context.resources, R.drawable.ic_rounded_star),
                    title = context.getString(R.string.feature),
                    content = BuildConfig.FLAVOR_feature
                ),
                SettingsGridItemConfig(
                    icon = ImageVector.vectorResource(context.theme, context.resources, R.drawable.ic_rounded_app_registration),
                    title = context.getString(R.string.abi),
                    content = BuildConfig.FLAVOR_abi
                ),
                SettingsGridItemConfig(
                    icon = ImageVector.vectorResource(context.theme, context.resources, R.drawable.ic_rounded_phone_android),
                    title = context.getString(R.string.architecture),
                    content = Build.SUPPORTED_ABIS.firstOrNull() ?: context.getString(R.string.loading_failed)
                ),
            )
        )
    )
    val uiState: State<SettingsUiState>
        get() = _uiState

    private fun setInfoCardItems(items: List<SettingsGridItemConfig>) {
        _uiState.value = uiState.value.copy(infoCardItems = items)
    }

    private fun compareVersionName(context: Context) {
        val latestVersionName = context.readLatestVersionName()
        val latestVersionLink = context.readLatestVersionLink()
        if (latestVersionName > BuildConfig.VERSION_NAME) {
            val tmp = uiState.value.infoCardItems.toMutableList()
            tmp[0] = tmp[0].copy(
                title = "${context.getString(R.string.version)} (${context.getString(R.string.update_available)})",
                onWarning = true,
                onClick = {
                    ExceptionUtil.tryOn(
                        block = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(latestVersionLink)))
                        },
                        onException = {
                            Toast.makeText(context, context.getString(R.string.no_browser), Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            )
            setInfoCardItems(tmp.toList())
        }
    }

    fun checkUpdate(context: Context) {
        viewModelScope.launch {
            withIOContext {
                val updateCheckTime = context.readUpdateCheckTime()
                val now = DateUtil.getTimestamp()
                val hasPassedOneHour = DateUtil.getNumberOfHoursPassed(updateCheckTime, now) >= 1
                if (hasPassedOneHour) {
                    context.saveUpdateCheckTime(now)
                    serverUtil.getReleases(
                        onSucceed = { releases ->
                            val appReleases = releases.filter { it.name.contains(ConstantUtil.AppReleasePrefix) }
                            if (appReleases.isNotEmpty()) {
                                val appRelease = appReleases.first()
                                context.saveLatestVersionName(appRelease.name.replace(ConstantUtil.AppReleasePrefix, ""))
                                context.saveLatestVersionLink(appRelease.url)
                                compareVersionName(context)
                            }
                        },
                        onFailed = {}
                    )
                } else {
                    compareVersionName(context)
                }
            }
        }
    }
}
