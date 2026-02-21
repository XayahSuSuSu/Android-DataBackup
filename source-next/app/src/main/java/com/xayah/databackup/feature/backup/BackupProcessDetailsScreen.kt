package com.xayah.databackup.feature.backup

import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.xayah.databackup.R
import com.xayah.databackup.data.ProcessAppDataItem
import com.xayah.databackup.data.ProcessAppItem
import com.xayah.databackup.data.isFailedStatus
import com.xayah.databackup.ui.component.defaultLargeTopAppBarColors
import com.xayah.databackup.util.LaunchedEffect
import com.xayah.databackup.util.formatToStorageSizePerSecond
import com.xayah.databackup.util.popBackStackSafely
import kotlinx.coroutines.Dispatchers
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt

@Composable
fun BackupProcessDetailsScreen(
    navController: NavHostController,
    viewModel: BackupProcessViewModel = koinViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val appsItem by viewModel.appsItem.collectAsStateWithLifecycle()
    val failedApps by viewModel.failedProcessedAppItems.collectAsStateWithLifecycle()
    val canceledApps by viewModel.canceledProcessedAppItems.collectAsStateWithLifecycle()
    val succeededApps by viewModel.succeededProcessedAppItems.collectAsStateWithLifecycle()

    var expandedKey by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(R.string.details),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${appsItem.currentIndex}/${appsItem.totalCount}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStackSafely() }) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_left),
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.defaultLargeTopAppBarColors(),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (failedApps.isNotEmpty()) {
                    item(FAILED_HEADER_KEY) {
                        GroupHeader(
                            title = stringResource(R.string.failed),
                            count = failedApps.size,
                        )
                    }
                    items(
                        items = failedApps,
                        key = { item -> toAppKey(item) }
                    ) { item ->
                        val itemKey = toAppKey(item)
                        val expanded = expandedKey == itemKey
                        AppDetailCard(
                            appItem = item,
                            expanded = expanded,
                            onExpandToggle = {
                                expandedKey = if (expanded) null else itemKey
                            }
                        )
                    }
                }

                if (canceledApps.isNotEmpty()) {
                    item(CANCELED_HEADER_KEY) {
                        GroupHeader(
                            title = stringResource(R.string.canceled),
                            count = canceledApps.size,
                        )
                    }
                    items(
                        items = canceledApps,
                        key = { item -> toAppKey(item) }
                    ) { item ->
                        val itemKey = toAppKey(item)
                        val expanded = expandedKey == itemKey
                        AppDetailCard(
                            appItem = item,
                            expanded = expanded,
                            onExpandToggle = {
                                expandedKey = if (expanded) null else itemKey
                            }
                        )
                    }
                }

                if (succeededApps.isNotEmpty()) {
                    item(SUCCEEDED_HEADER_KEY) {
                        GroupHeader(
                            title = stringResource(R.string.succeed),
                            count = succeededApps.size,
                        )
                    }
                    items(
                        items = succeededApps,
                        key = { item -> toAppKey(item) }
                    ) { item ->
                        val itemKey = toAppKey(item)
                        val expanded = expandedKey == itemKey
                        AppDetailCard(
                            appItem = item,
                            expanded = expanded,
                            onExpandToggle = {
                                expandedKey = if (expanded) null else itemKey
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.size(8.dp))
                }
            }
        }
    }
}

@Composable
private fun GroupHeader(
    title: String,
    count: Int,
) {
    Text(
        text = "$title ($count)",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}

@Composable
private fun AppDetailCard(
    appItem: ProcessAppItem,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
) {
    val context = LocalContext.current
    var icon: Drawable? by remember { mutableStateOf(null) }
    val rotation by animateFloatAsState(if (expanded) 90f else 0f, label = "chevronRotation")

    LaunchedEffect(context = Dispatchers.IO, appItem.packageName) {
        icon = runCatching { context.packageManager.getApplicationIcon(appItem.packageName) }.getOrNull()
        if (icon == null) {
            icon = AppCompatResources.getDrawable(context, android.R.drawable.sym_def_app_icon)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onExpandToggle)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AsyncImage(
                modifier = Modifier.size(36.dp),
                model = ImageRequest.Builder(context)
                    .data(icon)
                    .crossfade(true)
                    .build(),
                contentDescription = null
            )

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = appItem.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${(appItem.progress * 100).roundToInt()}%",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer {
                        rotationZ = rotation
                    }
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                AppDataItemRow(
                    item = appItem.apkItem,
                    icon = R.drawable.ic_resource_package,
                )
                HorizontalDivider()
                AppDataItemRow(
                    item = appItem.intDataItem,
                    icon = R.drawable.ic_user,
                )
                HorizontalDivider()
                AppDataItemRow(
                    item = appItem.extDataItem,
                    icon = R.drawable.ic_database,
                )
                HorizontalDivider()
                AppDataItemRow(
                    item = appItem.addlDataItem,
                    icon = R.drawable.ic_gamepad_2,
                )
            }
        }
    }
}

@Composable
private fun AppDataItemRow(
    item: ProcessAppDataItem,
    icon: Int,
) {
    val speedText = remember(item.details) {
        item.details.lastOrNull { it.speed > 0L }?.speed?.formatToStorageSizePerSecond
    }
    val subtitleText = if (speedText.isNullOrBlank().not()) {
        "${item.subtitle} | $speedText"
    } else {
        item.subtitle
    }
    val contentColor = if (item.enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_CONTENT_ALPHA)
    }
    val iconContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(
        alpha = if (item.enabled) NORMAL_ICON_CONTAINER_ENABLED_ALPHA else NORMAL_ICON_CONTAINER_DISABLED_ALPHA
    )
    val iconTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
        alpha = if (item.enabled) 1f else NORMAL_ICON_TINT_DISABLED_ALPHA
    )
    val errorInfoList = remember(item.details) {
        item.details
            .filter { isFailedStatus(it.status) && it.info.isNotBlank() }
            .map { it.info.trim() }
            .distinct()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
                color = iconContainerColor
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        imageVector = ImageVector.vectorResource(icon),
                        contentDescription = null,
                        tint = iconTint
                    )
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (item.enabled) 1f else 0.55f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Text(
                text = item.msg,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (item.enabled) 1f else 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        AnimatedVisibility(visible = errorInfoList.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = ERROR_INFO_CONTAINER_ALPHA)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            modifier = Modifier.size(14.dp),
                            imageVector = ImageVector.vectorResource(R.drawable.ic_badge_info),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = stringResource(R.string.error),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    errorInfoList.forEach { info ->
                        Text(
                            text = info,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }
        }
    }
}

private fun toAppKey(item: ProcessAppItem): String {
    return "${item.packageName}:${item.userId}"
}

private const val FAILED_HEADER_KEY = "failed_header"
private const val CANCELED_HEADER_KEY = "canceled_header"
private const val SUCCEEDED_HEADER_KEY = "succeeded_header"
private const val DISABLED_CONTENT_ALPHA = 0.5f
private const val ERROR_INFO_CONTAINER_ALPHA = 0.55f
private const val NORMAL_ICON_CONTAINER_ENABLED_ALPHA = 1f
private const val NORMAL_ICON_CONTAINER_DISABLED_ALPHA = 0.55f
private const val NORMAL_ICON_TINT_DISABLED_ALPHA = 0.55f
