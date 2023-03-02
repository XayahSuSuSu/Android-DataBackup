package com.xayah.databackup.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import com.xayah.databackup.R

@Composable
fun IconButton(
    icon: ImageVector,
    contentDescription: String? = null,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Icon(imageVector = icon, contentDescription = contentDescription)
    }
}

@Composable
fun TextButton(
    text: String,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        content = { Text(text = text) })
}

@Composable
fun IconTextButton(
    icon: ImageVector,
    text: String,
    contentDescription: String? = null,
    onClick: () -> Unit
) {
    val smallPadding = dimensionResource(R.dimen.padding_small)
    Button(
        onClick = onClick,
        content = {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.paddingEnd(smallPadding),
            )
            TitleSmallText(text = text)
        }
    )
}
