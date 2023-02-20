package com.xayah.databackup.compose.ui.activity.main

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class MainViewModel : ViewModel() {
    val isRemoteFileInitialized = MutableStateFlow(false)
}