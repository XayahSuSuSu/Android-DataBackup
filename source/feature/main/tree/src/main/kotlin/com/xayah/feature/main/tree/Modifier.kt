package com.xayah.feature.main.tree

import androidx.compose.ui.Modifier
import com.xayah.core.ui.component.shimmer

internal fun Modifier.treeShimmer(visible: Boolean) = shimmer(visible, 0.5f, 0.3f)
