package com.xayah.databackup.compose.ui.activity.list.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.xayah.databackup.R
import com.xayah.databackup.compose.ui.components.Scaffold

@ExperimentalMaterial3Api
@Composable
fun ListScaffold(
    isInitialized: Boolean,
    onManifest: Boolean,
    content: LazyListScope.() -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit
) {
    Scaffold(
        floatingActionButton = {
            if (isInitialized) {
                FloatingActionButton(
                    onClick = onNext,
                ) {
                    Icon(Icons.Rounded.ArrowForward, null)
                }
            }
        },
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
                scrollBehavior = this,
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
        topPaddingRate = 1,
        content = {
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
        }
    )
}
