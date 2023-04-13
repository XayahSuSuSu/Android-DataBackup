package com.xayah.databackup.ui.activity.list.blacklist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.xayah.databackup.ui.activity.list.blacklist.components.BlackListScaffold
import com.xayah.databackup.ui.theme.DataBackupTheme

@ExperimentalMaterial3Api
class BlackListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val viewModel = ViewModelProvider(this)[BlackListViewModel::class.java]
        viewModel.initializeExplorer(this)

        setContent {
            DataBackupTheme(
                content = {
                    BlackListScaffold(viewModel) { finish() }
                },
                onRootServiceInitialized = {
                    viewModel.initializeList(this)
                })
        }
    }
}

