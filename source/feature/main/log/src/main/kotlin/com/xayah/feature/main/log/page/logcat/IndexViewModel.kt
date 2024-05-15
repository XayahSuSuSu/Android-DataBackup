package com.xayah.feature.main.log.page.logcat

import androidx.compose.material3.ExperimentalMaterial3Api
import com.xayah.core.common.util.toLineString
import com.xayah.core.ui.viewmodel.BaseViewModel
import com.xayah.core.ui.viewmodel.IndexUiEffect
import com.xayah.core.ui.viewmodel.UiIntent
import com.xayah.core.ui.viewmodel.UiState
import com.xayah.core.data.repository.ContextRepository
import com.xayah.core.database.dao.LogcatDao
import com.xayah.core.model.database.LogcatEntity
import com.xayah.core.util.DateUtil
import com.xayah.core.util.LogUtil
import com.xayah.core.util.command.BaseUtil
import com.xayah.core.util.logDir
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import java.io.File
import javax.inject.Inject

data object IndexUiState : UiState

sealed class IndexUiIntent : UiIntent {
    data object Start : IndexUiIntent()
    data object Stop : IndexUiIntent()
    data object Clear : IndexUiIntent()
    data object Share : IndexUiIntent()
}

@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor(
    private val logcatDao: LogcatDao,
    private val contextRepo: ContextRepository,
) : BaseViewModel<IndexUiState, IndexUiIntent, IndexUiEffect>(IndexUiState) {
    companion object {
        private val _isRunning: MutableStateFlow<Boolean> = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
        private val timestamp: Long = DateUtil.getTimestamp()
        private val mutex = Mutex()
    }

    @DelicateCoroutinesApi
    override suspend fun onEvent(state: IndexUiState, intent: IndexUiIntent) {
        when (intent) {
            is IndexUiIntent.Start -> {
                _isRunning.value = true
                launchOnGlobal {
                    runCatching {
                        BaseUtil.logcat(timestamp, mutex, _isRunning) { cached ->
                            logcatDao.upsert(cached)
                        }
                    }
                }
            }

            is IndexUiIntent.Stop -> {
                _isRunning.value = false
            }

            is IndexUiIntent.Clear -> {
                logcatDao.clear()
            }

            is IndexUiIntent.Share -> {
                val contents = logcatDao.query().map { "${it.date} ${it.time} ${it.pid}-${it.tid} ${it.level} ${it.tag}: ${it.msg}" }.toLineString()
                contextRepo.withContext {
                    val name = "logcat_$timestamp.txt"
                    File(it.logDir(), name).writeText(contents)
                    LogUtil.shareLog(context = it, name = name)
                }
            }
        }
    }

    private val _contentItems: Flow<List<LogcatEntity>> = logcatDao.queryFlow().flowOnIO()
    val contentItems: StateFlow<List<LogcatEntity>> = _contentItems.stateInScope(listOf())
}
