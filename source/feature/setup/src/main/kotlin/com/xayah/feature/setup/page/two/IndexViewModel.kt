package com.xayah.feature.setup.page.two

import android.app.Activity
import android.content.Intent
import androidx.compose.material3.ExperimentalMaterial3Api
import com.xayah.core.datastore.saveAppVersionName
import com.xayah.core.ui.viewmodel.BaseViewModel
import com.xayah.core.ui.viewmodel.IndexUiEffect
import com.xayah.core.ui.viewmodel.UiIntent
import com.xayah.core.ui.viewmodel.UiState
import com.xayah.core.util.ActivityUtil
import com.xayah.core.work.WorkManagerInitializer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data object IndexUiState : UiState

sealed class IndexUiIntent : UiIntent {
    data class ToMain(val context: Activity) : IndexUiIntent()
}

@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor() : BaseViewModel<IndexUiState, IndexUiIntent, IndexUiEffect>(IndexUiState) {
    override suspend fun onEvent(state: IndexUiState, intent: IndexUiIntent) {
        when (intent) {
            is IndexUiIntent.ToMain -> {
                val context = intent.context
                WorkManagerInitializer.fullInitialize(context)
                context.saveAppVersionName()
                context.startActivity(Intent(context, ActivityUtil.classMainActivity))
                context.finish()
            }
        }
    }
}
