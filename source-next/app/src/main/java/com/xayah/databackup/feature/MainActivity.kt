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
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.topjohnwu.superuser.Shell
import com.xayah.databackup.App
import com.xayah.databackup.R
import com.xayah.databackup.feature.backup.BackupConfigScreen
import com.xayah.databackup.feature.backup.BackupConfigViewModel
import com.xayah.databackup.feature.backup.BackupProcessDetailsScreen
import com.xayah.databackup.feature.backup.BackupProcessScreen
import com.xayah.databackup.feature.backup.BackupSetupScreen
import com.xayah.databackup.feature.backup.apps.BackupAppsScreen
import com.xayah.databackup.feature.backup.call_logs.BackupCallLogsScreen
import com.xayah.databackup.feature.backup.contacts.BackupContactsScreen
import com.xayah.databackup.feature.backup.messages.BackupMessagesScreen
import com.xayah.databackup.feature.backup.networks.BackupNetworksScreen
import com.xayah.databackup.feature.backup.rustic.RusticBackupProcessScreen
import com.xayah.databackup.feature.dashboard.DashboardScreen
import com.xayah.databackup.feature.settings.SettingsScreen
import com.xayah.databackup.feature.setup.NoPermKey
import com.xayah.databackup.feature.setup.SetupActivity
import com.xayah.databackup.feature.update.UpdatesScreen
import com.xayah.databackup.ui.component.DataBackupDialog
import com.xayah.databackup.ui.component.DialogDestructiveButton
import com.xayah.databackup.ui.component.DialogDismissButton
import com.xayah.databackup.ui.component.DialogIcon
import com.xayah.databackup.ui.theme.DataBackupTheme
import com.xayah.databackup.util.FirstLaunch
import com.xayah.databackup.util.LogHelper
import com.xayah.databackup.util.NotificationHelper
import com.xayah.databackup.util.Navigator
import com.xayah.databackup.util.ProcessHelper
import com.xayah.databackup.util.ShellHelper
import com.xayah.databackup.util.preloadingDataStore
import com.xayah.databackup.util.readBoolean
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ErrorServiceDialog(onConfirm: () -> Unit, onRetry: () -> Unit) {
    DataBackupDialog(
        title = stringResource(R.string.error),
        onDismissRequest = {},
        icon = { DialogIcon(imageVector = ImageVector.vectorResource(R.drawable.ic_circle_x)) },
        iconContainerColor = MaterialTheme.colorScheme.errorContainer,
        iconContentColor = MaterialTheme.colorScheme.onErrorContainer,
        content = { Text(text = stringResource(R.string.error_service_desc)) },
        confirmButton = {
            DialogDestructiveButton(text = stringResource(R.string.confirm), onClick = onConfirm)
        },
        dismissButton = {
            DialogDismissButton(text = stringResource(R.string.retry), onClick = onRetry)
        },
    )
}

@Composable
fun NoSpaceLeftDialog(onDismissRequest: () -> Unit) {
    DataBackupDialog(
        title = stringResource(R.string.error),
        onDismissRequest = onDismissRequest,
        icon = { DialogIcon(imageVector = ImageVector.vectorResource(R.drawable.ic_circle_x)) },
        iconContainerColor = MaterialTheme.colorScheme.errorContainer,
        iconContentColor = MaterialTheme.colorScheme.onErrorContainer,
        content = { Text(text = stringResource(R.string.error_no_space_left_desc)) },
        confirmButton = {
            DialogDestructiveButton(text = stringResource(R.string.confirm), onClick = onDismissRequest)
        },
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
                val backStack = rememberNavBackStack(DashboardRoute)
                val navigator = remember(backStack) { Navigator(backStack) }

                if (uiState.showErrorServiceDialog) {
                    ErrorServiceDialog(onConfirm = { ProcessHelper.killSelf(this) }, onRetry = { mMainViewModel.checkRootService() })
                }

                if (uiState.showNoSpaceLeftDialog) {
                    NoSpaceLeftDialog {
                        mMainViewModel.dismissNoSpaceLeftDialog()
                    }
                }

                Surface {
                    NavDisplay(
                        backStack = backStack,
                        onBack = navigator::goBack,
                        entryDecorators = listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                            rememberViewModelStoreNavEntryDecorator(),
                        ),
                        transitionSpec = {
                            slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow)
                            ) togetherWith slideOutHorizontally(
                                targetOffsetX = { -it },
                                animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow)
                            )
                        },
                        popTransitionSpec = {
                            slideInHorizontally(
                                initialOffsetX = { -it },
                                animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow)
                            ) togetherWith slideOutHorizontally(
                                targetOffsetX = { it },
                                animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow)
                            )
                        },
                        predictivePopTransitionSpec = {
                            slideInHorizontally(
                                initialOffsetX = { -it },
                                animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow)
                            ) togetherWith slideOutHorizontally(
                                targetOffsetX = { it },
                                animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow)
                            )
                        },
                        entryProvider = entryProvider {
                            entry<DashboardRoute> {
                                DashboardScreen(navigator)
                            }

                            entry<SettingsRoute> {
                                SettingsScreen(navigator)
                            }

                            entry<UpdatesRoute> {
                                UpdatesScreen(navigator)
                            }

                            entry<BackupSetupRoute> {
                                BackupSetupScreen(navigator)
                            }

                            entry<BackupProcessRoute> {
                                BackupProcessScreen(navigator)
                            }

                            entry<RusticBackupProcessRoute> {
                                RusticBackupProcessScreen(navigator)
                            }

                            entry<BackupProcessDetailsRoute> {
                                BackupProcessDetailsScreen(navigator)
                            }

                            entry<BackupConfigRoute> { route ->
                                val viewModel = koinViewModel<BackupConfigViewModel> {
                                    parametersOf(route)
                                }
                                BackupConfigScreen(navigator, viewModel)
                            }

                            entry<BackupAppsRoute> {
                                BackupAppsScreen(navigator)
                            }

                            entry<BackupNetworksRoute> {
                                BackupNetworksScreen(navigator)
                            }

                            entry<BackupContactsRoute> {
                                BackupContactsScreen(navigator)
                            }

                            entry<BackupCallLogsRoute> {
                                BackupCallLogsScreen(navigator)
                            }

                            entry<BackupMessagesRoute> {
                                BackupMessagesScreen(navigator)
                            }
                        }
                    )
                }
            }
        }
    }
}
