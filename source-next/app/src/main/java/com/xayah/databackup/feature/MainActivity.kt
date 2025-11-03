package com.xayah.databackup.feature

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.topjohnwu.superuser.Shell
import com.xayah.databackup.App
import com.xayah.databackup.R
import com.xayah.databackup.feature.backup.BackupProcessScreen
import com.xayah.databackup.feature.backup.BackupSetupScreen
import com.xayah.databackup.feature.backup.apps.BackupAppsScreen
import com.xayah.databackup.feature.backup.call_logs.BackupCallLogsScreen
import com.xayah.databackup.feature.backup.contacts.BackupContactsScreen
import com.xayah.databackup.feature.backup.messages.BackupMessagesScreen
import com.xayah.databackup.feature.backup.networks.BackupNetworksScreen
import com.xayah.databackup.feature.dashboard.DashboardScreen
import com.xayah.databackup.feature.setup.NoPermKey
import com.xayah.databackup.feature.setup.SetupActivity
import com.xayah.databackup.ui.theme.DataBackupTheme
import com.xayah.databackup.util.FirstLaunch
import com.xayah.databackup.util.LogHelper
import com.xayah.databackup.util.NotificationHelper
import com.xayah.databackup.util.ProcessHelper
import com.xayah.databackup.util.ShellHelper
import com.xayah.databackup.util.preloadingDataStore
import com.xayah.databackup.util.readBoolean
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable

@Serializable
data object Dashboard

@Serializable
data object Backup

@Serializable
data object BackupSetup

@Serializable
data object BackupProcess

@Serializable
data object BackupApps

@Serializable
data object BackupNetworks

@Serializable
data object BackupContacts

@Serializable
data object BackupCallLogs

@Serializable
data object BackupMessages

@Composable
fun ErrorServiceDialog(onConfirm: () -> Unit, onRetry: () -> Unit) {
    AlertDialog(
        icon = { Icon(imageVector = ImageVector.vectorResource(R.drawable.ic_circle_x), contentDescription = stringResource(R.string.error)) },
        title = { Text(text = stringResource(R.string.error)) },
        text = { Text(text = stringResource(R.string.error_service_desc)) },
        onDismissRequest = {},
        confirmButton = { TextButton(onClick = onConfirm) { Text(text = stringResource(R.string.confirm)) } },
        dismissButton = { TextButton(onClick = onRetry) { Text(text = stringResource(R.string.retry)) } }
    )
}

@Composable
fun NoSpaceLeftDialog(onDismissRequest: () -> Unit) {
    AlertDialog(
        icon = { Icon(imageVector = ImageVector.vectorResource(R.drawable.ic_circle_x), contentDescription = stringResource(R.string.error)) },
        title = { Text(text = stringResource(R.string.error)) },
        text = { Text(text = stringResource(R.string.error_no_space_left_desc)) },
        onDismissRequest = onDismissRequest,
        confirmButton = { TextButton(onClick = onDismissRequest) { Text(text = stringResource(R.string.confirm)) } },
    )
}

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }
    private lateinit var mMainViewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            // Asynchronously preloading the data from DataStore
            preloadingDataStore()
        }

        splashScreen.setKeepOnScreenCondition { true }
        if (runBlocking { readBoolean(FirstLaunch).first() }) {
            // First launch
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
        } else {
            runCatching {
                runBlocking { ShellHelper.initMainShell(context = App.application) }
            }.onFailure { LogHelper.e(TAG, "onCreate", "Failed to init main shell.", it) }
            val isRoot = runCatching {
                Shell.getShell().isRoot
            }.getOrNull() ?: false
            if (isRoot.not()) {
                // Permissions are denied
                startActivity(Intent(this, SetupActivity::class.java).putExtra(NoPermKey, true))
                finish()
            }
        }
        NotificationHelper.createChannelIfNecessary(this)
        splashScreen.setKeepOnScreenCondition { false }

        enableEdgeToEdge()
        setContent {
            mMainViewModel = viewModel()
            mMainViewModel.initialize()

            DataBackupTheme {
                val uiState by mMainViewModel.uiState.collectAsStateWithLifecycle()
                val navController = rememberNavController()

                if (uiState.showErrorServiceDialog) {
                    ErrorServiceDialog(onConfirm = { ProcessHelper.killSelf(this) }, onRetry = { mMainViewModel.checkRootService() })
                }

                if (uiState.showNoSpaceLeftDialog) {
                    NoSpaceLeftDialog {
                        mMainViewModel.dismissNoSpaceLeftDialog()
                    }
                }

                Surface {
                    NavHost(
                        navController = navController,
                        startDestination = Dashboard,
                        enterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow)
                            )
                        },
                        exitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { -it },
                                animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow)
                            )
                        },
                        popEnterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { -it },
                                animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow)
                            )
                        },
                        popExitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { it },
                                animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow)
                            )
                        },
                    ) {
                        composable<Dashboard> {
                            DashboardScreen(navController)
                        }

                        navigation<Backup>(startDestination = BackupSetup) {
                            composable<BackupSetup> {
                                BackupSetupScreen(navController)
                            }

                            composable<BackupProcess> {
                                BackupProcessScreen(navController)
                            }

                            composable<BackupApps> {
                                BackupAppsScreen(navController)
                            }

                            composable<BackupNetworks> {
                                BackupNetworksScreen(navController)
                            }

                            composable<BackupContacts> {
                                BackupContactsScreen(navController)
                            }

                            composable<BackupCallLogs> {
                                BackupCallLogsScreen(navController)
                            }

                            composable<BackupMessages> {
                                BackupMessagesScreen(navController)
                            }
                        }
                    }
                }
            }
        }
    }
}
