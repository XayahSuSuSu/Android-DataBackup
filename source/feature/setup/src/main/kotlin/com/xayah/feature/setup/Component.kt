package com.xayah.feature.setup

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.xayah.core.ui.component.ActionButton
import com.xayah.core.ui.component.BodySmallText
import com.xayah.core.ui.component.InnerBottomSpacer
import com.xayah.core.ui.component.InnerTopSpacer
import com.xayah.core.ui.component.LabelLargeText
import com.xayah.core.ui.material3.Surface
import com.xayah.core.ui.theme.ThemedColorSchemeKeyTokens
import com.xayah.core.ui.theme.value
import com.xayah.core.ui.theme.withState
import com.xayah.core.ui.token.SizeTokens

@Composable
fun SetupScaffold(topBar: @Composable () -> Unit = {}, actions: @Composable RowScope.() -> Unit, content: @Composable LazyItemScope.() -> Unit) {
    Scaffold(
        containerColor = ThemedColorSchemeKeyTokens.Surface.value,
        topBar = topBar,
    ) { innerPadding ->
        Column {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        InnerTopSpacer(innerPadding = innerPadding)
                    }
                    item {
                        content()
                    }

                    item {
                        InnerBottomSpacer(innerPadding = innerPadding)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(SizeTokens.Level16),
                horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level12, Alignment.End),
            ) {
                actions()
            }

            InnerBottomSpacer(innerPadding = innerPadding)
        }
    }
}

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun PermissionButton(
    enabled: Boolean = true,
    title: String,
    desc: String,
    envState: EnvState,
    onSetting: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    ActionButton(
        enabled = enabled,
        icon = envState.icon,
        colorContainer = envState.colorContainer,
        colorL80D20 = envState.colorL80D20,
        onColorContainer = envState.onColorContainer,
        trailingIcon = {
            if (onSetting != null) {
                Surface(modifier = Modifier.size(SizeTokens.Level36), shape = CircleShape, onClick = onSetting, color = ThemedColorSchemeKeyTokens.Transparent.value.withState(enabled)) {
                    Icon(
                        modifier = Modifier.padding(SizeTokens.Level8),
                        imageVector = Icons.Outlined.Settings,
                        tint = ThemedColorSchemeKeyTokens.OnSurface.value.withState(enabled),
                        contentDescription = null
                    )
                }
            }

            Icon(
                imageVector = Icons.Rounded.KeyboardArrowRight,
                tint = envState.onColorContainer.value.withState(enabled),
                contentDescription = null
            )
        },
        onClick = onClick
    ) {
        Column(modifier = Modifier.weight(1f)) {
            LabelLargeText(text = title, color = ThemedColorSchemeKeyTokens.OnSurface.value)
            BodySmallText(text = desc, color = ThemedColorSchemeKeyTokens.OnSurfaceVariant.value)
        }
    }
}
