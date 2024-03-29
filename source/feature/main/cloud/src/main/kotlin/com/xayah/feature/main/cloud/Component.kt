package com.xayah.feature.main.cloud

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CloudCircle
import androidx.compose.material.icons.rounded.ManageAccounts
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.xayah.core.model.database.CloudEntity
import com.xayah.core.ui.component.BodySmallText
import com.xayah.core.ui.component.Divider
import com.xayah.core.ui.component.IconButton
import com.xayah.core.ui.component.ModalActionDropdownMenu
import com.xayah.core.ui.component.TitleLargeText
import com.xayah.core.ui.component.outlinedCardBorder
import com.xayah.core.ui.component.paddingBottom
import com.xayah.core.ui.component.paddingHorizontal
import com.xayah.core.ui.component.paddingTop
import com.xayah.core.ui.component.paddingVertical
import com.xayah.core.ui.material3.toColor
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens
import com.xayah.core.ui.model.ActionMenuItem
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.token.PaddingTokens
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.fromString
import com.xayah.core.ui.util.fromVector
import com.xayah.core.ui.util.value

@Composable
fun AccountRow(icon: ImageVectorToken, text: StringResourceToken, color: Color = LocalContentColor.current) {
    Row(horizontalArrangement = Arrangement.spacedBy(PaddingTokens.Level1), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon.value,
            tint = color,
            contentDescription = null
        )
        BodySmallText(text = text.value, color = color, fontWeight = FontWeight.Bold)
    }
}

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun AccountCard(
    modifier: Modifier = Modifier,
    item: CloudEntity,
    selected: Boolean,
    accountActions: List<ActionMenuItem>,
    onCardClick: () -> Unit,
    chipGroup: @Composable RowScope.() -> Unit,
) {
    AccountCard(
        modifier = modifier,
        name = item.name,
        user = item.user,
        remote = item.remote,
        selected = selected,
        accountActions = accountActions,
        onCardClick = onCardClick,
        chipGroup = chipGroup,
    )
}

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun AccountCard(
    modifier: Modifier = Modifier,
    name: String,
    user: String,
    remote: String,
    selected: Boolean,
    accountActions: List<ActionMenuItem>,
    onCardClick: () -> Unit,
    chipGroup: @Composable RowScope.() -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var accountMenuExpanded by remember { mutableStateOf(false) }

    com.xayah.core.ui.component.Card(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        enabled = true,
        onClick = onCardClick,
        onLongClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            accountMenuExpanded = true
        },
        border = if (selected) outlinedCardBorder(borderColor = ColorSchemeKeyTokens.Primary.toColor()) else null,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .paddingHorizontal(PaddingTokens.Level3)
                .paddingTop(PaddingTokens.Level3)
                .paddingBottom(PaddingTokens.Level1)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TitleLargeText(text = name, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.weight(1f))
                    if (selected) Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.Top),
                        tint = ColorSchemeKeyTokens.Primary.toColor(),
                    )
                }
                AccountRow(
                    icon = ImageVectorToken.fromVector(Icons.Rounded.AccountCircle),
                    text = StringResourceToken.fromString(user)
                )

                val remoteRowColor = if (remote.isEmpty()) ColorSchemeKeyTokens.Error.toColor() else LocalContentColor.current
                val remoteRowText = remote.ifEmpty { stringResource(id = R.string.remote_not_set) }
                AccountRow(
                    icon = ImageVectorToken.fromVector(Icons.Rounded.CloudCircle),
                    text = StringResourceToken.fromString(remoteRowText),
                    color = remoteRowColor,
                )
                Divider(modifier = Modifier.paddingTop(PaddingTokens.Level1))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically,
                    content = {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(PaddingTokens.Level1),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            chipGroup()
                        }
                        Divider(
                            modifier = Modifier
                                .fillMaxHeight()
                                .paddingVertical(PaddingTokens.Level1)
                                .width(SizeTokens.Level1)
                        )
                        Box(modifier = Modifier.wrapContentSize(Alignment.Center)) {
                            IconButton(icon = ImageVectorToken.fromVector(Icons.Rounded.ManageAccounts), onClick = { accountMenuExpanded = true })

                            ModalActionDropdownMenu(
                                expanded = accountMenuExpanded,
                                actionList = accountActions,
                                onDismissRequest = { accountMenuExpanded = false })
                        }
                    }
                )
            }
        }
    }
}
