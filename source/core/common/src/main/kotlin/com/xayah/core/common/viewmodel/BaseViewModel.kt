package com.xayah.core.common.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

interface UiState
interface UiIntent
interface UiEffect

interface IBaseViewModel<S : UiState, I : UiIntent, E : UiEffect> {

    suspend fun onEvent(state: S, intent: I)
}

abstract class BaseViewModel<S : UiState, I : UiIntent, E : UiEffect>(state: S) : IBaseViewModel<S, I, E>, ViewModel() {
    private val intentChannel = Channel<I>(Channel.UNLIMITED)

    private val _uiState = MutableStateFlow(state)
    val uiState: StateFlow<S> = _uiState.asStateFlow()

    init {
        withIOContext {
            intentChannel.consumeAsFlow().distinctUntilChanged().collect {
                onEvent(state = uiState.value, intent = it)
            }
        }
    }

    fun emitState(state: S) = withMainContext {
        _uiState.emit(state)
    }

    fun emitIntent(intent: I) = withIOContext {
        intentChannel.send(intent)
    }

    fun withIOContext(
        block: suspend CoroutineScope.() -> Unit,
    ) {
        viewModelScope.launch(context = Dispatchers.IO, block = block)
    }

    fun withMainContext(
        block: suspend CoroutineScope.() -> Unit,
    ) {
        viewModelScope.launch(context = Dispatchers.Main, block = block)
    }
}