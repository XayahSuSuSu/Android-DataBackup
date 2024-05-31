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
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.fromString
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.fromVector
import com.xayah.core.ui.util.value
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
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val viewModel = hiltViewModel<IndexViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    RestoreScaffold(
        scrollBehavior = scrollBehavior,
        title = StringResourceToken.fromStringId(R.string.reload),
        actions = {
            Button(
                enabled = uiState.isLoading.not(),
                onClick = {
                    viewModel.emitIntentOnIO(IndexUiIntent.Reload)
                }
            ) {
                Text(text = StringResourceToken.fromStringId(R.string.reload).value)
            }
        }
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Title(enabled = uiState.isLoading.not(), title = StringResourceToken.fromStringId(R.string.settings)) {
                    if (uiState.cloudName.isNotEmpty()) {
                        Clickable(
                            enabled = false,
                            title = StringResourceToken.fromString(uiState.cloudName),
                            value = StringResourceToken.fromString(uiState.cloudRemote),
                            leadingIcon = ImageVectorToken.fromVector(Icons.Outlined.Cloud),
                        )
                    }

                    Switchable(
                        enabled = uiState.isLoading.not(),
                        key = KeyReloadDumpApk,
                        title = StringResourceToken.fromStringId(R.string.dump_apk),
                        checkedText = StringResourceToken.fromStringId(R.string.dump_apk_desc),
                    )

                    val dialogState = LocalSlotScope.current!!.dialogSlot
                    val currentIndex = uiState.versionIndex
                    Selectable(
                        enabled = uiState.isLoading.not(),
                        title = StringResourceToken.fromStringId(R.string.version),
                        value = StringResourceToken.fromStringId(R.string.reload_version_desc),
                        current = uiState.versionList[currentIndex].title
                    ) {
                        val selectedIndex = dialogState.select(
                            title = StringResourceToken.fromStringId(R.string.version),
                            defIndex = currentIndex,
                            items = uiState.versionList
                        )
                        viewModel.emitState(uiState.copy(versionIndex = selectedIndex))
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
                        DotLottieView(isRefreshing = uiState.isLoading, text = StringResourceToken.fromString(uiState.text))
                    }
                }
            }
        }
    }
}
