package com.xayah.databackup.ui.activity.guide.components.card

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.vectorResource
import com.xayah.databackup.R
import com.xayah.databackup.ui.components.BodySmallText
import com.xayah.databackup.ui.components.IconTextButton
import com.xayah.databackup.ui.components.paddingTop

@ExperimentalMaterial3Api
@Composable
fun CardUpdate(
    version: String,
    content: String,
    link: String
) {
    val context = LocalContext.current
    val mediumPadding = dimensionResource(R.dimen.padding_medium)
    Card(
        Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Column(Modifier.padding(mediumPadding)) {
            BodySmallText(text = content)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .paddingTop(mediumPadding),
                horizontalArrangement = Arrangement.End
            ) {
                IconTextButton(
                    icon = ImageVector.vectorResource(id = R.drawable.ic_round_link),
                    text = version
                ) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
                }
            }
        }
    }
}