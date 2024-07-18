package com.xayah.core.ui.component

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
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
import kotlinx.coroutines.runBlocking

@ExperimentalFoundationApi
@Composable
fun PackageIconImage(enabled: Boolean = true, packageName: String, size: Dp = SizeTokens.Level32) {
    val context = LocalContext.current
    val icon = rememberDrawablePainter(drawable = runBlocking {
        BaseUtil.readIconFromPackageName(context, packageName) ?:
        BaseUtil.readIcon(context, "${context.iconDir()}/${PathUtil.getPackageIconRelativePath(packageName)}") ?:
        AppCompatResources.getDrawable(context, android.R.drawable.sym_def_app_icon)
    } )

    Image(
        modifier = Modifier.size(size),
        painter = icon,
        colorFilter = if (enabled) null else ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }),
        contentDescription = null
    )
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
