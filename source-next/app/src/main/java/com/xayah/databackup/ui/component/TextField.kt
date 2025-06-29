package com.xayah.databackup.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.xayah.databackup.R

@Composable
fun SearchTextField(
    modifier: Modifier = Modifier,
    value: String,
    onClose: () -> Unit,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        modifier = modifier,
        shape = CircleShape,
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        leadingIcon = {
            Icon(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(24.dp),
                imageVector = ImageVector.vectorResource(R.drawable.ic_search),
                contentDescription = null,
            )
        },
        trailingIcon = {
            IconButton(modifier = Modifier.padding(end = 8.dp), onClick = onClose) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    imageVector = ImageVector.vectorResource(R.drawable.ic_x),
                    contentDescription = stringResource(R.string.close),
                )
            }
        },
    )
}
