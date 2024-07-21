package com.xayah.feature.main.medium.restore.detail

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.model.DataType
import com.xayah.core.model.util.formatSize
import com.xayah.core.ui.component.BodyMediumText
import com.xayah.core.ui.component.FilledIconButton
import com.xayah.core.ui.component.LocalSlotScope
import com.xayah.core.ui.component.MediaIconImage
import com.xayah.core.ui.component.Switchable
import com.xayah.core.ui.component.Title
import com.xayah.core.ui.component.TitleLargeText
import com.xayah.core.ui.component.confirm
import com.xayah.core.ui.component.paddingBottom
import com.xayah.core.ui.component.paddingHorizontal
import com.xayah.core.ui.component.paddingStart
import com.xayah.core.ui.component.paddingTop
import com.xayah.core.ui.theme.ThemedColorSchemeKeyTokens
import com.xayah.core.ui.theme.value
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.LocalNavController
import com.xayah.core.ui.util.joinOf
import com.xayah.core.util.SymbolUtil
import com.xayah.core.util.withMainContext
import com.xayah.feature.main.medium.ListScaffold
import com.xayah.feature.main.medium.R

@ExperimentalFoundationApi
@ExperimentalLayoutApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun PageMediumRestoreDetail() {
    val context = LocalContext.current
    val navController = LocalNavController.current!!
    val dialogState = LocalSlotScope.current!!.dialogSlot
    val viewModel = hiltViewModel<IndexViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val mediaState by viewModel.mediaState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    LaunchedEffect(null) {
        viewModel.emitIntentOnIO(IndexUiIntent.OnRefresh)
    }

    ListScaffold(
        scrollBehavior = scrollBehavior,
        snackbarHostState = viewModel.snackbarHostState,
        title = stringResource(id = R.string.details),
        actions = {}
    ) {
        mediaState?.also { media ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                Row(
                    modifier = Modifier
                        .paddingHorizontal(SizeTokens.Level24)
                        .paddingTop(SizeTokens.Level12),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level24)
                ) {
                    MediaIconImage(name = uiState.name.firstOrNull()?.toString() ?: "", textStyle = MaterialTheme.typography.titleLarge, size = SizeTokens.Level64)
                    Column(modifier = Modifier.weight(1f)) {
                        TitleLargeText(text = media.name, color = ThemedColorSchemeKeyTokens.OnSurface.value)
                        BodyMediumText(
                            text = media.path.ifEmpty { stringResource(id = R.string.specify_a_path) },
                            color = (if (media.path.isEmpty()) ThemedColorSchemeKeyTokens.Error else ThemedColorSchemeKeyTokens.OnSurfaceVariant).value
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .paddingHorizontal(SizeTokens.Level24)
                        .paddingBottom(SizeTokens.Level12),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level4)
                ) {
                    Spacer(modifier = Modifier.paddingStart(SizeTokens.Level68))
                    FilledIconButton(
                        enabled = true,
                        icon = ImageVector.vectorResource(id = R.drawable.ic_rounded_folder_open),
                        containerColor = ThemedColorSchemeKeyTokens.BluePrimaryContainer,
                        contentColor = ThemedColorSchemeKeyTokens.BlueOnPrimaryContainer
                    ) {
                        viewModel.emitIntentOnIO(IndexUiIntent.SetPath(context = context, mediaEntity = media))
                    }
                    FilledIconButton(
                        enabled = media.preserveId == 0L,
                        icon = Icons.Outlined.Shield,
                        containerColor = ThemedColorSchemeKeyTokens.YellowPrimaryContainer,
                        contentColor = ThemedColorSchemeKeyTokens.YellowOnPrimaryContainer
                    ) {
                        viewModel.launchOnIO {
                            if (dialogState.confirm(title = context.getString(R.string.protect), text = context.getString(R.string.protect_desc))) {
                                viewModel.emitIntent(IndexUiIntent.Preserve(mediaEntity = media))
                                withMainContext {
                                    navController.popBackStack()
                                }
                            }
                        }
                    }
                    FilledIconButton(
                        enabled = true,
                        icon = Icons.Outlined.Delete,
                        containerColor = ThemedColorSchemeKeyTokens.ErrorContainer,
                        contentColor = ThemedColorSchemeKeyTokens.OnErrorContainer
                    ) {
                        viewModel.launchOnIO {
                            if (dialogState.confirm(title = context.getString(R.string.delete), text = context.getString(R.string.delete_desc))) {
                                viewModel.emitIntent(IndexUiIntent.Delete(mediaEntity = media))
                                withMainContext {
                                    navController.popBackStack()
                                }
                            }
                        }
                    }
                }
                Title(title = stringResource(id = R.string.backup_parts)) {
                    Switchable(
                        enabled = media.extraInfo.existed && media.path.isNotEmpty(),
                        checked = media.extraInfo.activated,
                        icon = ImageVector.vectorResource(id = R.drawable.ic_rounded_image),
                        title = DataType.MEDIA_MEDIA.type.uppercase(),
                        checkedText = if (uiState.isCalculating)
                            joinOf(
                                media.displayStatsBytes.formatSize(),
                                SymbolUtil.DOT.toString(),
                                stringResource(id = R.string.calculating),
                            )
                        else
                            media.displayStatsBytes.formatSize(),
                    ) {
                        viewModel.emitIntentOnIO(IndexUiIntent.UpdateMedia(media.copy(extraInfo = media.extraInfo.copy(activated = media.extraInfo.activated.not()))))
                    }
                }
            }
        }
    }
}
