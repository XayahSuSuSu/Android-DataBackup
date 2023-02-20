package com.xayah.databackup.compose.ui.activity.guide.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.xayah.databackup.R

@ExperimentalMaterial3Api
@Composable
fun GuideScaffold(
    title: String,
    icon: ImageVector,
    showBtnIcon: Boolean,
    nextBtnIcon: ImageVector,
    onNextBtnClick: () -> Unit,
    items: LazyListScope.() -> Unit
) {
    val colorOnSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val nonePadding = dimensionResource(R.dimen.padding_none)
    val smallPadding = dimensionResource(R.dimen.padding_small)
    val mediumPadding = dimensionResource(R.dimen.padding_medium)
    val iconMediumSize = dimensionResource(R.dimen.icon_medium_size)
    Scaffold(
        floatingActionButton = {
            if (showBtnIcon)
                FloatingActionButton(
                    onClick = onNextBtnClick,
                ) {
                    Icon(nextBtnIcon, null)
                }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(mediumPadding, nonePadding),
            verticalArrangement = Arrangement.spacedBy(mediumPadding),
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(
                                innerPadding.calculateTopPadding() * 2
                            )
                    )
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = colorOnSurfaceVariant,
                        modifier = Modifier
                            .size(iconMediumSize)
                            .padding(nonePadding, nonePadding, nonePadding, smallPadding)
                    )
                    Text(
                        title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            items(this)
            item {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(
                            innerPadding.calculateBottomPadding()
                        )
                )
            }
        }
    }
}