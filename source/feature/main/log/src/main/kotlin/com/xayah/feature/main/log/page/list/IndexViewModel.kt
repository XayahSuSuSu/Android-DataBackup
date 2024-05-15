package com.xayah.feature.main.log.page.list

import androidx.compose.material3.ExperimentalMaterial3Api
import com.xayah.core.ui.viewmodel.BaseViewModel
import com.xayah.core.ui.viewmodel.IndexUiEffect
import com.xayah.core.ui.viewmodel.UiIntent
import com.xayah.core.ui.viewmodel.UiState
import com.xayah.core.util.FileUtil
import com.xayah.core.util.LogUtil
import com.xayah.feature.main.log.LogCardItem
import com.xayah.feature.main.log.LogListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class IndexUiState(
    val updating: Boolean = true,
) : UiState

sealed class IndexUiIntent : UiIntent {
    data object Update : IndexUiIntent()
    data class Delete(val path: String) : IndexUiIntent()
    data object DeleteAll : IndexUiIntent()
    data class ShareLog(val name: String) : IndexUiIntent()
}

@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor(
    private val logListRepository: LogListRepository,
) : BaseViewModel<IndexUiState, IndexUiIntent, IndexUiEffect>(IndexUiState()) {
    override suspend fun onEvent(state: IndexUiState, intent: IndexUiIntent) {
        when (intent) {
            is IndexUiIntent.Update -> {
                emitState(state.copy(updating = true))
                _logCardItems.value = logListRepository.listLogFiles()
                emitState(state.copy(updating = false))
            }

            is IndexUiIntent.Delete -> {
                FileUtil.deleteRecursively(intent.path)
                _logCardItems.value = logListRepository.listLogFiles()
            }

            is IndexUiIntent.DeleteAll -> {
                _logCardItems.value.forEach {
                    if (it.name != LogUtil.getLogFileName()) {
                        FileUtil.deleteRecursively(it.path)
                    }
                }
                _logCardItems.value = logListRepository.listLogFiles()
            }

            is IndexUiIntent.ShareLog -> {
                logListRepository.shareLog(name = intent.name)
            }
        }
    }

    private val _logCardItems: MutableStateFlow<List<LogCardItem>> = MutableStateFlow(listOf())
    val logCardItems: StateFlow<List<LogCardItem>> = _logCardItems.asStateFlow()
}
