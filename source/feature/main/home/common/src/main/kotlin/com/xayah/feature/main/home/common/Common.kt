package com.xayah.feature.main.home.common

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavHostController
import com.xayah.core.ui.component.AnimatedTextContainer
import com.xayah.core.ui.component.BodySmallText
import com.xayah.core.ui.component.LabelLargeText
import com.xayah.core.ui.component.LabelSmallText
import com.xayah.core.ui.component.ModalStringListDropdownMenu
import com.xayah.core.ui.component.TitleLargeText
import com.xayah.core.ui.component.TitleMediumText
import com.xayah.core.ui.component.TitleSmallText
import com.xayah.core.ui.component.TopBarTitle
import com.xayah.core.ui.component.paddingBottom
import com.xayah.core.ui.component.paddingEnd
import com.xayah.core.ui.component.paddingHorizontal
import com.xayah.core.ui.component.paddingStart
import com.xayah.core.ui.component.paddingTop
import com.xayah.core.ui.component.paddingVertical
import com.xayah.core.ui.material3.toColor
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.token.ModalMenuTokens
import com.xayah.core.ui.token.PaddingTokens
import com.xayah.core.ui.util.navigateAndPopBackStack
import com.xayah.core.ui.util.value
import com.xayah.feature.main.home.common.model.BottomBarItem
import com.xayah.feature.main.home.common.model.MapItem
import com.xayah.feature.main.home.common.model.SettingsInfoItem

@ExperimentalMaterial3Api
@Composable
fun TopBar(scrollBehavior: TopAppBarScrollBehavior?, title: String) {
    CenterAlignedTopAppBar(
        title = { TopBarTitle(text = title) },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
fun BottomBar(currentRoute: String?, navController: NavHostController, routeList: List<BottomBarItem>) {
    NavigationBar {
        routeList.forEach { item ->
            NavigationBarItem(
                icon = { Icon(imageVector = item.iconToken.value, contentDescription = null) },
                label = { Text(text = item.label.value) },
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route)
                        navController.navigateAndPopBackStack(item.route)
                }
            )
        }
    }
}

@Composable
fun OverLookCard(icon: ImageVectorToken, title: StringResourceToken.StringIdToken, content: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        colors = CardDefaults.cardColors(containerColor = ColorSchemeKeyTokens.PrimaryContainer.toColor()),
    ) {
        Column(modifier = Modifier.padding(PaddingTokens.Level3)) {
            Row(
                modifier = Modifier.paddingBottom(PaddingTokens.Level3),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon.value,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.weight(1f))
                LabelLargeText(text = stringResource(R.string.overlook), color = ColorSchemeKeyTokens.Secondary.toColor(), fontWeight = FontWeight.ExtraBold)
            }
            BodySmallText(text = title.value)
            TitleLargeText(
                text = content.ifEmpty { stringResource(id = R.string.none) },
                color = ColorSchemeKeyTokens.Primary.toColor(),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun Module(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(PaddingTokens.Level2)) {
        TitleMediumText(text = title, fontWeight = FontWeight.Bold)
        content()
    }
}

@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@Composable
fun FossActivityCard(
    modifier: Modifier = Modifier,
    label: StringResourceToken,
    icon: ImageVectorToken,
    details: List<MapItem>,
    onClick: () -> Unit,
) {
    ActivityCard(
        modifier = modifier,
        label = label,
        icon = icon,
        onClick = onClick,
        details = details,
    )
}

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun ActivityCard(
    modifier: Modifier = Modifier,
    label: StringResourceToken,
    icon: ImageVectorToken,
    details: List<MapItem>,
    onClick: () -> Unit,
    content: @Composable (ColumnScope.() -> Unit)? = null,
) {
    OutlinedCard(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = ColorSchemeKeyTokens.SecondaryContainer.toColor()),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(PaddingTokens.Level1)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(PaddingTokens.Level1),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = icon.value, tint = ColorSchemeKeyTokens.Secondary.toColor(), contentDescription = null)
                LabelLargeText(modifier = Modifier.weight(1f), text = label.value, color = ColorSchemeKeyTokens.Secondary.toColor())
                Icon(imageVector = Icons.Rounded.KeyboardArrowRight, tint = ColorSchemeKeyTokens.Secondary.toColor(), contentDescription = null)
            }
            Spacer(modifier = Modifier.height(PaddingTokens.Level3))
            details.forEach {
                val value by it.value.collectAsState(initial = "")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LabelSmallText(text = it.key.value)
                    Spacer(modifier = Modifier.weight(1f))
                    AnimatedTextContainer(targetState = value) { text ->
                        LabelSmallText(text = text)
                    }
                }
            }
        }
        content?.invoke(this)
    }
}

