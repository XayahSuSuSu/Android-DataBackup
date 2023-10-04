package com.xayah.databackup.ui.activity.main.page.log

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.xayah.databackup.ui.component.JetbrainsMonoLabelMediumText
import com.xayah.databackup.ui.component.Loader
import com.xayah.databackup.ui.token.CommonTokens

@Composable
fun PageLog(viewModel: LogViewModel) {
    val uiState by viewModel.uiState
    val logText = uiState.logText

    Loader(
        modifier = Modifier.fillMaxSize(),
        onLoading = {
            viewModel.initializeUiState()
        },
        content = {
            Column(modifier = Modifier.fillMaxSize()) {
                SelectionContainer {
                    JetbrainsMonoLabelMediumText(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .horizontalScroll(rememberScrollState())
                            .padding(CommonTokens.PaddingMedium),
                        text = logText,
                    )
                }
            }
        }
    )
}
