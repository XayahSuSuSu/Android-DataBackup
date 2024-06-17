package com.xayah.feature.main.configurations

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.ui.component.Checkable
import com.xayah.core.ui.component.LocalSlotScope
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.util.fromDrawable
import com.xayah.core.ui.util.fromString
import com.xayah.core.ui.util.fromStringArgs
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.fromVector
import com.xayah.core.ui.util.value

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

    ConfigurationsScaffold(
        scrollBehavior = scrollBehavior, snackbarHostState = viewModel.snackbarHostState,
        title = StringResourceToken.fromStringId(R.string.configurations), actions = {
            OutlinedButton(enabled = blockedPackagesState.size + blockedFilesState.size + accounts.size + files.size != 0 && uiState.selectedCount != 0, onClick = {
                viewModel.emitIntentOnIO(IndexUiIntent.Export)
            }) {
                Text(
                    text = StringResourceToken.fromStringArgs(
                        StringResourceToken.fromStringId(R.string.export),
                        StringResourceToken.fromString(" (${uiState.selectedCount})"),
                    ).value
                )
            }
            Button(enabled = true, onClick = {
                viewModel.emitIntentOnIO(IndexUiIntent.Import(dialogState))
            }) {
                Text(text = StringResourceToken.fromStringId(R.string._import).value)
            }
        }
    ) {
        Checkable(
            icon = ImageVectorToken.fromVector(Icons.Outlined.Block),
            title = StringResourceToken.fromStringId(R.string.blacklist),
            value = StringResourceToken.fromString((blockedPackagesState.size + blockedFilesState.size).toString()),
            checked = uiState.blacklistSelected,
        ) {
            viewModel.emitStateOnMain(uiState.copy(selectedCount = if (it) uiState.selectedCount - 1 else uiState.selectedCount + 1, blacklistSelected = it.not()))
        }
        Checkable(
            icon = ImageVectorToken.fromVector(Icons.Outlined.Cloud),
            title = StringResourceToken.fromStringId(R.string.cloud),
            value = StringResourceToken.fromString(accounts.size.toString()),
            checked = uiState.cloudSelected,
        ) {
            viewModel.emitStateOnMain(uiState.copy(selectedCount = if (it) uiState.selectedCount - 1 else uiState.selectedCount + 1, cloudSelected = it.not()))
        }
        Checkable(
            icon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_folder_open),
            title = StringResourceToken.fromStringId(R.string.files),
            value = StringResourceToken.fromString(files.size.toString()),
            checked = uiState.fileSelected,
        ) {
            viewModel.emitStateOnMain(uiState.copy(selectedCount = if (it) uiState.selectedCount - 1 else uiState.selectedCount + 1, fileSelected = it.not()))
        }
    }
}