@Composable
fun SettingsInfo(modifier: Modifier = Modifier, info: SettingsInfoItem) {
    Column(modifier = modifier) {
        Icon(imageVector = info.icon.value, contentDescription = null)
        CompositionLocalProvider(LocalContentColor provides ColorSchemeKeyTokens.OnSurfaceVariant.toColor()) {
            LabelSmallText(
                modifier = Modifier.paddingTop(PaddingTokens.Level1),
                text = info.title.value,
                color = if (info.onWarning) ColorSchemeKeyTokens.Error.toColor() else Color.Unspecified
            )
        }
        TitleMediumText(
            text = info.content.value,
            color = if (info.onWarning) ColorSchemeKeyTokens.Error.toColor() else Color.Unspecified,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SettingsClickable(
    modifier: Modifier = Modifier,
    leadingContent: @Composable (() -> Unit)? = null,
    headlineContent: @Composable () -> Unit,
    supportingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .paddingVertical(PaddingTokens.Level3)
            .paddingEnd(PaddingTokens.Level3),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(PaddingTokens.Level7), contentAlignment = Alignment.Center) {
            leadingContent?.invoke()
        }
        Column(modifier = Modifier.weight(1f)) {
            headlineContent.invoke()
            supportingContent?.let { content ->
                CompositionLocalProvider(LocalContentColor provides ColorSchemeKeyTokens.OnSurfaceVariant.toColor(), content = content)
            }
        }
        trailingContent?.invoke()
    }
}

@Composable
fun SettingsSwitch(
    modifier: Modifier = Modifier,
    icon: ImageVectorToken? = null,
    title: StringResourceToken,
    content: StringResourceToken,
    checked: Boolean = false,
    onCheckedChange: () -> Unit,
) {
    SettingsClickable(
        modifier = modifier,
        leadingContent = {
            icon?.let { Icon(imageVector = it.value, contentDescription = null) }
        },
        headlineContent = {
            TitleMediumText(text = title.value)
        },
        supportingContent = {
            BodySmallText(text = content.value, fontWeight = FontWeight.Bold)
        },
        trailingContent = {
            Switch(modifier = Modifier.paddingStart(PaddingTokens.Level3), checked = checked, onCheckedChange = {
                onCheckedChange()
            })
        }
    ) {
        onCheckedChange()
    }
}

@Composable
fun SettingsModalDropdownMenu(
    modifier: Modifier = Modifier,
    icon: ImageVectorToken? = null,
    title: StringResourceToken,
    content: StringResourceToken,
    selected: String,
    selectedIndex: Int = 0,
    list: List<String>,
    onClick: (() -> Unit)? = null,
    onSelected: (index: Int, selected: String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    SettingsClickable(
        modifier = modifier,
        leadingContent = {
            icon?.let { Icon(imageVector = it.value, contentDescription = null) }
        },
        headlineContent = {
            TitleMediumText(text = title.value)
        },
        supportingContent = {
            BodySmallText(text = content.value, fontWeight = FontWeight.Bold)
        },
        trailingContent = {
            Box(modifier = Modifier.wrapContentSize(Alignment.Center)) {
                TitleMediumText(modifier = Modifier.paddingStart(PaddingTokens.Level3), text = selected)

                ModalStringListDropdownMenu(
                    expanded = expanded,
                    selectedIndex = selectedIndex,
                    list = list,
                    maxDisplay = ModalMenuTokens.DefaultMaxDisplay,
                    onSelected = { index, selected ->
                        expanded = false
                        onSelected(index, selected)
                    },
                    onDismissRequest = { expanded = false }
                )
            }
        }
    ) {
        expanded = true
        onClick?.invoke()
    }
}

@Composable
fun SettingsTitle(modifier: Modifier = Modifier, title: StringResourceToken) {
    TitleSmallText(
        modifier = modifier.paddingHorizontal(PaddingTokens.Level7),
        text = title.value,
        color = ColorSchemeKeyTokens.Primary.toColor(),
        fontWeight = FontWeight.Bold
    )
}
