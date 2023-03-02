package com.xayah.databackup.ui.activity.guide.components

import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.res.stringResource
import com.xayah.databackup.R
import com.xayah.databackup.data.GuideType
import com.xayah.databackup.data.LoadingState
import com.xayah.databackup.ui.activity.guide.GuideViewModel
import com.xayah.databackup.ui.activity.guide.components.card.CardUpdate
import com.xayah.databackup.ui.components.LoadingView

@ExperimentalMaterial3Api
@Composable
fun PageUpdate(viewModel: GuideViewModel) {
    val loadingState = viewModel.loadingState
    val updateList = viewModel.updateList.collectAsState()
    LaunchedEffect(null) {
        if (updateList.value.isEmpty())
            viewModel.getUpdateList(
                onSuccess = {
                    loadingState.value = LoadingState.Success
                },
                onFailed = {
                    loadingState.value = LoadingState.Failed
                })
    }

    GuideScaffold(
        title = stringResource(R.string.update_records),
        icon = Icons.Rounded.Notifications,
        showBtnIcon = true,
        nextBtnIcon = Icons.Rounded.ArrowForward,
        onNextBtnClick = {
            viewModel.initType.value = GuideType.Env
        },
        items = {
            if (loadingState.value != LoadingState.Success) {
                item {
                    LoadingView(loadingState.value)
                }
            } else {
                items(items = updateList.value, key = { it.version }) {
                    CardUpdate(
                        version = it.version,
                        content = it.content,
                        link = it.link
                    )
                }
            }
        }
    )
}
