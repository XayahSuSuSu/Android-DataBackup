package com.xayah.feature.main.home.restore

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.xayah.core.ui.component.VerticalGrid
import com.xayah.core.ui.component.paddingHorizontal
import com.xayah.core.ui.component.paddingTop
import com.xayah.core.ui.material3.toColor
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.route.MainRoutes
import com.xayah.core.ui.token.PaddingTokens
import com.xayah.core.ui.util.LocalNavController
import com.xayah.core.ui.util.fromDrawable
import com.xayah.core.ui.util.fromString
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.value
import com.xayah.feature.main.home.common.ActivityCard
import com.xayah.feature.main.home.common.Module
import com.xayah.feature.main.home.common.OverLookCard
import com.xayah.feature.main.home.common.model.ActivityCardItem
import com.xayah.feature.main.home.common.model.MapItem
import com.xayah.feature.main.home.common.model.UtilityChipItem
import com.xayah.feature.main.home.foss.R
import kotlinx.coroutines.flow.flow
import com.xayah.core.ui.R as UiR

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun PageRestore() {
    val viewModel = hiltViewModel<IndexViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val navController = LocalNavController.current!!
    val lastRestoreTime by uiState.lastRestoreTime.collectAsState(initial = "")

    LazyColumn(
        modifier = Modifier.paddingHorizontal(PaddingTokens.Level2),
        verticalArrangement = Arrangement.spacedBy(PaddingTokens.Level4)
    ) {
        item {
            Spacer(modifier = Modifier.paddingTop(PaddingTokens.Level3))
            OverLookCard(
                icon = ImageVectorToken.fromDrawable(UiR.drawable.ic_rounded_token),
                title = StringResourceToken.StringIdToken(R.string.last_restore),
                content = lastRestoreTime
            )
        }

        item {
            Module(title = stringResource(R.string.activities)) {
                val items = listOf(
                    ActivityCardItem(
                        label = StringResourceToken.fromStringId(R.string.app_and_data),
                        icon = ImageVectorToken.fromDrawable(UiR.drawable.ic_rounded_palette),
                        details = listOf(
                            MapItem(StringResourceToken.fromString(""), flow { }),
                            MapItem(StringResourceToken.fromString(""), flow { }),
                        ),
                        onClick = {
                            navController.navigate(MainRoutes.TaskPackages.routeRestore)
                        }
                    ),
                    ActivityCardItem(
                        label = StringResourceToken.fromStringId(R.string.media),
                        icon = ImageVectorToken.fromDrawable(UiR.drawable.ic_rounded_image),
                        details = listOf(
                            MapItem(StringResourceToken.fromString(""), flow { }),
                            MapItem(StringResourceToken.fromString(""), flow { }),
                        ),
                        onClick = {
                            navController.navigate(MainRoutes.TaskMedium.routeRestore)
                        }
                    ),
                    ActivityCardItem(
                        label = StringResourceToken.fromStringId(R.string.telephony),
                        icon = ImageVectorToken.fromDrawable(UiR.drawable.ic_rounded_call),
                        details = listOf(
                            MapItem(StringResourceToken.fromString(""), flow { }),
                            MapItem(StringResourceToken.fromString(""), flow { }),
                        ),
                        onClick = {
                        }
                    )
                )

                VerticalGrid(
                    columns = 2,
                    count = items.size,
                    horizontalArrangement = Arrangement.spacedBy(PaddingTokens.Level3),
                    verticalArrangement = Arrangement.spacedBy(PaddingTokens.Level3)
                ) { index ->
                    ActivityCard(
                        modifier = Modifier.fillMaxWidth(),
                        label = items[index].label,
                        icon = items[index].icon,
                        details = items[index].details,
                        onClick = items[index].onClick,
                    )
                }
            }
        }

        item {
            Module(title = stringResource(R.string.utilities)) {
                val items = listOf(
                    UtilityChipItem(
                        label = StringResourceToken.fromStringId(R.string.directory),
                        icon = ImageVectorToken.fromDrawable(UiR.drawable.ic_rounded_folder_open),
                        onClick = {
                            navController.navigate(MainRoutes.Directory.routeRestore)
                        }
                    ),
                    UtilityChipItem(
                        label = StringResourceToken.fromStringId(R.string.structure),
                        icon = ImageVectorToken.fromDrawable(UiR.drawable.ic_rounded_account_tree),
                        onClick = {}
                    ),
                    UtilityChipItem(
                        label = StringResourceToken.fromStringId(R.string.log),
                        icon = ImageVectorToken.fromDrawable(UiR.drawable.ic_rounded_bug_report),
                        onClick = {
                            navController.navigate(MainRoutes.Log.route)
                        }
                    )
                )

                VerticalGrid(
                    columns = 2,
                    count = items.size,
                    horizontalArrangement = Arrangement.spacedBy(PaddingTokens.Level3),
                ) { index ->
                    AssistChip(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = items[index].onClick,
                        label = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(PaddingTokens.Level1),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(modifier = Modifier.weight(1f), text = items[index].label.value)
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowRight,
                                    tint = ColorSchemeKeyTokens.Primary.toColor(),
                                    contentDescription = null,
                                    modifier = Modifier.size(AssistChipDefaults.IconSize)
                                )
                            }
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = items[index].icon.value,
                                contentDescription = null,
                                modifier = Modifier.size(AssistChipDefaults.IconSize)
                            )
                        },
                    )
                }
            }
        }
    }
}
