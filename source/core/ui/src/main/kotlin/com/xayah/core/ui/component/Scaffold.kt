package com.xayah.core.ui.component

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.xayah.core.common.util.BuildConfigUtil
import com.xayah.core.ui.material3.toColor
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.value

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun MainIndexSubScaffold(scrollBehavior: TopAppBarScrollBehavior, title: StringResourceToken, actions: @Composable RowScope.() -> Unit = {}, content: @Composable (BoxScope.() -> Unit)) {
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Row(modifier = Modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level8)) {
                        Text(text = title.value)
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .fillMaxHeight()
                                .background(ColorSchemeKeyTokens.PrimaryContainer.toColor()),
                            contentAlignment = Alignment.Center
                        ) {
                            LabelLargeText(modifier = Modifier.paddingHorizontal(SizeTokens.Level8), text = BuildConfigUtil.VERSION_NAME)
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                actions = actions,
            )
        },
    ) { innerPadding ->
        Column {
            InnerTopSpacer(innerPadding = innerPadding)

            Box(modifier = Modifier.weight(1f), content = content)
        }
    }
}
