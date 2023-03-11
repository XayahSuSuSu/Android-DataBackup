package com.xayah.databackup.ui.activity.guide.components.card

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import com.xayah.databackup.R
import com.xayah.databackup.ui.components.TitleSmallText
import com.xayah.databackup.ui.components.paddingHorizontal

@Composable
fun SerialWord(modifier: Modifier = Modifier, serial: String) {
    val colorOnSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val colorSurface = MaterialTheme.colorScheme.surface
    val serialCircleSize = dimensionResource(R.dimen.serial_circle_size)
    Surface(
        shape = CircleShape,
        modifier = modifier.size(serialCircleSize),
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

@Composable
fun SerialText(modifier: Modifier = Modifier, serial: String) {
    val colorOnSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val colorSurface = MaterialTheme.colorScheme.surface
    val serialCircleSize = dimensionResource(R.dimen.serial_circle_size)
    val serialPaddingHorizontal = dimensionResource(R.dimen.serial_padding_horizontal)
    Surface(
        shape = CircleShape,
        modifier = modifier.height(serialCircleSize),
        color = colorOnSurfaceVariant
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            TitleSmallText(
                modifier = Modifier.paddingHorizontal(serialPaddingHorizontal),
                text = serial,
                color = colorSurface
            )
        }
    }
}

@Composable
fun SerialIcon(modifier: Modifier = Modifier, icon: ImageVector) {
    val colorOnSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val colorSurface = MaterialTheme.colorScheme.surface
    val tinyPadding = dimensionResource(R.dimen.padding_tiny)
    val cardSerialCircleSize = dimensionResource(R.dimen.serial_circle_size)
    Surface(
        shape = CircleShape,
        modifier = modifier.size(cardSerialCircleSize),
        color = colorOnSurfaceVariant
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                modifier = Modifier.padding(tinyPadding),
                imageVector = icon,
                tint = colorSurface,
                contentDescription = null
            )
        }
    }
}
