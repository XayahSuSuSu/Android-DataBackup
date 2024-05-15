package com.xayah.feature.main.cloud.list

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import com.xayah.core.ui.material3.SnackbarDuration
import androidx.navigation.NavHostController
import com.xayah.core.ui.viewmodel.BaseViewModel
import com.xayah.core.ui.viewmodel.IndexUiEffect
import com.xayah.core.ui.viewmodel.UiIntent
import com.xayah.core.ui.viewmodel.UiState
import com.xayah.core.data.repository.CloudRepository
import com.xayah.core.datastore.saveCloudActivatedAccountName
import com.xayah.core.model.database.CloudEntity
import com.xayah.core.network.client.getCloud
import com.xayah.feature.main.cloud.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data object IndexUiState : UiState

sealed class IndexUiIntent : UiIntent {
    data class Delete(val entity: CloudEntity) : IndexUiIntent()
    data class SetRemote(val context: ComponentActivity, val entity: CloudEntity) : IndexUiIntent()
    data class TestConnection(val entity: CloudEntity) : IndexUiIntent()
    data class Navigate(val context: Context, val entity: CloudEntity, val navController: NavHostController, val route: String) : IndexUiIntent()
}

@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor(
    private val cloudRepository: CloudRepository,
) : BaseViewModel<IndexUiState, IndexUiIntent, IndexUiEffect>(IndexUiState) {
    override suspend fun onEvent(state: IndexUiState, intent: IndexUiIntent) {
        when (intent) {
            is IndexUiIntent.Delete -> {
                cloudRepository.delete(intent.entity)
            }

            is IndexUiIntent.SetRemote -> {
                val context = intent.context
                val entity = intent.entity
                emitEffect(IndexUiEffect.DismissSnackbar)
                emitEffectOnIO(
                    IndexUiEffect.ShowSnackbar(
                        message = "${cloudRepository.getString(R.string.processing)}...",
                        duration = SnackbarDuration.Indefinite
                    )
                )
                runCatching {
                    val client = entity.getCloud()
                    client.setRemote(
                        context = context,
                        onSet = { remote, extraString ->
                            cloudRepository.upsert(entity.copy(remote = remote, extra = extraString))
                            emitEffect(IndexUiEffect.DismissSnackbar)
                        }
                    )
                }.onFailure {
                    emitEffect(IndexUiEffect.DismissSnackbar)
                    if (it.localizedMessage != null)
                        emitEffect(IndexUiEffect.ShowSnackbar(message = it.localizedMessage!!, duration = SnackbarDuration.Long))
                }
            }

            is IndexUiIntent.TestConnection -> {
                val entity = intent.entity
                emitEffect(IndexUiEffect.DismissSnackbar)
                emitEffect(
                    IndexUiEffect.ShowSnackbar(
                        message = "${cloudRepository.getString(R.string.processing)}...",
                        duration = SnackbarDuration.Indefinite
                    )
                )
                runCatching {
                    val client = entity.getCloud()
                    client.testConnection()
                    emitEffect(IndexUiEffect.DismissSnackbar)
                    emitEffect(IndexUiEffect.ShowSnackbar(message = cloudRepository.getString(R.string.connection_established)))
                }.onFailure {
                    emitEffect(IndexUiEffect.DismissSnackbar)
                    if (it.localizedMessage != null)
                        emitEffect(IndexUiEffect.ShowSnackbar(message = it.localizedMessage!!, duration = SnackbarDuration.Long))
                }
            }

            is IndexUiIntent.Navigate -> {
                val entity = intent.entity
                val context = intent.context
                val navController = intent.navController
                val route = intent.route
                if (entity.remote.isEmpty()) {
                    emitEffect(IndexUiEffect.ShowSnackbar(message = cloudRepository.getString(R.string.remote_not_set)))
                } else {
                    context.saveCloudActivatedAccountName(entity.name)
                    withMainContext {
                        navController.navigate(route = route)
                    }
                }
            }
        }
    }

    private val _accounts: Flow<List<CloudEntity>> = cloudRepository.clouds.flowOnIO()
    val accounts: StateFlow<List<CloudEntity>> = _accounts.stateInScope(listOf())
}
