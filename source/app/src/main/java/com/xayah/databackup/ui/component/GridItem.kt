package com.xayah.databackup.ui.component

import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.core.graphics.drawable.toDrawable
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.xayah.core.model.EmojiString
import com.xayah.databackup.ui.token.GridItemTokens
import com.xayah.databackup.util.PathUtil
import com.xayah.core.rootservice.util.ExceptionUtil.tryOn
import com.xayah.core.rootservice.util.withIOContext
import java.io.File

@ExperimentalMaterial3Api
@Composable
fun GridItemPackage(packageName: String, label: String) {
    val context = LocalContext.current
    var icon by remember { mutableStateOf<Any>(0) }
    LaunchedEffect(null) {
        // Read icon from cached internal dir.
        withIOContext {
            tryOn {
                val bytes = File(PathUtil.getIconPath(context, packageName)).readBytes()
                icon = BitmapFactory.decodeByteArray(bytes, 0, bytes.size).toDrawable(context.resources)
            }
        }
    }
    Column(
        modifier = Modifier.paddingVertical(GridItemTokens.PaddingVertical),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(GridItemTokens.PaddingTiny)
    ) {
        AsyncImage(
            modifier = Modifier.size(GridItemTokens.IconSize),
            model = ImageRequest.Builder(context)
                .data(icon)
                .crossfade(true)
                .build(),
            contentDescription = null
        )
        LabelSmallText(text = label, textAlign = TextAlign.Center)
    }
}

data class GridItemCompletionConfig(val emoji: EmojiString, val title: String, val content: String)

@Composable
fun GridItemCompletion(config: GridItemCompletionConfig) {
    Row(horizontalArrangement = Arrangement.spacedBy(GridItemTokens.PaddingTiny), verticalAlignment = Alignment.CenterVertically) {
        EmojiText(emoji = config.emoji, size = GridItemTokens.EmojiSize)
        Column {
            BodySmallText(text = config.title)
            TitleLargeBoldText(text = config.content)
        }
    }
}
