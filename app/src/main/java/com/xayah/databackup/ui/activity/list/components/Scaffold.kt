package com.xayah.databackup.ui.activity.list.components

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.xayah.databackup.R
import com.xayah.databackup.ui.components.Scaffold
import com.xayah.databackup.ui.components.animation.ContentFade

@ExperimentalMaterial3Api
@Composable
fun ListScaffold(
    isInitialized: MutableTransitionState<Boolean>,
    topBarTitle: String,
    onManifest: Boolean,
    actions: @Composable() (RowScope.() -> Unit) = {},
    content: LazyListScope.() -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit
) {
    val mediumPadding = dimensionResource(R.dimen.padding_medium)
    Scaffold(
        floatingActionButton = {
            ContentFade(isInitialized) {
                FloatingActionButton(
                    modifier = Modifier.padding(mediumPadding),
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
                        else topBarTitle,
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
                actions = {
                    ContentFade(isInitialized) {
                        actions()
                    }
                }
            )
        },
        topPaddingRate = 1,
        isInitialized = isInitialized,
        content = {
            content(this)
        }
    )
}
