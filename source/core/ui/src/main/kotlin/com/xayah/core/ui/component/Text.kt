package com.xayah.core.ui.component

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.xayah.core.ui.material3.DisabledAlpha

@Composable
fun TopBarTitle(modifier: Modifier = Modifier, text: String) {
    Text(
        modifier = modifier,
        text = text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun HeadlineMediumText(
    modifier: Modifier = Modifier,
    text: String,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    fontWeight: FontWeight? = null,
    enabled: Boolean = true,
) {
    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.headlineMedium,
        color = if (enabled) color else color.copy(alpha = DisabledAlpha),
        textAlign = textAlign,
        fontWeight = fontWeight,
    )
}

@Composable
fun TitleLargeText(
    modifier: Modifier = Modifier,
    text: String,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    fontWeight: FontWeight? = null,
    enabled: Boolean = true,
) {
    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = if (enabled) color else color.copy(alpha = DisabledAlpha),
        textAlign = textAlign,
        fontWeight = fontWeight,
    )
}

@Composable
fun AutoTitleLargeText(
    modifier: Modifier = Modifier,
    text: String,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    fontWeight: FontWeight? = null,
    enabled: Boolean = true,
) {
    AutoSizeText(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = if (enabled) color else color.copy(alpha = DisabledAlpha),
        textAlign = textAlign,
        fontWeight = fontWeight,
    )
}

@Composable
fun TitleMediumText(
    modifier: Modifier = Modifier,
    text: String,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    fontWeight: FontWeight? = null,
    enabled: Boolean = true,
) {
    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = if (enabled) color else color.copy(alpha = DisabledAlpha),
        textAlign = textAlign,
        fontWeight = fontWeight,
    )
}

@Composable
fun TitleSmallText(
    modifier: Modifier = Modifier,
    text: String,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    fontWeight: FontWeight? = null,
    enabled: Boolean = true,
) {
    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = if (enabled) color else color.copy(alpha = DisabledAlpha),
        textAlign = textAlign,
        fontWeight = fontWeight,
    )
}

@Composable
fun LabelLargeText(
    modifier: Modifier = Modifier,
    text: String,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    fontWeight: FontWeight? = null,
    enabled: Boolean = true,
) {
    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = if (enabled) color else color.copy(alpha = DisabledAlpha),
        textAlign = textAlign,
        fontWeight = fontWeight,
    )
}

@Composable
fun LabelSmallText(
    modifier: Modifier = Modifier,
    text: String,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    enabled: Boolean = true,
) {
    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = if (enabled) color else color.copy(alpha = DisabledAlpha),
        textAlign = textAlign,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
    )
}

@Composable
fun BodySmallText(
    modifier: Modifier = Modifier,
    text: String,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    fontWeight: FontWeight? = null,
    enabled: Boolean = true,
) {
    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = if (enabled) color else color.copy(alpha = DisabledAlpha),
        textAlign = textAlign,
        fontWeight = fontWeight,
    )
}

@Composable
fun BodyMediumText(
    modifier: Modifier = Modifier,
    text: String,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    fontWeight: FontWeight? = null,
    enabled: Boolean = true,
) {
    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (enabled) color else color.copy(alpha = DisabledAlpha),
        textAlign = textAlign,
        fontWeight = fontWeight,
    )
}

@Composable
fun AutoSizeText(
    modifier: Modifier = Modifier,
    text: String,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    textAlign: TextAlign? = null,
    fontWeight: FontWeight? = null,
    enabled: Boolean = true,
) {
    var autoStyle by remember { mutableStateOf(style) }
    Text(
        modifier = modifier,
        text = text,
        style = autoStyle,
        color = if (enabled) color else color.copy(alpha = DisabledAlpha),
        textAlign = textAlign,
        fontWeight = fontWeight,
        maxLines = 1,
        softWrap = false,
        onTextLayout = { textLayoutResult ->
            val fraction = textLayoutResult.size.width / textLayoutResult.multiParagraph.width
            if (textLayoutResult.didOverflowWidth) {
                autoStyle = autoStyle.copy(fontSize = autoStyle.fontSize * fraction)
            }
        },
    )
}
