package com.xayah.databackup.ui.activity.guide.components.card

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.xayah.databackup.R
import com.xayah.databackup.ui.components.BodySmallText
import com.xayah.databackup.ui.components.TitleLargeText
import com.xayah.databackup.ui.components.paddingVertical

@ExperimentalMaterial3Api
@Composable
fun CardPermission(
    serial: String,
    title: String,
    subtitle: String,
    content: String,
) {
    val smallPadding = dimensionResource(R.dimen.padding_small)
    val mediumPadding = dimensionResource(R.dimen.padding_medium)
    Card(
        Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Column(Modifier.padding(mediumPadding)) {
            SerialView(serial = serial)
            TitleLargeText(
                text = title,
                modifier = Modifier.paddingVertical(smallPadding),
            )
            BodySmallText(text = subtitle, modifier = Modifier.paddingVertical(smallPadding))
            BodySmallText(text = content, modifier = Modifier.paddingVertical(smallPadding))
        }
    }
}