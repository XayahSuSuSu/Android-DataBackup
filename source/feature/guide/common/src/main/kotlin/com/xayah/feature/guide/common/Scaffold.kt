package com.xayah.feature.guide.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.xayah.core.ui.component.InnerTopSpacer
import com.xayah.core.ui.component.TopBarTitle
import com.xayah.core.ui.component.paddingBottom
import com.xayah.core.ui.component.paddingHorizontal
import com.xayah.core.ui.material3.toColor
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.token.PaddingTokens
import com.xayah.core.ui.util.value

@Composable
fun GuideScaffold(
    isInitializing: Boolean,
    snackbarHostState: SnackbarHostState,
    topBarTitle: StringResourceToken,
    topBarIcon: ImageVectorToken,
    fabIcon: ImageVectorToken,
    onFabClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Scaffold(
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            AnimatedVisibility(visible = isInitializing.not(), enter = scaleIn(), exit = scaleOut()) {
                FloatingActionButton(onClick = { onFabClick.invoke() }) {
                    Icon(imageVector = fabIcon.value, contentDescription = null)
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Column(modifier = Modifier.paddingHorizontal(PaddingTokens.Level3)) {
            InnerTopSpacer(innerPadding = innerPadding)
            InnerTopSpacer(innerPadding = innerPadding)

            // TopBar
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = topBarIcon.value,
                    contentDescription = null,
                    tint = ColorSchemeKeyTokens.OnSurfaceVariant.toColor(),
                    modifier = Modifier
                        .size(PaddingTokens.Level6)
                        .paddingBottom(PaddingTokens.Level1)
                )
                TopBarTitle(text = topBarTitle.value)
            }
            content()
        }
    }
}