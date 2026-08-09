package com.xayah.databackup.feature.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xayah.databackup.BuildConfig
import com.xayah.databackup.R
import com.xayah.databackup.feature.TranslatorsRoute
import com.xayah.databackup.ui.component.Preference
import com.xayah.databackup.ui.component.PreferenceGroup
import com.xayah.databackup.ui.component.SectionHeader
import com.xayah.databackup.ui.component.rememberFadingEdgeState
import com.xayah.databackup.ui.component.surfaceTopAppBarColors
import com.xayah.databackup.ui.component.verticalFadingEdges
import com.xayah.databackup.util.Navigator
import com.xayah.databackup.util.navigateSafely
import com.xayah.databackup.util.openUrl
import com.xayah.databackup.util.popBackStackSafely

private const val GitHubUrl = "https://github.com/XayahSuSuSu/Android-DataBackup"
private const val DocsUrl = "https://DataBackupOfficial.github.io"
private const val ContactUrl = "https://t.me/databackupchat"

@Composable
fun AboutScreen(navigator: Navigator) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val scrollState = rememberScrollState()
    val fadingEdgeState = rememberFadingEdgeState(scrollState, label = "about")

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.about),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStackSafely() }) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_left),
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                colors = TopAppBarDefaults.surfaceTopAppBarColors(),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .verticalFadingEdges(fadingEdgeState)
                .verticalScroll(scrollState),
        ) {
            AboutAppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )

            SectionHeader(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                title = stringResource(R.string.about_app),
            )

            AboutLinksCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                onOpenUrl = context::openUrl,
                onOpenTranslators = { navigator.navigateSafely(TranslatorsRoute) },
            )

            Spacer(modifier = Modifier.size(innerPadding.calculateBottomPadding() + 16.dp))
        }
    }
}

@Composable
private fun AboutAppCard(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                modifier = Modifier.size(88.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape,
            ) {
                Icon(
                    modifier = Modifier.fillMaxSize(),
                    imageVector = ImageVector.vectorResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    tint = Color.Unspecified,
                )
            }

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            AssistChip(
                onClick = {},
                label = {
                    Text(
                        text = "${stringResource(R.string.version)} ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center,
                    )
                },
                shape = CircleShape,
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
                border = null,
            )
            Text(
                text = stringResource(R.string.app_short_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun AboutLinksCard(
    modifier: Modifier = Modifier,
    onOpenUrl: (String) -> Unit,
    onOpenTranslators: () -> Unit,
) {
    PreferenceGroup(modifier = modifier) {
        ExternalLinkPreference(
            icon = ImageVector.vectorResource(R.drawable.ic_code),
            title = stringResource(R.string.github),
            subtitle = GitHubUrl,
            onClick = { onOpenUrl(GitHubUrl) },
        )
        ExternalLinkPreference(
            icon = ImageVector.vectorResource(R.drawable.ic_book_text),
            title = stringResource(R.string.docs),
            subtitle = DocsUrl,
            onClick = { onOpenUrl(DocsUrl) },
        )
        ExternalLinkPreference(
            icon = ImageVector.vectorResource(R.drawable.ic_message_circle),
            title = stringResource(R.string.contact),
            subtitle = ContactUrl,
            onClick = { onOpenUrl(ContactUrl) },
        )
        Preference(
            icon = ImageVector.vectorResource(R.drawable.ic_languages),
            title = stringResource(R.string.translators),
            subtitle = stringResource(R.string.translators_desc),
            slot = {
                Icon(
                    modifier = Modifier.size(24.dp),
                    imageVector = ImageVector.vectorResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                )
            },
            onClick = onOpenTranslators,
        )
    }
}

@Composable
private fun ExternalLinkPreference(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Preference(
        icon = icon,
        title = title,
        subtitle = subtitle,
        slot = {
            Icon(
                modifier = Modifier.size(20.dp),
                imageVector = ImageVector.vectorResource(R.drawable.ic_external_link),
                contentDescription = null,
            )
        },
        onClick = onClick,
    )
}
