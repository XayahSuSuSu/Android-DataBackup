package com.xayah.databackup.feature.backup.apps

import android.content.pm.UserInfo
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.xayah.databackup.R
import com.xayah.databackup.rootservice.RemoteRootService
import com.xayah.databackup.ui.component.AppListItem
import com.xayah.databackup.ui.component.FilterButton
import com.xayah.databackup.ui.component.filterButtonSecondaryColors
import com.xayah.databackup.ui.component.horizontalFadingEdges
import com.xayah.databackup.util.FilterBackupUser
import com.xayah.databackup.util.FiltersSystemAppsBackup
import com.xayah.databackup.util.FiltersUserAppsBackup
import com.xayah.databackup.util.KeyFiltersSystemAppsBackup
import com.xayah.databackup.util.KeyFiltersUserAppsBackup
import com.xayah.databackup.util.KeySortsTypeBackup
import com.xayah.databackup.util.SortsSequence
import com.xayah.databackup.util.SortsSequenceBackup
import com.xayah.databackup.util.SortsType
import com.xayah.databackup.util.SortsTypeBackup
import com.xayah.databackup.util.popBackStackSafely
import com.xayah.databackup.util.readBoolean
import com.xayah.databackup.util.readEnum
import com.xayah.databackup.util.readInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BackupAppsScreen(
    navController: NavHostController,
    viewModel: AppsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val filterSheetState = rememberModalBottomSheetState()
    var showFilterSheet by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()
    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.select_apps),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "13/79 items selected • 1.6 GB",
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
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_funnel),
                            contentDescription = stringResource(R.string.filters)
                        )
                    }
                    IconButton(onClick = { /* do something */ }) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_search),
                            contentDescription = stringResource(R.string.search)
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
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
                            contentDescription = null
                        )
                        Text(
                            text = "List is empty",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(state = lazyListState) {
                        Snapshot.withoutReadObservation {
                            lazyListState.requestScrollToItem(
                                index = lazyListState.firstVisibleItemIndex,
                                scrollOffset = lazyListState.firstVisibleItemScrollOffset
                            )
                        }

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
                LaunchedEffect(null) {
                    scope.launch {
                        withContext(Dispatchers.Default) {
                            users = RemoteRootService.getUsers()
                        }
                    }
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
