package com.xayah.feature.main.medium.list

import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.navigation.NavHostController
import com.xayah.core.common.viewmodel.BaseViewModel
import com.xayah.core.common.viewmodel.IndexUiEffect
import com.xayah.core.common.viewmodel.UiIntent
import com.xayah.core.common.viewmodel.UiState
import com.xayah.core.data.repository.MediaRepository
import com.xayah.core.model.database.MediaEntity
import com.xayah.core.model.database.MediaEntityWithCount
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.model.TopBarState
import com.xayah.core.ui.route.MainRoutes
import com.xayah.core.ui.util.fromStringId
import com.xayah.feature.main.medium.R
import com.xayah.libpickyou.ui.PickYouLauncher
import com.xayah.libpickyou.ui.model.PickerType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

data class IndexUiState(
    val isRefreshing: Boolean,
) : UiState

sealed class IndexUiIntent : UiIntent {
    data object OnRefresh : IndexUiIntent()
    data class ToPageMediaDetail(
        val navController: NavHostController,
        val mediaEntity: MediaEntity
    ) : IndexUiIntent()

    data class FilterByKey(val key: String) : IndexUiIntent()

    data class AddMedia(val context: Context) : IndexUiIntent()
}

@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor(
    private val mediaRepo: MediaRepository,
    rootService: RemoteRootService,
) : BaseViewModel<IndexUiState, IndexUiIntent, IndexUiEffect>(IndexUiState(isRefreshing = false)) {
    init {
        rootService.onFailure = {
            val msg = it.message
            if (msg != null)
                emitEffect(IndexUiEffect.ShowSnackbar(message = msg))
        }
    }

    override suspend fun onEvent(state: IndexUiState, intent: IndexUiIntent) {
        when (intent) {
            is IndexUiIntent.OnRefresh -> {
                emitStateSuspend(state.copy(isRefreshing = true))
                mediaRepo.refresh(topBarState = _topBarState)
                emitStateSuspend(state.copy(isRefreshing = false))
            }

            is IndexUiIntent.ToPageMediaDetail -> {
                val entity = intent.mediaEntity
                withMainContext {
                    intent.navController.navigate(MainRoutes.MediumDetail.getRoute(entity.name))
                }
            }

            is IndexUiIntent.FilterByKey -> {
                _keyState.value = intent.key
            }

            is IndexUiIntent.AddMedia -> {
                withMainContext {
                    val context = intent.context
                    PickYouLauncher().apply {
                        setTitle(context.getString(R.string.select_target_directory))
                        setType(PickerType.DIRECTORY)
                        setLimitation(0)
                        launch(context) { pathList ->
                            launchOnIO {
                                emitEffect(IndexUiEffect.ShowSnackbar(mediaRepo.addMedia(pathList)))
                            }
                        }
                    }
                }
            }
        }
    }

    private val _topBarState: MutableStateFlow<TopBarState> = MutableStateFlow(TopBarState(title = StringResourceToken.fromStringId(R.string.media)))
    val topBarState: StateFlow<TopBarState> = _topBarState.asStateFlow()

    private val _medium: Flow<List<MediaEntityWithCount>> = mediaRepo.medium.flowOnIO()
    private var _keyState: MutableStateFlow<String> = MutableStateFlow("")
    private val _mediumState: Flow<List<MediaEntityWithCount>> = combine(_medium, _keyState) { packages, key ->
        packages.filter(mediaRepo.getKeyPredicate(key = key))
    }.flowOnIO()
    val mediumState: StateFlow<List<MediaEntityWithCount>> = _mediumState.stateInScope(listOf())
}
