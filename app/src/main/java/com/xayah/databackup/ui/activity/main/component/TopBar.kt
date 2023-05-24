package com.xayah.databackup.ui.activity.main.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.xayah.databackup.ui.component.TopBarTitle
import com.xayah.databackup.ui.component.paddingBottom
import com.xayah.databackup.ui.theme.ColorScheme
import com.xayah.databackup.ui.token.CommonTokens

@Composable
fun GuideTopBar(title: String, icon: ImageVector) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ColorScheme.onSurfaceVariant(),
            modifier = Modifier
                .size(CommonTokens.IconMediumSize)
                .paddingBottom(CommonTokens.PaddingSmall)
        )
        TopBarTitle(text = title)
    }
}

@ExperimentalMaterial3Api
@Composable
fun MainTopBar(title: String) {
    CenterAlignedTopAppBar(title = { TopBarTitle(text = title) })
}