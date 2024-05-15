package com.xayah.feature.main.cloud.redesigned.add

import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavHostController
import com.xayah.core.data.repository.CloudRepository
import com.xayah.core.model.CloudType
import com.xayah.core.model.SmbVersion
import com.xayah.core.model.database.CloudEntity
import com.xayah.core.model.database.FTPExtra
import com.xayah.core.model.database.SMBExtra
import com.xayah.core.network.client.getCloud
import com.xayah.core.ui.material3.SnackbarDuration
import com.xayah.core.ui.material3.SnackbarType
import com.xayah.core.ui.route.MainRoutes
import com.xayah.core.ui.viewmodel.BaseViewModel
import com.xayah.core.ui.viewmodel.IndexUiEffect
import com.xayah.core.ui.viewmodel.UiIntent
import com.xayah.core.ui.viewmodel.UiState
import com.xayah.core.util.GsonUtil
import com.xayah.feature.main.cloud.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class IndexUiState(
    val currentName: String,
    val cloudEntity: CloudEntity?,
    val isProcessing: Boolean,
) : UiState

sealed class IndexUiIntent : UiIntent {
    data object Initialize : IndexUiIntent()

    data class UpdateEntity(
        val name: String,
        val remote: String,
        val type: CloudType,
        val url: String,
        val username: String,
        val password: String,
        val extra: String,
    ) : IndexUiIntent()

    data class CreateAccount(val navController: NavHostController) : IndexUiIntent()

    data object TestConnection : IndexUiIntent()
    data class DeleteAccount(val navController: NavHostController) : IndexUiIntent()
    data class SetRemotePath(val context: Context) : IndexUiIntent()
}

@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor(
    private val cloudRepo: CloudRepository,
    args: SavedStateHandle,
) : BaseViewModel<IndexUiState, IndexUiIntent, IndexUiEffect>(
    IndexUiState(
        currentName = args.get<String>(MainRoutes.ArgAccountName)?.trim() ?: "",
        cloudEntity = null,
        isProcessing = false,
    )
) {

    suspend fun updateFTPEntity(name: String, remote: String, url: String, username: String, password: String, port: String) {
        val extra = GsonUtil().toJson(FTPExtra(port = port.toIntOrNull() ?: 21))
        emitIntent(
            IndexUiIntent.UpdateEntity(
                name = name,
                type = CloudType.FTP,
                url = url,
                username = username,
                password = password,
                extra = extra,
                remote = remote,
            )
        )
    }

    suspend fun updateWebDAVEntity(name: String, remote: String, url: String, username: String, password: String) {
        emitIntent(
            IndexUiIntent.UpdateEntity(
                name = name,
                type = CloudType.WEBDAV,
                url = url,
                username = username,
                password = password,
                extra = "{}",
                remote = remote,
            )
        )
    }

    suspend fun updateSMBEntity(name: String, remote: String, url: String, username: String, password: String, share: String, port: String, domain: String) {
        val extra = GsonUtil().toJson(
            SMBExtra(
                share = share,
                port = port.toIntOrNull() ?: 445,
                domain = domain,
                version = listOf(SmbVersion.SMB_2_0_2, SmbVersion.SMB_2_1, SmbVersion.SMB_3_0, SmbVersion.SMB_3_0_2, SmbVersion.SMB_3_1_1)
            )
        )
        emitIntent(
            IndexUiIntent.UpdateEntity(
                name = name,
                type = CloudType.SMB,
                url = url,
                username = username,
                password = password,
                extra = extra,
                remote = remote,
            )
        )
    }

    override suspend fun onEvent(state: IndexUiState, intent: IndexUiIntent) {
        when (intent) {
            is IndexUiIntent.Initialize -> {
                if (uiState.value.currentName.isNotEmpty()) {
                    emitState(state.copy(cloudEntity = cloudRepo.queryByName(uiState.value.currentName)))
                }
            }

            is IndexUiIntent.CreateAccount -> {
                cloudRepo.upsert(state.cloudEntity!!)
                withMainContext {
                    intent.navController.popBackStack()
                    if (state.currentName.isEmpty()) {
                        intent.navController.popBackStack()
                    }
                }
            }

            is IndexUiIntent.UpdateEntity -> {
                emitState(
                    state.copy(
                        cloudEntity = CloudEntity(
                            name = intent.name,
                            type = intent.type,
                            host = intent.url,
                            user = intent.username,
                            pass = intent.password,
                            remote = intent.remote,
                            extra = intent.extra,
                            activated = false,
                        )
                    )
                )
            }

            is IndexUiIntent.TestConnection -> {
                emitState(state.copy(isProcessing = true))
                emitEffect(IndexUiEffect.DismissSnackbar)
                emitEffectOnIO(
                    IndexUiEffect.ShowSnackbar(
                        type = SnackbarType.Loading,
                        message = cloudRepo.getString(R.string.processing),
                        duration = SnackbarDuration.Indefinite,
                    )
                )
                runCatching {
                    val client = state.cloudEntity!!.getCloud()
                    client.testConnection()
                    emitEffect(IndexUiEffect.DismissSnackbar)
                    emitEffectOnIO(IndexUiEffect.ShowSnackbar(type = SnackbarType.Success, message = cloudRepo.getString(R.string.connection_established)))
                }.onFailure {
                    emitEffect(IndexUiEffect.DismissSnackbar)
                    if (it.localizedMessage != null)
                        emitEffectOnIO(IndexUiEffect.ShowSnackbar(type = SnackbarType.Error, message = it.localizedMessage!!, duration = SnackbarDuration.Long))
                }
                emitState(state.copy(isProcessing = false))
            }

            is IndexUiIntent.DeleteAccount -> {
                cloudRepo.delete(cloudRepo.queryByName(uiState.value.currentName)!!)
                withMainContext {
                    intent.navController.popBackStack()
                }
            }

            is IndexUiIntent.SetRemotePath -> {
                emitState(state.copy(isProcessing = true))
                val context = intent.context
                emitEffect(IndexUiEffect.DismissSnackbar)
                emitEffectOnIO(
                    IndexUiEffect.ShowSnackbar(
                        message = cloudRepo.getString(R.string.processing),
                        duration = SnackbarDuration.Indefinite,
                        type = SnackbarType.Loading,
                    )
                )
                runCatching {
                    val client = state.cloudEntity!!.getCloud()
                    client.setRemote(
                        context = context,
                        onSet = { remote, extraString ->
                            emitState(state.copy(cloudEntity = state.cloudEntity.copy(remote = remote, extra = extraString)))
                            emitEffect(IndexUiEffect.DismissSnackbar)
                        }
                    )
                }.onFailure {
                    emitEffect(IndexUiEffect.DismissSnackbar)
                    if (it.localizedMessage != null)
                        emitEffectOnIO(IndexUiEffect.ShowSnackbar(type = SnackbarType.Error, message = it.localizedMessage!!, duration = SnackbarDuration.Long))
                }
                emitState(state.copy(isProcessing = false))
            }
        }
    }
}
