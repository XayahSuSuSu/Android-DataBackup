package com.xayah.databackup.compose.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.dimensionResource
import com.xayah.databackup.R

@ExperimentalMaterial3Api
@Composable
fun Scaffold(
    topBar: @Composable TopAppBarScrollBehavior.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    topPaddingRate: Int = 1,
    bottomPaddingRate: Int = 1,
    content: LazyListScope.() -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val nonePadding = dimensionResource(R.dimen.padding_none)
    val mediumPadding = dimensionResource(R.dimen.padding_medium)
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { scrollBehavior.topBar() },
        floatingActionButton = floatingActionButton,
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
}
