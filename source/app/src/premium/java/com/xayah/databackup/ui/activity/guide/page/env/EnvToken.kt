package com.xayah.databackup.ui.activity.guide.page.env

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.xayah.databackup.R

@Composable
fun envTitles() = listOf(
    stringResource(id = R.string.start),
    stringResource(id = R.string.network),
    stringResource(id = R.string.application),
    stringResource(id = R.string.storage)
)

@Composable
fun envSubtitles() = listOf(
    stringResource(id = R.string.start_subtitle),
    stringResource(id = R.string.network_subtitle),
    stringResource(id = R.string.application_subtitle),
    stringResource(id = R.string.storage_subtitle)
)

@Composable
fun envContents() = listOf(
    stringResource(id = R.string.start_desc),
    stringResource(id = R.string.network_desc),
    stringResource(id = R.string.application_desc),
    stringResource(id = R.string.storage_desc)
)
