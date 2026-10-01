package com.xayah.databackup.feature.restore

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.xayah.databackup.data.restore.RestoreSession
import com.xayah.databackup.feature.RestoreAppsRoute
import com.xayah.databackup.feature.RestoreCallLogsRoute
import com.xayah.databackup.feature.RestoreContactsRoute
import com.xayah.databackup.feature.RestoreMessagesRoute
import com.xayah.databackup.feature.RestoreNetworksRoute
import com.xayah.databackup.feature.RestoreProcessRoute
import com.xayah.databackup.feature.RestoreSetupRoute
import com.xayah.databackup.feature.restore.apps.AppsViewModel
import com.xayah.databackup.feature.restore.apps.RestoreAppsScreen
import com.xayah.databackup.feature.restore.call_logs.CallLogsViewModel
import com.xayah.databackup.feature.restore.call_logs.RestoreCallLogsScreen
import com.xayah.databackup.feature.restore.contacts.ContactsViewModel
import com.xayah.databackup.feature.restore.contacts.RestoreContactsScreen
import com.xayah.databackup.feature.restore.messages.MessagesViewModel
import com.xayah.databackup.feature.restore.messages.RestoreMessagesScreen
import com.xayah.databackup.feature.restore.networks.NetworksViewModel
import com.xayah.databackup.feature.restore.networks.RestoreNetworksScreen
import com.xayah.databackup.ui.component.backwardNavigationTransition
import com.xayah.databackup.ui.component.forwardNavigationTransition
import com.xayah.databackup.util.Navigator
import com.xayah.databackup.util.popBackStackSafely
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun RestoreNavigationHost(navigator: Navigator, viewModel: RestoreViewModel) {
    RestoreNavigationContent(viewModel.session, onBack = navigator::popBackStackSafely, onRetry = viewModel::load)
}

@Composable
internal fun RestoreNavigationContent(
    session: RestoreSession,
    onBack: () -> Unit,
    onRetry: () -> Unit = {},
    initialRoute: NavKey = RestoreSetupRoute,
) {
    val backStack = rememberNavBackStack(initialRoute)
    val navigator = remember(backStack) { Navigator(backStack) }
    NavDisplay(
        backStack = backStack,
        onBack = navigator::goBack,
        entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator(), rememberViewModelStoreNavEntryDecorator()),
        transitionSpec = { forwardNavigationTransition() },
        popTransitionSpec = { backwardNavigationTransition() },
        predictivePopTransitionSpec = { backwardNavigationTransition() },
        entryProvider = entryProvider {
            entry<RestoreProcessRoute> {
                val viewModel = koinViewModel<RestoreProcessViewModel> { parametersOf(session) }
                RestoreProcessScreen(viewModel, onFinish = onBack)
            }
            entry<RestoreSetupRoute> {
                val viewModel = koinViewModel<RestoreSetupViewModel> { parametersOf(session) }
                RestoreSetupScreen(navigator, viewModel, onBack, onRetry)
            }
            entry<RestoreAppsRoute> {
                val viewModel = koinViewModel<AppsViewModel> { parametersOf(session) }
                RestoreAppsScreen(navigator, viewModel)
            }
            entry<RestoreNetworksRoute> {
                val viewModel = koinViewModel<NetworksViewModel> { parametersOf(session) }
                RestoreNetworksScreen(navigator, viewModel)
            }
            entry<RestoreContactsRoute> {
                val viewModel = koinViewModel<ContactsViewModel> { parametersOf(session) }
                RestoreContactsScreen(navigator, viewModel)
            }
            entry<RestoreCallLogsRoute> {
                val viewModel = koinViewModel<CallLogsViewModel> { parametersOf(session) }
                RestoreCallLogsScreen(navigator, viewModel)
            }
            entry<RestoreMessagesRoute> {
                val viewModel = koinViewModel<MessagesViewModel> { parametersOf(session) }
                RestoreMessagesScreen(navigator, viewModel)
            }
        },
    )
}
