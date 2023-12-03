package com.xayah.feature.main.home.cloud.page.account

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavHostController
import com.xayah.core.common.util.toSpaceString
import com.xayah.core.common.viewmodel.BaseViewModel
import com.xayah.core.common.viewmodel.IndexUiEffect
import com.xayah.core.common.viewmodel.UiIntent
import com.xayah.core.common.viewmodel.UiState
import com.xayah.core.data.repository.CloudRepository
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.util.SymbolUtil.QUOTE
import com.xayah.core.util.command.Rclone
import com.xayah.feature.main.home.HomeRoutes
import com.xayah.feature.main.home.cloud.TypeConfig
import com.xayah.feature.main.home.cloud.TypeConfigTokens
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

internal data class IndexUiState(
    val currentAccountName: String,
    val typeIndex: Int = 0,
    val typeList: List<TypeConfig> = listOf(
        TypeConfigTokens.getFTP(null),
        TypeConfigTokens.getWebDAV(null),
        TypeConfigTokens.getSMB(null),
    ),
) : UiState {
    val currentConfig: TypeConfig
        get() = typeList[typeIndex.coerceIn(0, typeList.size - 1)]
}

sealed class IndexUiIntent : UiIntent {
    object Initialize : IndexUiIntent()
    data class SetTypeIndex(val index: Int) : IndexUiIntent()
    data class Confirm(val navController: NavHostController) : IndexUiIntent()
}

@ExperimentalMaterial3Api
@HiltViewModel
internal class IndexViewModel @Inject constructor(
    rootService: RemoteRootService,
    private val cloudRepository: CloudRepository,
    args: SavedStateHandle,
) : BaseViewModel<IndexUiState, IndexUiIntent, IndexUiEffect>(IndexUiState(currentAccountName = args.get<String>(HomeRoutes.ArgAccountName)?.trim() ?: "")) {
    init {
        rootService.onFailure = {
            val msg = it.message
            if (msg != null)
                emitEffect(IndexUiEffect.ShowSnackbar(message = msg))
        }
    }

    override suspend fun onEvent(state: IndexUiState, intent: IndexUiIntent) {
        when (intent) {
            is IndexUiIntent.Initialize -> {
                if (uiState.value.currentAccountName.isNotEmpty()) {
                    val cloud = cloudRepository.queryCloudByName(uiState.value.currentAccountName)
                    if (cloud != null) {
                        val typeList = uiState.value.typeList.map { it.type }
                        val index = typeList.indexOf(cloud.account.type)
                        if (index != -1) {
                            emitStateSuspend(
                                uiState.value.copy(
                                    typeIndex = index,
                                    typeList = listOf(
                                        TypeConfigTokens.getFTP(cloud),
                                        TypeConfigTokens.getWebDAV(cloud),
                                        TypeConfigTokens.getSMB(cloud),
                                    )
                                )
                            )
                        }
                    }
                }
            }

            is IndexUiIntent.SetTypeIndex -> {
                emitStateSuspend(state.copy(typeIndex = intent.index))
            }

            is IndexUiIntent.Confirm -> {
                val currentConfig = state.currentConfig
                val name by currentConfig.name

                if (name.isEmpty()) {
                    currentConfig.nameEmphasizedState.value = currentConfig.nameEmphasizedState.value.not()
                } else {
                    var allFilled = true
                    currentConfig.textFields.forEach {
                        if (it.value.value.isEmpty()) {
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
                        val args = currentConfig.textFields.map { "${it.key}=${QUOTE}${it.value.value}${QUOTE}" }.toMutableList().apply {
                            if (currentConfig.fixedArgs.isNotEmpty()) addAll(currentConfig.fixedArgs)
                        }.toList()
                        Rclone.Config.create(name = name, type = currentConfig.type, args.toSpaceString()).also { result ->
                            if (result.isSuccess.not()) {
                                emitEffect(IndexUiEffect.ShowSnackbar(message = result.out.lastOrNull() ?: "", duration = SnackbarDuration.Long))
                            } else {
                                withMainContext {
                                    intent.navController.popBackStack()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
