package com.xayah.core.ui.component

import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.xayah.core.ui.R
import com.xayah.core.ui.theme.ThemedColorSchemeKeyTokens
import com.xayah.core.ui.theme.value
import com.xayah.core.ui.theme.withState
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.util.PathUtil
import com.xayah.core.util.command.BaseUtil
import com.xayah.core.util.iconDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@ExperimentalFoundationApi
@Composable
fun PackageIconImage(enabled: Boolean = true, packageName: String, size: Dp = SizeTokens.Level32) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var icon by remember { mutableStateOf<Drawable?>(null) }
    LaunchedEffect(null) {
        // Read icon from cached internal dir.
        scope.launch(Dispatchers.IO) {
            icon =
                BaseUtil.readIcon(context, "${context.iconDir()}/${PathUtil.getPackageIconRelativePath(packageName)}") ?: AppCompatResources.getDrawable(context, android.R.drawable.sym_def_app_icon)
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

@ExperimentalFoundationApi
@Composable
fun MediaIconImage(enabled: Boolean = true, name: String, textStyle: TextStyle = MaterialTheme.typography.labelMedium, size: Dp = SizeTokens.Level32) {
    Surface(modifier = Modifier.size(size), indication = null, shape = CircleShape, color = ThemedColorSchemeKeyTokens.PrimaryContainer.value.withState(), enabled = enabled) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = name,
                style = textStyle,
                color = ThemedColorSchemeKeyTokens.OnPrimaryContainer.value.withState(enabled),
            )
        }
    }
}

@Composable
fun AppIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(SizeTokens.Level128)
            .clip(CircleShape)
            .background(colorResource(id = R.color.ic_launcher_background)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            modifier = Modifier.size(SizeTokens.Level100),
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_launcher_foreground_tonal),
            contentDescription = null
        )
    }
}
