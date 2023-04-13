package com.xayah.databackup.ui.activity.guide

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.xayah.databackup.App
import com.xayah.databackup.data.GuideType
import com.xayah.databackup.ui.activity.guide.components.PageEnvironment
import com.xayah.databackup.ui.activity.guide.components.PageIntroduction
import com.xayah.databackup.ui.activity.guide.components.PageUpdate
import com.xayah.databackup.ui.activity.main.MainActivity
import com.xayah.databackup.ui.theme.DataBackupTheme
import com.xayah.databackup.util.checkPackageUsageStatsPermission
import com.xayah.databackup.util.readInitializedVersionName
import com.xayah.databackup.util.readIsSupportUsageAccess
import com.xayah.databackup.util.saveInitializedVersionName

@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
class GuideActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val viewModel = ViewModelProvider(this)[GuideViewModel::class.java]

        if (readInitializedVersionName().isNotEmpty()) {
            if (readInitializedVersionName() != App.versionName) {
                // 版本更新
                viewModel.initType.value = GuideType.Update
            } else if (checkPackageUsageStatsPermission().not() && readIsSupportUsageAccess()) {
                // 权限不够
                viewModel.initType.value = GuideType.Env
            }
        }
        setContent {
            DataBackupTheme(
                content = {
                    Crossfade(targetState = viewModel.initType.value) { screen ->
                        when (screen) {
                            GuideType.Introduction -> {
                                PageIntroduction(viewModel)
                            }
                            GuideType.Update -> {
                                PageUpdate(viewModel)
                            }
                            GuideType.Env -> {
                                PageEnvironment(viewModel) {
                                    App.globalContext.saveInitializedVersionName(App.versionName)
                                    startActivity(Intent(this, MainActivity::class.java))
                                    finish()
                                }
                            }
                        }
                    }
                })
        }
    }
}

