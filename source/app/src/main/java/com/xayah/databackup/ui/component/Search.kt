package com.xayah.databackup.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.xayah.databackup.R
import com.xayah.databackup.ui.token.SearchTokens

@Composable
fun SearchBar(onTextChange: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }

    TextField(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape),
        value = text,
        placeholder = { Text(stringResource(R.string.search_bar_hint)) },
        onValueChange = {
            text = it
            onTextChange(it)
        },
        singleLine = true,
        leadingIcon = {
            Icon(
                modifier = Modifier
                    .paddingStart(SearchTokens.IconPaddingStart)
                    .size(SearchTokens.IconSize),
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
            )
        },
        trailingIcon = if (text.isNotEmpty()) {
            {
                IconButton(
                    modifier = Modifier.paddingEnd(SearchTokens.IconButtonPaddingEnd),
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
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        )
    )
}
