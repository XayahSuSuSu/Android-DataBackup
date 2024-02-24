package com.xayah.feature.setup.page.two

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import com.xayah.core.common.viewmodel.BaseViewModel
import com.xayah.core.common.viewmodel.IndexUiEffect
import com.xayah.core.common.viewmodel.UiIntent
import com.xayah.core.common.viewmodel.UiState
import com.xayah.core.datastore.saveAppVersionName
import com.xayah.core.datastore.saveBackupSavePath
import com.xayah.core.util.ActivityUtil
import com.xayah.feature.setup.R
import com.xayah.libpickyou.ui.PickYouLauncher
import com.xayah.libpickyou.ui.model.PermissionType
import com.xayah.libpickyou.ui.model.PickerType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data object IndexUiState : UiState

sealed class IndexUiIntent : UiIntent {
    data class ToMain(val context: ComponentActivity) : IndexUiIntent()
    data class SelectBackupDir(val context: Context) : IndexUiIntent()
}

@ExperimentalMaterial3Api
@HiltViewModel
class IndexViewModel @Inject constructor() : BaseViewModel<IndexUiState, IndexUiIntent, IndexUiEffect>(IndexUiState) {
    override suspend fun onEvent(state: IndexUiState, intent: IndexUiIntent) {
        when (intent) {
            is IndexUiIntent.ToMain -> {
                val context = intent.context
                context.saveAppVersionName()
                context.startActivity(Intent(context, ActivityUtil.classMainActivity))
                context.finish()
            }

            is IndexUiIntent.SelectBackupDir -> {
                val context = intent.context
                PickYouLauncher.apply {
                    sTitle = context.getString(R.string.select_target_directory)
                    sPickerType = PickerType.DIRECTORY
                    sLimitation = 1
                    sPermissionType = PermissionType.ROOT
                }
                withMainContext {
                    val pathList = PickYouLauncher.awaitPickerOnce(context)
                    pathList.firstOrNull()?.also { pathString ->
                        context.saveBackupSavePath(pathString)
                    }
                }
            }
        }
    }
}
