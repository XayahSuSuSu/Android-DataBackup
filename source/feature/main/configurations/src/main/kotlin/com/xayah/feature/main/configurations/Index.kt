package com.xayah.feature.main.configurations

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.ui.component.Checkable
import com.xayah.core.ui.component.LocalSlotScope
import com.xayah.core.ui.util.joinOf

@ExperimentalFoundationApi
@ExperimentalLayoutApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun PageConfigurations() {
    val dialogState = LocalSlotScope.current!!.dialogSlot
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val viewModel = hiltViewModel<IndexViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val blockedPackagesState by viewModel.blockedPackagesState.collectAsStateWithLifecycle()
    val blockedFilesState by viewModel.blockedFilesState.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val files by viewModel.files.collectAsStateWithLifecycle()
    val labels by viewModel.labels.collectAsStateWithLifecycle()
    val labelAppRefs by viewModel.labelAppRefs.collectAsStateWithLifecycle()
    val labelFileRefs by viewModel.labelFileRefs.collectAsStateWithLifecycle()

    ConfigurationsScaffold(
        scrollBehavior = scrollBehavior, snackbarHostState = viewModel.snackbarHostState,
        title = stringResource(id = R.string.configurations), actions = {
            OutlinedButton(
                enabled = blockedPackagesState.size + blockedFilesState.size + accounts.size + files.size + labels.size + labelAppRefs.size + labelFileRefs.size != 0 && uiState.selectedCount != 0,
                onClick = {
                viewModel.emitIntentOnIO(IndexUiIntent.Export)
            }) {
                Text(
                    text = joinOf(
                        stringResource(id = R.string.export),
                        " (${uiState.selectedCount})",
                    )
                )
            }
            Button(enabled = true, onClick = {
                viewModel.emitIntentOnIO(IndexUiIntent.Import(dialogState))
            }) {
                Text(text = stringResource(id = R.string._import))
            }
        }
    ) {
        Checkable(
            icon = Icons.Outlined.Block,
            title = stringResource(id = R.string.blacklist),
            value = (blockedPackagesState.size + blockedFilesState.size).toString(),
            checked = uiState.blacklistSelected,
        ) {
            viewModel.emitStateOnMain(uiState.copy(selectedCount = if (it) uiState.selectedCount - 1 else uiState.selectedCount + 1, blacklistSelected = it.not()))
        }
        Checkable(
            icon = Icons.Outlined.Cloud,
            title = stringResource(id = R.string.cloud),
            value = accounts.size.toString(),
            checked = uiState.cloudSelected,
        ) {
            viewModel.emitStateOnMain(uiState.copy(selectedCount = if (it) uiState.selectedCount - 1 else uiState.selectedCount + 1, cloudSelected = it.not()))
        }
        Checkable(
            icon = ImageVector.vectorResource(id = R.drawable.ic_rounded_folder_open),
            title = stringResource(id = R.string.files),
            value = files.size.toString(),
            checked = uiState.fileSelected,
        ) {
            viewModel.emitStateOnMain(uiState.copy(selectedCount = if (it) uiState.selectedCount - 1 else uiState.selectedCount + 1, fileSelected = it.not()))
        }
        Checkable(
            icon = Icons.Outlined.BookmarkBorder,
            title = stringResource(id = R.string.labels),
            value = (labels.size + labelAppRefs.size + labelFileRefs.size).toString(),
            checked = uiState.labelSelected,
        ) {
            viewModel.emitStateOnMain(uiState.copy(selectedCount = if (it) uiState.selectedCount - 1 else uiState.selectedCount + 1, labelSelected = it.not()))
        }
    }
}
