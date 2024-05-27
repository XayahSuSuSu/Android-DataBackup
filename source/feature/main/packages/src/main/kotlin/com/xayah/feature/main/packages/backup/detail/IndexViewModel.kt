package com.xayah.feature.main.packages.backup.detail

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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data class IndexUiState(
    val packageName: String,
    val userId: Int,
    val infoExpanded: Boolean,
) : UiState

sealed class IndexUiIntent : UiIntent {
    data object OnRefresh : IndexUiIntent()
    data class UpdatePackage(val packageEntity: PackageEntity) : IndexUiIntent()
}

@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor(
    args: SavedStateHandle,
    private val packageRepo: PackageRepository,
    private val rootService: RemoteRootService,
) : BaseViewModel<IndexUiState, IndexUiIntent, IndexUiEffect>(
    IndexUiState(
        packageName = args.get<String>(MainRoutes.ARG_PACKAGE_NAME) ?: "",
        userId = args.get<String>(MainRoutes.ARG_USER_ID)?.toIntOrNull() ?: 0,
        infoExpanded = false,
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
                packageRepo.updateLocalPackageDataSize(state.packageName, OpType.BACKUP, state.userId, 0)
            }

            is IndexUiIntent.UpdatePackage -> {
                packageRepo.upsert(intent.packageEntity)
            }
        }
    }

    private val _package: Flow<PackageEntity?> = packageRepo.getPackage(uiState.value.packageName, OpType.BACKUP, uiState.value.userId, 0).flowOnIO()
    val packageState: StateFlow<PackageEntity?> = _package.stateInScope(null)
}
