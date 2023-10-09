package com.xayah.databackup.ui.component

import androidx.activity.ComponentActivity
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.navigation.NavHostController
import com.xayah.databackup.R
import com.xayah.databackup.ui.activity.main.page.cloud.router.CloudRoutes
import com.xayah.databackup.ui.activity.main.page.log.LogViewModel
import com.xayah.databackup.ui.activity.main.page.tree.TreeViewModel
import com.xayah.databackup.ui.activity.main.router.MainRoutes
import com.xayah.databackup.ui.activity.main.router.currentRoute
import com.xayah.databackup.ui.component.material3.Tab
import com.xayah.databackup.ui.component.material3.TabRow
import com.xayah.databackup.ui.theme.ColorScheme
import com.xayah.databackup.ui.token.AnimationTokens
import com.xayah.databackup.ui.token.CommonTokens
import com.xayah.databackup.ui.token.MenuTokens
import com.xayah.databackup.ui.token.TopBarTokens
import com.xayah.databackup.util.ConstantUtil
import com.xayah.databackup.util.DateUtil
import kotlinx.coroutines.launch
import androidx.compose.material3.ColorScheme as MaterialColorScheme

@Composable
fun GuideTopBar(title: String, icon: ImageVector) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ColorScheme.onSurfaceVariant(),
            modifier = Modifier
                .size(CommonTokens.IconMediumSize)
                .paddingBottom(CommonTokens.PaddingSmall)
        )
        TopBarTitle(text = title)
    }
}

@ExperimentalMaterial3Api
@Composable
fun MainTopBar(scrollBehavior: TopAppBarScrollBehavior?) {
    val context = LocalContext.current
    val navController = LocalSlotScope.current!!.navController
    val routes = ConstantUtil.MainBottomBarRoutes
    val currentRoute = navController.currentRoute()

    CenterAlignedTopAppBar(
        title = { TopBarTitle(text = MainRoutes.ofTitle(context, currentRoute)) },
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            Crossfade(targetState = currentRoute, label = AnimationTokens.CrossFadeLabel) { route ->
                if ((route in routes).not())
                    ArrowBackButton {
                        navController.popBackStack()
                    }
            }
        },
    )
}

@ExperimentalMaterial3Api
@Composable
fun CloudTopBar(scrollBehavior: TopAppBarScrollBehavior?, cloudNavController: NavHostController) {
    val context = LocalContext.current
    val routes = ConstantUtil.MainBottomBarRoutes
    val currentRoute = cloudNavController.currentRoute()

    CenterAlignedTopAppBar(
        title = { TopBarTitle(text = CloudRoutes.ofTitle(context, currentRoute)) },
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            if (currentRoute != CloudRoutes.Main.route) {
                Crossfade(targetState = currentRoute, label = AnimationTokens.CrossFadeLabel) { route ->
                    if ((route in routes).not())
                        ArrowBackButton {
                            cloudNavController.popBackStack()
                        }
                }
            }
        },
        actions = {}
    )
}

@ExperimentalMaterial3Api
@Composable
fun TreeTopBar(scrollBehavior: TopAppBarScrollBehavior?, viewModel: TreeViewModel) {
    val context = LocalContext.current
    val navController = LocalSlotScope.current!!.navController
    val routes = ConstantUtil.MainBottomBarRoutes
    val currentRoute = navController.currentRoute()

    CenterAlignedTopAppBar(
        title = { TopBarTitle(text = MainRoutes.ofTitle(context, currentRoute)) },
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            Crossfade(targetState = currentRoute, label = AnimationTokens.CrossFadeLabel) { route ->
                if ((route in routes).not())
                    ArrowBackButton {
                        navController.popBackStack()
                    }
            }
        },
        actions = {
            val scope = rememberCoroutineScope()
            var expanded by remember { mutableStateOf(false) }
            val uiState by viewModel.uiState
            val typeList = uiState.typeList
            val typeStringList = uiState.typeList.map { it.ofString(context) }
            val selectedIndex = uiState.selectedIndex

            Box(
                modifier = Modifier.wrapContentSize(Alignment.TopStart)
            ) {
                IconButton(onClick = { expanded = true }) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_rounded_filter_list),
                        contentDescription = null
                    )
                }
                ModalStringListDropdownMenu(
                    expanded = expanded,
                    selectedIndex = selectedIndex,
                    list = typeStringList,
                    onSelected = { index, _ ->
                        scope.launch {
                            expanded = false
                            viewModel.setTreeType(context = context, type = typeList[index])
                        }
                    },
                    onDismissRequest = { expanded = false })
            }
        }
    )
}

