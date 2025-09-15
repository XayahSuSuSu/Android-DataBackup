package com.xayah.feature.main.verify

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.xayah.core.ui.component.Title
import com.xayah.core.ui.token.SizeTokens
import com.xayah.app.R // Changed to use R from app module

@ExperimentalMaterial3Api
@Composable
fun VerifyBackupPage(
    storageMode: String,
    cloudName: String?,
    backupDir: String,
    viewModel: VerifyBackupViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val verificationStatus by viewModel.verificationStatus.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.startVerification(context, storageMode, cloudName, backupDir)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(id = R.string.verify_backup_title)) })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(SizeTokens.Level16)
        ) {
            if (verificationStatus.isVerifying) {
                Text(stringResource(id = R.string.verifying_backup_in_progress))
                // Optionally, show a progress indicator
            } else {
                Title(title = stringResource(id = R.string.verification_results))
                LazyColumn {
                    items(verificationStatus.results.entries.toList()) { (file, status) ->
                        Text("$file: $status")
                    }
                }
                if (verificationStatus.overallResult) {
                    Text(stringResource(id = R.string.verification_success))
                } else {
                    Text(stringResource(id = R.string.verification_failed))
                }
            }
        }
    }
}
