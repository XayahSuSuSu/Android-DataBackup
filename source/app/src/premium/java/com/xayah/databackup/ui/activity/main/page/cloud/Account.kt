package com.xayah.databackup.ui.activity.main.page.cloud

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.xayah.databackup.R
import com.xayah.databackup.ui.activity.main.page.cloud.router.CloudRoutes
import com.xayah.databackup.ui.component.AnimatedSerial
import com.xayah.databackup.ui.component.FabScaffold
import com.xayah.databackup.ui.component.ListItemCloudAccount
import com.xayah.databackup.ui.component.Loader
import com.xayah.databackup.ui.component.Serial
import com.xayah.databackup.ui.component.paddingBottom
import com.xayah.databackup.ui.component.paddingHorizontal
import com.xayah.databackup.ui.component.paddingTop
import com.xayah.databackup.ui.token.CommonTokens

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@Composable
fun PageAccount(navController: NavHostController) {
    val viewModel = hiltViewModel<AccountViewModel>()
    val uiState by viewModel.uiState
    val accounts = uiState.accounts

    LaunchedEffect(null) {
        viewModel.initialize()
    }
    FabScaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    navController.navigate(CloudRoutes.CreateAccount.route)
                },
                expanded = true,
                icon = { Icon(imageVector = Icons.Rounded.Add, contentDescription = null) },
                text = { Text(text = stringResource(id = R.string.add)) },
            )
        },
        floatingActionButtonPosition = FabPosition.Center
    ) {
        Loader(modifier = Modifier.fillMaxSize(), isLoading = uiState.isLoading) {
            LazyColumn(
                modifier = Modifier.paddingHorizontal(CommonTokens.PaddingMedium),
                verticalArrangement = Arrangement.spacedBy(CommonTokens.PaddingMedium)
            ) {
                item {
                    Spacer(modifier = Modifier.paddingTop(CommonTokens.PaddingMedium))
                }

                items(items = accounts) { item ->
                    ListItemCloudAccount(
                        account = item,
                        onCardClick = {},
                        chipGroup = {
                            if (item.config.type.isNotEmpty()) Serial(serial = item.config.type)
                            if (item.config.vendor.isNotEmpty()) Serial(serial = item.config.vendor)
                            AnimatedSerial(serial = item.config.sizeDisplay)
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.paddingBottom(CommonTokens.PaddingMedium))
                }
            }
        }
    }
}
