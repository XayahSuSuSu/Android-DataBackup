package com.xayah.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.xayah.core.ui.material3.toColor
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.token.ModalMenuTokens
import com.xayah.core.ui.token.PaddingTokens
import com.xayah.core.ui.util.fromVector
import com.xayah.core.ui.util.value

@Composable
fun HeaderItem(expand: Boolean, title: StringResourceToken, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TitleMediumText(
            modifier = Modifier.paddingBottom(PaddingTokens.Level1),
            text = title.value,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.weight(1f))
        IconButton(icon = ImageVectorToken.fromVector(if (expand) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore), onClick = onClick)
    }
}

@Composable
fun HeaderItem(title: StringResourceToken) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TitleMediumText(
            modifier = Modifier.paddingBottom(PaddingTokens.Level1),
            text = title.value,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun InfoItem(title: StringResourceToken, content: StringResourceToken) {
    Row {
        LabelMediumText(
            text = title.value,
            color = ColorSchemeKeyTokens.OnSurfaceVariant.toColor()
        )
        Spacer(modifier = Modifier.weight(1f))
        LabelMediumText(
            text = content.value,
            color = ColorSchemeKeyTokens.OnSurfaceVariant.toColor(),
        )
    }
}

@ExperimentalMaterial3Api
@Composable
fun InfoItem(
    title: StringResourceToken,
    content: StringResourceToken,
    selectedIndex: Int,
    list: List<String>,
    onSelected: (index: Int, selected: String) -> Unit,
) {
    Row {
        LabelMediumText(
            text = title.value,
            color = ColorSchemeKeyTokens.OnSurfaceVariant.toColor()
        )
        Spacer(modifier = Modifier.weight(1f))
        var expanded by remember { mutableStateOf(false) }
        Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
            LabelMediumText(
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = true,
                    onClick = {
                        if (list.isNotEmpty()) expanded = true
                    }
                ),
                text = content.value,
                color = com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens.Primary.toColor(),
                fontWeight = FontWeight.Bold,
                textDecoration = TextDecoration.Underline,
            )

            ModalStringListDropdownMenu(
                expanded = expanded,
                selectedIndex = selectedIndex,
                list = list,
                maxDisplay = ModalMenuTokens.DefaultMaxDisplay,
                onSelected = { index, selected ->
                    onSelected(index, selected)
                    expanded = false
                },
                onDismissRequest = { expanded = false }
            )
        }
    }
}
