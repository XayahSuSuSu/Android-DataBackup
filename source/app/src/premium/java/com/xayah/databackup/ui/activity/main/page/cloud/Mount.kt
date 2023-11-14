package com.xayah.databackup.ui.activity.main.page.cloud

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.xayah.databackup.R
import com.xayah.databackup.ui.component.ListItemCloudMount
import com.xayah.databackup.ui.component.Serial
import com.xayah.databackup.ui.component.SnackbarScaffold
import com.xayah.databackup.ui.component.ignorePaddingHorizontal
import com.xayah.databackup.ui.component.paddingBottom
import com.xayah.databackup.ui.component.paddingHorizontal
import com.xayah.databackup.ui.component.paddingTop
import com.xayah.databackup.ui.token.CommonTokens
import com.xayah.databackup.ui.token.ListItemTokens
import com.xayah.core.rootservice.util.withIOContext
import kotlinx.coroutines.launch

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@Composable
fun PageMount(navController: NavHostController) {
    val viewModel = hiltViewModel<MountViewModel>()
    val uiState by viewModel.uiState
    val cloudEntities by uiState.cloudEntities.collectAsState(initial = listOf())

    SnackbarScaffold(uiState.snackbarHostState) {
        LazyColumn(
            modifier = Modifier.paddingHorizontal(CommonTokens.PaddingMedium),
            verticalArrangement = Arrangement.spacedBy(CommonTokens.PaddingMedium)
        ) {
            item {
                Spacer(modifier = Modifier.paddingTop(CommonTokens.PaddingMedium))
                Row(
                    modifier = Modifier
                        .ignorePaddingHorizontal(CommonTokens.PaddingMedium)
                        .horizontalScroll(rememberScrollState())
                        .paddingHorizontal(CommonTokens.PaddingMedium),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(CommonTokens.PaddingMedium)
                ) {
                    FilterChip(
                        selected = true,
                        onClick = {
                            viewModel.viewModelScope.launch {
                                withIOContext {
                                    viewModel.unmountAll()
                                }
                            }
                        },
                        label = { Text(stringResource(R.string.unmount_all)) },
                        leadingIcon = {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_rounded_settings_power),
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    )
                }
            }

            items(items = cloudEntities) { item ->
                ListItemCloudMount(
                    entity = item,
                    onCardClick = {},
                    chipGroup = {
                        if (item.mount.mounted) Serial(
                            modifier = Modifier.paddingTop(ListItemTokens.PaddingSmall),
                            serial = stringResource(id = R.string.mounted)
                        )
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.paddingBottom(CommonTokens.PaddingMedium))
            }
        }
    }
}
