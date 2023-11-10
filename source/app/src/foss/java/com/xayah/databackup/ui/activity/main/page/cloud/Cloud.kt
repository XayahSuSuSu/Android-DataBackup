package com.xayah.databackup.ui.activity.main.page.cloud

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.xayah.databackup.R
import com.xayah.core.model.EmojiString
import com.xayah.databackup.ui.component.EmojiText
import com.xayah.databackup.ui.component.TitleMediumBoldText
import com.xayah.databackup.ui.component.paddingHorizontal
import com.xayah.databackup.ui.theme.ColorScheme
import com.xayah.databackup.ui.token.CommonTokens

@Composable
fun PageCloud() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(CommonTokens.PaddingMedium)) {
            EmojiText(emoji = EmojiString.SWEAT_DROPLETS, size = CommonTokens.EmojiLargeSize)

            TitleMediumBoldText(
                modifier = Modifier.paddingHorizontal(CommonTokens.PaddingLarge),
                text = "${stringResource(R.string.premium_for_this_feature)} 🌟",
                color = ColorScheme.primary()
            )
        }
    }
}
