package com.xayah.feature.main.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.TaskAlt
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.ui.component.VerticalGrid
import com.xayah.core.ui.component.paddingHorizontal
import com.xayah.core.ui.component.paddingTop
import com.xayah.core.ui.material3.toColor
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.token.PaddingTokens
import com.xayah.core.ui.util.LocalNavController
import com.xayah.core.ui.util.fromDrawable
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.fromVector
import com.xayah.core.ui.util.value
import com.xayah.core.ui.R as UiR

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun PageHome() {
    val viewModel = hiltViewModel<IndexViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = LocalNavController.current!!
    val lastBackupTime by uiState.lastBackupTime.collectAsState(initial = 0)

    LazyColumn(
        modifier = Modifier.paddingHorizontal(PaddingTokens.Level3),
        verticalArrangement = Arrangement.spacedBy(PaddingTokens.Level5)
    ) {
        item {
            Spacer(modifier = Modifier.paddingTop(PaddingTokens.Level4))
            OverLookCard(
                icon = ImageVectorToken.fromDrawable(UiR.drawable.ic_rounded_token),
                title = StringResourceToken.StringIdToken(R.string.last_backup),
                content = lastBackupTime.toString()
            )
        }

        item {
            Module(title = stringResource(R.string.activities)) {
                val items = listOf(
                    ActivityCardItem(
                        label = StringResourceToken.fromStringId(R.string.app_and_data),
                        icon = ImageVectorToken.fromDrawable(UiR.drawable.ic_rounded_palette),
                        onClick = {
                            viewModel.emitIntent(IndexUiIntent.ToPagePackages(navController))
                        },
                    ),
                    ActivityCardItem(
                        label = StringResourceToken.fromStringId(R.string.media),
                        icon = ImageVectorToken.fromDrawable(UiR.drawable.ic_rounded_image),
                        onClick = {
                            viewModel.emitIntent(IndexUiIntent.ToPageMedium(navController))
                        },
                    ),
                    /**
                     * TODO: Telephony
                     * ActivityCard(
                     *     label = StringResourceToken.fromStringId(R.string.telephony),
                     *     icon = ImageVectorToken.fromDrawable(UiR.drawable.ic_rounded_call),
                     *     onClick = {},
                     * )
                     */
                )

                VerticalGrid(
                    columns = 2,
                    count = items.size,
                    horizontalArrangement = Arrangement.spacedBy(PaddingTokens.Level4),
                    verticalArrangement = Arrangement.spacedBy(PaddingTokens.Level4)
                ) { index ->
                    ActivityCard(
                        modifier = Modifier.fillMaxWidth(),
                        label = items[index].label,
                        icon = items[index].icon,
                        onClick = items[index].onClick,
                    )
                }
            }
        }

        item {
            Module(title = stringResource(R.string.utilities)) {
                val items = listOf(
                    UtilityChipItem(
                        label = StringResourceToken.fromStringId(R.string.task),
                        icon = ImageVectorToken.fromVector(Icons.Rounded.TaskAlt),
                        onClick = {
                            viewModel.emitIntent(IndexUiIntent.ToPageTaskList(navController))
                        }
                    ),
                    UtilityChipItem(
                        label = StringResourceToken.fromStringId(R.string.directory),
                        icon = ImageVectorToken.fromDrawable(UiR.drawable.ic_rounded_folder_open),
                        onClick = {
                            viewModel.emitIntent(IndexUiIntent.ToPageDirectory(navController))
                        }
                    ),
                    UtilityChipItem(
                        label = StringResourceToken.fromStringId(R.string.structure),
                        icon = ImageVectorToken.fromDrawable(UiR.drawable.ic_rounded_account_tree),
                        onClick = {
                            viewModel.emitIntent(IndexUiIntent.ToPageTree(navController))
                        }
                    ),
                    UtilityChipItem(
                        label = StringResourceToken.fromStringId(R.string.log),
                        icon = ImageVectorToken.fromDrawable(UiR.drawable.ic_rounded_bug_report),
                        onClick = {
                            viewModel.emitIntent(IndexUiIntent.ToPageLog(navController))
                        }
                    )
                )

                VerticalGrid(
                    columns = 2,
                    count = items.size,
                    horizontalArrangement = Arrangement.spacedBy(PaddingTokens.Level4),
                ) { index ->
                    AssistChip(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = items[index].onClick,
                        label = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(PaddingTokens.Level2),
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
