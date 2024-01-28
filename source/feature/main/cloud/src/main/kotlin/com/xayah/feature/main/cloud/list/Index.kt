package com.xayah.feature.main.cloud.list

import androidx.activity.ComponentActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.xayah.core.ui.component.AddIconButton
import com.xayah.core.ui.component.LocalActionsState
import com.xayah.core.ui.component.RoundChip
import com.xayah.core.ui.component.paddingHorizontal
import com.xayah.core.ui.model.ActionMenuItem
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.model.getActionMenuConfirmItem
import com.xayah.core.ui.model.getActionMenuReturnItem
import com.xayah.core.ui.route.MainRoutes
import com.xayah.core.ui.token.PaddingTokens
import com.xayah.core.ui.util.fromString
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.fromVector
import com.xayah.core.ui.util.value
import com.xayah.feature.main.cloud.AccountCard
import com.xayah.feature.main.cloud.R

@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun PageCloud(navController: NavHostController, snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val viewModel = hiltViewModel<IndexViewModel>()
    val accounts by viewModel.accounts.collectAsState()
    val actions = LocalActionsState.current
    actions?.setActions {
        AddIconButton {
            navController.navigate(MainRoutes.CloudAccount.getRoute(" "))
        }
    }

    LaunchedEffect(null) {
        viewModel.snackbarHostState = snackbarHostState
    }

    LazyColumn(
        modifier = Modifier.paddingHorizontal(PaddingTokens.Level3),
        verticalArrangement = Arrangement.spacedBy(PaddingTokens.Level3)
    ) {
        item {
            Spacer(modifier = Modifier.height(PaddingTokens.Level0))
        }

        items(items = accounts, key = { it.name }) { item ->
            Row(Modifier.animateItemPlacement()) {

                AccountCard(
                    item = item,
                    selected = false,
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
                                navController.navigate(MainRoutes.CloudAccount.getRoute(item.name))
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
                    RoundChip(
                        text = StringResourceToken.fromString(item.type.toString()).value,
                        enabled = true
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(PaddingTokens.Level3))
        }
    }
}
