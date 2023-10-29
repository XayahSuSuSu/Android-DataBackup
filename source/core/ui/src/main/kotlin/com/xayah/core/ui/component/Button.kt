package com.xayah.core.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.util.fromVector
import com.xayah.core.ui.util.value

@Composable
fun IconButton(modifier: Modifier = Modifier, icon: ImageVectorToken, onClick: () -> Unit) {
    IconButton(modifier = modifier, onClick = onClick) {
        Icon(
            imageVector = icon.value,
            contentDescription = null
        )
    }
}

@Composable
fun ArrowBackButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    IconButton(modifier = modifier, icon = ImageVectorToken.fromVector(Icons.Rounded.ArrowBack), onClick = onClick)
}

@Composable
fun ExtendedFab(expanded: Boolean = true, icon: ImageVectorToken, text: StringResourceToken, onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        expanded = expanded,
        icon = { Icon(imageVector = icon.value, contentDescription = null) },
        text = { Text(text = text.value) },
    )
}
