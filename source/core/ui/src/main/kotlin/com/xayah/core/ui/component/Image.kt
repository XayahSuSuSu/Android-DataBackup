package com.xayah.core.ui.component

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.xayah.core.ui.R
import com.xayah.core.ui.material3.toColor
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.fromDrawable
import com.xayah.core.ui.util.value
import com.xayah.core.util.PathUtil
import com.xayah.core.util.command.BaseUtil
import com.xayah.core.util.iconDir
import com.xayah.core.util.withIOContext

@ExperimentalFoundationApi
@Composable
fun PackageIconImage(enabled: Boolean = true, packageName: String, label: String = "", textStyle: TextStyle = MaterialTheme.typography.labelMedium, size: Dp = SizeTokens.Level32) {
    val context = LocalContext.current
    var icon by remember { mutableStateOf<Any?>(null) }
    LaunchedEffect(null) {
        // Read icon from cached internal dir.
        withIOContext {
            icon = BaseUtil.readIcon(context, "${context.iconDir()}/${PathUtil.getPackageIconRelativePath(packageName)}")
        }
    }

    if (icon == null) {
        Surface(modifier = Modifier.size(size), indication = null, shape = CircleShape, color = ColorSchemeKeyTokens.PrimaryContainer.toColor(enabled), enabled = enabled) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = label,
                    style = textStyle,
                    color = ColorSchemeKeyTokens.OnPrimaryContainer.toColor(enabled),
                )
            }
        }
    } else {
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
}

@ExperimentalFoundationApi
@Composable
fun MediaIconImage(enabled: Boolean = true, name: String, textStyle: TextStyle = MaterialTheme.typography.labelMedium, size: Dp = SizeTokens.Level32) {
    Surface(modifier = Modifier.size(size), indication = null, shape = CircleShape, color = ColorSchemeKeyTokens.PrimaryContainer.toColor(enabled), enabled = enabled) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = name,
                style = textStyle,
                color = ColorSchemeKeyTokens.OnPrimaryContainer.toColor(enabled),
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
            imageVector = ImageVectorToken.fromDrawable(R.drawable.ic_launcher_foreground_tonal).value,
            contentDescription = null
        )
    }
}
