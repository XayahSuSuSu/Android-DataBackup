package com.xayah.core.ui.component

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.util.PathUtil
import com.xayah.core.util.command.BaseUtil
import com.xayah.core.util.iconDir
import com.xayah.core.util.withIOContext

@Composable
fun PackageIconImage(packageName: String, size: Dp = SizeTokens.Level5) {
    val context = LocalContext.current
    var icon by remember { mutableStateOf<Any?>(null) }
    LaunchedEffect(null) {
        // Read icon from cached internal dir.
        withIOContext {
            icon = BaseUtil.readIcon(context, "${context.iconDir()}/${PathUtil.getPackageIconRelativePath(packageName)}")
        }
    }

    AsyncImage(
        modifier = Modifier
            .size(size),
        model = ImageRequest.Builder(context)
            .data(icon)
            .crossfade(true)
            .build(),
        contentDescription = null
    )
}