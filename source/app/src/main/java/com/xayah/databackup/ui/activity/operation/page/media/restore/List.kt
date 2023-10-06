package com.xayah.databackup.ui.activity.operation.page.media.restore

import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.xayah.databackup.R
import com.xayah.databackup.ui.activity.operation.page.media.backup.OpType
import com.xayah.databackup.ui.activity.operation.page.packages.backup.confirmExit
import com.xayah.databackup.ui.component.ChipDropdownMenu
import com.xayah.databackup.ui.component.ListItemMediaRestore
import com.xayah.databackup.ui.component.Loader
import com.xayah.databackup.ui.component.LocalSlotScope
import com.xayah.databackup.ui.component.MediaListScaffold
import com.xayah.databackup.ui.component.ignorePaddingHorizontal
import com.xayah.databackup.ui.component.paddingBottom
import com.xayah.databackup.ui.component.paddingHorizontal
import com.xayah.databackup.ui.token.CommonTokens
import com.xayah.databackup.util.DateUtil
import com.xayah.librootservice.util.withIOContext
import kotlinx.coroutines.launch

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun PageMediaRestoreList() {
    val context = LocalContext.current
    val viewModel = hiltViewModel<MediaRestoreListViewModel>()
    val scope = rememberCoroutineScope()
    val dialogSlot = LocalSlotScope.current!!.dialogSlot
    val uiState by viewModel.uiState
    val isProcessing = uiState.opType == OpType.PROCESSING
    val medium by uiState.medium.collectAsState(initial = listOf())
    val mediumDisplay = if (isProcessing) medium.filter { it.media.selected } else medium
    val selectedCount by uiState.selectedCount.collectAsState(initial = 0)
    var allSelected by remember { mutableStateOf(false) }

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
            OpType.LIST -> stringResource(id = R.string.restore_list)
            OpType.PROCESSING -> stringResource(id = R.string.restoring)
        },
        selectedCount = selectedCount,
        opType = uiState.opType,
        onFabClick = {
            viewModel.onProcessing()
        },
        onCheckListPressed = {
            scope.launch {
                withIOContext {
                    allSelected = allSelected.not()
                    viewModel.updateRestoreSelected(allSelected)
                }
            }
        },
        onAddClick = null
    ) {
        Loader(modifier = Modifier.fillMaxSize(), isLoading = uiState.isLoading) {
            LazyColumn(
                modifier = Modifier.paddingHorizontal(CommonTokens.PaddingMedium),
                verticalArrangement = Arrangement.spacedBy(CommonTokens.PaddingMedium)
            ) {
                item {
                    Spacer(modifier = Modifier.height(CommonTokens.PaddingMedium))
                    if (isProcessing.not()) Row(
                        modifier = Modifier
                            .ignorePaddingHorizontal(CommonTokens.PaddingMedium)
                            .horizontalScroll(rememberScrollState())
                            .paddingHorizontal(CommonTokens.PaddingMedium),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(CommonTokens.PaddingMedium)
                    ) {
                        val dateList = uiState.timestamps.map { timestamp -> DateUtil.formatTimestamp(timestamp) }
                        ChipDropdownMenu(
                            label = stringResource(R.string.date),
                            trailingIcon = ImageVector.vectorResource(R.drawable.ic_rounded_unfold_more),
                            defaultSelectedIndex = uiState.selectedIndex,
                            list = dateList,
                            onSelected = { index, _ ->
                                scope.launch {
                                    viewModel.setSelectedIndex(index)
                                }
                            },
                            onClick = {}
                        )
                    }
                }

                items(items = mediumDisplay, key = { it.media.id }) { item ->
                    ListItemMediaRestore(entity = item)
                }

                item {
                    Spacer(modifier = Modifier.paddingBottom(CommonTokens.PaddingMedium))
                }
            }
        }
    }
}
