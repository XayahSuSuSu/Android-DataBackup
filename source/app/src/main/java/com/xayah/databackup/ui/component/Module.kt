package com.xayah.databackup.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.xayah.databackup.ui.token.ModuleTokens

@Composable
fun Module(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(ModuleTokens.InnerPadding)) {
        TitleMediumBoldText(text = title)
        content()
    }
}
