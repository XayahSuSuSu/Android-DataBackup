package com.xayah.feature.main.packages.redesigned.backup.processing

import androidx.compose.material3.ExperimentalMaterial3Api
import com.xayah.core.common.viewmodel.BaseViewModel
import com.xayah.core.common.viewmodel.IndexUiEffect
import com.xayah.core.common.viewmodel.UiIntent
import com.xayah.core.common.viewmodel.UiState
import com.xayah.core.data.repository.TaskRepository
import com.xayah.core.model.OperationState
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.service.packages.backup.LocalProcessingImpl
import com.xayah.core.ui.model.ProcessingCardItem
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.util.addInfo
import com.xayah.core.ui.util.fromString
import com.xayah.core.ui.util.toProcessingCardItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject

data class IndexUiState(
    val state: OperationState,
) : UiState

sealed class IndexUiIntent : UiIntent {
    data object Backup : IndexUiIntent()
    data object Initialize : IndexUiIntent()
    data object DestroyService : IndexUiIntent()
}

@ExperimentalCoroutinesApi
@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor(
    rootService: RemoteRootService,
    private val taskRepo: TaskRepository,
    private val localBackupService: LocalProcessingImpl,
) : BaseViewModel<IndexUiState, IndexUiIntent, IndexUiEffect>(
    IndexUiState(
        state = OperationState.IDLE,
    )
) {
    init {
        rootService.onFailure = {
            val msg = it.message
            if (msg != null)
                emitEffect(IndexUiEffect.ShowSnackbar(message = msg))
        }
    }

    override suspend fun onSuspendEvent(state: IndexUiState, intent: IndexUiIntent) {
        when (intent) {
            is IndexUiIntent.DestroyService -> {
                localBackupService.destroyService()
            }

            else -> {

            }
        }
    }

    override suspend fun onEvent(state: IndexUiState, intent: IndexUiIntent) {
        when (intent) {
            is IndexUiIntent.Initialize -> {
                _taskId.value = localBackupService.initialize()
            }

            is IndexUiIntent.Backup -> {
                emitStateSuspend(state.copy(state = OperationState.PROCESSING))
                val backupPreprocessing = localBackupService.preprocessing()
                localBackupService.processing()
                localBackupService.postProcessing(backupPreprocessing = backupPreprocessing)
                localBackupService.destroyService()
                emitStateSuspend(state.copy(state = OperationState.DONE))
            }

            else -> {}
        }
    }

    private val _taskId: MutableStateFlow<Long> = MutableStateFlow(-1)
    private var _preItems: Flow<List<ProcessingCardItem>> = _taskId.flatMapLatest { id ->
        taskRepo.queryPackagePreFlow(id)
            .map {
                val items = mutableListOf<ProcessingCardItem>()
                it?.apply {
                    items.addInfo(preInfo)
                }
                items
            }
            .flowOnIO()
    }
    private val _postItems: Flow<List<ProcessingCardItem>> = _taskId.flatMapLatest { id ->
        taskRepo.queryPackagePostFlow(id)
            .map {
                val items = mutableListOf<ProcessingCardItem>()
                it?.apply {
                    items.addInfo(postInfo)
                    items.addInfo(backupItselfInfo)
                    items.addInfo(saveIconsInfo)
                }
                items
            }
            .flowOnIO()
    }
    private val _packageItems: Flow<List<ProcessingCardItem>> = _taskId.flatMapLatest { id ->
        taskRepo.queryPackageFlow(id)
            .map { packages ->
                packages.map {
                    ProcessingCardItem(
                        title = StringResourceToken.fromString(it.packageEntity.packageInfo.label),
                        state = it.state,
                        secondaryItems = listOf(
                            it.apkInfo.toProcessingCardItem,
                            it.userInfo.toProcessingCardItem,
                            it.userDeInfo.toProcessingCardItem,
                            it.dataInfo.toProcessingCardItem,
                            it.obbInfo.toProcessingCardItem,
                            it.mediaInfo.toProcessingCardItem,
                        )
                    )
                }
            }
            .flowOnIO()
    }

    var preItems: StateFlow<List<ProcessingCardItem>> = _preItems.stateInScope(listOf())
    val postItems: StateFlow<List<ProcessingCardItem>> = _postItems.stateInScope(listOf())
    val packageItems: StateFlow<List<ProcessingCardItem>> = _packageItems.stateInScope(listOf())
}
