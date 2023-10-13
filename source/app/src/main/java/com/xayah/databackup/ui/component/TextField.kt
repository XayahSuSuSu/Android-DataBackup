package com.xayah.databackup.ui.component

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.xayah.databackup.ui.theme.ColorScheme
import com.xayah.databackup.ui.token.SearchTokens
import com.xayah.databackup.ui.token.TextFieldTokens

@Composable
fun RoundedTextField(
    modifier: Modifier = Modifier,
    value: String,
    placeholder: String,
    enabled: Boolean,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        modifier = modifier,
        shape = CircleShape,
        value = value,
        enabled = enabled,
        placeholder = { TitleMediumBoldText(text = placeholder) },
        onValueChange = onValueChange,
        singleLine = true,
        leadingIcon = if (leadingIcon != null) {
            {
                Icon(
                    modifier = Modifier
                        .paddingStart(SearchTokens.IconPaddingStart)
                        .size(TextFieldTokens.IconSize),
                    imageVector = leadingIcon,
                    contentDescription = null,
                )
            }
        } else {
            null
        },
        trailingIcon = if (trailingIcon != null) {
            {
                Icon(
                    modifier = Modifier
                        .paddingEnd(SearchTokens.IconButtonPaddingEnd)
                        .size(TextFieldTokens.IconSize),
                    imageVector = trailingIcon,
                    contentDescription = null
                )
            }
        } else {
            null
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = ColorScheme.surfaceVariant(),
            unfocusedContainerColor = ColorScheme.surfaceVariant(),
            disabledContainerColor = ColorScheme.surfaceVariant(),
            errorContainerColor = ColorScheme.surfaceVariant(),
        )
    )
}
