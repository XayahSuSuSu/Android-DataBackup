package com.xayah.feature.main.home.cloud.page.list

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import com.xayah.core.common.util.toPathString
import com.xayah.core.common.viewmodel.BaseViewModel
import com.xayah.core.common.viewmodel.UiEffect
import com.xayah.core.common.viewmodel.UiIntent
import com.xayah.core.common.viewmodel.UiState
import com.xayah.core.data.repository.CloudRepository
import com.xayah.core.database.model.CloudEntity
import com.xayah.core.datastore.readRcloneMainAccountName
import com.xayah.core.datastore.saveRcloneMainAccountName
import com.xayah.core.datastore.saveRcloneMainAccountRemote
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.util.CloudTmpTestFileName
import com.xayah.core.util.PathUtil
import com.xayah.core.util.command.Rclone
import com.xayah.core.util.toPathList
import com.xayah.feature.main.home.premium.R
import com.xayah.libpickyou.ui.PickYouLauncher
import com.xayah.libpickyou.ui.activity.PickerType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

data class IndexUiState(
    val updating: Boolean = true,
) : UiState

sealed class IndexUiIntent : UiIntent {
    object Update : IndexUiIntent()
    data class Delete(val entity: CloudEntity) : IndexUiIntent()
    data class SetAsMainAccount(val context: Context, val entity: CloudEntity) : IndexUiIntent()
    data class SetRemote(val context: ComponentActivity, val entity: CloudEntity) : IndexUiIntent()
    data class TestConnection(val entity: CloudEntity) : IndexUiIntent()
}

sealed class IndexUiEffect : UiEffect {
    data class ShowSnackbar(
        val message: String,
        val actionLabel: String? = null,
        val withDismissAction: Boolean = false,
        val duration: SnackbarDuration = if (actionLabel == null) SnackbarDuration.Short else SnackbarDuration.Indefinite,
    ) : IndexUiEffect()

    object DismissSnackbar : IndexUiEffect()
}

@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor(
    rootService: RemoteRootService,
    private val cloudRepository: CloudRepository,
) : BaseViewModel<IndexUiState, IndexUiIntent, IndexUiEffect>(IndexUiState()) {
    init {
        rootService.onFailure = {
            val msg = it.message
            if (msg != null)
                emitEffect(IndexUiEffect.ShowSnackbar(message = msg))
        }
    }

    override suspend fun onEvent(state: IndexUiState, intent: IndexUiIntent) {
        when (intent) {
            is IndexUiIntent.Update -> {
                emitState(state.copy(updating = true))
                cloudRepository.update()
                emitState(state.copy(updating = false))
            }

            is IndexUiIntent.Delete -> {
                cloudRepository.delete(intent.entity).also { result ->
                    if (result.isSuccess.not()) {
                        emitEffect(IndexUiEffect.ShowSnackbar(message = result.outString))
                    }
                }
            }

            is IndexUiIntent.SetAsMainAccount -> {
                val entity = intent.entity
                if (entity.mount.remote.isEmpty()) {
                    emitEffectSuspend(IndexUiEffect.ShowSnackbar(message = cloudRepository.getString(R.string.remote_not_set)))
                } else {
                    intent.context.saveRcloneMainAccountName(entity.name)
                    intent.context.saveRcloneMainAccountRemote(entity.mount.remote)
                }
            }

            is IndexUiIntent.SetRemote -> {
                PickYouLauncher().apply {
                    val context = intent.context
                    val entity = intent.entity
                    val tmpMountPath = cloudRepository.getTmpMountPath(entity.name)
                    launchOnIO {
                        cloudRepository.mountTmp(name = entity.name, tmpMountPath = tmpMountPath)
                    }

                    withMainContext {
                        setTitle(context.getString(R.string.select_target_directory))
                        setType(PickerType.DIRECTORY)
                        setLimitation(1)
                        val pathSplitList = tmpMountPath.toPathList()
                        val pathSize = pathSplitList.size
                        setDefaultPath(PathUtil.getParentPath(tmpMountPath))
                        setPathPrefixHiddenNum(pathSize - 2)
                        launch(context) { pathList ->
                            launchOnIO {
                                pathList.firstOrNull()?.also { pathString ->
                                    val pathSplit = pathString.toPathList().toMutableList()
                                    // Remove "mount/${name}"
                                    repeat(2) {
                                        pathSplit.removeFirst()
                                    }
                                    val remote = pathSplit.toPathString()
                                    // Add "${name}:"
                                    val finalPath = "${entity.name}:${remote}"
                                    cloudRepository.upsertCloud(entity.copy(mount = entity.mount.copy(remote = finalPath)))
                                    if (context.readRcloneMainAccountName().first() == entity.name) {
                                        context.saveRcloneMainAccountRemote(finalPath)
                                    }
                                }
                                launchOnIO {
                                    cloudRepository.unmountTmp(tmpMountPath)
                                }
                            }
                        }
                    }
                }
            }

            is IndexUiIntent.TestConnection -> {
                val entity = intent.entity
                emitEffectSuspend(IndexUiEffect.DismissSnackbar)
                if (entity.mount.remote.isEmpty()) {
                    emitEffectSuspend(IndexUiEffect.ShowSnackbar(message = cloudRepository.getString(R.string.remote_not_set)))
                } else {
                    emitEffect(
                        IndexUiEffect.ShowSnackbar(
                            message = "${cloudRepository.getString(R.string.processing)}...",
                            duration = SnackbarDuration.Indefinite
                        )
                    )
                    Rclone.mkdir(dst = "${entity.mount.remote}/$CloudTmpTestFileName", dryRun = true).also { result ->
                        emitEffectSuspend(IndexUiEffect.DismissSnackbar)
                        if (result.isSuccess) {
                            emitEffectSuspend(IndexUiEffect.ShowSnackbar(message = cloudRepository.getString(R.string.connection_established)))
                        } else {
                            emitEffectSuspend(IndexUiEffect.ShowSnackbar(message = result.outString, duration = SnackbarDuration.Long))
                        }
                    }
                }
            }
        }
    }

    var snackbarHostState: SnackbarHostState = SnackbarHostState()
    override suspend fun onEffect(effect: IndexUiEffect) {
        when (effect) {
            is IndexUiEffect.ShowSnackbar -> {
                snackbarHostState.showSnackbar(effect.message, effect.actionLabel, effect.withDismissAction, effect.duration)
            }

            is IndexUiEffect.DismissSnackbar -> {
                snackbarHostState.currentSnackbarData?.dismiss()
            }
        }
    }

    private val _accounts: Flow<List<CloudEntity>> = cloudRepository.clouds.flowOnIO()
    val accounts: StateFlow<List<CloudEntity>> = _accounts.stateInScope(listOf())
}
