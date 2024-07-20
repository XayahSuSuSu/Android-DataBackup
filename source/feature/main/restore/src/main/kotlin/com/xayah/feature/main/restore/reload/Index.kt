package com.xayah.feature.main.restore.reload

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.datastore.KeyReloadDumpApk
import com.xayah.core.ui.component.Clickable
import com.xayah.core.ui.component.Divider
import com.xayah.core.ui.component.LocalSlotScope
import com.xayah.core.ui.component.Selectable
import com.xayah.core.ui.component.Switchable
import com.xayah.core.ui.component.Title
import com.xayah.core.ui.component.paddingHorizontal
import com.xayah.core.ui.component.paddingTop
import com.xayah.core.ui.component.select
import com.xayah.core.ui.token.SizeTokens
import com.xayah.feature.main.restore.DotLottieView
import com.xayah.feature.main.restore.R
import com.xayah.feature.main.restore.RestoreScaffold

@SuppressLint("StringFormatInvalid")
@ExperimentalFoundationApi
@ExperimentalLayoutApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun PageReload() {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val viewModel = hiltViewModel<IndexViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    RestoreScaffold(
        scrollBehavior = scrollBehavior,
        title = stringResource(id = R.string.reload),
        actions = {
            Button(
                enabled = uiState.isLoading.not(),
                onClick = {
                    viewModel.emitIntentOnIO(IndexUiIntent.Reload)
                }
            ) {
                Text(text = stringResource(id = R.string.reload))
            }
        }
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Title(enabled = uiState.isLoading.not(), title = stringResource(id = R.string.settings)) {
                    if (uiState.cloudName.isNotEmpty()) {
                        Clickable(
                            enabled = false,
                            title = uiState.cloudName,
                            value = uiState.cloudRemote,
                            leadingIcon = Icons.Outlined.Cloud,
                        )
                    }

                    Switchable(
                        enabled = uiState.isLoading.not(),
                        key = KeyReloadDumpApk,
                        title = stringResource(id = R.string.dump_apk),
                        checkedText = stringResource(id = R.string.dump_apk_desc),
                    )

                    val dialogState = LocalSlotScope.current!!.dialogSlot
                    val currentIndex = uiState.versionIndex
                    Selectable(
                        enabled = uiState.isLoading.not(),
                        title = stringResource(id = R.string.version),
                        value = stringResource(id = R.string.reload_version_desc),
                        current = uiState.versionList[currentIndex].title
                    ) {
                        val (state, selectedIndex) = dialogState.select(
                            title = context.getString(R.string.version),
                            defIndex = currentIndex,
                            items = uiState.versionList
                        )
                        if (state.isConfirm) {
                            viewModel.emitState(uiState.copy(versionIndex = selectedIndex))
                        }
                    }
                }
                Divider()
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .paddingTop(SizeTokens.Level16),
                    contentAlignment = Alignment.Center
                ) {
                    Column(modifier = Modifier.paddingHorizontal(SizeTokens.Level16), horizontalAlignment = Alignment.CenterHorizontally) {
                        DotLottieView(isRefreshing = uiState.isLoading, text = uiState.text)
                    }
                }
            }
        }
    }
}
