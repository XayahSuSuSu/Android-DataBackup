package com.xayah.databackup.ui.activity.list.common.components.menu.top

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import com.xayah.databackup.R
import com.xayah.databackup.ui.components.BodyMediumText

@Composable
fun MenuTopActionButton(icon: ImageVector, title: String, onClick: () -> Unit) {
    val iconSmallSize = dimensionResource(R.dimen.icon_small_size)
    val tinyPadding = dimensionResource(R.dimen.padding_tiny)
    val smallPadding = dimensionResource(R.dimen.padding_small)

    Column(modifier = Modifier
        .clip(RoundedCornerShape(smallPadding))
        .clickable { onClick() }
        .padding(smallPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(tinyPadding)
    ) {
        Icon(
            modifier = Modifier.size(iconSmallSize),
            imageVector = icon,
            contentDescription = null
        )
        BodyMediumText(text = title, bold = true)
    }
}
