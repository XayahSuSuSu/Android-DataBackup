package com.xayah.databackup.ui.component

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import com.xayah.databackup.ui.theme.JetbrainsMonoFamily
import com.xayah.databackup.ui.token.CommonTokens

@Composable
fun TopBarTitle(modifier: Modifier = Modifier, textAlign: TextAlign? = null, text: String) {
    Text(
        modifier = modifier,
        text = text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = textAlign,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun HeadlineLargeBoldText(
    modifier: Modifier = Modifier,
    text: String,
    color: Color = Color.Unspecified,
) {
    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        color = color,
    )
}

@Composable
fun HeadlineMediumBoldText(
    modifier: Modifier = Modifier,
    text: String,
    color: Color = Color.Unspecified,
) {
    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = color,
    )
}

@Composable
fun TitleLargeText(modifier: Modifier = Modifier, text: String, color: Color = Color.Unspecified) {
    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = color,
    )
}

@Composable
fun TitleLargeBoldText(modifier: Modifier = Modifier, text: String) {
    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
fun TitleMediumBoldText(
    modifier: Modifier = Modifier,
    text: String,
    color: Color = Color.Unspecified,
) {
    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = color,
    )
}

@Composable
fun TitleMediumText(
    modifier: Modifier = Modifier,
    text: String,
    color: Color = Color.Unspecified,
) {
    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = color,
    )
}

@Composable
fun TitleSmallBoldText(
    modifier: Modifier = Modifier,
    text: String,
    color: Color = Color.Unspecified,
) {
    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = color,
    )
}

@Composable
fun BodySmallText(modifier: Modifier = Modifier, text: String) {
    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
fun BodySmallBoldText(modifier: Modifier = Modifier, text: String, color: Color = Color.Unspecified, enabled: Boolean = true) {
    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Bold,
        color = if (enabled) color else color.copy(alpha = CommonTokens.DisabledAlpha),
    )
}

@Composable
fun BodyMediumBoldText(modifier: Modifier = Modifier, text: String, color: Color = Color.Unspecified) {
    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        color = color
    )
}

@Composable
fun LabelLargeText(modifier: Modifier = Modifier, text: String) {
    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.labelLarge,
    )
}

@Composable
fun LabelLargeExtraBoldText(modifier: Modifier = Modifier, text: String) {
    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.ExtraBold,
    )
}

@Composable
fun LabelMediumText(modifier: Modifier = Modifier, textAlign: TextAlign? = null, text: String) {
    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.labelMedium,
        textAlign = textAlign,
    )
}

@Composable
fun LabelSmallText(modifier: Modifier = Modifier, textAlign: TextAlign? = null, text: String) {
    Text(
        modifier = modifier,
        text = text,
        style = MaterialTheme.typography.labelSmall,
        textAlign = textAlign,
    )
}

@Composable
fun JetbrainsMonoText(modifier: Modifier = Modifier, text: String, style: TextStyle = LocalTextStyle.current) {
    Text(
        text = text,
        modifier = modifier,
        fontFamily = JetbrainsMonoFamily,
        style = style,
    )
}

@Composable
fun JetbrainsMonoLabelSmallText(modifier: Modifier = Modifier, text: String) {
    JetbrainsMonoText(modifier = modifier, text = text, style = MaterialTheme.typography.labelSmall)
}

@Composable
fun TabText(text: String) {
    Text(text = text, maxLines = 2, overflow = TextOverflow.Ellipsis)
}

enum class EmojiString(val emoji: String) {
    PARTY_POPPER("🎉"),
    ALARM_CLOCK("⏰"),
    SPARKLING_HEART("💖"),
    BROKEN_HEART("💔"),
    SWEAT_DROPLETS("💦"),
}

@Composable
fun EmojiText(emoji: EmojiString, size: TextUnit) {
    Text(text = emoji.emoji, fontSize = size)
}
