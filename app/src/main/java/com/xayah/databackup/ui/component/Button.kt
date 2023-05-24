package com.xayah.databackup.ui.component

import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.xayah.databackup.ui.token.ButtonTokens

@Composable
fun IconTextButton(icon: ImageVector, text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        content = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.paddingEnd(ButtonTokens.IconTextButtonPadding),
            )
            TitleSmallBoldText(text = text)
        }
    )
}
