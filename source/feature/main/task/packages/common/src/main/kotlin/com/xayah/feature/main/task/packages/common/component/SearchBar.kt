package com.xayah.feature.main.task.packages.common.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.xayah.core.ui.component.CleanableTextField
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.util.fromString
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.fromVector
import com.xayah.feature.main.task.packages.common.R

@Composable
fun SearchBar(modifier: Modifier = Modifier, enabled: Boolean, onTextChange: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }

    CleanableTextField(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape),
        value = StringResourceToken.fromString(text),
        placeholder = StringResourceToken.fromStringId(R.string.search_bar_hint),
        enabled = enabled,
        leadingIcon = ImageVectorToken.fromVector(Icons.Rounded.Search),
        onCleanClick = {
            text = ""
            onTextChange("")
        },
        onValueChange = {
            text = it
            onTextChange(it)
        },
    )
}
