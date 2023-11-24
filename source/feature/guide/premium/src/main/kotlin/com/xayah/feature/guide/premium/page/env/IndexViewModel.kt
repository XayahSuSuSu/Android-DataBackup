package com.xayah.feature.guide.premium.page.env

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import com.topjohnwu.superuser.Shell
import com.xayah.core.common.viewmodel.BaseViewModel
import com.xayah.core.common.viewmodel.UiEffect
import com.xayah.core.common.viewmodel.UiIntent
import com.xayah.core.common.viewmodel.UiState
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.util.BuildConfigUtil
import com.xayah.core.util.command.BaseUtil
import com.xayah.feature.guide.common.EnvItem
import com.xayah.feature.guide.common.EnvState
import com.xayah.feature.guide.premium.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

data class IndexUiState(
    val rootItem: EnvItem = EnvItem(content = StringResourceToken.fromStringId(R.string.grant_root_access), state = EnvState.Idle),
    val binItem: EnvItem = EnvItem(content = StringResourceToken.fromStringId(R.string.release_prebuilt_binaries), state = EnvState.Idle),
    val abiItem: EnvItem = EnvItem(content = StringResourceToken.fromStringId(R.string.abi_validation), state = EnvState.Idle),
) : UiState

sealed class IndexUiIntent : UiIntent {
    data class Initialize(val context: Context) : IndexUiIntent()
    data class ValidateRoot(val context: Context) : IndexUiIntent()
    data class ValidateBin(val context: Context) : IndexUiIntent()
    data class ValidateAbi(val context: Context) : IndexUiIntent()
}

sealed class IndexUiEffect : UiEffect {
    data class ShowSnackbar(
        val message: String,
        val actionLabel: String? = null,
        val withDismissAction: Boolean = false,
        val duration: SnackbarDuration = if (actionLabel == null) SnackbarDuration.Short else SnackbarDuration.Indefinite,
    ) : IndexUiEffect()
}

@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor() : BaseViewModel<IndexUiState, IndexUiIntent, IndexUiEffect>(IndexUiState()) {
    private val mutex = Mutex()

    val snackbarHostState: SnackbarHostState = SnackbarHostState()
    override suspend fun onEffect(effect: IndexUiEffect) {
        when (effect) {
            is IndexUiEffect.ShowSnackbar -> {
                snackbarHostState.showSnackbar(effect.message, effect.actionLabel, effect.withDismissAction, effect.duration)
            }
        }
    }

    @SuppressLint("StringFormatInvalid")
    override suspend fun onEvent(state: IndexUiState, intent: IndexUiIntent) {
        when (intent) {
            is IndexUiIntent.Initialize -> {
                // Kill daemon
                BaseUtil.kill("${intent.context.packageName}:root:daemon")
            }

            is IndexUiIntent.ValidateRoot -> {
                mutex.withLock {
                    runCatching {
                        BaseUtil.initializeEnvironment(context = intent.context)
                    }

                    val rootItem = state.rootItem
                    emitStateSuspend(state = uiState.value.copy(rootItem = rootItem.copy(state = EnvState.Processing)))
                    emitStateSuspend(state = uiState.value.copy(rootItem = rootItem.copy(state = if (Shell.getShell().isRoot) EnvState.Succeed else EnvState.Failed)))
                }
            }

            is IndexUiIntent.ValidateBin -> {
                mutex.withLock {
                    val binItem = state.binItem
                    emitStateSuspend(state = uiState.value.copy(binItem = binItem.copy(state = EnvState.Processing)))
                    emitStateSuspend(
                        state = uiState.value.copy(
                            binItem = binItem.copy(
                                state = if (runCatching { BaseUtil.releaseBase(context = intent.context) }.getOrElse { false }) {
                                    EnvState.Succeed
                                } else {
                                    EnvState.Failed
                                }
                            )
                        )
                    )
                }
            }

            is IndexUiIntent.ValidateAbi -> {
                mutex.withLock {
                    val abiItem = state.abiItem
                    emitStateSuspend(state = uiState.value.copy(abiItem = abiItem.copy(state = EnvState.Processing)))
                    val buildABI = BuildConfigUtil.FLAVOR_abi
                    val deviceABI = Build.SUPPORTED_ABIS.firstOrNull().toString()
                    if (buildABI == deviceABI) {
                        emitStateSuspend(state = uiState.value.copy(abiItem = abiItem.copy(state = EnvState.Succeed)))
                    } else {
                        emitStateSuspend(state = uiState.value.copy(abiItem = abiItem.copy(state = EnvState.Failed)))
                        emitEffectSuspend(
                            IndexUiEffect.ShowSnackbar(
                                message = intent.context.getString(
                                    R.string.this_version_only_supports_but_your_device_is_please_install_version,
                                    buildABI,
                                    deviceABI,
                                    deviceABI
                                )
                            )
                        )
                    }
                }
            }
        }
    }

    val allValidated: StateFlow<Boolean> = uiState.map {
        var allValidated = true
        if (it.rootItem.succeed.not()) allValidated = false
        if (it.binItem.succeed.not()) allValidated = false
        if (it.abiItem.succeed.not()) allValidated = false
        allValidated
    }.flowOnIO().stateInScope(false)
}
