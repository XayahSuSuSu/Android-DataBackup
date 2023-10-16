package com.xayah.databackup.ui.component

import androidx.activity.ComponentActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.xayah.databackup.R
import com.xayah.databackup.data.CloudAccountEntity
import com.xayah.databackup.data.CloudMountEntity
import com.xayah.databackup.ui.activity.main.page.cloud.AccountViewModel
import com.xayah.databackup.ui.activity.main.page.cloud.MountViewModel
import com.xayah.databackup.ui.activity.main.page.cloud.router.AccountDetailArg
import com.xayah.databackup.ui.activity.main.page.cloud.router.CloudRoutes
import com.xayah.databackup.ui.component.material3.Card
import com.xayah.databackup.ui.component.material3.outlinedCardBorder
import com.xayah.databackup.ui.theme.ColorScheme
import com.xayah.databackup.ui.token.ListItemTokens
import com.xayah.librootservice.util.withIOContext
import com.xayah.librootservice.util.withMainContext
import kotlinx.coroutines.launch

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun ListItemCloudAccount(
    modifier: Modifier = Modifier,
    entity: CloudAccountEntity,
    navController: NavHostController,
    onCardClick: () -> Unit,
    chipGroup: @Composable RowScope.() -> Unit,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val viewModel = hiltViewModel<AccountViewModel>()
    val dialogSlot = LocalSlotScope.current!!.dialogSlot
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        enabled = true,
        onClick = onCardClick,
        onLongClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            expanded = true
        },
        border = outlinedCardBorder(lineColor = ColorScheme.primary()),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ListItemTokens.PaddingMedium)
        ) {
            Column {
                Row {
                    HeadlineMediumBoldText(text = entity.name)
                    Spacer(modifier = Modifier.weight(1f))
                }
                BodySmallBoldText(text = entity.account.url.ifEmpty { entity.account.host })
                Divider(modifier = Modifier.paddingVertical(ListItemTokens.PaddingSmall))
                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .wrapContentSize(Alignment.Center)
                    ) {
                        val actions = remember(entity) {
                            listOf(
                                ActionMenuItem(
                                    title = context.getString(R.string.edit),
                                    icon = Icons.Rounded.Edit,
                                    enabled = true,
                                    onClick = {
                                        viewModel.viewModelScope.launch {
                                            withIOContext {
                                                expanded = false
                                                withMainContext {
                                                    navController.navigate("${CloudRoutes.AccountDetail.route}?$AccountDetailArg=${entity.name}")
                                                }
                                            }
                                        }
                                    }
                                ),
                                ActionMenuItem(
                                    title = context.getString(R.string.delete),
                                    icon = Icons.Rounded.Delete,
                                    enabled = true,
                                    onClick = {
                                        viewModel.viewModelScope.launch {
                                            withIOContext {
                                                expanded = false
                                                dialogSlot.openConfirmDialog(context, context.getString(R.string.confirm_delete))
                                                    .also { (confirmed, _) ->
                                                        if (confirmed) {
                                                            viewModel.delete(entity)
                                                        }
                                                    }
                                            }
                                        }
                                    }
                                )
                            )
                        }

                        Spacer(modifier = Modifier.align(Alignment.BottomEnd))

                        ModalActionDropdownMenu(expanded = expanded, actionList = actions, onDismissRequest = { expanded = false })
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(ListItemTokens.PaddingSmall),
                        content = {
                            chipGroup()
                        }
                    )
                }
            }
        }
    }
}

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun ListItemCloudMount(
    modifier: Modifier = Modifier,
    entity: CloudMountEntity,
    onCardClick: () -> Unit,
    chipGroup: @Composable RowScope.() -> Unit,
) {
    val context = LocalContext.current
    val viewModel = hiltViewModel<MountViewModel>()
    val haptic = LocalHapticFeedback.current
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        enabled = true,
        onClick = onCardClick,
        onLongClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            expanded = true
        },
        border = outlinedCardBorder(lineColor = ColorScheme.primary()),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ListItemTokens.PaddingMedium)
        ) {
            Column {
                Row {
                    HeadlineMediumBoldText(text = entity.name)
                    Spacer(modifier = Modifier.weight(1f))
                }
                BodySmallBoldText(text = entity.mount.remote)
                Divider(modifier = Modifier.paddingVertical(ListItemTokens.PaddingSmall))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ListItemTokens.PaddingSmall)) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowForward,
                        contentDescription = null
                    )
                    BodySmallBoldText(text = entity.mount.local)
                }
                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .wrapContentSize(Alignment.Center)
                    ) {
                        val actions = remember(entity) {
                            listOf(
                                ActionMenuItem(
                                    title = context.getString(R.string.set_remote),
                                    icon = ImageVector.vectorResource(context.theme, context.resources, R.drawable.ic_rounded_cloud),
                                    enabled = true,
                                    onClick = {
                                        viewModel.viewModelScope.launch {
                                            withIOContext {
                                                expanded = false
                                                viewModel.setRemote((context as ComponentActivity), entity)
                                            }
                                        }
                                    }
                                ),
                                ActionMenuItem(
                                    title = context.getString(R.string.set_local),
                                    icon = ImageVector.vectorResource(context.theme, context.resources, R.drawable.ic_rounded_phone_android),
                                    enabled = true,
                                    onClick = {
                                        viewModel.viewModelScope.launch {
                                            withIOContext {
                                                expanded = false
                                                viewModel.setLocal((context as ComponentActivity), entity)
                                            }
                                        }
                                    }
                                ),
                                ActionMenuItem(
                                    title = context.getString(R.string.mount),
                                    icon = ImageVector.vectorResource(context.theme, context.resources, R.drawable.ic_rounded_install_desktop),
                                    enabled = true,
                                    onClick = {
                                        viewModel.viewModelScope.launch {
                                            withIOContext {
                                                expanded = false
                                                viewModel.mount(entity)
                                            }
                                        }
                                    }
                                ),
                                ActionMenuItem(
                                    title = context.getString(R.string.unmount),
                                    icon = ImageVector.vectorResource(context.theme, context.resources, R.drawable.ic_rounded_power_settings_new),
                                    enabled = true,
                                    onClick = {
                                        viewModel.viewModelScope.launch {
                                            withIOContext {
                                                expanded = false
                                                viewModel.unmount(entity)
                                            }
                                        }
                                    }
                                )
                            )
                        }

                        Spacer(modifier = Modifier.align(Alignment.BottomEnd))

                        ModalActionDropdownMenu(expanded = expanded, actionList = actions, onDismissRequest = { expanded = false })
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(ListItemTokens.PaddingSmall),
                        content = {
                            chipGroup()
                        }
                    )
                }
            }
        }
    }
}
