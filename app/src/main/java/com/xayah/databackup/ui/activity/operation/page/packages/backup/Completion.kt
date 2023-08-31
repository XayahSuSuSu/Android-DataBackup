package com.xayah.databackup.ui.activity.operation.page.packages.backup

import androidx.activity.ComponentActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.xayah.databackup.R
import com.xayah.databackup.ui.component.EmojiString
import com.xayah.databackup.ui.component.EmojiText
import com.xayah.databackup.ui.component.GridItemCompletion
import com.xayah.databackup.ui.component.GridItemCompletionConfig
import com.xayah.databackup.ui.component.HeadlineMediumBoldText
import com.xayah.databackup.ui.component.Loader
import com.xayah.databackup.ui.component.TopSpacer
import com.xayah.databackup.ui.component.VerticalGrid
import com.xayah.databackup.ui.component.paddingHorizontal
import com.xayah.databackup.ui.component.paddingVertical
import com.xayah.databackup.ui.token.CommonTokens

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun PackageBackupCompletion() {
    val context = LocalContext.current
    val viewModel = hiltViewModel<CompletionViewModel>()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val uiState = viewModel.uiState.value
    val relativeTime = uiState.relativeTime
    val succeedNum = uiState.succeedNum
    val failedNum = uiState.failedNum
    val completionItems = listOf(
        GridItemCompletionConfig(emoji = EmojiString.SPARKLING_HEART, title = stringResource(id = R.string.succeed), content = succeedNum.toString()),
        GridItemCompletionConfig(emoji = EmojiString.BROKEN_HEART, title = stringResource(id = R.string.failed), content = failedNum.toString()),
        GridItemCompletionConfig(emoji = EmojiString.ALARM_CLOCK, title = stringResource(R.string.time), content = relativeTime),
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                modifier = Modifier.padding(CommonTokens.PaddingMedium),
                onClick = {
                    (context as ComponentActivity).finish()
                },
                expanded = true,
                icon = { Icon(imageVector = Icons.Rounded.KeyboardArrowLeft, contentDescription = null) },
                text = { Text(text = stringResource(R.string.word_return)) },
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
    ) { innerPadding ->
        Column {
            TopSpacer(innerPadding = innerPadding)
            TopSpacer(innerPadding = innerPadding)

            Box(modifier = Modifier.weight(1f)) {
                Loader(
                    modifier = Modifier.fillMaxSize(),
                    onLoading = {
                        viewModel.initializeUiState()
                    },
                    content = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .paddingHorizontal(CommonTokens.PaddingMedium),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            EmojiText(emoji = EmojiString.PARTY_POPPER, size = CommonTokens.EmojiLargeSize)
                            HeadlineMediumBoldText(text = stringResource(R.string.backup_completed))
                            Divider(modifier = Modifier.paddingVertical(CommonTokens.PaddingMedium))
                            VerticalGrid(
                                columns = 2,
                                count = completionItems.size,
                                verticalArrangement = Arrangement.spacedBy(CommonTokens.PaddingMedium),
                            ) { index ->
                                GridItemCompletion(config = completionItems[index])
                            }
                        }
                    }
                )
            }
        }
    }
}
