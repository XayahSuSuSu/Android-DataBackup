package com.xayah.feature.main.cloud.account

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavHostController
import com.xayah.core.ui.viewmodel.BaseViewModel
import com.xayah.core.ui.viewmodel.IndexUiEffect
import com.xayah.core.ui.viewmodel.UiIntent
import com.xayah.core.ui.viewmodel.UiState
import com.xayah.core.data.repository.CloudRepository
import com.xayah.core.model.CloudType
import com.xayah.core.model.database.CloudEntity
import com.xayah.core.model.database.FTPExtra
import com.xayah.core.model.database.SMBExtra
import com.xayah.core.network.util.getExtraEntity
import com.xayah.core.ui.route.MainRoutes
import com.xayah.core.util.GsonUtil
import com.xayah.feature.main.cloud.TypeConfig
import com.xayah.feature.main.cloud.TypeConfigTokens
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

internal data class IndexUiState(
    val currentAccountName: String,
    val typeIndex: Int = 0,
    val typeList: List<TypeConfig> = listOf(
        TypeConfigTokens.getFTP(null, null),
        TypeConfigTokens.getWebDAV(null),
        TypeConfigTokens.getSMB(null, null),
    ),
) : UiState {
    val currentConfig: TypeConfig
        get() = typeList[typeIndex.coerceIn(0, typeList.size - 1)]
}

internal sealed class IndexUiIntent : UiIntent {
    data object Initialize : IndexUiIntent()
    data class SetTypeIndex(val index: Int) : IndexUiIntent()
    data class Confirm(val navController: NavHostController) : IndexUiIntent()
}

@ExperimentalMaterial3Api
@HiltViewModel
internal class IndexViewModel @Inject constructor(
    private val cloudRepository: CloudRepository,
    args: SavedStateHandle,
) : BaseViewModel<IndexUiState, IndexUiIntent, IndexUiEffect>(IndexUiState(currentAccountName = args.get<String>(MainRoutes.ArgAccountName)?.trim() ?: "")) {
    override suspend fun onEvent(state: IndexUiState, intent: IndexUiIntent) {
        when (intent) {
            is IndexUiIntent.Initialize -> {
                if (uiState.value.currentAccountName.isNotEmpty()) {
                    val cloudEntity = cloudRepository.queryByName(uiState.value.currentAccountName)
                    if (cloudEntity != null) {
                        val typeList = uiState.value.typeList.map { it.type }
                        val index = typeList.indexOf(cloudEntity.type)
                        if (index != -1) {
                            emitState(
                                uiState.value.copy(
                                    typeIndex = index,
                                    typeList = listOf(
                                        TypeConfigTokens.getFTP(cloudEntity, runCatching { cloudEntity.getExtraEntity<FTPExtra>() }.getOrNull()),
                                        TypeConfigTokens.getWebDAV(cloudEntity),
                                        TypeConfigTokens.getSMB(cloudEntity, runCatching { cloudEntity.getExtraEntity<SMBExtra>() }.getOrNull()),
                                    )
                                )
                            )
                        }
                    }
                }
            }

            is IndexUiIntent.SetTypeIndex -> {
                emitState(state.copy(typeIndex = intent.index))
            }

            is IndexUiIntent.Confirm -> {
                val currentConfig = state.currentConfig
                val name by currentConfig.name

                if (name.isEmpty()) {
                    currentConfig.nameEmphasizedState.value = currentConfig.nameEmphasizedState.value.not()
                } else {
                    var allFilled = true
                    currentConfig.commonTextFields.forEach {
                        if (it.value.value.isEmpty() && it.allowEmpty.not()) {
                            it.emphasizedState.value = it.emphasizedState.value.not()
                            allFilled = false
                        } else if (it.keyboardOptions.keyboardType == KeyboardType.Number) {
                            if (it.value.value.toLongOrNull() == null) {
                                it.emphasizedState.value = it.emphasizedState.value.not()
                                allFilled = false
                            }
                        }
                    }
                    currentConfig.extraTextFields.forEach {
                        if (it.value.value.isEmpty() && it.allowEmpty.not()) {
                            it.emphasizedState.value = it.emphasizedState.value.not()
                            allFilled = false
                        } else if (it.keyboardOptions.keyboardType == KeyboardType.Number) {
                            if (it.value.value.toLongOrNull() == null) {
                                it.emphasizedState.value = it.emphasizedState.value.not()
                                allFilled = false
                            }
                        }
                    }
                    if (allFilled) {
                        val extra = runCatching {
                            GsonUtil().toJson(
                                when (currentConfig.type) {
                                    CloudType.FTP -> {
                                        FTPExtra(currentConfig.extraTextFields[0].value.value.toInt())
                                    }

                                    CloudType.WEBDAV -> {
                                        Any()
                                    }

                                    CloudType.SMB -> {
                                        SMBExtra(
                                            currentConfig.extraTextFields[0].value.value,
                                            currentConfig.extraTextFields[1].value.value.toInt(),
                                            currentConfig.extraTextFields[2].value.value,
                                            version = currentConfig.smbVersionConfigs?.filter { it.selected.value }?.map { it.version } ?: listOf()
                                        )
                                    }
                                }
                            )
                        }.getOrNull()
                        cloudRepository.upsert(
                            CloudEntity(
                                name = name,
                                type = currentConfig.type,
                                host = currentConfig.commonTextFields[0].value.value,
                                user = currentConfig.commonTextFields[1].value.value,
                                pass = currentConfig.commonTextFields[2].value.value,
                                remote = "",
                                extra = extra ?: "",
                                activated = false,
                            )
                        )
                        withMainContext {
                            intent.navController.popBackStack()
                        }
                    }
                }
            }
        }

    }
}
