package com.xayah.core.common.viewmodel

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface UiState
interface UiIntent
interface UiEffect

interface IBaseViewModel<S : UiState, I : UiIntent, E : UiEffect> {

    suspend fun onEvent(state: S, intent: I)
    suspend fun onSuspendEvent(state: S, intent: I)
    suspend fun onEffect(effect: E)
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

abstract class BaseViewModel<S : UiState, I : UiIntent, E : IndexUiEffect>(state: S) : IBaseViewModel<S, I, IndexUiEffect>, ViewModel() {
    private val intentChannel = Channel<I>(Channel.UNLIMITED)
    private val effectChannel = Channel<E>(Channel.UNLIMITED)

    private val _uiState = MutableStateFlow(state)
    val uiState: StateFlow<S> = _uiState.asStateFlow()
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

    override suspend fun onSuspendEvent(state: S, intent: I) {}

    init {
        launchOnIO {
            intentChannel.consumeAsFlow().collect {
                launchOnIO {
                    onEvent(state = uiState.value, intent = it)
                }
            }
        }
        launchOnIO {
            effectChannel.consumeAsFlow().collect {
                launchOnIO {
                    onEffect(effect = it)
                }
            }
        }
    }

    fun emitState(state: S) = launchOnMain {
        _uiState.emit(state)
    }

    fun emitIntent(intent: I) = launchOnIO {
        intentChannel.send(intent)
    }

    fun emitEffect(effect: E) = launchOnIO {
        effectChannel.send(effect)
    }

    suspend fun emitStateSuspend(state: S) = withMainContext {
        _uiState.emit(state)
    }

    suspend fun emitIntentSuspend(intent: I) = withIOContext {
        intentChannel.send(intent)
    }

    suspend fun emitEffectSuspend(effect: E) = withIOContext {
        effectChannel.send(effect)
    }

    suspend fun suspendEmitIntent(intent: I) = withIOContext {
        onSuspendEvent(state = uiState.value, intent = intent)
    }

    suspend fun withIOContext(block: suspend CoroutineScope.() -> Unit) = withContext(Dispatchers.IO, block = block)

    suspend fun withMainContext(block: suspend CoroutineScope.() -> Unit) = withContext(Dispatchers.Main, block = block)

    fun launchOnIO(block: suspend CoroutineScope.() -> Unit) = viewModelScope.launch(context = Dispatchers.IO, block = block)

    fun launchOnMain(block: suspend CoroutineScope.() -> Unit) = viewModelScope.launch(context = Dispatchers.Main, block = block)

    fun <T> Flow<T>.stateInScope(initialValue: T): StateFlow<T> = stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = initialValue
    )

    fun <T> Flow<T>.flowOnIO(): Flow<T> = flowOn(Dispatchers.IO)

    @ExperimentalCoroutinesApi
    fun <R> flatMapLatestUiState(transform: suspend (value: S) -> Flow<R>) = _uiState.flatMapLatest(transform)
}
