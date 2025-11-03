package com.xayah.databackup.feature.backup.apps

import android.content.Context
import android.content.pm.UserInfo
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.xayah.databackup.R
import com.xayah.databackup.database.entity.App
import com.xayah.databackup.rootservice.RemoteRootService
import com.xayah.databackup.ui.component.FadeVisibility
import com.xayah.databackup.ui.component.FilterButton
import com.xayah.databackup.ui.component.SearchTextField
import com.xayah.databackup.ui.component.SelectableChip
import com.xayah.databackup.ui.component.defaultLargeTopAppBarColors
import com.xayah.databackup.ui.component.filterButtonSecondaryColors
import com.xayah.databackup.ui.component.horizontalFadingEdges
import com.xayah.databackup.ui.component.verticalFadingEdges
import com.xayah.databackup.ui.material3.ModalDropdownMenu
import com.xayah.databackup.ui.material3.ModalDropdownMenuItem
import com.xayah.databackup.util.DefStorageSize
import com.xayah.databackup.util.FilterBackupUser
import com.xayah.databackup.util.FiltersSystemAppsBackup
import com.xayah.databackup.util.FiltersUserAppsBackup
import com.xayah.databackup.util.KeyFiltersSystemAppsBackup
import com.xayah.databackup.util.KeyFiltersUserAppsBackup
import com.xayah.databackup.util.KeySortsTypeBackup
import com.xayah.databackup.util.LaunchedEffect
import com.xayah.databackup.util.SortsSequence
import com.xayah.databackup.util.SortsSequenceBackup
import com.xayah.databackup.util.SortsType
import com.xayah.databackup.util.SortsTypeBackup
import com.xayah.databackup.util.formatToStorageSize
import com.xayah.databackup.util.popBackStackSafely
import com.xayah.databackup.util.readBoolean
import com.xayah.databackup.util.readEnum
import com.xayah.databackup.util.readInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.koin.androidx.compose.koinViewModel

