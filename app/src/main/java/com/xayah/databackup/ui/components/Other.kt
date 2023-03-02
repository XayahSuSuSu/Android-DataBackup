package com.xayah.databackup.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.xayah.databackup.R
import com.xayah.databackup.data.LoadingState

@Composable
fun LoadingView(state: LoadingState = LoadingState.Loading) {
    val colorError = MaterialTheme.colorScheme.error
    val iconMediumSize = dimensionResource(R.dimen.icon_medium_size)
    val smallPadding = dimensionResource(R.dimen.padding_small)
    val mediumPadding = dimensionResource(R.dimen.padding_medium)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .paddingVertical(mediumPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (state) {
            LoadingState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.paddingBottom(smallPadding)
                )
                TitleSmallText(
                    text = stringResource(R.string.loading),
                )
            }
            LoadingState.Failed -> {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = null,
                    tint = colorError,
                    modifier = Modifier
                        .size(iconMediumSize)
                        .paddingBottom(smallPadding)
                )
                TitleSmallText(
                    text = stringResource(R.string.loading_failed),
                    color = colorError,
                )
            }
            else -> {}
        }
    }
}