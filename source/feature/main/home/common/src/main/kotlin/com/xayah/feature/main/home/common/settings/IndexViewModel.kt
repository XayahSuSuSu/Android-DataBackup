package com.xayah.feature.main.home.common.settings

import android.content.pm.UserInfo
import android.os.Build
import androidx.compose.material3.ExperimentalMaterial3Api
import com.xayah.core.common.viewmodel.BaseViewModel
import com.xayah.core.common.viewmodel.IndexUiEffect
import com.xayah.core.common.viewmodel.UiIntent
import com.xayah.core.common.viewmodel.UiState
import com.xayah.core.data.repository.UserRepository
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.util.fromDrawable
import com.xayah.core.ui.util.fromString
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.util.BuildConfigUtil
import com.xayah.feature.main.home.common.R
import com.xayah.feature.main.home.common.model.SettingsInfoItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.xayah.core.ui.R as UiR

data class IndexUiState(
    val settingsInfoItems: List<SettingsInfoItem> = infoList,
    val userList: List<UserInfo> = listOf(),
) : UiState

sealed class IndexUiIntent : UiIntent {
    object UpdateUserList : IndexUiIntent()
}

@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor(
    rootService: RemoteRootService,
    private val userRepository: UserRepository,
) : BaseViewModel<IndexUiState, IndexUiIntent, IndexUiEffect>(IndexUiState()) {
    init {
        rootService.onFailure = {
            val msg = it.message
            if (msg != null)
                emitEffect(IndexUiEffect.ShowSnackbar(message = msg))
        }
    }

    override suspend fun onEvent(state: IndexUiState, intent: IndexUiIntent) {
        when (intent) {
            is IndexUiIntent.UpdateUserList -> {
                emitState(state.copy(userList = userRepository.getUsers()))
            }
        }
    }
}

private val infoList = listOf(
    SettingsInfoItem(
        icon = ImageVectorToken.fromDrawable(UiR.drawable.ic_rounded_apps),
        title = StringResourceToken.fromStringId(R.string.version),
        content = StringResourceToken.fromString(BuildConfigUtil.VERSION_NAME),
        onClick = {
        }
    ),
    SettingsInfoItem(
        icon = ImageVectorToken.fromDrawable(UiR.drawable.ic_rounded_star),
        title = StringResourceToken.fromStringId(R.string.feature),
        content = StringResourceToken.fromString(BuildConfigUtil.FLAVOR_feature),
    ),
    SettingsInfoItem(
        icon = ImageVectorToken.fromDrawable(UiR.drawable.ic_rounded_app_registration),
        title = StringResourceToken.fromStringId(R.string.abi),
        content = StringResourceToken.fromString(BuildConfigUtil.FLAVOR_abi),
    ),
    SettingsInfoItem(
        icon = ImageVectorToken.fromDrawable(UiR.drawable.ic_rounded_phone_android),
        title = StringResourceToken.fromStringId(R.string.architecture),
        content = StringResourceToken.fromString(Build.SUPPORTED_ABIS.firstOrNull() ?: "")
    ),
)
