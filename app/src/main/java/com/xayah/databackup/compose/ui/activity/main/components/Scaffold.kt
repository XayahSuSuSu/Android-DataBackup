package com.xayah.databackup.compose.ui.activity.main.components

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.xayah.databackup.R
import com.xayah.databackup.activity.list.AppListBackupActivity
import com.xayah.databackup.activity.list.AppListRestoreActivity
import com.xayah.databackup.compose.ui.activity.settings.SettingsActivity
import com.xayah.databackup.data.ProcessingActivityTag
import com.xayah.databackup.data.TypeBackupApp

@ExperimentalMaterial3Api
@Composable
fun MainScaffold() {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val nonePadding = dimensionResource(R.dimen.padding_none)
    val mediumPadding = dimensionResource(R.dimen.padding_medium)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = {
                        context.startActivity(
                            Intent(context, SettingsActivity::class.java)
                        )
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(id = R.string.settings)
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(mediumPadding, nonePadding),
            verticalArrangement = Arrangement.spacedBy(mediumPadding),
        ) {
            item {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(innerPadding.calculateTopPadding())
                )
            }
            item {
                val colorYellow = colorResource(id = R.color.yellow)
                ItemCard(
                    icon = ImageVector.vectorResource(id = R.drawable.ic_round_apps),
                    iconTint = colorYellow,
                    title = stringResource(id = R.string.application),
                    subtitle = stringResource(R.string.card_app_subtitle),
                    onBackupClick = {
                        context.startActivity(
                            Intent(context, AppListBackupActivity::class.java).apply {
                                putExtra(
                                    ProcessingActivityTag,
                                    TypeBackupApp
                                )
                            })
                    },
                    onRestoreClick = {
                        context.startActivity(
                            Intent(context, AppListRestoreActivity::class.java).apply {
                                putExtra(
                                    ProcessingActivityTag,
                                    TypeBackupApp
                                )
                            })
                    }
                )
            }
            item {
                val colorYellow = colorResource(id = R.color.blue)
                ItemCard(
                    icon = ImageVector.vectorResource(id = R.drawable.ic_round_image),
                    iconTint = colorYellow,
                    title = stringResource(id = R.string.media),
                    subtitle = stringResource(R.string.card_media_subtitle),
                    onBackupClick = {},
                    onRestoreClick = {}
                )
            }
            item {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(innerPadding.calculateBottomPadding())
                )
            }
        }
    }
}