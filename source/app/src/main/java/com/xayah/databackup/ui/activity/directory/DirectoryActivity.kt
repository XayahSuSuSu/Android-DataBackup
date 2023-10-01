package com.xayah.databackup.ui.activity.directory

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.view.WindowCompat
import com.xayah.databackup.ui.activity.directory.router.DirectoryNavHost
import com.xayah.databackup.ui.activity.directory.router.DirectoryRoutes
import com.xayah.databackup.ui.component.LocalSlotScope
import com.xayah.databackup.ui.component.rememberSlotScope
import com.xayah.databackup.ui.theme.DataBackupTheme
import com.xayah.databackup.util.IntentUtil
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DirectoryActivity : ComponentActivity() {
    @ExperimentalFoundationApi
    @ExperimentalMaterial3Api
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val route = intent.getStringExtra(IntentUtil.ExtraRoute) ?: DirectoryRoutes.DirectoryBackup.route
        setContent {
            DataBackupTheme {
                val slotScope = rememberSlotScope()

                CompositionLocalProvider(LocalSlotScope provides slotScope) {
                    DirectoryNavHost(route)
                }
            }
        }
    }
}
