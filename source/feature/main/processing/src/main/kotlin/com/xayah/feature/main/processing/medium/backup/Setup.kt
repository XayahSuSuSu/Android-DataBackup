package com.xayah.feature.main.processing.medium.backup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.xayah.core.datastore.KeyAutoScreenOff
import com.xayah.core.datastore.KeyBackupConfigs
import com.xayah.core.datastore.KeyResetBackupList
import com.xayah.core.model.StorageMode
import com.xayah.core.ui.component.Clickable
import com.xayah.core.ui.component.LocalSlotScope
import com.xayah.core.ui.component.Selectable
import com.xayah.core.ui.component.Switchable
import com.xayah.core.ui.component.Title
import com.xayah.core.ui.component.paddingHorizontal
import com.xayah.core.ui.component.paddingTop
import com.xayah.core.ui.component.paddingVertical
import com.xayah.core.ui.component.select
import com.xayah.core.ui.route.MainRoutes
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.LocalNavController
import com.xayah.core.ui.util.icon
import com.xayah.core.util.navigateSingle
import com.xayah.feature.main.processing.FinishSetup
import com.xayah.feature.main.processing.ProcessingSetupScaffold
import com.xayah.feature.main.processing.R
import com.xayah.feature.main.processing.SetCloudEntity
import com.xayah.feature.main.processing.UpdateFiles
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@ExperimentalFoundationApi
@ExperimentalLayoutApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun PageMediumBackupProcessingSetup(localNavController: NavHostController, viewModel: BackupViewModelImpl) {
    val navController = LocalNavController.current!!
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val isTesting by viewModel.isTesting.collectAsStateWithLifecycle()
    val mediumSize by viewModel.mediumSize.collectAsStateWithLifecycle()

    LaunchedEffect(null) {
        viewModel.emitIntentOnIO(UpdateFiles)
    }

    ProcessingSetupScaffold(
        scrollBehavior = scrollBehavior,
        snackbarHostState = viewModel.snackbarHostState,
        title = stringResource(id = R.string.setup),
        actions = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .paddingHorizontal(SizeTokens.Level24)
                    .paddingVertical(SizeTokens.Level8),
                horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level12, Alignment.End),
            ) {
                Button(
                    enabled = uiState.storageType == StorageMode.Local || (uiState.cloudEntity != null && isTesting.not()),
                    onClick = {
                        viewModel.emitIntentOnIO(FinishSetup(navController = localNavController))
                    }) {
                    Text(text = stringResource(id = R.string._continue))
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxSize(),
        ) {
            val storageOptions = remember { listOf(context.getString(R.string.local), context.getString(R.string.cloud)) }
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .paddingHorizontal(SizeTokens.Level16)
                    .paddingTop(SizeTokens.Level16)
            ) {
                storageOptions.forEachIndexed { index, label ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = storageOptions.size),
                        onClick = {
                            viewModel.emitStateOnMain(state = uiState.copy(storageIndex = index, storageType = if (index == 0) StorageMode.Local else StorageMode.Cloud))
                        },
                        selected = index == uiState.storageIndex
                    ) {
                        Text(label)
                    }
                }
            }

            Title(title = stringResource(id = R.string.storage)) {
                AnimatedVisibility(uiState.storageIndex == 1) {
                    if (accounts.isEmpty()) {
                        Clickable(
                            title = stringResource(id = R.string.account),
                            value = stringResource(id = R.string.no_available_account),
                            leadingIcon = ImageVector.vectorResource(id = R.drawable.ic_rounded_cancel_circle),
                            trailingIcon = Icons.Rounded.KeyboardArrowRight,
                        ) {
                            navController.navigateSingle(MainRoutes.Cloud.route)
                        }
                    } else {
                        val dialogState = LocalSlotScope.current!!.dialogSlot
                        var currentIndex by remember { mutableIntStateOf(if (uiState.cloudEntity == null) 0 else accounts.indexOfFirst { it.title == uiState.cloudEntity!!.name }) }
                        LaunchedEffect(currentIndex) {
                            viewModel.emitIntentOnIO(SetCloudEntity(name = accounts[currentIndex].title))
                        }
                        Selectable(
                            title = stringResource(id = R.string.account),
                            leadingIcon = uiState.cloudEntity?.type?.icon ?: ImageVector.vectorResource(id = R.drawable.ic_rounded_person),
                            value = if (uiState.cloudEntity == null) stringResource(id = R.string.choose_an_account) else accounts[currentIndex].desc,
                            current = if (uiState.cloudEntity == null) stringResource(id = R.string.not_selected) else accounts[currentIndex].title
                        ) {
                            viewModel.launchOnIO {
                                val (state, selectedIndex) = dialogState.select(
                                    title = context.getString(R.string.account),
                                    defIndex = currentIndex,
                                    items = accounts
                                )
                                if (state.isConfirm) {
                                    currentIndex = selectedIndex
                                }
                            }
                        }
                    }
                }

                Clickable(
                    title = stringResource(id = R.string.files),
                    value = mediumSize,
                    leadingIcon = ImageVector.vectorResource(id = R.drawable.ic_rounded_folder_open),
                )
            }
            Title(title = stringResource(id = R.string.settings)) {
                Switchable(
                    key = KeyAutoScreenOff,
                    defValue = false,
                    title = stringResource(id = R.string.auto_screen_off),
                    checkedText = stringResource(id = R.string.auto_screen_off_desc),
                )
                Switchable(
                    key = KeyResetBackupList,
                    defValue = false,
                    title = stringResource(id = R.string.reset_backup_list),
                    checkedText = stringResource(id = R.string.reset_backup_list_desc),
                )
                Switchable(
                    key = KeyBackupConfigs,
                    defValue = true,
                    title = stringResource(id = R.string.backup_configs),
                    checkedText = stringResource(id = R.string.backup_configs_desc),
                )
            }
        }
    }
}
