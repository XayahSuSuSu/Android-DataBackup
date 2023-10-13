package com.xayah.databackup.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.xayah.databackup.ui.theme.ColorScheme
import com.xayah.databackup.ui.token.SearchTokens
import com.xayah.databackup.ui.token.TextFieldTokens

@Composable
fun RoundedTextField(
    modifier: Modifier = Modifier,
    value: String,
    placeholder: String,
    enabled: Boolean,
    visualTransformation: VisualTransformation,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions,
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
        visualTransformation = visualTransformation,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        keyboardOptions = keyboardOptions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = ColorScheme.surfaceVariant(),
            unfocusedContainerColor = ColorScheme.surfaceVariant(),
            disabledContainerColor = ColorScheme.surfaceVariant(),
            errorContainerColor = ColorScheme.surfaceVariant(),
        )
    )
}

@Composable
fun CleanableTextField(
    modifier: Modifier = Modifier,
    value: String,
    placeholder: String,
    enabled: Boolean,
    leadingIcon: ImageVector? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onCleanClick: () -> Unit,
    onValueChange: (String) -> Unit,
) {
    RoundedTextField(
        modifier = modifier,
        value = value,
        placeholder = placeholder,
        enabled = enabled,
        visualTransformation = VisualTransformation.None,
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
        trailingIcon = if (value.isNotEmpty() && enabled) {
            {
                IconButton(
                    modifier = Modifier.paddingEnd(SearchTokens.IconButtonPaddingEnd),
                    onClick = onCleanClick
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = null
                    )
                }
            }
        } else {
            null
        },
        keyboardOptions = keyboardOptions,
        onValueChange = onValueChange
    )
}

@Composable
fun CleanablePasswordTextField(
    modifier: Modifier = Modifier,
    value: String,
    placeholder: String,
    enabled: Boolean,
    leadingIcon: ImageVector? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onCleanClick: () -> Unit,
    onValueChange: (String) -> Unit,
) {
    var visible by rememberSaveable { mutableStateOf(false) }

    RoundedTextField(
        modifier = modifier,
        value = value,
        placeholder = placeholder,
        enabled = enabled,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
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
        trailingIcon = if (value.isNotEmpty() && enabled) {
            {
                Row(modifier = Modifier.paddingEnd(SearchTokens.IconButtonPaddingEnd), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { visible = visible.not() }) {
                        Icon(
                            imageVector = if (visible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                            contentDescription = null
                        )
                    }
                    IconButton(onClick = onCleanClick) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = null
                        )
                    }
                }
            }
        } else {
            null
        },
        keyboardOptions = keyboardOptions,
        onValueChange = onValueChange
    )
}
