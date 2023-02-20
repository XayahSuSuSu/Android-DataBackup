package com.xayah.databackup.compose.ui.activity.list.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.xayah.databackup.R

@ExperimentalMaterial3Api
@Composable
fun ListScaffold(
    isInitialized: Boolean,
    onManifest: Boolean,
    content: LazyListScope.() -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val nonePadding = dimensionResource(R.dimen.padding_none)
    val mediumPadding = dimensionResource(R.dimen.padding_medium)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (onManifest) stringResource(R.string.manifest)
                        else stringResource(R.string.select_backup_app),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = onFinish) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            if (isInitialized) {
                FloatingActionButton(
                    onClick = onNext,
                ) {
                    Icon(Icons.Rounded.ArrowForward, null)
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center
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
            if (isInitialized) {
                content(this)
            } else {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator()
                    }
                }
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
