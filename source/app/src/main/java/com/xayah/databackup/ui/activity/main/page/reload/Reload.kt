package com.xayah.databackup.ui.activity.main.page.reload

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.xayah.databackup.R
import com.xayah.databackup.data.OperationMask
import com.xayah.databackup.ui.component.FabSnackbarScaffold
import com.xayah.databackup.ui.component.ListItemReload
import com.xayah.databackup.ui.component.Serial
import com.xayah.databackup.ui.component.TitleMediumBoldText
import com.xayah.databackup.ui.component.material3.SegmentedButton
import com.xayah.databackup.ui.component.material3.SegmentedButtonDefaults
import com.xayah.databackup.ui.component.material3.SingleChoiceSegmentedButtonRow
import com.xayah.databackup.ui.component.paddingHorizontal
import com.xayah.databackup.ui.token.CommonTokens
import kotlinx.coroutines.launch

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun PageReload(viewModel: ReloadViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState
    val scope = rememberCoroutineScope()
    val selectedIndex = uiState.selectedIndex
    val options = uiState.options
    val packages = uiState.packages
    val medium = uiState.medium

    LaunchedEffect(null) {
        viewModel.initialize()
    }

    FabSnackbarScaffold(
        floatingActionButton = {
            if (uiState.isLoading.not()) {
                ExtendedFloatingActionButton(
                    onClick = {
                        scope.launch {
                            viewModel.reloading(context)
                        }
                    },
                    expanded = true,
                    icon = { Icon(imageVector = Icons.Rounded.Refresh, contentDescription = null) },
                    text = { Text(text = stringResource(id = R.string.reload)) },
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        snackbarHostState = uiState.snackbarHostState,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .paddingHorizontal(CommonTokens.PaddingMedium),
            verticalArrangement = Arrangement.spacedBy(CommonTokens.PaddingMedium)
        ) {
            item {
                Spacer(modifier = Modifier.height(CommonTokens.PaddingMedium))
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    options.forEachIndexed { index, label ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                            onClick = {
                                viewModel.setSelectedIndex(index)
                                viewModel.initialize()
                            },
                            selected = index == selectedIndex
                        ) {
                            Text(label)
                        }
                    }
                }
            }

            item {
                TitleMediumBoldText(text = stringResource(id = R.string.app_and_data))
            }

            if (packages.isEmpty() && uiState.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                items(count = packages.size, key = { it }) {
                    ListItemReload(title = packages[it].label, subtitle = packages[it].packageName) {
                        if (OperationMask.isApkSelected(packages[it].backupOpCode))
                            Serial(serial = stringResource(id = R.string.apk))
                        if (OperationMask.isDataSelected(packages[it].backupOpCode))
                            Serial(serial = stringResource(id = R.string.data))
                    }
                }
            }

            item {
                TitleMediumBoldText(text = stringResource(id = R.string.media))
            }

            if (medium.isEmpty() && uiState.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                items(count = medium.size, key = { it }) {
                    ListItemReload(title = medium[it].name, subtitle = medium[it].path) {
                        Serial(serial = stringResource(id = R.string.data))
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(CommonTokens.PaddingMedium))
            }
        }
    }
}
