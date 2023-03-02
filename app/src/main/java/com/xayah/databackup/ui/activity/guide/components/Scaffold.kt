package com.xayah.databackup.ui.activity.guide.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import com.xayah.databackup.R
import com.xayah.databackup.ui.components.Scaffold
import com.xayah.databackup.ui.components.TopBarTitle
import com.xayah.databackup.ui.components.paddingBottom

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
    val smallPadding = dimensionResource(R.dimen.padding_small)
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
        topPaddingRate = 2,
        content = {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = colorOnSurfaceVariant,
                        modifier = Modifier
                            .size(iconMediumSize)
                            .paddingBottom(smallPadding)
                    )
                    TopBarTitle(text = title)
                }
            }
            items(this)
        }
    )
}