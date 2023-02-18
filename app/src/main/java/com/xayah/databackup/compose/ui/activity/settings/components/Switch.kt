package com.xayah.databackup.compose.ui.activity.settings.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.xayah.databackup.R

data class SwitchItem(
    val title: String,
    val subtitle: String,
    @DrawableRes val iconId: Int,
    val isChecked: MutableState<Boolean>,
    val isEnabled: Boolean = true,
    val onCheckedChange: (isChecked: Boolean) -> Unit,
)

@ExperimentalMaterial3Api
@Preview(showBackground = true)
@Composable
fun SwitchPreview() {
    val checked = remember {
        mutableStateOf(true)
    }
    Switch(
        title = stringResource(id = R.string.backup_user),
        subtitle = stringResource(id = R.string.settings_backup_user_subtitle),
        isChecked = checked,
        icon = ImageVector.vectorResource(id = R.drawable.ic_round_person)
    ) {}
}

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
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        },
        supportingText = {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
            )
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
