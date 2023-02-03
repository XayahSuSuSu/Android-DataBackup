package com.xayah.databackup.compose.ui.activity.guide

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import com.xayah.databackup.App
import com.xayah.databackup.activity.main.MainActivity
import com.xayah.databackup.compose.ui.activity.guide.components.Env
import com.xayah.databackup.compose.ui.activity.guide.components.Introduction
import com.xayah.databackup.compose.ui.activity.guide.components.Update
import com.xayah.databackup.compose.ui.theme.DataBackupTheme
import com.xayah.databackup.data.GuideType
import com.xayah.databackup.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@ExperimentalMaterial3Api
class GuideActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        var initType = GuideType.Introduction
        if (readInitializedVersionName().isNotEmpty()) {
            if (readInitializedVersionName() != App.versionName) {
                // 版本更新
                initType = GuideType.Update
            } else if (checkPackageUsageStatsPermission().not() && readIsSupportUsageAccess()) {
                // 权限不够
                initType = GuideType.Env
            }
        }
        setContent {
            DataBackupTheme {
                val (type, setType) = remember { mutableStateOf(initType) }
                Crossfade(targetState = type) { screen ->
                    when (screen) {
                        GuideType.Introduction -> {
                            Introduction(setType)
                        }
                        GuideType.Update -> {
                            Update(setType)
                        }
                        GuideType.Env -> {
                            Env {
                                App.globalContext.saveInitializedVersionName(App.versionName)
                                CoroutineScope(Dispatchers.IO).launch {
                                    ExtendCommand.rcloneUnmountAll()
                                }
                                startActivity(Intent(this, MainActivity::class.java))
                                finish()
                            }
                        }
                    }
                }

            }
        }
    }
}

