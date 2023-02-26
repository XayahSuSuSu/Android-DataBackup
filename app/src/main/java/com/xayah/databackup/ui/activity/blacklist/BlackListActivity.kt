package com.xayah.databackup.ui.activity.blacklist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.xayah.databackup.ui.activity.blacklist.components.BlackListScaffold
import com.xayah.databackup.ui.theme.DataBackupTheme
import com.xayah.materialyoufileexplorer.MaterialYouFileExplorer

@ExperimentalMaterial3Api
class BlackListActivity : ComponentActivity() {
    private lateinit var explorer: MaterialYouFileExplorer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val viewModel = ViewModelProvider(this)[BlackListViewModel::class.java]
        explorer = MaterialYouFileExplorer().apply {
            initialize(this@BlackListActivity)
        }

        setContent {
            DataBackupTheme {
                BlackListScaffold(viewModel, explorer) { finish() }
            }
        }
    }
}

