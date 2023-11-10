package com.xayah.core.ui.component

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import com.xayah.core.ui.material3.toColor
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.token.TextFieldTokens
import com.xayah.core.ui.util.value

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
    prefix: String?,
    onValueChange: (String) -> Unit,
) {

    OutlinedTextField(
        modifier = modifier,
        shape = CircleShape,
        value = value,
        enabled = enabled,
        placeholder = { TitleMediumText(text = placeholder, fontWeight = FontWeight.Bold) },
        onValueChange = onValueChange,
        singleLine = true,
        visualTransformation = visualTransformation,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        keyboardOptions = keyboardOptions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = ColorSchemeKeyTokens.SurfaceVariant.toColor(),
            unfocusedContainerColor = ColorSchemeKeyTokens.SurfaceVariant.toColor(),
            disabledContainerColor = ColorSchemeKeyTokens.SurfaceVariant.toColor(),
            errorContainerColor = ColorSchemeKeyTokens.SurfaceVariant.toColor(),
        ),
        prefix = if (prefix != null) {
            { Text(prefix) }
        } else {
            null
        },
    )
}

@Composable
fun CleanableTextField(
    modifier: Modifier = Modifier,
    value: StringResourceToken,
    placeholder: StringResourceToken,
    enabled: Boolean = true,
    leadingIcon: ImageVectorToken? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    prefix: StringResourceToken? = null,
    onCleanClick: () -> Unit,
    onValueChange: (String) -> Unit,
) {
    RoundedTextField(
        modifier = modifier,
        value = value.value,
        placeholder = placeholder.value,
        enabled = enabled,
        visualTransformation = VisualTransformation.None,
        leadingIcon = if (leadingIcon != null) {
            {
                Icon(
                    modifier = Modifier
                        .paddingStart(TextFieldTokens.LeadingIconPaddingStart)
                        .size(TextFieldTokens.IconSize),
                    imageVector = leadingIcon.value,
                    contentDescription = null,
                )
            }
        } else {
            null
        },
        trailingIcon = if (value.value.isNotEmpty() && enabled) {
            {
                IconButton(
                    modifier = Modifier.paddingEnd(TextFieldTokens.TrailingIconPaddingEnd),
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
        prefix = prefix?.value,
        onValueChange = onValueChange
    )
}