@Composable
fun BackupAppsScreen(
    navController: NavHostController,
    viewModel: AppsViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val searchScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val allSelected by viewModel.allSelected.collectAsStateWithLifecycle()
    val selectedBytes by viewModel.selectedBytes.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val filterSheetState = rememberModalBottomSheetState()
    var showFilterSheet by remember { mutableStateOf(false) }
    val searchText by viewModel.searchText.collectAsStateWithLifecycle()
    var onSearch by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val lazyListState = rememberLazyListState()

    var showStartEdge by remember { mutableStateOf(false) }
    var showEndEdge by remember { mutableStateOf(false) }
    val startEdgeRange: Float by animateFloatAsState(if (showStartEdge) 1f else 0f, label = "alpha")
    val endEdgeRange: Float by animateFloatAsState(if (showEndEdge) 1f else 0f, label = "alpha")
    LaunchedEffect(context = Dispatchers.Default, lazyListState.canScrollBackward) {
        showStartEdge = lazyListState.canScrollBackward
    }
    LaunchedEffect(context = Dispatchers.Default, lazyListState.canScrollForward) {
        showEndEdge = lazyListState.canScrollForward
    }

    LaunchedEffect(onSearch) {
        if (onSearch) {
            focusRequester.requestFocus()
        }
    }

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .fillMaxSize(),
        topBar = {
            AnimatedContent(onSearch) { target ->
                if (target) {
                    TopAppBar(
                        title = {
                            SearchTextField(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = 8.dp)
                                    .focusRequester(focusRequester),
                                value = searchText,
                                onClose = {
                                    onSearch = false
                                    viewModel.changeSearchText("")
                                }
                            ) { viewModel.changeSearchText(it) }
                        },
                        actions = {
                            IconButton(onClick = { showFilterSheet = true }) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_funnel),
                                    contentDescription = stringResource(R.string.filters)
                                )
                            }
                            SelectIconButton(viewModel = viewModel)
                        },
                        scrollBehavior = searchScrollBehavior,
                    )
                } else {
                    LargeTopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = stringResource(R.string.select_apps),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (selectedBytes == DefStorageSize) {
                                        stringResource(R.string.items_selected, allSelected, apps.size)
                                    } else {
                                        stringResource(R.string.items_selected_and_size, allSelected, apps.size, selectedBytes)
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStackSafely() }) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_left),
                                    contentDescription = stringResource(R.string.back)
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { onSearch = true }) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_search),
                                    contentDescription = stringResource(R.string.search)
                                )
                            }
                            IconButton(onClick = { showFilterSheet = true }) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_funnel),
                                    contentDescription = stringResource(R.string.filters)
                                )
                            }
                            SelectIconButton(viewModel = viewModel)
                        },
                        scrollBehavior = scrollBehavior,
                        colors = TopAppBarDefaults.defaultLargeTopAppBarColors(),
                    )
                }
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier) {
            Spacer(modifier = Modifier.size(innerPadding.calculateTopPadding()))

            AnimatedContent(targetState = apps.isEmpty()) { isAppsEmpty ->
                if (isAppsEmpty) {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            modifier = Modifier.size(300.dp),
                            imageVector = ImageVector.vectorResource(R.drawable.img_empty),
                            contentDescription = stringResource(R.string.it_is_empty)
                        )
                        Text(
                            text = stringResource(R.string.it_is_empty),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.verticalFadingEdges(startEdgeRange, endEdgeRange),
                        state = lazyListState
                    ) {
                        items(items = apps, key = { it.pkgUserKey }) { app ->
                            AppListItem(
                                modifier = Modifier.animateItem(),
                                context = context,
                                scope = scope,
                                app = app,
                                viewModel = viewModel
                            )
                        }

                        item(key = "-1") {
                            Spacer(modifier = Modifier.size(innerPadding.calculateBottomPadding()))
                        }
                    }
                }
            }
        }

        if (showFilterSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showFilterSheet = false
                },
                sheetState = filterSheetState
            ) {
                val filterUser by context.readInt(FilterBackupUser).collectAsStateWithLifecycle(initialValue = FilterBackupUser.second)
                val sortsType by context.readEnum(SortsTypeBackup).collectAsStateWithLifecycle(initialValue = SortsTypeBackup.second)
                val sequenceBackup by context.readEnum(SortsSequenceBackup).collectAsStateWithLifecycle(initialValue = SortsSequenceBackup.second)
                val filtersUserApps by context.readBoolean(FiltersUserAppsBackup)
                    .collectAsStateWithLifecycle(initialValue = FiltersUserAppsBackup.second)
                val filtersSystemApps by context.readBoolean(FiltersSystemAppsBackup)
                    .collectAsStateWithLifecycle(initialValue = FiltersSystemAppsBackup.second)
                var users by remember { mutableStateOf(listOf<UserInfo>()) }
                LaunchedEffect(context = Dispatchers.Default, null) {
                    users = RemoteRootService.getUsers()
                }

                Column {
                    Text(
                        modifier = Modifier.padding(start = 24.dp),
                        text = stringResource(R.string.users),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val usersScrollState = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .padding(vertical = 24.dp)
                            .horizontalScroll(usersScrollState)
                            .horizontalFadingEdges(usersScrollState),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Spacer(modifier = Modifier.size(0.dp))
                        users.forEach {
                            FilterButton(it.id == filterUser, it.name, "${it.id}", ImageVector.vectorResource(R.drawable.ic_book_user)) {
                                viewModel.changeUser(filterUser, it)
                            }
                        }
                        Spacer(modifier = Modifier.size(0.dp))
                    }
                    HorizontalDivider(
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 12.dp)
                    )

                    Text(
                        modifier = Modifier.padding(start = 24.dp),
                        text = stringResource(R.string.sorts),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val sortsScrollState = rememberScrollState()

                    Row(modifier = Modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
                        Spacer(modifier = Modifier.width(24.dp))

                        val isAscending by remember(sequenceBackup) { mutableStateOf(sequenceBackup == SortsSequence.ASCENDING) }
                        val animatedIcon = rememberAnimatedVectorPainter(
                            animatedImageVector = AnimatedImageVector.animatedVectorResource(R.drawable.ic_animted_arrow_up_down_a_z),
                            atEnd = isAscending
                        )

                        FilterButton(
                            selected = true,
                            title = if (isAscending) stringResource(R.string.ascending) else stringResource(R.string.descending),
                            colors = filterButtonSecondaryColors(),
                            icon = animatedIcon
                        ) {
                            viewModel.changeSequence(sequenceBackup)
                        }

                        Spacer(modifier = Modifier.width(24.dp))

                        VerticalDivider(modifier = Modifier.padding(vertical = 24.dp))

                        Row(
                            modifier = Modifier
                                .padding(vertical = 24.dp)
                                .horizontalScroll(sortsScrollState)
                                .horizontalFadingEdges(sortsScrollState),
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            val a2zSelected by remember(sortsType) { mutableStateOf(sortsType == SortsType.A2Z) }
                            val dataSizeSelected by remember(sortsType) { mutableStateOf(sortsType == SortsType.DATA_SIZE) }
                            val installTimeSelected by remember(sortsType) { mutableStateOf(sortsType == SortsType.INSTALL_TIME) }
                            val updateTimeSelected by remember(sortsType) { mutableStateOf(sortsType == SortsType.UPDATE_TIME) }

                            Spacer(modifier = Modifier.size(0.dp))

                            FilterButton(
                                selected = a2zSelected,
                                title = stringResource(R.string.a2z),
                                icon = ImageVector.vectorResource(R.drawable.ic_book_a)
                            ) {
                                viewModel.changeSort(a2zSelected, KeySortsTypeBackup, SortsType.A2Z)
                            }
                            FilterButton(
                                selected = dataSizeSelected,
                                title = stringResource(R.string.data_size),
                                icon = ImageVector.vectorResource(R.drawable.ic_book_text)
                            ) {
                                viewModel.changeSort(dataSizeSelected, KeySortsTypeBackup, SortsType.DATA_SIZE)
                            }
                            FilterButton(
                                selected = installTimeSelected,
                                title = stringResource(R.string.install_time),
                                icon = ImageVector.vectorResource(R.drawable.ic_book_down)
                            ) {
                                viewModel.changeSort(installTimeSelected, KeySortsTypeBackup, SortsType.INSTALL_TIME)
                            }
                            FilterButton(
                                selected = updateTimeSelected,
                                title = stringResource(R.string.update_time),
                                icon = ImageVector.vectorResource(R.drawable.ic_book_up)
                            ) {
                                viewModel.changeSort(updateTimeSelected, KeySortsTypeBackup, SortsType.UPDATE_TIME)
                            }

                            Spacer(modifier = Modifier.size(0.dp))
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 12.dp)
                    )

                    Text(
                        modifier = Modifier.padding(start = 24.dp),
                        text = stringResource(R.string.filters),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val filtersScrollState = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .padding(vertical = 24.dp)
                            .horizontalScroll(filtersScrollState)
                            .horizontalFadingEdges(filtersScrollState),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Spacer(modifier = Modifier.size(0.dp))
                        FilterButton(
                            selected = filtersUserApps,
                            title = stringResource(R.string.user_apps),
                            icon = ImageVector.vectorResource(R.drawable.ic_resource_package)
                        ) {
                            if (filtersUserApps.not() || filtersSystemApps) {
                                viewModel.changeFilter(KeyFiltersUserAppsBackup, filtersUserApps.not())
                            }
                        }
                        FilterButton(
                            selected = filtersSystemApps,
                            title = stringResource(R.string.system_apps),
                            icon = ImageVector.vectorResource(R.drawable.ic_package_2)
                        ) {
                            if (filtersSystemApps.not() || filtersUserApps) {
                                viewModel.changeFilter(KeyFiltersSystemAppsBackup, filtersSystemApps.not())
                            }
                        }
                        Spacer(modifier = Modifier.size(0.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AppListItem(
    modifier: Modifier,
    context: Context,
    scope: CoroutineScope,
    app: App,
    viewModel: AppsViewModel,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        onClick = { viewModel.selectAll(app.packageName, app.userId, app.toggleableState) },
    ) {
        var expanded by remember { mutableStateOf(false) }

        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(colorResource(id = R.color.ic_launcher_background)),
                    contentAlignment = Alignment.Center
                ) {
                    var icon: Drawable? by remember { mutableStateOf(null) }
                    LaunchedEffect(context = Dispatchers.IO, app.pkgUserKey) {
                        icon = runCatching { context.packageManager.getApplicationIcon(app.packageName) }.getOrNull()
                        if (icon == null) {
                            icon = AppCompatResources.getDrawable(context, android.R.drawable.sym_def_app_icon)
                        }
                    }
                    AsyncImage(
                        modifier = Modifier.size(32.dp),
                        model = ImageRequest.Builder(context)
                            .data(icon)
                            .crossfade(true)
                            .build(),
                        contentDescription = null
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.info.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = app.packageName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val storage by remember(app.selectedBytes, app.totalBytes) {
                        mutableStateOf("${app.selectedBytes.formatToStorageSize} / ${app.totalBytes.formatToStorageSize}")
                    }
                    FadeVisibility(visible = app.totalBytes != 0L) {
                        Text(
                            text = storage,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                val animatedCheckIcon = rememberAnimatedVectorPainter(
                    animatedImageVector = AnimatedImageVector.animatedVectorResource(R.drawable.ic_animated_chevron_right_to_down),
                    atEnd = expanded
                )
                IconButton(onClick = { expanded = expanded.not() }) {
                    Icon(
                        painter = animatedCheckIcon,
                        contentDescription = if (expanded) stringResource(R.string.collapsed) else stringResource(R.string.expand)
                    )
                }

                TriStateCheckbox(
                    state = app.toggleableState,
                    onClick = { viewModel.selectAll(app.packageName, app.userId, app.toggleableState) }
                )
            }
            FadeVisibility(expanded) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Spacer(modifier = Modifier.width(6.dp))

                    SelectableChip(
                        selected = app.option.apk,
                        icon = AnimatedImageVector.animatedVectorResource(R.drawable.ic_animated_resource_package),
                        text = stringResource(R.string.apk),
                        onCheckedChange = { viewModel.selectApk(app.packageName, app.userId, it.not()) },
                    )

                    SelectableChip(
                        selected = app.option.internalData,
                        icon = AnimatedImageVector.animatedVectorResource(R.drawable.ic_animated_user),
                        text = stringResource(R.string.internal_data),
                        onCheckedChange = { viewModel.selectInternalData(app.packageName, app.userId, it.not()) },
                    )

                    SelectableChip(
                        selected = app.option.externalData,
                        icon = AnimatedImageVector.animatedVectorResource(R.drawable.ic_animated_database),
                        text = stringResource(R.string.external_data),
                        onCheckedChange = { viewModel.selectExternalData(app.packageName, app.userId, it.not()) },
                    )

                    SelectableChip(
                        selected = app.option.additionalData,
                        icon = AnimatedImageVector.animatedVectorResource(R.drawable.ic_animated_gamepad_2),
                        text = stringResource(R.string.additional_data),
                        onCheckedChange = { viewModel.selectAdditionalData(app.packageName, app.userId, it.not()) },
                    )

                    Spacer(modifier = Modifier.width(6.dp))
                }
            }
        }
    }
}

@Composable
private fun SelectIconButton(viewModel: AppsViewModel) {
    var mainExpanded by remember { mutableStateOf(false) }
    var customExpanded by remember { mutableStateOf(false) }

    val apkAllSelected by viewModel.apkAllSelected.collectAsStateWithLifecycle()
    val dataAllSelected by viewModel.dataAllSelected.collectAsStateWithLifecycle()
    val intDataAllSelected by viewModel.intDataAllSelected.collectAsStateWithLifecycle()
    val extDataAllSelected by viewModel.extDataAllSelected.collectAsStateWithLifecycle()
    val addlDataAllSelected by viewModel.addlDataAllSelected.collectAsStateWithLifecycle()

    Box {
        IconButton(onClick = {
            mainExpanded = !mainExpanded
        }) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_list_checks),
                contentDescription = stringResource(R.string.select_all)
            )
        }
        ModalDropdownMenu(
            expanded = mainExpanded,
            onDismissRequest = { mainExpanded = false }
        ) {
            ModalDropdownMenuItem(
                text = { Text(if (apkAllSelected.not()) stringResource(R.string.select_all_apk) else stringResource(R.string.unselect_all_apk)) },
                leadingIcon = {
                    Icon(
                        imageVector = if (apkAllSelected.not())
                            ImageVector.vectorResource(R.drawable.ic_square_check_big)
                        else
                            ImageVector.vectorResource(R.drawable.ic_square),
                        contentDescription = if (apkAllSelected.not()) stringResource(R.string.select_all_apk) else stringResource(R.string.unselect_all_apk)
                    )
                },
                onClick = {
                    viewModel.selectAllApk()
                }
            )
            ModalDropdownMenuItem(
                text = { Text(if (dataAllSelected.not()) stringResource(R.string.select_all_data) else stringResource(R.string.unselect_all_data)) },
                leadingIcon = {
                    Icon(
                        imageVector = if (dataAllSelected.not())
                            ImageVector.vectorResource(R.drawable.ic_square_check_big)
                        else
                            ImageVector.vectorResource(R.drawable.ic_square),
                        contentDescription = if (dataAllSelected.not()) stringResource(R.string.select_all_data) else stringResource(R.string.unselect_all_data)
                    )
                },
                onClick = {
                    viewModel.selectAllData()
                }
            )
            HorizontalDivider()
            ModalDropdownMenuItem(
                text = { Text(stringResource(R.string.custom_selection)) },
                trailingIcon = {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_right),
                        contentDescription = stringResource(R.string.custom_selection)
                    )
                },
                onClick = {
                    mainExpanded = false
                    customExpanded = true
                }
            )
        }

        ModalDropdownMenu(
            expanded = customExpanded,
            onDismissRequest = { customExpanded = false }
        ) {
            ModalDropdownMenuItem(
                text = { Text(stringResource(R.string.word_return)) },
                leadingIcon = {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_left),
                        contentDescription = stringResource(R.string.word_return)
                    )
                },
                onClick = {
                    mainExpanded = true
                    customExpanded = false
                }
            )
            HorizontalDivider()
            ModalDropdownMenuItem(
                text = { Text(if (apkAllSelected.not()) stringResource(R.string.select_all_apk) else stringResource(R.string.unselect_all_apk)) },
                leadingIcon = {
                    Icon(
                        imageVector = if (apkAllSelected.not())
                            ImageVector.vectorResource(R.drawable.ic_square_check_big)
                        else
                            ImageVector.vectorResource(R.drawable.ic_square),
                        contentDescription = if (apkAllSelected.not()) stringResource(R.string.select_all_apk) else stringResource(R.string.unselect_all_apk)
                    )
                },
                onClick = {
                    viewModel.selectAllApk()
                }
            )
            ModalDropdownMenuItem(
                text = { Text(if (intDataAllSelected.not()) stringResource(R.string.select_all_int_data) else stringResource(R.string.unselect_all_int_data)) },
                leadingIcon = {
                    Icon(
                        imageVector = if (intDataAllSelected.not())
                            ImageVector.vectorResource(R.drawable.ic_square_check_big)
                        else
                            ImageVector.vectorResource(R.drawable.ic_square),
                        contentDescription = if (intDataAllSelected.not()) stringResource(R.string.select_all_int_data) else stringResource(R.string.unselect_all_int_data)
                    )
                },
                onClick = {
                    viewModel.selectAllIntData()
                }
            )
            ModalDropdownMenuItem(
                text = { Text(if (extDataAllSelected.not()) stringResource(R.string.select_all_ext_data) else stringResource(R.string.unselect_all_ext_data)) },
                leadingIcon = {
                    Icon(
                        imageVector = if (extDataAllSelected.not())
                            ImageVector.vectorResource(R.drawable.ic_square_check_big)
                        else
                            ImageVector.vectorResource(R.drawable.ic_square),
                        contentDescription = if (extDataAllSelected.not()) stringResource(R.string.select_all_ext_data) else stringResource(R.string.unselect_all_ext_data)
                    )
                },
                onClick = {
                    viewModel.selectAllExtData()
                }
            )
            ModalDropdownMenuItem(
                text = { Text(if (addlDataAllSelected.not()) stringResource(R.string.select_all_addl_data) else stringResource(R.string.unselect_all_addl_data)) },
                leadingIcon = {
                    Icon(
                        imageVector = if (addlDataAllSelected.not())
                            ImageVector.vectorResource(R.drawable.ic_square_check_big)
                        else
                            ImageVector.vectorResource(R.drawable.ic_square),
                        contentDescription = if (addlDataAllSelected.not()) stringResource(R.string.select_all_addl_data) else stringResource(R.string.unselect_all_addl_data)
                    )
                },
                onClick = {
                    viewModel.selectAllAddlData()
                }
            )
        }
    }
}
