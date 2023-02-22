package com.xayah.databackup.ui.activity.list.components.content

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.vectorResource
import com.xayah.databackup.R
import com.xayah.databackup.ui.activity.list.components.ManifestDesc
import com.xayah.databackup.ui.activity.list.components.ManifestDescItem
import com.xayah.databackup.ui.components.animation.ContentFade

@ExperimentalMaterial3Api
fun LazyListScope.contentManifest(list: List<ManifestDescItem>) {
    item {
        ContentFade(remember {
            MutableTransitionState(false).apply {
                targetState = true
            }
        }) {
            Card(modifier = Modifier.fillMaxWidth()) {
                val mediumPadding = dimensionResource(R.dimen.padding_medium)

                Column(
                    modifier = Modifier.padding(mediumPadding),
                    verticalArrangement = Arrangement.spacedBy(mediumPadding),
                ) {
                    for (i in list) {
                        ManifestDesc(
                            title = i.title,
                            subtitle = i.subtitle,
                            icon = i.icon ?: ImageVector.vectorResource(
                                id = i.iconId
                            )
                        )
                    }
                }
            }
        }
    }
}