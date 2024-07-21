package com.xayah.feature.main.restore.reload

import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.SavedStateHandle
import com.xayah.core.data.repository.PackageRepository
import com.xayah.core.ui.model.DialogRadioItem
import com.xayah.core.ui.route.MainRoutes
import com.xayah.core.ui.viewmodel.BaseViewModel
import com.xayah.core.ui.viewmodel.IndexUiEffect
import com.xayah.core.ui.viewmodel.UiIntent
import com.xayah.core.ui.viewmodel.UiState
import com.xayah.core.util.decodeURL
import com.xayah.feature.main.restore.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

data class IndexUiState(
    val cloudName: String,
    val cloudRemote: String,
    val versionList: List<DialogRadioItem<Any>>,
    val versionIndex: Int,
    val isLoading: Boolean,
    val text: String,
) : UiState

sealed class IndexUiIntent : UiIntent {
    data object Reload : IndexUiIntent()
}

@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val packageRepo: PackageRepository,
    args: SavedStateHandle,
) : BaseViewModel<IndexUiState, IndexUiIntent, IndexUiEffect>(
    IndexUiState(
        cloudName = args.get<String>(MainRoutes.ARG_ACCOUNT_NAME)?.decodeURL()?.trim() ?: "",
        cloudRemote = args.get<String>(MainRoutes.ARG_ACCOUNT_REMOTE)?.decodeURL()?.trim() ?: "",
        versionList = listOf(
            DialogRadioItem(
                title = "current",
            ),
            DialogRadioItem(
                title = "1.1.x",
            ),
            DialogRadioItem(
                title = "1.0.x",
            ),
        ),
        versionIndex = 0,
        isLoading = false,
        text = context.getString(R.string.idle),
    )
) {
    override suspend fun onEvent(state: IndexUiState, intent: IndexUiIntent) {
        when (intent) {
            is IndexUiIntent.Reload -> {
                emitState(uiState.value.copy(isLoading = true))
                when (uiState.value.versionIndex) {
                    1 -> {
                        // 1.1.x
                        if (state.cloudName.isEmpty()) {
                            // Local
                            packageRepo.modifyAppsStructureFromLocal11x {
                                emitState(uiState.value.copy(text = it))
                            }
                            packageRepo.modifyFilesStructureFromLocal11x {
                                emitState(uiState.value.copy(text = it))
                            }
                        } else {
                            // Cloud
                            packageRepo.modifyAppsStructureFromCloud11x(state.cloudName) {
                                emitState(uiState.value.copy(text = it))
                            }
                            packageRepo.modifyFilesStructureFromCloud11x(state.cloudName) {
                                emitState(uiState.value.copy(text = it))
                            }
                        }
                    }

                    2 -> {
                        // 1.0.x
                        if (state.cloudName.isEmpty()) {
                            // Local
                            packageRepo.modifyAppsStructureFromLocal10x {
                                emitState(uiState.value.copy(text = it))
                            }
                            packageRepo.modifyFilesStructureFromLocal10x {
                                emitState(uiState.value.copy(text = it))
                            }
                        } else {
                            // Cloud
                            packageRepo.modifyAppsStructureFromCloud10x(state.cloudName) {
                                emitState(uiState.value.copy(text = it))
                            }
                            packageRepo.modifyFilesStructureFromCloud10x(state.cloudName) {
                                emitState(uiState.value.copy(text = it))
                            }
                        }
                    }
                }

                if (state.cloudName.isEmpty()) {
                    // Local
                    packageRepo.reloadAppsFromLocal12x {
                        emitState(uiState.value.copy(text = it))
                    }
                    packageRepo.reloadFilesFromLocal12x {
                        emitState(uiState.value.copy(text = it))
                    }
                } else {
                    // Cloud
                    packageRepo.reloadAppsFromCloud12x(state.cloudName) {
                        emitState(uiState.value.copy(text = it))
                    }
                    packageRepo.reloadFilesFromCloud12x(state.cloudName) {
                        emitState(uiState.value.copy(text = it))
                    }
                }
                emitState(uiState.value.copy(isLoading = false, text = context.getString(R.string.finished)))
            }
        }
    }
}
