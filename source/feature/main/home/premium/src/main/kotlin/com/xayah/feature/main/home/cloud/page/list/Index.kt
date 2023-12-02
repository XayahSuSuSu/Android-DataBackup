package com.xayah.feature.main.home.cloud.page.list

import android.annotation.SuppressLint
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.xayah.core.datastore.readRcloneMainAccountName
import com.xayah.core.ui.component.ExtendedFab
import com.xayah.core.ui.component.RoundChip
import com.xayah.core.ui.component.paddingHorizontal
import com.xayah.core.ui.model.ActionMenuItem
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.model.getActionMenuConfirmItem
import com.xayah.core.ui.model.getActionMenuReturnItem
import com.xayah.core.ui.route.MainRoutes
import com.xayah.core.ui.token.AnimationTokens
import com.xayah.core.ui.token.PaddingTokens
import com.xayah.core.ui.util.LocalNavController
import com.xayah.core.ui.util.fromDrawable
import com.xayah.core.ui.util.fromString
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.fromVector
import com.xayah.core.ui.util.value
import com.xayah.feature.main.home.HomeRoutes
import com.xayah.feature.main.home.cloud.AccountCard
import com.xayah.feature.main.home.cloud.AccountCardShimmer
import com.xayah.feature.main.home.cloud.getActionMenuMediumItem
import com.xayah.feature.main.home.cloud.getActionMenuPackagesItem
import com.xayah.feature.main.home.cloud.getActionMenuTelephonyItem
import com.xayah.feature.main.home.premium.R

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun PageCloudList(navController: NavHostController) {
    val context = LocalContext.current
    val globalNavController = LocalNavController.current!!
    val viewModel = hiltViewModel<IndexViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val accounts by viewModel.accounts.collectAsState()

    LaunchedEffect(null) {
        viewModel.emitIntent(IndexUiIntent.Update)
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFab(
                expanded = true,
                icon = ImageVectorToken.fromVector(Icons.Rounded.Add),
                text = StringResourceToken.fromStringId(R.string.add),
                onClick = {
                    navController.navigate(HomeRoutes.CloudAccount.getRoute(" "))
                }
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
        snackbarHost = { SnackbarHost(hostState = viewModel.snackbarHostState) },
    ) { _ ->
        Column {
            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    modifier = Modifier.paddingHorizontal(PaddingTokens.Level3),
                    targetState = uiState.updating,
                    label = AnimationTokens.AnimatedContentLabel
                ) { targetState ->
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(PaddingTokens.Level3)) {
                        item {
                            Spacer(modifier = Modifier.height(PaddingTokens.Level0))
                        }

                        if (targetState) {
                            items(count = 9) { _ ->
                                Row(Modifier.animateItemPlacement()) {
                                    AccountCardShimmer()
                                }
                            }
                        } else {
                            items(items = accounts, key = { it.name }) { item ->
                                Row(Modifier.animateItemPlacement()) {
                                    val mainAccountName by context.readRcloneMainAccountName().collectAsStateWithLifecycle(initialValue = "")

                                    AccountCard(item = item,
                                        selected = item.name == mainAccountName,
                                        actions = listOf(
                                            ActionMenuItem(
                                                title = StringResourceToken.fromStringId(R.string.backup),
                                                icon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_acute),
                                                enabled = true,
                                                secondaryMenu = listOf(
                                                    getActionMenuReturnItem(),
                                                    getActionMenuPackagesItem {
                                                        viewModel.emitIntent(
                                                            IndexUiIntent.Navigate(
                                                                context = context,
                                                                entity = item,
                                                                navController = globalNavController,
                                                                route = MainRoutes.TaskPackagesCloud.routeBackup
                                                            )
                                                        )
                                                    },
                                                    getActionMenuMediumItem {},
                                                    getActionMenuTelephonyItem {},
                                                ),
                                                dismissOnClick = true,
                                                onClick = {}
                                            ),
                                            ActionMenuItem(
                                                title = StringResourceToken.fromStringId(R.string.restore),
                                                icon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_history),
                                                enabled = true,
                                                secondaryMenu = listOf(
                                                    getActionMenuReturnItem(),
                                                    getActionMenuPackagesItem {
                                                        viewModel.emitIntent(
                                                            IndexUiIntent.Navigate(
                                                                context = context,
                                                                entity = item,
                                                                navController = globalNavController,
                                                                route = MainRoutes.TaskPackagesCloud.routeRestore
                                                            )
                                                        )
                                                    },
                                                    getActionMenuMediumItem {},
                                                    getActionMenuTelephonyItem {},
                                                ),
                                                dismissOnClick = true,
                                                onClick = {}
                                            ),
                                        ),
                                        accountActions = listOf(
                                            ActionMenuItem(
                                                title = StringResourceToken.fromStringId(R.string.test_connection),
                                                icon = ImageVectorToken.fromVector(Icons.Rounded.Sensors),
                                                enabled = true,
                                                secondaryMenu = listOf(),
                                                dismissOnClick = true,
                                                onClick = {
                                                    viewModel.emitIntent(IndexUiIntent.TestConnection(entity = item))
                                                }
                                            ),
                                            ActionMenuItem(
                                                title = StringResourceToken.fromStringId(R.string.set_remote),
                                                icon = ImageVectorToken.fromVector(Icons.Rounded.Cloud),
                                                enabled = true,
                                                secondaryMenu = listOf(),
                                                dismissOnClick = true,
                                                onClick = {
                                                    viewModel.emitIntent(IndexUiIntent.SetRemote(context = context as ComponentActivity, entity = item))
                                                }
                                            ),
                                            ActionMenuItem(
                                                title = StringResourceToken.fromStringId(R.string.edit),
                                                icon = ImageVectorToken.fromVector(Icons.Rounded.Edit),
                                                enabled = true,
                                                secondaryMenu = listOf(),
                                                dismissOnClick = true,
                                                onClick = {
                                                    navController.navigate(HomeRoutes.CloudAccount.getRoute(item.name))
                                                }
                                            ),
                                            ActionMenuItem(
                                                title = StringResourceToken.fromStringId(R.string.delete),
                                                icon = ImageVectorToken.fromVector(Icons.Rounded.Delete),
                                                enabled = true,
                                                secondaryMenu = listOf(
                                                    getActionMenuReturnItem(),
                                                    getActionMenuConfirmItem {
                                                        viewModel.emitIntent(IndexUiIntent.Delete(entity = item))
                                                    }
                                                ),
                                                onClick = {}
                                            )
                                        ),
                                        onCardClick = {
                                        }
                                    ) {
                                        if (item.name == mainAccountName) RoundChip(
                                            text = StringResourceToken.fromStringId(R.string.main_account).value,
                                            enabled = true
                                        )
                                        if (item.account.type.isNotEmpty()) RoundChip(
                                            text = StringResourceToken.fromString(item.account.type).value,
                                            enabled = true
                                        )
                                        if (item.account.vendor.isNotEmpty()) RoundChip(
                                            text = StringResourceToken.fromString(item.account.vendor).value,
                                            enabled = true
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(PaddingTokens.Level3))
                        }
                    }
                }
            }
        }
    }
}
