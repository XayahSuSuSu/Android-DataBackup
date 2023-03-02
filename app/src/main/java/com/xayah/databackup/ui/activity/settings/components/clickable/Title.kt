package com.xayah.databackup.ui.activity.settings.components.clickable

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import com.xayah.databackup.R
import com.xayah.databackup.ui.components.paddingHorizontal

@ExperimentalMaterial3Api
@Composable
fun Title(title: String) {
    val colorPrimary = MaterialTheme.colorScheme.primary
    val settingsTitlePadding = dimensionResource(R.dimen.padding_settings_title)
    Text(
        modifier = Modifier.paddingHorizontal(settingsTitlePadding),
        text = title,
        color = colorPrimary,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
    )
}
