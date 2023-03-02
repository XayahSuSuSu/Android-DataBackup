package com.xayah.databackup.ui.activity.guide.components.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.xayah.databackup.R
import com.xayah.databackup.ui.components.TitleSmallText

@ExperimentalMaterial3Api
@Composable
fun SerialView(serial: String) {
    val colorOnSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val colorSurface = MaterialTheme.colorScheme.surface
    val cardSerialCircleSize = dimensionResource(R.dimen.card_serial_circle_size)
    Surface(
        shape = CircleShape,
        modifier = Modifier.size(cardSerialCircleSize),
        color = colorOnSurfaceVariant
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            TitleSmallText(
                text = serial,
                color = colorSurface
            )
        }
    }
}