@ExperimentalMaterial3Api
@Composable
fun LogTopBar(scrollBehavior: TopAppBarScrollBehavior?, viewModel: LogViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState
    val dateList = uiState.startTimestamps.map { timestamp -> DateUtil.formatTimestamp(timestamp) }
    val selectedIndex = uiState.selectedIndex
    val navController = LocalSlotScope.current!!.navController
    val routes = ConstantUtil.MainBottomBarRoutes
    val currentRoute = navController.currentRoute()

    CenterAlignedTopAppBar(
        title = { TopBarTitle(text = MainRoutes.ofTitle(context, currentRoute)) },
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            Crossfade(targetState = currentRoute, label = AnimationTokens.CrossFadeLabel) { route ->
                if ((route in routes).not())
                    ArrowBackButton {
                        navController.popBackStack()
                    }
            }
        },
        actions = {
            var expanded by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier.wrapContentSize(Alignment.TopStart)
            ) {
                IconButton(onClick = { if (dateList.isNotEmpty()) expanded = true }) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_rounded_unfold_more),
                        contentDescription = null
                    )
                }
                ModalStringListDropdownMenu(
                    expanded = expanded,
                    selectedIndex = selectedIndex,
                    list = dateList,
                    maxDisplay = MenuTokens.DefaultMaxDisplay,
                    onSelected = { index, _ ->
                        scope.launch {
                            expanded = false
                            viewModel.setSelectedIndex(index)
                        }
                    },
                    onDismissRequest = { expanded = false })
            }
            IconButton(onClick = { scope.launch { viewModel.deleteCurrentLog() } }) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = null
                )
            }
        }
    )
}

@ExperimentalMaterial3Api
@Composable
fun ListTopBar(scrollBehavior: TopAppBarScrollBehavior, title: String) {
    val context = LocalContext.current
    CenterAlignedTopAppBar(
        title = { TopBarTitle(text = title) },
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            ArrowBackButton {
                (context as ComponentActivity).finish()
            }
        },
    )
}

@ExperimentalMaterial3Api
@Composable
fun ListSelectionModeTopBar(
    scrollBehavior: TopAppBarScrollBehavior,
    title: String,
    onArrowBackPressed: () -> Unit,
    onCheckListPressed: () -> Unit,
    chipContent: @Composable RowScope.() -> Unit,
) {
    val navController = LocalSlotScope.current!!.navController
    CustomTopAppBar(scrollBehavior) { _ ->
        Box(
            modifier = Modifier
                .paddingTop(TopBarTokens.VerticalPadding)
                .paddingHorizontal(TopBarTokens.HorizontalPadding),
            contentAlignment = Alignment.CenterStart
        ) {
            ArrowBackButton(modifier = Modifier.align(Alignment.CenterStart), onClick = onArrowBackPressed)
            TopBarTitle(modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, text = title)
            CheckListButton(modifier = Modifier.align(Alignment.CenterEnd), onClick = onCheckListPressed)
        }
        Row(
            modifier = Modifier.paddingHorizontal(CommonTokens.PaddingMedium),
            horizontalArrangement = Arrangement.spacedBy(CommonTokens.PaddingMedium),
            content = chipContent
        )
    }
}

fun MaterialColorScheme.applyTonalElevation(backgroundColor: Color, elevation: Dp): Color {
    return if (backgroundColor == surface) {
        surfaceColorAtElevation(elevation)
    } else {
        backgroundColor
    }
}

@Composable
internal fun containerColor(
    containerColor: Color, scrolledContainerColor: Color, colorTransitionFraction: Float
): Color {
    return lerp(
        containerColor,
        scrolledContainerColor,
        FastOutLinearInEasing.transform(colorTransitionFraction)
    )
}

