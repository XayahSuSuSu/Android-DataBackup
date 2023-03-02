package com.xayah.databackup.ui.activity.list.components.manifest

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.remember
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
            ManifestCard(list = list)
        }
    }
}