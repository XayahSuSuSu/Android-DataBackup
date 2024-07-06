package com.xayah.feature.main.packages.restore.detail

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.SavedStateHandle
import com.xayah.core.data.repository.PackageRepository
import com.xayah.core.model.OpType
import com.xayah.core.model.database.PackageEntity
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.ui.route.MainRoutes
import com.xayah.core.ui.viewmodel.BaseViewModel
import com.xayah.core.ui.viewmodel.IndexUiEffect
import com.xayah.core.ui.viewmodel.UiIntent
import com.xayah.core.ui.viewmodel.UiState
import com.xayah.core.util.decodeURL
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data class IndexUiState(
    val packageName: String,
    val userId: Int,
    val preserveId: Long,
    val isCalculating: Boolean,
) : UiState

sealed class IndexUiIntent : UiIntent {
    data object OnRefresh : IndexUiIntent()
    data class UpdatePackage(val packageEntity: PackageEntity) : IndexUiIntent()
    data class Delete(val packageEntity: PackageEntity) : IndexUiIntent()
    data class Preserve(val packageEntity: PackageEntity) : IndexUiIntent()
}

@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor(
    args: SavedStateHandle,
    private val packageRepo: PackageRepository,
    rootService: RemoteRootService,
) : BaseViewModel<IndexUiState, IndexUiIntent, IndexUiEffect>(
    IndexUiState(
        packageName = args.get<String>(MainRoutes.ARG_PACKAGE_NAME)?.decodeURL()?.trim() ?: "",
        userId = args.get<String>(MainRoutes.ARG_USER_ID)?.toIntOrNull() ?: 0,
        preserveId = args.get<String>(MainRoutes.ARG_PRESERVE_ID)?.toLongOrNull() ?: 0,
        isCalculating = false,
    )
) {
    init {
        rootService.onFailure = {
            val msg = it.message
            if (msg != null)
                emitEffectOnIO(IndexUiEffect.ShowSnackbar(message = msg))
        }
    }

    @DelicateCoroutinesApi
    override suspend fun onEvent(state: IndexUiState, intent: IndexUiIntent) {
        when (intent) {
            is IndexUiIntent.OnRefresh -> {
                emitState(state.copy(isCalculating = true))
                packageRepo.updateLocalPackageArchivesSize(state.packageName, OpType.RESTORE, state.userId)
                emitState(state.copy(isCalculating = false))
            }

            is IndexUiIntent.UpdatePackage -> {
                packageRepo.upsert(intent.packageEntity)
            }

            is IndexUiIntent.Delete -> {
                packageRepo.delete(intent.packageEntity)
            }

            is IndexUiIntent.Preserve -> {
                packageRepo.preserve(intent.packageEntity)
            }
        }
    }

    private val _package: Flow<PackageEntity?> = packageRepo.getPackage(uiState.value.packageName, OpType.RESTORE, uiState.value.userId, uiState.value.preserveId).flowOnIO()
    val packageState: StateFlow<PackageEntity?> = _package.stateInScope(null)
}
