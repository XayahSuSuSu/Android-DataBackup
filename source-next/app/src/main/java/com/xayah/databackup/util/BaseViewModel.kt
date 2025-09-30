package com.xayah.databackup.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

abstract class BaseViewModel : ViewModel() {
    private val mMutex = Mutex()

    fun withLock(context: CoroutineContext = EmptyCoroutineContext, block: suspend () -> Unit) {
        viewModelScope.launch(context) {
            mMutex.withLock {
                block.invoke()
            }
        }
    }
}
