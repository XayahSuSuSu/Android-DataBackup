package com.xayah.feature.main.cloud

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.IntSize
import com.xayah.core.ui.component.Divider
import com.xayah.core.ui.component.InnerBottomSpacer
import com.xayah.core.ui.component.InnerTopSpacer
import com.xayah.core.ui.component.SecondaryLargeTopBar
import com.xayah.core.ui.component.paddingBottom
import com.xayah.core.ui.component.paddingEnd
import com.xayah.core.ui.component.paddingStart
import com.xayah.core.ui.material3.SnackbarHost
import com.xayah.core.ui.material3.SnackbarHostState
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.token.TextFieldTokens

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun CloudScaffold(
    scrollBehavior: TopAppBarScrollBehavior,
    snackbarHostState: SnackbarHostState? = null,
    title: String,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (BoxScope.() -> Unit)
) {
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SecondaryLargeTopBar(
                scrollBehavior = scrollBehavior,
                title = title,
                actions = actions
            )
        },
        snackbarHost = {
            if (snackbarHostState != null)
                SnackbarHost(hostState = snackbarHostState)
        },
    ) { innerPadding ->
        Column {
            InnerTopSpacer(innerPadding = innerPadding)

            Box(modifier = Modifier.weight(1f), content = content)
        }
    }
}

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun AccountSetupScaffold(
    scrollBehavior: TopAppBarScrollBehavior, title: String,
    snackbarHostState: SnackbarHostState,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (BoxScope.(innerPadding: PaddingValues) -> Unit)
) {
    var _innerPadding by remember { mutableStateOf(PaddingValues(SizeTokens.Level0)) }
    var bottomBarSize by remember { mutableStateOf(IntSize.Zero) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SecondaryLargeTopBar(
                scrollBehavior = scrollBehavior,
                title = title,
            )
        },
        snackbarHost = {
            with(LocalDensity.current) {
                SnackbarHost(
                    modifier = Modifier
                        .consumeWindowInsets(_innerPadding)
                        .imePadding()
                        .paddingBottom(bottomBarSize.height.toDp() + SizeTokens.Level24 + SizeTokens.Level4),
                    hostState = snackbarHostState,
                )
            }
        },
    ) { innerPadding ->
        _innerPadding = innerPadding
        Column(
            modifier = Modifier
                .consumeWindowInsets(innerPadding)
                .imePadding()
        ) {
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
                        content(innerPadding)
                    }

                    item {
                        InnerBottomSpacer(innerPadding = innerPadding)
                    }
                }
            }

            Divider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(SizeTokens.Level16)
                    .onSizeChanged { bottomBarSize = it },
                horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level12, Alignment.End),
            ) {
                actions()
            }

            InnerBottomSpacer(innerPadding = innerPadding)
        }
    }
}

@Composable
fun SetupTextField(
    modifier: Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    value: String,
    leadingIcon: ImageVector,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    prefix: String? = null,
    label: String,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onClick: (() -> Unit)? = null,
    singleLine: Boolean = true,
    interactionSource: MutableInteractionSource = if (onClick == null)
        remember { MutableInteractionSource() }
    else
        remember { MutableInteractionSource() }
            .also { src ->
                LaunchedEffect(src) {
                    src.interactions.collect {
                        if (it is PressInteraction.Release) {
                            onClick()
                        }
                    }
                }
            },
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        value = value,
        singleLine = singleLine,
        leadingIcon = {
            Icon(
                modifier = Modifier
                    .paddingStart(TextFieldTokens.LeadingIconPaddingStart)
                    .size(TextFieldTokens.IconSize),
                imageVector = leadingIcon,
                contentDescription = null,
            )
        },
        trailingIcon = if (trailingIcon == null) null else {
            {
                IconButton(modifier = Modifier.paddingEnd(TextFieldTokens.TrailingIconPaddingEnd), onClick = { onTrailingIconClick?.invoke() }) {
                    Icon(imageVector = trailingIcon, contentDescription = null)
                }
            }
        },
        prefix = if (prefix == null) null else {
            {
                Text(
                    text = prefix
                )
            }
        },
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        shape = CircleShape,
        onValueChange = onValueChange,
        label = { Text(text = label) },
        interactionSource = interactionSource,
    )
}