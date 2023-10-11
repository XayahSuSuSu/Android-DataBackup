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
import com.xayah.databackup.BuildConfig
import com.xayah.databackup.R
import com.xayah.databackup.ui.component.SettingsGridItemConfig
import com.xayah.databackup.util.ServerUtil
import com.xayah.databackup.util.readLatestVersionLink
import com.xayah.librootservice.util.ExceptionUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

data class SettingsUiState(
    val serverUtil: ServerUtil,
    val infoCardItems: List<SettingsGridItemConfig>,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext context: Context,
    val serverUtil: ServerUtil,
) : ViewModel() {
    private val _uiState = mutableStateOf(
        SettingsUiState(
            serverUtil = serverUtil,
            infoCardItems = listOf(
                SettingsGridItemConfig(
                    icon = ImageVector.vectorResource(context.theme, context.resources, R.drawable.ic_rounded_apps),
                    title = context.getString(R.string.version),
                    content = BuildConfig.VERSION_NAME,
                    onClick = {
                        ExceptionUtil.tryOn(
                            block = {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(context.readLatestVersionLink())).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                })
                            },
                            onException = {
                                Toast.makeText(context, context.getString(R.string.no_browser), Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
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

    fun setInfoCardItems(items: List<SettingsGridItemConfig>) {
        _uiState.value = uiState.value.copy(infoCardItems = items)
    }
}
