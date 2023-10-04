package com.xayah.databackup.ui.activity.main.page.log

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.xayah.databackup.ui.component.JetbrainsMonoLabelSmallText
import com.xayah.databackup.ui.component.Loader
import com.xayah.databackup.ui.component.paddingHorizontal
import com.xayah.databackup.ui.token.CommonTokens

@Composable
fun PageLog(viewModel: LogViewModel) {
    val uiState by viewModel.uiState
    val logTextList = uiState.logTextList

    Loader(
        modifier = Modifier.fillMaxSize(),
        onLoading = {
            viewModel.initializeUiState()
        },
        content = {
            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    item {
                        Spacer(modifier = Modifier.height(CommonTokens.PaddingMedium))
                    }

                    items(items = logTextList) {
                        JetbrainsMonoLabelSmallText(modifier = Modifier.paddingHorizontal(CommonTokens.PaddingMedium), text = it)
                    }

                    item {
                        Spacer(modifier = Modifier.height(CommonTokens.PaddingMedium))
                    }
                }
            }
        }
    )
}
