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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import com.xayah.core.ui.theme.withState

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
        color = color.withState(enabled),
        textAlign = textAlign,
        fontWeight = fontWeight,
    )
}

@Composable
fun HeadlineSmallText(
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
        style = MaterialTheme.typography.headlineSmall,
        color = color.withState(enabled),
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
    overflow: TextOverflow = TextOverflow.Ellipsis,
    maxLines: Int = Int.MAX_VALUE,
    enabled: Boolean = true,
) {
    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = color.withState(enabled),
        textAlign = textAlign,
        fontWeight = fontWeight,
        overflow = overflow,
        maxLines = maxLines,
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
        color = color.withState(enabled),
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
        color = color.withState(enabled),
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
        color = color.withState(enabled),
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
    fontFamily: FontFamily? = null,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    maxLines: Int = Int.MAX_VALUE,
    enabled: Boolean = true,
) {
    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = color.withState(enabled),
        textAlign = textAlign,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        maxLines = maxLines,
        overflow = overflow,
    )
}

@Composable
fun AutoLabelLargeText(
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
        style = MaterialTheme.typography.labelLarge,
        color = color.withState(enabled),
        textAlign = textAlign,
        fontWeight = fontWeight,
    )
}

@Composable
fun LabelMediumText(
    modifier: Modifier = Modifier,
    text: String,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    fontWeight: FontWeight? = null,
    textDecoration: TextDecoration? = null,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    maxLines: Int = Int.MAX_VALUE,
    enabled: Boolean = true,
) {
    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = color.withState(enabled),
        textAlign = textAlign,
        fontWeight = fontWeight,
        textDecoration = textDecoration,
        overflow = overflow,
        maxLines = maxLines,
    )
}

@Composable
fun AutoLabelMediumText(
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
        style = MaterialTheme.typography.labelMedium,
        color = color.withState(enabled),
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
    overflow: TextOverflow = TextOverflow.Ellipsis,
    maxLines: Int = Int.MAX_VALUE,
    enabled: Boolean = true,
) {
    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color.withState(enabled),
        textAlign = textAlign,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        maxLines = maxLines,
        overflow = overflow,
    )
}

@Composable
fun BodyLargeText(
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
        style = MaterialTheme.typography.bodyLarge,
        color = color.withState(enabled),
        textAlign = textAlign,
        fontWeight = fontWeight,
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
        color = color.withState(enabled),
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
    overflow: TextOverflow = TextOverflow.Ellipsis,
    maxLines: Int = Int.MAX_VALUE,
    enabled: Boolean = true,
) {
    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = color.withState(enabled),
        textAlign = textAlign,
        fontWeight = fontWeight,
        overflow = overflow,
        maxLines = maxLines,
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
        color = color.withState(enabled),
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
