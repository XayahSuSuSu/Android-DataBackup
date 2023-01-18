package com.xayah.databackup.fragment.guide

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xayah.databackup.util.GlobalString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class GuideUpdateViewModel : ViewModel() {
    val subtitle by lazy {
        MutableStateFlow(GlobalString.fetching)
    }
    val content by lazy {
        MutableStateFlow(GlobalString.fetching)
    }

    /**
     * 切换至ViewModelScope协程运行
     */
    fun <T> runOnScope(block: suspend () -> T) {
        viewModelScope.launch { block() }
    }
}