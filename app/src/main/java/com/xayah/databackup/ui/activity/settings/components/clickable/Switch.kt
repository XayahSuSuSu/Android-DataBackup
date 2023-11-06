package com.xayah.databackup.ui.activity.settings.components.clickable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import com.xayah.databackup.R
import com.xayah.databackup.ui.components.BodySmallText
import com.xayah.databackup.ui.components.TitleMediumText

@ExperimentalMaterial3Api
@Composable
fun Switch(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isChecked: MutableState<Boolean>,
    isEnabled: Boolean = true,
    onCheckedChange: (isChecked: Boolean) -> Unit,
) {
    val mediumPadding = dimensionResource(R.dimen.padding_medium)

    ListItem(
        modifier = Modifier
            .clip(RoundedCornerShape(mediumPadding))
            .clickable {
                if (isEnabled) {
                    isChecked.value = isChecked.value.not()
                    onCheckedChange(isChecked.value)
                }
            },
        headlineText = {
            TitleMediumText(text = title)
        },
        supportingText = {
            BodySmallText(text = subtitle)
        },
        trailingContent = {
            Switch(
                checked = isChecked.value,
                enabled = isEnabled,
                onCheckedChange = {
                    isChecked.value = it
                    onCheckedChange(isChecked.value)
                })
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
            )
        }
    )
}
