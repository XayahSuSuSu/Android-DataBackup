package com.xayah.databackup.ui.activity.list.common.components.manifest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import com.xayah.databackup.R
import com.xayah.databackup.ui.components.BodySmallText

@ExperimentalMaterial3Api
@Composable
fun ManifestDesc(
    title: String,
    subtitle: String,
    icon: ImageVector,
) {
    val smallPadding = dimensionResource(R.dimen.padding_small)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(smallPadding)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
            )
            BodySmallText(text = subtitle)
        }
    }
}
