package com.xayah.databackup.ui.activity.settings.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import com.xayah.databackup.R

@ExperimentalMaterial3Api
@Composable
fun Title(title: String) {
    val colorPrimary = MaterialTheme.colorScheme.primary
    val settingsTitlePadding = dimensionResource(R.dimen.padding_settings_title)
    val nonePadding = dimensionResource(R.dimen.padding_none)
    Text(
        modifier = Modifier.padding(settingsTitlePadding, nonePadding),
        text = title,
        color = colorPrimary,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
    )
}
