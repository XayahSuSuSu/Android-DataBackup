package com.xayah.databackup.ui.components

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.dimensionResource
import com.xayah.databackup.R
import com.xayah.databackup.ui.components.animation.ContentFade

@ExperimentalMaterial3Api
@Composable
fun Scaffold(
    topBar: @Composable TopAppBarScrollBehavior.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    topPaddingRate: Int = 1,
    bottomPaddingRate: Int = 1,
    isInitialized: MutableTransitionState<Boolean>? = null,
    content: LazyListScope.() -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { scrollBehavior.topBar() },
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = FabPosition.Center
    ) { innerPadding ->
        if (isInitialized != null) {
            if (isInitialized.targetState.not()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(innerPadding.calculateTopPadding() * topPaddingRate)
                    )
                    CircularProgressIndicator()
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(innerPadding.calculateBottomPadding() * bottomPaddingRate)
                    )
                }
            }
            ContentFade(isInitialized) {
                StandardContent(
                    innerPadding = innerPadding,
                    topPaddingRate = topPaddingRate,
                    bottomPaddingRate = bottomPaddingRate,
                    content = content
                )
            }
        } else {
            StandardContent(
                innerPadding = innerPadding,
                topPaddingRate = topPaddingRate,
                bottomPaddingRate = bottomPaddingRate,
                content = content
            )
        }
    }
}

@Composable
fun StandardContent(
    innerPadding: PaddingValues,
    topPaddingRate: Int,
    bottomPaddingRate: Int,
    content: LazyListScope.() -> Unit
) {
    val nonePadding = dimensionResource(R.dimen.padding_none)
    val mediumPadding = dimensionResource(R.dimen.padding_medium)
    LazyColumn(
        modifier = Modifier.padding(mediumPadding, nonePadding),
        verticalArrangement = Arrangement.spacedBy(mediumPadding),
    ) {
        item {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(innerPadding.calculateTopPadding() * topPaddingRate)
            )
        }
        content()
        item {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(innerPadding.calculateBottomPadding() * bottomPaddingRate)
            )
        }
    }
}
