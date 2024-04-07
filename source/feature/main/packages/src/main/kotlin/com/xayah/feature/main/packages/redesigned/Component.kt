package com.xayah.feature.main.packages.redesigned

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringArrayResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dotlottie.dlplayer.Mode
import com.lottiefiles.dotlottie.core.compose.ui.DotLottieAnimation
import com.lottiefiles.dotlottie.core.util.DotLottieSource
import com.xayah.core.model.database.PackageEntity
import com.xayah.core.ui.component.AnimatedLinearProgressIndicator
import com.xayah.core.ui.component.BodyLargeText
import com.xayah.core.ui.component.BodyMediumText
import com.xayah.core.ui.component.BodySmallText
import com.xayah.core.ui.component.ChipRow
import com.xayah.core.ui.component.Divider
import com.xayah.core.ui.component.FilterChip
import com.xayah.core.ui.component.InnerTopSpacer
import com.xayah.core.ui.component.LabelLargeText
import com.xayah.core.ui.component.MultipleSelectionFilterChip
import com.xayah.core.ui.component.PackageIconImage
import com.xayah.core.ui.component.SecondaryTopBar
import com.xayah.core.ui.component.SortChip
import com.xayah.core.ui.component.TitleLargeText
import com.xayah.core.ui.component.paddingEnd
import com.xayah.core.ui.component.paddingHorizontal
import com.xayah.core.ui.component.paddingVertical
import com.xayah.core.ui.material3.Surface
import com.xayah.core.ui.material3.toColor
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.RefreshState
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.fromDrawable
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.fromVector
import com.xayah.core.ui.util.value
import com.xayah.feature.main.packages.R
import com.xayah.feature.main.packages.redesigned.backup.IndexUiIntent
import com.xayah.feature.main.packages.redesigned.backup.IndexViewModel

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun ListScaffold(
    scrollBehavior: TopAppBarScrollBehavior,
    title: StringResourceToken,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (BoxScope.(innerPadding: PaddingValues) -> Unit)
) {
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SecondaryTopBar(
                scrollBehavior = scrollBehavior,
                title = title,
                actions = actions
            )
        },
    ) { innerPadding ->
        Column {
            InnerTopSpacer(innerPadding = innerPadding)

            Box(modifier = Modifier.weight(1f), content = {
                content(this, innerPadding)
            })
        }
    }
}

@Composable
fun DotLottieView(isRefreshing: Boolean, refreshState: RefreshState) {
    DotLottieAnimation(
        source = if (isRefreshing) DotLottieSource.Asset("loading.lottie") else DotLottieSource.Asset("squirrel.lottie"),
        autoplay = true,
        loop = true,
        playMode = Mode.FORWARD,
        modifier = Modifier.background(Color.Transparent)
    )
    if (isRefreshing) {
        BodyLargeText(text = refreshState.user, color = ColorSchemeKeyTokens.OnSurface.toColor())
        AnimatedLinearProgressIndicator(
            modifier = Modifier.paddingVertical(SizeTokens.Level8),
            progress = refreshState.progress,
            strokeCap = StrokeCap.Round
        )
        BodySmallText(text = refreshState.pkg, color = ColorSchemeKeyTokens.OnSurfaceVariant.toColor())
    } else {
        BodyLargeText(text = StringResourceToken.fromStringId(R.string.pull_down_to_refresh).value, color = ColorSchemeKeyTokens.OnSurfaceVariant.toColor())
    }
}

@ExperimentalFoundationApi
@Composable
fun PackageItem(item: PackageEntity, onCheckedChange: ((Boolean) -> Unit)?, onClick: () -> Unit) {
    Surface(onClick = onClick) {
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .padding(SizeTokens.Level16),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level16)
        ) {
            PackageIconImage(packageName = item.packageName, label = "${item.packageInfo.label.firstOrNull() ?: ""}")
            Column(modifier = Modifier.weight(1f)) {
                TitleLargeText(text = item.packageInfo.label, color = ColorSchemeKeyTokens.OnSurface.toColor())
                BodyMediumText(text = "${item.packageName} (${item.packageInfo.versionName})", color = ColorSchemeKeyTokens.OnSurfaceVariant.toColor())
            }
            Divider(
                modifier = Modifier
                    .height(SizeTokens.Level36)
                    .width(SizeTokens.Level1)
                    .fillMaxHeight()
            )
            Checkbox(checked = item.extraInfo.activated, onCheckedChange = onCheckedChange)
        }
    }
}

@ExperimentalMaterial3Api
@Composable
fun FilterModalBottomSheetContent(onDismissRequest: () -> Unit) {
    val viewModel = hiltViewModel<IndexViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val flagIndexState by viewModel.flagIndexState.collectAsStateWithLifecycle()
    val userIdIndexListState by viewModel.userIdIndexListState.collectAsStateWithLifecycle()
    val sortIndexState by viewModel.sortIndexState.collectAsStateWithLifecycle()
    val sortTypeState by viewModel.sortTypeState.collectAsStateWithLifecycle()
    Column {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .paddingHorizontal(SizeTokens.Level24),
            horizontalAlignment = Alignment.End
        ) {
            val onSelectAll = {
                viewModel.emitIntent(IndexUiIntent.SelectAll(uiState.selectAll.not()))
                viewModel.emitState(uiState.copy(selectAll = uiState.selectAll.not()))
                onDismissRequest()
            }
            Surface(onClick = { onSelectAll() }, color = Color.Transparent, shape = CircleShape) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = uiState.selectAll, onCheckedChange = { onSelectAll() })
                    BodyLargeText(modifier = Modifier.paddingEnd(SizeTokens.Level16), text = StringResourceToken.fromStringId(R.string.select_all).value)
                }
            }
        }

        LabelLargeText(modifier = Modifier.paddingHorizontal(SizeTokens.Level24), text = StringResourceToken.fromStringId(R.string.filters).value)
        ChipRow(horizontalSpace = SizeTokens.Level24) {
            MultipleSelectionFilterChip(
                enabled = true,
                leadingIcon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_person),
                label = StringResourceToken.fromStringId(R.string.user),
                selectedIndexList = userIdIndexListState,
                list = uiState.userIdList.map { it.toString() },
                onSelected = { indexList ->
                    if (indexList.isNotEmpty()) {
                        viewModel.emitIntent(IndexUiIntent.SetUserIdIndexList(indexList))
                    }
                },
                onClick = {
                    viewModel.emitIntent(IndexUiIntent.GetUserIds)
                }
            )

            FilterChip(
                enabled = true,
                leadingIcon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_deployed_code),
                selectedIndex = flagIndexState,
                list = stringArrayResource(id = R.array.flag_type_items).toList(),
                onSelected = { index, _ ->
                    viewModel.emitIntent(IndexUiIntent.FilterByFlag(index = index))
                },
                onClick = {}
            )
        }
        LabelLargeText(modifier = Modifier.paddingHorizontal(SizeTokens.Level24), text = StringResourceToken.fromStringId(R.string.sort).value)
        ChipRow(horizontalSpace = SizeTokens.Level24) {
            SortChip(
                enabled = true,
                leadingIcon = ImageVectorToken.fromVector(Icons.Rounded.Sort),
                selectedIndex = sortIndexState,
                type = sortTypeState,
                list = stringArrayResource(id = R.array.backup_sort_type_items).toList(),
                onSelected = { index, _ ->
                    viewModel.emitIntent(IndexUiIntent.Sort(index = index, type = sortTypeState))
                },
                onClick = {}
            )
        }
    }
}
