package com.xayah.databackup.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import com.xayah.databackup.R

@ExperimentalMaterial3Api
@Composable
fun SearchBar(onTextChange: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val smallPadding = dimensionResource(R.dimen.padding_small)
    val mediumPadding = dimensionResource(R.dimen.padding_medium)
    val iconTinySize = dimensionResource(R.dimen.icon_tiny_size)

    TextField(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape),
        value = text,
        onValueChange = {
            text = it
            onTextChange(it)
        },
        singleLine = true,
        leadingIcon = {
            Icon(
                modifier = Modifier
                    .paddingStart(mediumPadding)
                    .size(iconTinySize),
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
            )
        },
        trailingIcon = if (text.isNotEmpty()) {
            {
                IconButton(
                    modifier = Modifier.paddingEnd(smallPadding),
                    onClick = {
                        text = ""
                        onTextChange("")
                    }
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
        colors = TextFieldDefaults.textFieldColors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        )
    )
}
