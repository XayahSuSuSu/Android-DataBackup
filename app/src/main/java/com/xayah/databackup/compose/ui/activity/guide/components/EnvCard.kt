package com.xayah.databackup.compose.ui.activity.guide.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.xayah.databackup.R
import com.xayah.databackup.data.LoadingState

@ExperimentalMaterial3Api
@Preview(showBackground = true)
@Composable
fun EnvCardPreview() {
    EnvCard(
        item = stringResource(id = R.string.grant_root_access)
    ) { LoadingState.Success }
}

@ExperimentalMaterial3Api
@Composable
fun EnvCard(
    item: String,
    onCardClick: ((LoadingState) -> Unit) -> Unit,
) {
    val colorOnSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val colorError = MaterialTheme.colorScheme.error
    val colorErrorContainer = MaterialTheme.colorScheme.errorContainer
    val colorGreen = colorResource(id = R.color.green)
    val colorGreenContainer = colorResource(id = R.color.greenContainer)
    val colorSurfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val iconSmallSize = dimensionResource(R.dimen.icon_small_size)
    val nonePadding = dimensionResource(R.dimen.padding_none)
    val smallPadding = dimensionResource(R.dimen.padding_small)
    val mediumPadding = dimensionResource(R.dimen.padding_medium)

    val (cardState, setCardState) = remember {
        mutableStateOf(LoadingState.Loading)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clickable {
                if (cardState != LoadingState.Success) {
                    onCardClick(setCardState)
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = when (cardState) {
                LoadingState.Loading -> {
                    colorSurfaceVariant
                }
                LoadingState.Success -> {
                    colorGreen
                }
                LoadingState.Failed -> {
                    colorError
                }
            }
        )
    ) {
        Column(Modifier.padding(mediumPadding)) {
            when (cardState) {
                LoadingState.Loading -> {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_outline_light),
                        contentDescription = null,
                        tint = colorOnSurfaceVariant,
                        modifier = Modifier
                            .size(iconSmallSize)
                            .padding(nonePadding, nonePadding, nonePadding, smallPadding)
                    )
                }
                LoadingState.Success -> {
                    Icon(
                        imageVector = Icons.Rounded.Done,
                        contentDescription = null,
                        tint = colorGreenContainer,
                        modifier = Modifier
                            .size(iconSmallSize)
                            .padding(nonePadding, nonePadding, nonePadding, smallPadding)
                    )
                }
                LoadingState.Failed -> {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = null,
                        tint = colorErrorContainer,
                        modifier = Modifier
                            .size(iconSmallSize)
                            .padding(nonePadding, nonePadding, nonePadding, smallPadding)
                    )
                }
            }

            Text(
                modifier = Modifier.padding(nonePadding, smallPadding, nonePadding, nonePadding),
                text = item,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = when (cardState) {
                    LoadingState.Loading -> {
                        colorOnSurfaceVariant
                    }
                    LoadingState.Success -> {
                        colorGreenContainer
                    }
                    LoadingState.Failed -> {
                        colorErrorContainer
                    }
                }
            )
        }
    }
}