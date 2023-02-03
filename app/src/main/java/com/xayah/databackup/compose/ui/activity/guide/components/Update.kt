package com.xayah.databackup.compose.ui.activity.guide.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.xayah.databackup.R
import com.xayah.databackup.data.GuideType
import com.xayah.databackup.data.LoadingState
import com.xayah.databackup.util.Server
import com.xayah.databackup.util.appReleaseList

@Preview(showBackground = true)
@ExperimentalMaterial3Api
@Composable
fun LoadingState(state: LoadingState = LoadingState.Loading) {
    val colorError = MaterialTheme.colorScheme.error
    val iconMediumSize = dimensionResource(R.dimen.icon_medium_size)
    val nonePadding = dimensionResource(R.dimen.padding_none)
    val smallPadding = dimensionResource(R.dimen.padding_small)
    val mediumPadding = dimensionResource(R.dimen.padding_medium)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(nonePadding, mediumPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (state) {
            LoadingState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.padding(nonePadding, nonePadding, nonePadding, smallPadding)
                )
                Text(
                    stringResource(R.string.loading),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            LoadingState.Success -> {}
            LoadingState.Failed -> {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = null,
                    tint = colorError,
                    modifier = Modifier
                        .size(iconMediumSize)
                        .padding(nonePadding, nonePadding, nonePadding, smallPadding)
                )
                Text(
                    stringResource(R.string.loading_failed),
                    style = MaterialTheme.typography.titleSmall,
                    color = colorError,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@ExperimentalMaterial3Api
@Composable
fun Update(setType: (GuideType) -> Unit) {
    val (loadingState, setLoadingState) = remember {
        mutableStateOf(LoadingState.Loading)
    }
    val updateList = remember {
        mutableStateListOf<UpdateItem>()
    }
    LaunchedEffect(updateList) {
        Server.getInstance().releases(
            successCallback = { releaseList ->
                val mReleaseList = releaseList.appReleaseList()
                for (i in mReleaseList) {
                    updateList.add(
                        UpdateItem(
                            i.name,
                            i.body.replace("* ", "").replace("*", ""),
                            i.html_url
                        )
                    )
                }
                setLoadingState(LoadingState.Success)
            },
            failedCallback = {
                setLoadingState(LoadingState.Failed)
            })
    }

    GuideScaffold(
        title = stringResource(R.string.update_records),
        icon = Icons.Rounded.Notifications,
        showBtnIcon = true,
        nextBtnIcon = Icons.Rounded.ArrowForward,
        onNextBtnClick = {
            setType(GuideType.Env)
        },
        items = {
            if (loadingState != LoadingState.Success) {
                item {
                    LoadingState(loadingState)
                }
            } else {
                items(count = updateList.size) {
                    UpdateCard(
                        version = updateList[it].version,
                        content = updateList[it].content,
                        link = updateList[it].link
                    )
                }
            }
        }
    )
}