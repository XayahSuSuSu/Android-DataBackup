package com.xayah.databackup.ui.activity.guide.page.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.xayah.databackup.ui.component.State
import com.xayah.databackup.ui.component.UpdateCard
import com.xayah.databackup.ui.component.paddingBottom
import com.xayah.databackup.ui.component.paddingTop
import com.xayah.databackup.ui.token.CommonTokens
import com.xayah.databackup.ui.token.State

@ExperimentalMaterial3Api
@Composable
fun PageUpdate() {
    val viewModel = hiltViewModel<UpdateViewModel>()
    val state: MutableState<State> = remember { mutableStateOf(State.Loading) }
    val releases = viewModel.releases
    LaunchedEffect(null) {
        if (releases.value.isEmpty())
            viewModel.getReleases(
                onSucceed = {
                    releases.value = it
                    state.value = State.Succeed
                },
                onFailed = {
                    state.value = State.Failed
                })
        else
            state.value = State.Succeed
    }

    State(modifier = Modifier.paddingTop(CommonTokens.PaddingMedium), state = state.value) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(CommonTokens.PaddingMedium)) {
            items(items = releases.value, key = { it.name }) {
                UpdateCard(content = it.content, version = it.name, link = it.url)
            }
            item {
                Spacer(modifier = Modifier.paddingBottom(CommonTokens.PaddingSmall))
            }
        }
    }
}
