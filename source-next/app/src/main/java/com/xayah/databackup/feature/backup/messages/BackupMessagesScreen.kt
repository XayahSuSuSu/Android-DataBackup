package com.xayah.databackup.feature.backup.messages

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.xayah.databackup.R
import com.xayah.databackup.database.entity.MmsDeserialized
import com.xayah.databackup.database.entity.SmsDeserialized
import com.xayah.databackup.ui.component.SearchTextField
import com.xayah.databackup.ui.component.defaultLargeTopAppBarColors
import com.xayah.databackup.ui.component.verticalFadingEdges
import com.xayah.databackup.util.LaunchedEffect
import com.xayah.databackup.util.popBackStackSafely
import kotlinx.coroutines.Dispatchers
import org.koin.androidx.compose.koinViewModel

@Composable
fun BackupMessagesScreen(
    navController: NavHostController,
    viewModel: MessagesViewModel = koinViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val searchScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val smsList by viewModel.smsList.collectAsStateWithLifecycle()
    val mmsList by viewModel.mmsList.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()
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
                            IconButton(onClick = {
                                when (uiState.selectedIndex) {
                                    0 -> viewModel.selectAllSms()
                                    1 -> viewModel.selectAllMms()
                                }
                            }) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_list_checks),
                                    contentDescription = stringResource(R.string.select_all)
                                )
                            }
                        },
                        scrollBehavior = searchScrollBehavior,
                    )
                } else {
                    Column {
                        LargeTopAppBar(
                            title = {
                                Column {
                                    Text(
                                        text = stringResource(R.string.select_messages),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = stringResource(R.string.items_selected, selected, smsList.size + mmsList.size),
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
                                IconButton(onClick = {
                                    when (uiState.selectedIndex) {
                                        0 -> viewModel.selectAllSms()
                                        1 -> viewModel.selectAllMms()
                                    }
                                }) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.ic_list_checks),
                                        contentDescription = stringResource(R.string.select_all)
                                    )
                                }
                            },
                            scrollBehavior = scrollBehavior,
                            colors = TopAppBarDefaults.defaultLargeTopAppBarColors(),
                        )

                        val options = listOf(stringResource(R.string.sms), stringResource(R.string.mms))
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            options.forEachIndexed { index, label ->
                                SegmentedButton(
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = options.size
                                    ),
                                    onClick = {
                                        viewModel.updateUiState(uiState.copy(index))
                                    },
                                    selected = index == uiState.selectedIndex,
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier) {
            Spacer(modifier = Modifier.size(innerPadding.calculateTopPadding()))

            AnimatedContent(targetState = smsList.isEmpty()) { isAppsEmpty ->
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
                    Column {
                        AnimatedContent(targetState = uiState.selectedIndex) { selectedIndex ->
                            when (selectedIndex) {
                                0 -> {
                                    LazyColumn(
                                        modifier = Modifier.verticalFadingEdges(startEdgeRange, endEdgeRange),
                                        state = lazyListState,
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        item(key = -1) {
                                            Spacer(modifier = Modifier.height(0.dp))
                                        }

                                        items(items = smsList, key = { it.id }) { sms ->
                                            SmsListItem(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp),
                                                sms = sms,
                                                viewModel = viewModel,
                                            )
                                        }

                                        item(key = "-1") {
                                            Spacer(modifier = Modifier.size(innerPadding.calculateBottomPadding()))
                                        }
                                    }
                                }

                                1 -> {
                                    LazyColumn(
                                        modifier = Modifier.verticalFadingEdges(startEdgeRange, endEdgeRange),
                                        state = lazyListState,
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        item(key = -1) {
                                            Spacer(modifier = Modifier.height(0.dp))
                                        }

                                        items(items = mmsList, key = { it.id }) { mms ->
                                            MmsListItem(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp),
                                                mms = mms,
                                                viewModel = viewModel,
                                            )
                                        }

                                        item(key = "-1") {
                                            Spacer(modifier = Modifier.size(innerPadding.calculateBottomPadding()))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SmsListItem(
    modifier: Modifier,
    sms: SmsDeserialized,
    viewModel: MessagesViewModel,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp),
        onClick = {
            viewModel.selectSms(sms.id, sms.selected.not())
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(modifier = Modifier.size(36.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary,
                        imageVector = ImageVector.vectorResource(R.drawable.ic_user_round),
                        contentDescription = null
                    )
                }
            }

            Column(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = sms.address,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = sms.body,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Checkbox(
                checked = sms.selected,
                onCheckedChange = {
                    viewModel.selectSms(sms.id, it)
                }
            )
        }
    }
}

@Composable
fun MmsListItem(
    modifier: Modifier,
    mms: MmsDeserialized,
    viewModel: MessagesViewModel,
) {
    val bodyTextStyle = MaterialTheme.typography.bodySmall
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp),
        onClick = {
            viewModel.selectMms(mms.id, mms.selected.not())
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(modifier = Modifier.size(36.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary,
                        imageVector = ImageVector.vectorResource(R.drawable.ic_user_round),
                        contentDescription = null
                    )
                }
            }

            Column(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = mms.address,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val inlineContent = remember {
                    mapOf(
                        mms.iconMod to InlineTextContent(
                            Placeholder(
                                width = bodyTextStyle.fontSize,
                                height = bodyTextStyle.fontSize,
                                placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                            )
                        ) {
                            Icon(
                                modifier = Modifier.fillMaxSize(),
                                tint = MaterialTheme.colorScheme.secondary,
                                imageVector = ImageVector.vectorResource(R.drawable.ic_paperclip),
                                contentDescription = null
                            )
                        }
                    )
                }
                if (mms.body.isNotEmpty()) {
                    Text(
                        inlineContent = inlineContent,
                        text = mms.body,
                        style = bodyTextStyle,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Checkbox(
                checked = mms.selected,
                onCheckedChange = {
                    viewModel.selectMms(mms.id, it)
                }
            )
        }
    }
}
