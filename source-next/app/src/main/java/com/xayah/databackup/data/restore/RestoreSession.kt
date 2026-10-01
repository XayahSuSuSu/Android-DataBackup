package com.xayah.databackup.data.restore

import com.xayah.databackup.data.rustic.RusticSourceCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class RestoreSession(initialState: RestoreSessionState = RestoreSessionState()) {
    private val _state = MutableStateFlow(initialState)
    val state = _state.asStateFlow()
    private var mPendingRequest: RestoreRequest? = null

    internal fun prepareRestore() {
        mPendingRequest = null
        mPendingRequest = state.value.toRestoreRequest()
    }

    internal fun consumeRestoreRequest(): RestoreRequest? = mPendingRequest.also { mPendingRequest = null }

    internal fun updateState(state: RestoreSessionState) {
        _state.value = state
    }

    fun selectCategory(category: RestoreCategory, checked: Boolean) {
        _state.update { it.selectCategory(category, checked) }
    }

    fun selectItem(id: String, checked: Boolean) {
        _state.update { it.selectItem(id, checked) }
    }

    fun selectItems(ids: Set<String>, checked: Boolean) {
        _state.update { it.selectItems(ids, checked) }
    }

    fun selectAppPart(id: String, part: RusticSourceCategory, checked: Boolean) {
        _state.update { it.selectAppPart(id, part, checked) }
    }

    fun selectAppParts(ids: Set<String>, parts: Set<RusticSourceCategory>, checked: Boolean) {
        _state.update { it.selectAppParts(ids, parts, checked) }
    }
}
