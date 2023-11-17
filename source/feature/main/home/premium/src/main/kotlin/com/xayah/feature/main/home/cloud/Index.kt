package com.xayah.feature.main.home.cloud

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.xayah.core.ui.component.paddingHorizontal
import com.xayah.core.ui.component.paddingTop
import com.xayah.core.ui.token.PaddingTokens

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun PageCloud() {
    LazyColumn(
        modifier = Modifier.paddingHorizontal(PaddingTokens.Level2),
        verticalArrangement = Arrangement.spacedBy(PaddingTokens.Level4)
    ) {
        item {
            Spacer(modifier = Modifier.paddingTop(PaddingTokens.Level3))
        }
    }
}
