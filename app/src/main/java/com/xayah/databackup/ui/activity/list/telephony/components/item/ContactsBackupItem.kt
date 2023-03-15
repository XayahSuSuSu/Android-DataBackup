package com.xayah.databackup.ui.activity.list.telephony.components.item

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.xayah.databackup.R
import com.xayah.databackup.data.ContactItem
import com.xayah.databackup.ui.activity.guide.components.card.SerialText
import com.xayah.databackup.ui.components.BodySmallText
import com.xayah.databackup.ui.components.TitleMediumText
import com.xayah.databackup.ui.components.paddingTop

@ExperimentalMaterial3Api
@Composable
fun ContactsBackupItem(item: ContactItem) {
    val smallPadding = dimensionResource(R.dimen.padding_small)
    val mediumPadding = dimensionResource(R.dimen.padding_medium)
    val onClick = { it: Boolean ->
        item.isSelected.value = it
    }

    Card(
        Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Card(
            Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clickable {
                    if (item.isInLocal.value.not())
                        onClick(item.isSelected.value.not())
                }
        ) {
            Column(
                modifier = Modifier
                    .padding(mediumPadding, smallPadding)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TitleMediumText(
                        modifier = Modifier.weight(1f),
                        text = item.rawContact.displayNamePrimary
                    )
                    IconToggleButton(
                        checked = item.isSelected.value,
                        enabled = item.isInLocal.value.not(),
                        onCheckedChange = { onClick(it) }) {
                        if (item.isSelected.value) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null
                            )
                        }
                    }
                }
                Divider()
                BodySmallText(
                    modifier = Modifier.paddingTop(smallPadding),
                    text = item.bodyText,
                    bold = false
                )
                Row(
                    modifier = Modifier.paddingTop(smallPadding),
                    horizontalArrangement = Arrangement.spacedBy(smallPadding)
                ) {
                    if (item.isInLocal.value)
                        SerialText(serial = stringResource(R.string.backed_up))
                }
            }
        }
    }
}
