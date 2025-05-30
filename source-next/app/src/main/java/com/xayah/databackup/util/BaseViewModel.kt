package com.xayah.databackup.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

abstract class BaseViewModel : ViewModel() {
    private val mMutex = Mutex()

    fun withLock(context: CoroutineContext, block: suspend () -> Unit) {
        viewModelScope.launch {
            withContext(context) {
                mMutex.withLock {
                    block.invoke()
                }
            }
        }
    }
}
