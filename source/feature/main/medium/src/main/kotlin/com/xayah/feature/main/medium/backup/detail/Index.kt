package com.xayah.feature.main.medium.backup.detail

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.datastore.ConstantUtil
import com.xayah.core.model.DataType
import com.xayah.core.model.util.formatSize
import com.xayah.core.ui.component.BodyMediumText
import com.xayah.core.ui.component.LocalSlotScope
import com.xayah.core.ui.component.MediaIconImage
import com.xayah.core.ui.component.Switchable
import com.xayah.core.ui.component.Title
import com.xayah.core.ui.component.TitleLargeText
import com.xayah.core.ui.component.confirm
import com.xayah.core.ui.component.paddingHorizontal
import com.xayah.core.ui.component.paddingStart
import com.xayah.core.ui.component.paddingTop
import com.xayah.core.ui.component.paddingVertical
import com.xayah.core.ui.theme.ThemedColorSchemeKeyTokens
import com.xayah.core.ui.theme.value
import com.xayah.core.ui.theme.withState
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
fun PageMediumBackupDetail() {
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
        actions = {},
    ) {
        mediaState?.also { media ->
            val enabled by remember(media.path) {
                mutableStateOf((media.path !in ConstantUtil.DefaultMediaList.map { it.second }) || media.path.isEmpty())
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                Row(
                    modifier = Modifier
                        .paddingHorizontal(SizeTokens.Level24)
                        .paddingVertical(SizeTokens.Level12),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level24)
                ) {
                    MediaIconImage(name = uiState.name.firstOrNull()?.toString() ?: "", textStyle = MaterialTheme.typography.titleLarge, size = SizeTokens.Level64)
                    Column(modifier = Modifier.weight(1f)) {
                        TitleLargeText(text = media.name, color = ThemedColorSchemeKeyTokens.OnSurface.value)
                        BodyMediumText(text = media.path, color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value)
                    }
                }
                Title(title = stringResource(id = R.string.backup_parts)) {
                    Switchable(
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
                TextButton(enabled = enabled, modifier = Modifier
                    .paddingStart(SizeTokens.Level12)
                    .paddingTop(SizeTokens.Level12), onClick = {
                    viewModel.launchOnIO {
                        if (dialogState.confirm(title = context.getString(R.string.delete), text = context.getString(R.string.delete_desc))) {
                            viewModel.emitIntent(IndexUiIntent.Delete(mediaEntity = media))
                            withMainContext {
                                navController.popBackStack()
                            }
                        }
                    }
                }) {
                    Text(
                        text = stringResource(id = R.string.delete), color = ThemedColorSchemeKeyTokens.Error.value.withState(enabled)
                    )
                }
            }
        }
    }
}
