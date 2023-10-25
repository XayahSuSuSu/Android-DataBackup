package com.xayah.databackup.ui.activity.main.page.cloud

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.navigation.NavHostController
import com.xayah.databackup.R
import com.xayah.databackup.ui.activity.main.page.cloud.router.CloudRoutes
import com.xayah.databackup.ui.activity.main.page.cloud.router.ReloadArg
import com.xayah.databackup.ui.activity.operation.router.OperationRoutes
import com.xayah.databackup.ui.component.CardActionButton
import com.xayah.databackup.ui.component.Module
import com.xayah.databackup.ui.component.OverLookCloudCard
import com.xayah.databackup.ui.component.SnackbarScaffold
import com.xayah.databackup.ui.component.VerticalGrid
import com.xayah.databackup.ui.component.ignorePaddingHorizontal
import com.xayah.databackup.ui.component.paddingBottom
import com.xayah.databackup.ui.component.paddingHorizontal
import com.xayah.databackup.ui.component.paddingTop
import com.xayah.databackup.ui.token.CommonTokens
import com.xayah.databackup.util.IntentUtil
import com.xayah.databackup.util.readCloudActiveName
import kotlinx.coroutines.launch

@ExperimentalMaterial3Api
@Composable
fun PageMain(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val activeName by context.readCloudActiveName().collectAsState(initial = "")

    SnackbarScaffold(snackbarHostState) {
        LazyColumn(
            modifier = Modifier.paddingHorizontal(CommonTokens.PaddingMedium),
            verticalArrangement = Arrangement.spacedBy(CommonTokens.PaddingLarge)
        ) {
            item {
                Spacer(modifier = Modifier.paddingTop(CommonTokens.PaddingMedium))
                OverLookCloudCard()
            }

            item {
                Module(title = stringResource(R.string.utilities)) {
                    val actions = listOf(
                        context.getString(R.string.account),
                        context.getString(R.string.mount),
                        stringResource(R.string.reload),
                    )
                    val icons = listOf(
                        ImageVector.vectorResource(R.drawable.ic_rounded_badge),
                        ImageVector.vectorResource(R.drawable.ic_rounded_install_desktop),
                        ImageVector.vectorResource(R.drawable.ic_rounded_folder_open),
                    )
                    val onClicks = listOf<suspend () -> Unit>(
                        {
                            navController.navigate(CloudRoutes.Account.route)
                        },
                        {
                            navController.navigate(CloudRoutes.Mount.route)
                        },
                        {
                            navController.navigate("${CloudRoutes.Reload.route}?$ReloadArg=true")
                        },
                    )
                    Row(
                        modifier = Modifier
                            .ignorePaddingHorizontal(CommonTokens.PaddingMedium)
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                    ) {
                        Spacer(modifier = Modifier.width(CommonTokens.PaddingMedium))
                        Row(horizontalArrangement = Arrangement.spacedBy(CommonTokens.PaddingLarge)) {
                            repeat(actions.size) { index ->
                                CardActionButton(
                                    text = actions[index],
                                    icon = icons[index],
                                    onClick = {
                                        scope.launch {
                                            onClicks[index]()
                                        }
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(CommonTokens.PaddingMedium))
                    }
                }
            }

            item {
                Module(title = stringResource(R.string.activities)) {
                    val items = listOf(
                        stringResource(R.string.app_and_data),
                        stringResource(R.string.media),
                        stringResource(R.string.telephony)
                    )
                    val icons = listOf(
                        ImageVector.vectorResource(R.drawable.ic_rounded_palette),
                        ImageVector.vectorResource(R.drawable.ic_rounded_image),
                        ImageVector.vectorResource(R.drawable.ic_rounded_call),
                    )
                    val onClicks = listOf(
                        {
                            IntentUtil.toOperationActivity(context = context, route = OperationRoutes.PackageBackup, cloudMode = true)
                        },
                        {
                        },
                        {
                        },
                    )
                    VerticalGrid(
                        columns = 2,
                        count = items.size,
                        horizontalArrangement = Arrangement.spacedBy(CommonTokens.PaddingLarge)
                    ) { index ->
                        AssistChip(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                scope.launch {
                                    if (activeName.isEmpty()) {
                                        snackbarHostState.showSnackbar(
                                            message = context.getString(R.string.please_set_up_main_account),
                                            duration = SnackbarDuration.Short
                                        )
                                    } else {
                                        onClicks[index].invoke()
                                    }
                                }
                            },
                            label = { Text(items[index]) },
                            leadingIcon = {
                                Icon(
                                    imageVector = icons[index],
                                    contentDescription = null,
                                    modifier = Modifier.size(AssistChipDefaults.IconSize)
                                )
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.paddingBottom(CommonTokens.PaddingMedium))
            }
        }
    }
}
