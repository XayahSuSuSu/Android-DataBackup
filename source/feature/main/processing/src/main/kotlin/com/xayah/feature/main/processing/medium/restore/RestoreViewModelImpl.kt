package com.xayah.feature.main.processing.medium.restore

import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.SavedStateHandle
import com.xayah.core.data.repository.CloudRepository
import com.xayah.core.data.repository.MediaRepository
import com.xayah.core.data.repository.TaskRepository
import com.xayah.core.model.OpType
import com.xayah.core.model.StorageMode
import com.xayah.core.model.database.MediaEntity
import com.xayah.core.model.util.formatSize
import com.xayah.core.network.client.getCloud
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.service.medium.restore.ProcessingServiceProxyCloudImpl
import com.xayah.core.service.medium.restore.ProcessingServiceProxyLocalImpl
import com.xayah.core.ui.material3.SnackbarDuration
import com.xayah.core.ui.material3.SnackbarType
import com.xayah.core.ui.model.DialogRadioItem
import com.xayah.core.ui.route.MainRoutes
import com.xayah.core.ui.viewmodel.IndexUiEffect
import com.xayah.core.util.LogUtil
import com.xayah.core.util.decodeURL
import com.xayah.core.util.localBackupSaveDir
import com.xayah.core.util.navigateSingle
import com.xayah.feature.main.processing.AbstractMediumProcessingViewModel
import com.xayah.feature.main.processing.FinishSetup
import com.xayah.feature.main.processing.IndexUiState
import com.xayah.feature.main.processing.ProcessingUiIntent
import com.xayah.feature.main.processing.R
import com.xayah.feature.main.processing.SetCloudEntity
import com.xayah.feature.main.processing.UpdateFiles
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@ExperimentalCoroutinesApi
@ExperimentalMaterial3Api
@HiltViewModel
class RestoreViewModelImpl @Inject constructor(
    @ApplicationContext private val mContext: Context,
    private val mRootService: RemoteRootService,
    mTaskRepo: TaskRepository,
    private val mMediaRepo: MediaRepository,
    private val mCloudRepo: CloudRepository,
    mLocalService: ProcessingServiceProxyLocalImpl,
    mCloudService: ProcessingServiceProxyCloudImpl,
    private val args: SavedStateHandle,
) : AbstractMediumProcessingViewModel(mContext, mRootService, mTaskRepo, mLocalService, mCloudService) {
    override suspend fun onOtherEvent(state: IndexUiState, intent: ProcessingUiIntent) {
        when (intent) {
            is UpdateFiles -> {
                val cloud: String
                val backupSaveDir: String
                if (uiState.value.cloudEntity == null) {
                    cloud = ""
                    backupSaveDir = mContext.localBackupSaveDir()
                } else {
                    cloud = uiState.value.cloudEntity!!.name
                    backupSaveDir = uiState.value.cloudEntity!!.remote
                }
                val medium = mMediaRepo.queryActivated(OpType.RESTORE, cloud, backupSaveDir)
                LogUtil.log { "RestoreViewModelImpl.UpdateApps" to "Query activated files, cloud: $cloud, backupDir: $backupSaveDir" }
                LogUtil.log { "RestoreViewModelImpl.UpdateApps" to "Queried files count: ${medium.size}" }
                var bytes = 0.0
                medium.forEach {
                    bytes += it.displayStatsBytes
                }
                _medium.value = medium
                _mediumSize.value = bytes.formatSize()
            }

            is SetCloudEntity -> {
                val name = args.get<String>(MainRoutes.ARG_ACCOUNT_NAME)?.decodeURL()?.trim() ?: ""
                if (name.isNotEmpty()) {
                    emitState(state.copy(storageIndex = 1, storageType = StorageMode.Cloud, cloudEntity = mCloudRepo.queryByName(name)))
                } else {
                    emitState(state.copy(storageIndex = 0, storageType = StorageMode.Local, cloudEntity = null))
                }
            }

            is FinishSetup -> {
                if (state.storageType == StorageMode.Cloud) {
                    _isTesting.value = true
                    emitEffect(IndexUiEffect.DismissSnackbar)
                    emitEffectOnIO(
                        IndexUiEffect.ShowSnackbar(
                            type = SnackbarType.Loading,
                            message = mCloudRepo.getString(R.string.processing),
                            duration = SnackbarDuration.Indefinite,
                        )
                    )
                    runCatching {
                        val client = state.cloudEntity!!.getCloud()
                        client.testConnection()
                        emitEffect(IndexUiEffect.DismissSnackbar)
                        withMainContext {
                            intent.navController.popBackStack()
                            intent.navController.navigateSingle(MainRoutes.MediumRestoreProcessing.route)
                        }
                    }.onFailure {
                        emitEffect(IndexUiEffect.DismissSnackbar)
                        if (it.localizedMessage != null)
                            emitEffectOnIO(IndexUiEffect.ShowSnackbar(type = SnackbarType.Error, message = it.localizedMessage!!, duration = SnackbarDuration.Long))
                    }
                    _isTesting.value = false
                } else {
                    withMainContext {
                        intent.navController.popBackStack()
                        intent.navController.navigateSingle(MainRoutes.MediumRestoreProcessing.route)
                    }
                }
            }

            else -> {

            }
        }
    }

    private val _accounts: Flow<List<DialogRadioItem<Any>>> = mCloudRepo.clouds.map { entities ->
        entities.map {
            DialogRadioItem(
                enum = Any(),
                title = it.name,
                desc = it.user,
            )
        }
    }.flowOnIO()
    private val _isTesting: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val _medium: MutableStateFlow<List<MediaEntity>> = MutableStateFlow(listOf())
    private val _mediumSize: MutableStateFlow<String> = MutableStateFlow("")

    val accounts: StateFlow<List<DialogRadioItem<Any>>> = _accounts.stateInScope(listOf())
    val isTesting: StateFlow<Boolean> = _isTesting.stateInScope(false)
    val medium: StateFlow<List<MediaEntity>> = _medium.stateInScope(listOf())
    val mediumSize: StateFlow<String> = _mediumSize.stateInScope("")
}