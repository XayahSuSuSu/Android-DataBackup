package com.xayah.feature.guide.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.xayah.core.ui.component.BodyMediumText
import com.xayah.core.ui.component.BodySmallText
import com.xayah.core.ui.component.RoundChip
import com.xayah.core.ui.component.TitleLargeText
import com.xayah.core.ui.component.paddingBottom
import com.xayah.core.ui.component.paddingTop
import com.xayah.core.ui.component.paddingVertical
import com.xayah.core.ui.material3.toColor
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.token.PaddingTokens
import com.xayah.core.ui.util.value

@ExperimentalMaterial3Api
@Composable
fun IntroCard(serial: Char, title: StringResourceToken, subtitle: StringResourceToken, content: StringResourceToken) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Column(modifier = Modifier.padding(PaddingTokens.Level4)) {
            RoundChip(text = serial.toString())
            TitleLargeText(modifier = Modifier.paddingVertical(PaddingTokens.Level2), text = title.value)
            BodySmallText(modifier = Modifier.paddingVertical(PaddingTokens.Level2), text = subtitle.value, fontWeight = FontWeight.Bold)
            BodySmallText(modifier = Modifier.paddingVertical(PaddingTokens.Level2), text = content.value, fontWeight = FontWeight.Bold)
        }
    }
}

@ExperimentalMaterial3Api
@Composable
fun EnvCard(content: StringResourceToken, state: EnvState, enabled: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        onClick = {
            if (enabled) onClick()
        },
        colors = CardDefaults.cardColors(containerColor = state.backgroundColor.toColor())
    ) {
        Column(modifier = Modifier.padding(PaddingTokens.Level4)) {
            Icon(
                imageVector = state.icon.value,
                contentDescription = null,
                tint = state.tint.toColor(),
                modifier = Modifier
                    .size(PaddingTokens.Level6)
                    .paddingBottom(PaddingTokens.Level2)
            )
            BodyMediumText(
                modifier = Modifier.paddingTop(PaddingTokens.Level2),
                text = content.value,
                color = state.tint.toColor(),
                fontWeight = FontWeight.Bold
            )
        }
    }
}
