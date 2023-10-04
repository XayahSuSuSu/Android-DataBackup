package com.xayah.databackup.ui.activity.operation.page.media.backup

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.xayah.databackup.R
import com.xayah.databackup.ui.activity.operation.page.packages.backup.confirmExit
import com.xayah.databackup.ui.component.ListItemMediaBackup
import com.xayah.databackup.ui.component.Loader
import com.xayah.databackup.ui.component.LocalSlotScope
import com.xayah.databackup.ui.component.MediaListScaffold
import com.xayah.databackup.ui.component.paddingBottom
import com.xayah.databackup.ui.component.paddingHorizontal
import com.xayah.databackup.ui.component.paddingTop
import com.xayah.databackup.ui.token.CommonTokens
import kotlinx.coroutines.launch

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun PageMediaBackupList() {
    val context = LocalContext.current
    val viewModel = hiltViewModel<MediaBackupListViewModel>()
    val scope = rememberCoroutineScope()
    val dialogSlot = LocalSlotScope.current!!.dialogSlot
    val uiState by viewModel.uiState
    val isProcessing = uiState.opType == OpType.PROCESSING
    val medium by uiState.medium.collectAsState(initial = listOf())
    val mediumDisplay = if (isProcessing) medium.filter { it.media.selected } else medium
    val selectedCount by uiState.selectedCount.collectAsState(initial = 0)

    LaunchedEffect(null) {
        viewModel.initialize()
    }

    if (isProcessing) BackHandler {
        scope.launch {
            confirmExit(dialogSlot, context)
        }
    }

    MediaListScaffold(
        title = when (uiState.opType) {
            OpType.LIST -> stringResource(id = R.string.backup_list)
            OpType.PROCESSING -> stringResource(id = R.string.backing_up)
        },
        selectedCount = selectedCount,
        opType = uiState.opType,
        onFabClick = {
            viewModel.onProcessing()
        },
        onAddClick = {
            viewModel.onAdd(context = (context as ComponentActivity))
        }
    ) {
        Loader(modifier = Modifier.fillMaxSize(), isLoading = uiState.isLoading) {
            LazyColumn(
                modifier = Modifier.paddingHorizontal(CommonTokens.PaddingMedium),
                verticalArrangement = Arrangement.spacedBy(CommonTokens.PaddingMedium)
            ) {
                item {
                    Spacer(modifier = Modifier.paddingTop(CommonTokens.PaddingMedium))
                }

                items(items = mediumDisplay, key = { it.media.path }) { item ->
                    ListItemMediaBackup(entity = item)
                }

                item {
                    Spacer(modifier = Modifier.paddingBottom(CommonTokens.PaddingMedium))
                }
            }
        }
    }
}
