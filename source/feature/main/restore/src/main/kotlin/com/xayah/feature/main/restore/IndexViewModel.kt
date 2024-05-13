package com.xayah.feature.main.restore

import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3Api
import com.xayah.core.common.viewmodel.BaseViewModel
import com.xayah.core.common.viewmodel.IndexUiEffect
import com.xayah.core.common.viewmodel.UiIntent
import com.xayah.core.common.viewmodel.UiState
import com.xayah.core.data.repository.PackageRepository
import com.xayah.core.datastore.readLastRestoreTime
import com.xayah.core.model.OpType
import com.xayah.core.model.database.PackageEntity
import com.xayah.core.model.util.formatSize
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data class IndexUiState(
    val packages: List<PackageEntity>,
    val packagesSize: String
) : UiState

sealed class IndexUiIntent : UiIntent {
    data object UpdateApps : IndexUiIntent()
}

@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val pkgRepo: PackageRepository,
) : BaseViewModel<IndexUiState, IndexUiIntent, IndexUiEffect>(IndexUiState(packages = listOf(), packagesSize = "")) {
    override suspend fun onEvent(state: IndexUiState, intent: IndexUiIntent) {
        when (intent) {
            is IndexUiIntent.UpdateApps -> {
                val packages = pkgRepo.queryPackages(OpType.RESTORE)
                var bytes = 0L
                packages.forEach {
                    bytes += it.displayStats.apkBytes
                    bytes += it.displayStats.userBytes
                    bytes += it.displayStats.userDeBytes
                    bytes += it.displayStats.dataBytes
                    bytes += it.displayStats.obbBytes
                    bytes += it.displayStats.mediaBytes
                }
                emitState(state.copy(packages = packages, packagesSize = bytes.toDouble().formatSize()))
            }
        }
    }

    private val _lastRestoreTime: Flow<Long> = context.readLastRestoreTime().flowOnIO()
    val lastRestoreTimeState: StateFlow<Long> = _lastRestoreTime.stateInScope(0)
}
