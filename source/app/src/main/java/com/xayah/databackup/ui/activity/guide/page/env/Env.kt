package com.xayah.databackup.ui.activity.guide.page.env

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import com.xayah.databackup.R
import com.xayah.databackup.ui.activity.guide.page.GuideUiState
import com.xayah.databackup.ui.activity.guide.page.GuideViewModel
import com.xayah.databackup.ui.component.EnvCard
import com.xayah.databackup.ui.component.LocalSlotScope
import com.xayah.databackup.ui.component.paddingBottom
import com.xayah.databackup.ui.component.paddingTop
import com.xayah.databackup.ui.token.CommonTokens
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock

@ExperimentalMaterial3Api
@Composable
fun PageEnv(guideViewModel: GuideViewModel) {
    val context = LocalContext.current
    val dialogSlot = LocalSlotScope.current!!.dialogSlot
    val viewModel = hiltViewModel<EnvViewModel>()
    val uiState by viewModel.uiState
    val envItems = uiState.envItems

    LaunchedEffect(null) {
        guideViewModel.toUiState(
            GuideUiState.Env(
                title = context.getString(R.string.environment_detection),
                fabIcon = Icons.Rounded.ArrowForward,
                onFabClick = { _ ->
                    viewModel.viewModelScope.launch {
                        uiState.mutex.withLock {
                            if (uiState.processing.not()) {
                                viewModel.setProcessing(true)
                                if (uiState.allValidated.not()) {
                                    for (i in envItems) {
                                        i.onClick.invoke(context, guideViewModel, dialogSlot)
                                    }
                                }
                            }
                        }
                    }
                }
            )
        )
    }

    LazyColumn(
        modifier = Modifier.paddingTop(CommonTokens.PaddingMedium),
        verticalArrangement = Arrangement.spacedBy(CommonTokens.PaddingMedium)
    ) {
        items(items = envItems) { item ->
            EnvCard(
                content = item.content,
                state = item.state,
                onClick = {
                    viewModel.viewModelScope.launch {
                        uiState.mutex.withLock {
                            if (uiState.processing.not()) {
                                viewModel.setProcessing(true)
                                if (uiState.allValidated.not()) {
                                    item.onClick.invoke(context, guideViewModel, dialogSlot)
                                }
                            }
                            viewModel.setProcessing(false)
                        }
                    }
                })
        }
        item {
            Spacer(modifier = Modifier.paddingBottom(CommonTokens.PaddingSmall))
        }
    }
}
