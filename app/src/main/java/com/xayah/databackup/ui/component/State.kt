package com.xayah.databackup.ui.component

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.xayah.databackup.R
import com.xayah.databackup.ui.theme.ColorScheme
import com.xayah.databackup.ui.token.State
import com.xayah.databackup.ui.token.StateTokens

@Composable
fun State(modifier: Modifier = Modifier, state: State = State.Loading, content: @Composable () -> Unit) {
    val colorError = ColorScheme.error()
    Crossfade(targetState = state, label = StateTokens.CrossFadeLabel) {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (it) {
                State.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.paddingBottom(StateTokens.IndicatorPadding)
                    )
                    TitleSmallBoldText(
                        text = stringResource(R.string.loading),
                    )
                }

                State.Failed -> {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = null,
                        tint = colorError,
                        modifier = Modifier
                            .size(StateTokens.IconSize)
                            .paddingBottom(StateTokens.IndicatorPadding)
                    )
                    TitleSmallBoldText(
                        text = stringResource(R.string.loading_failed),
                        color = colorError,
                    )
                }

                State.Succeed -> {
                    content()
                }
            }
        }
    }
}