@ExperimentalMaterial3Api
@Composable
internal fun CustomTopAppBar(scrollBehavior: TopAppBarScrollBehavior, content: @Composable (appBarContainerColor: Color) -> Unit = {}) {
    // Sets the app bar's height offset to collapse the entire bar's height when content is
    // scrolled.
    val heightOffsetLimit = with(LocalDensity.current) { -TopBarTokens.ContainerHeight.toPx() }
    SideEffect {
        if (scrollBehavior.state.heightOffsetLimit != heightOffsetLimit) {
            scrollBehavior.state.heightOffsetLimit = heightOffsetLimit
        }
    }

    val containerColor = MaterialTheme.colorScheme.surface
    val scrolledContainerColor = MaterialTheme.colorScheme.applyTonalElevation(
        backgroundColor = containerColor,
        elevation = TopBarTokens.OnScrollContainerElevation
    )

    // Obtain the container color from the TopAppBarColors using the `overlapFraction`. This
    // ensures that the colors will adjust whether the app bar behavior is pinned or scrolled.
    // This may potentially animate or interpolate a transition between the container-color and the
    // container's scrolled-color according to the app bar's scroll state.
    val colorTransitionFraction = scrollBehavior.state.overlappedFraction
    val fraction = if (colorTransitionFraction > 0.01f) 1f else 0f
    val appBarContainerColor by animateColorAsState(
        targetValue = containerColor(containerColor, scrolledContainerColor, fraction),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "ColorAnimation"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        color = appBarContainerColor
    ) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(TopAppBarDefaults.windowInsets)
                // clip after padding so we don't show the title over the inset area
                .clipToBounds()
        ) {
            content(appBarContainerColor = appBarContainerColor)
        }
    }
}

@ExperimentalMaterial3Api
@Composable
internal fun ColumnExtendedTopAppBar(
    scrollBehavior: TopAppBarScrollBehavior,
    title: String,
    onArrowBackPressed: () -> Unit,
    content: @Composable (appBarContainerColor: Color) -> Unit = {},
) {
    CustomTopAppBar(scrollBehavior) { appBarContainerColor ->
        Box(
            modifier = Modifier
                .paddingVertical(TopBarTokens.VerticalPadding)
                .paddingHorizontal(TopBarTokens.HorizontalPadding),
            contentAlignment = Alignment.CenterStart
        ) {
            ArrowBackButton(onClick = onArrowBackPressed)
            TopBarTitle(modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, text = title)
        }
        content(appBarContainerColor = appBarContainerColor)
    }
}

@ExperimentalMaterial3Api
@Composable
fun ManifestTopBar(
    scrollBehavior: TopAppBarScrollBehavior,
    title: String,
    selectedTabIndex: Int,
    onTabClick: (index: Int) -> Unit,
    titles: List<String>,
) {
    val navController = LocalSlotScope.current!!.navController
    ColumnExtendedTopAppBar(
        scrollBehavior = scrollBehavior,
        title = title,
        onArrowBackPressed = { navController.popBackStack() }
    ) { appBarContainerColor ->
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = appBarContainerColor,
            indicator = { tabPositions ->
                RoundedCornerIndicator(tabPositions = tabPositions, selectedTabIndex = selectedTabIndex, percent = 0.8f)
            },
            divider = {}
        ) {
            titles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { onTabClick(index) },
                    text = { TabText(text = title, color = if (selectedTabIndex == index) ColorScheme.onPrimary() else ColorScheme.primary()) }
                )
            }
        }
    }
}

@ExperimentalMaterial3Api
@Composable
fun ProcessingTopBar(scrollBehavior: TopAppBarScrollBehavior, title: String, onArrowBackPressed: () -> Unit) {
    CenterAlignedTopAppBar(
        title = { TopBarTitle(text = title) },
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            ArrowBackButton(onClick = onArrowBackPressed)
        },
    )
}

@ExperimentalMaterial3Api
@Composable
fun CompletionTopBar(scrollBehavior: TopAppBarScrollBehavior, title: String) {
    val context = LocalContext.current
    CenterAlignedTopAppBar(
        title = { TopBarTitle(text = title) },
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            ArrowBackButton {
                (context as ComponentActivity).finish()
            }
        },
    )
}

@ExperimentalMaterial3Api
@Composable
fun DirectoryTopBar(scrollBehavior: TopAppBarScrollBehavior?, title: String) {
    val context = LocalContext.current
    CenterAlignedTopAppBar(
        title = { TopBarTitle(text = title) },
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            ArrowBackButton {
                (context as ComponentActivity).finish()
            }
        },
    )
}

@ExperimentalMaterial3Api
@Composable
fun MediaListTopBar(
    scrollBehavior: TopAppBarScrollBehavior?,
    title: String,
    actionsVisible: Boolean,
    onCheckListPressed: (() -> Unit)?,
    onAddClick: (() -> Unit)?,
) {
    val context = LocalContext.current
    CenterAlignedTopAppBar(
        title = { TopBarTitle(text = title) },
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            ArrowBackButton {
                (context as ComponentActivity).finish()
            }
        },
        actions = {
            if (actionsVisible) {
                if (onCheckListPressed != null) CheckListButton(onClick = { onCheckListPressed.invoke() })
                if (onAddClick != null) IconButton(onClick = { onAddClick.invoke() }) { Icon(imageVector = Icons.Rounded.Add, contentDescription = null) }
            }
        }
    )
}
