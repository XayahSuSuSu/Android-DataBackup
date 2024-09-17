package com.xayah.core.ui.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRowScope
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.xayah.core.ui.material3.SegmentedButtonContent
import com.xayah.core.ui.material3.interactionCountAsState
import com.xayah.core.ui.material3.interactionZIndex
import com.xayah.core.ui.token.SizeTokens

@Composable
@ExperimentalMaterial3Api
fun SingleChoiceSegmentedButtonRowScope.ActionSegmentedButton(
    onClick: () -> Unit,
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
    baseShape: CornerBasedShape = RoundedCornerShape(SizeTokens.Level24),
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(containerColor),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit,
) {
    val interactionCount = interactionSource.interactionCountAsState()

    androidx.compose.material3.Surface(
        modifier = modifier
            .weight(1f)
            .interactionZIndex(false, interactionCount)
            .defaultMinSize(
                minWidth = ButtonDefaults.MinWidth,
                minHeight = ButtonDefaults.MinHeight
            )
            .semantics { role = Role.RadioButton },
        selected = false,
        onClick = onClick,
        enabled = enabled,
        shape = SegmentedButtonDefaults.itemShape(index = index, count = count, baseShape = baseShape),
        color = containerColor,
        contentColor = contentColor,
        border = null,
        interactionSource = interactionSource
    ) {
        SegmentedButtonContent({}, content)
    }
}