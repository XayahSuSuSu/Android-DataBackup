package com.xayah.feature.main.reload

import androidx.compose.material3.ExperimentalMaterial3Api
import com.xayah.core.common.viewmodel.BaseViewModel
import com.xayah.core.common.viewmodel.IndexUiEffect
import com.xayah.core.common.viewmodel.UiIntent
import com.xayah.core.common.viewmodel.UiState
import com.xayah.core.model.EmojiString
import com.xayah.feature.main.reload.model.MediumReloadingState
import com.xayah.feature.main.reload.model.PackagesReloadingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

data class IndexUiState(
    val typeIndex: Int = 0,
    val typeList: List<String>,
    val versionIndex: Int = 0,
    val versionList: List<String>,
) : UiState

sealed class IndexUiIntent : UiIntent {
    object Update : IndexUiIntent()
    object Save : IndexUiIntent()
}

@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor(
    private val reloadRepository: ReloadRepository,
) : BaseViewModel<IndexUiState, IndexUiIntent, IndexUiEffect>(IndexUiState(typeList = reloadRepository.typeList, versionList = reloadRepository.versionList)) {
    override suspend fun onEvent(state: IndexUiState, intent: IndexUiIntent) {
        when (intent) {
            is IndexUiIntent.Update -> {
                reloadRepository.getMedium(typeIndex = uiState.value.typeIndex, versionIndex = uiState.value.versionIndex, mutableState = _medium)
                reloadRepository.getPackages(typeIndex = uiState.value.typeIndex, versionIndex = uiState.value.versionIndex, mutableState = _packages)
            }

            is IndexUiIntent.Save -> {
                if (savingState.value.not()) {
                    _savingState.value = true
                    reloadRepository.saveMedium(medium = mediumState.value.medium, versionIndex = state.versionIndex)
                    reloadRepository.savePackages(packages = packagesState.value.packages, versionIndex = state.versionIndex)
                    _savingState.value = false
                    emitEffect(
                        IndexUiEffect.ShowSnackbar(
                            message = reloadRepository.getString(R.string.succeed) + EmojiString.PARTY_POPPER.emoji
                        )
                    )
                    emitIntent(IndexUiIntent.Update)
                }
            }
        }
    }

    private val _medium: MutableStateFlow<MediumReloadingState> = MutableStateFlow(MediumReloadingState())
    val mediumState: StateFlow<MediumReloadingState> = _medium.asStateFlow()

    private val _packages: MutableStateFlow<PackagesReloadingState> = MutableStateFlow(PackagesReloadingState())
    val packagesState: StateFlow<PackagesReloadingState> = _packages.asStateFlow()

    private val _reloadState: Flow<Boolean> = combine(_medium, _packages) { medium, packages ->
        medium.isFinished && packages.isFinished
    }.flowOnIO()
    val reloadState: StateFlow<Boolean> = _reloadState.stateInScope(false)

    private val _savingState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val savingState: StateFlow<Boolean> = _savingState.asStateFlow()
}
