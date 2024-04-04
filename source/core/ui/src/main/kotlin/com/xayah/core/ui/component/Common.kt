package com.xayah.core.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.xayah.core.ui.material3.toColor
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.value

@ExperimentalMaterial3Api
@Composable
fun Section(title: StringResourceToken, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.paddingHorizontal(SizeTokens.Level16), verticalArrangement = Arrangement.spacedBy(SizeTokens.Level8)) {
        LabelLargeText(text = title.value, color = ColorSchemeKeyTokens.OnSurfaceVariant.toColor(), fontWeight = FontWeight.SemiBold)

        content()
    }
}

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun ActionButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVectorToken,
    colorContainer: ColorSchemeKeyTokens,
    colorL80D20: ColorSchemeKeyTokens,
    onColorContainer: ColorSchemeKeyTokens,
    actionIcon: ImageVectorToken? = null,
    onClick: () -> Unit = {},
    content: @Composable RowScope.() -> Unit
) {
    Card(
        modifier = modifier.wrapContentHeight(),
        shape = RoundedCornerShape(SizeTokens.Level18),
        colors = CardDefaults.cardColors(containerColor = colorContainer.toColor(enabled)),
        onClick = onClick,
        enabled = enabled,
    ) {
        Row(
            modifier = Modifier
                .padding(SizeTokens.Level12)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level10)
        ) {
            Surface(modifier = Modifier.size(SizeTokens.Level36), shape = CircleShape, color = colorL80D20.toColor(enabled), enabled = enabled) {
                Icon(
                    modifier = Modifier.padding(SizeTokens.Level8),
                    imageVector = icon.value,
                    tint = onColorContainer.toColor(enabled),
                    contentDescription = null,
                )
            }
            content()
            if (actionIcon != null)
                Icon(
                    imageVector = actionIcon.value,
                    tint = onColorContainer.toColor(enabled),
                    contentDescription = null
                )
        }
    }
}
