package com.xayah.feature.main.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.xayah.core.ui.component.BodySmallText
import com.xayah.core.ui.component.LabelLargeText
import com.xayah.core.ui.component.TitleLargeText
import com.xayah.core.ui.component.TitleMediumText
import com.xayah.core.ui.component.paddingBottom
import com.xayah.core.ui.material3.toColor
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.token.PaddingTokens
import com.xayah.core.ui.util.value

@Composable
fun OverLookCard(icon: ImageVectorToken, title: StringResourceToken.StringIdToken, content: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        colors = CardDefaults.cardColors(containerColor = ColorSchemeKeyTokens.PrimaryContainer.toColor()),
    ) {
        Column(modifier = Modifier.padding(PaddingTokens.Level4)) {
            Row(
                modifier = Modifier.paddingBottom(PaddingTokens.Level4),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon.value,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.weight(1f))
                LabelLargeText(
                    text = stringResource(R.string.overlook),
                    color = ColorSchemeKeyTokens.Secondary.toColor(),
                    fontWeight = FontWeight.ExtraBold
                )
            }
            BodySmallText(text = title.value)
            TitleLargeText(
                text = content.ifEmpty { stringResource(id = R.string.none) },
                color = ColorSchemeKeyTokens.Primary.toColor(),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun Module(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(PaddingTokens.Level3)) {
        TitleMediumText(text = title, fontWeight = FontWeight.Bold)
        content()
    }
}

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun ActivityCard(
    modifier: Modifier = Modifier,
    label: StringResourceToken,
    icon: ImageVectorToken,
    onClick: () -> Unit,
) {
    OutlinedCard(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = ColorSchemeKeyTokens.SecondaryContainer.toColor()),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(PaddingTokens.Level2)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(PaddingTokens.Level2),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = icon.value, tint = ColorSchemeKeyTokens.Secondary.toColor(), contentDescription = null)
                LabelLargeText(
                    modifier = Modifier.weight(1f),
                    text = label.value,
                    color = ColorSchemeKeyTokens.Secondary.toColor()
                )
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowRight,
                    tint = ColorSchemeKeyTokens.Secondary.toColor(),
                    contentDescription = null
                )
            }
            Spacer(modifier = Modifier.height(PaddingTokens.Level4 * 3))
        }
    }
}
