package com.xayah.feature.guide.premium.page.env

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.datastore.saveAppVersionName
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.token.PaddingTokens
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.fromVector
import com.xayah.core.util.ActivityUtil
import com.xayah.feature.guide.common.EnvCard
import com.xayah.feature.guide.common.LocalMainViewModel
import com.xayah.feature.guide.common.MainUiIntent
import com.xayah.feature.guide.common.MainUiState
import com.xayah.feature.guide.premium.R

@ExperimentalMaterial3Api
@Composable
fun PageEnv() {
    val mainViewModel = LocalMainViewModel.current!!
    val viewModel = hiltViewModel<IndexViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val allValidated by viewModel.allValidated.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(null) {
        mainViewModel.emitIntent(
            MainUiIntent.SetUiState(
                MainUiState(
                    isInitializing = false,
                    snackbarHostState = viewModel.snackbarHostState,
                    topBarTitle = StringResourceToken.fromStringId(R.string.environment_detection),
                    topBarIcon = ImageVectorToken.fromVector(Icons.Rounded.CheckCircle),
                    fabIcon = ImageVectorToken.fromVector(Icons.Rounded.ArrowForward),
                    onFabClick = {
                        if (allValidated) {
                            viewModel.launchOnIO {
                                context.saveAppVersionName()
                                context.startActivity(Intent(context, ActivityUtil.classMainActivity))
                                (context as ComponentActivity).finish()
                            }
                        } else {
                            viewModel.launchOnIO {
                                if (uiState.rootItem.enabled)
                                    viewModel.emitIntent(IndexUiIntent.ValidateRoot(context = context))
                                if (uiState.binItem.enabled)
                                    viewModel.emitIntent(IndexUiIntent.ValidateBin(context = context))
                                if (uiState.abiItem.enabled)
                                    viewModel.emitIntent(IndexUiIntent.ValidateAbi(context = context))
                            }
                        }
                    }
                )
            )
        )
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(PaddingTokens.Level4)) {
        item {
            Spacer(modifier = Modifier.height(PaddingTokens.Level2))
        }

        item {
            EnvCard(
                content = uiState.rootItem.content,
                state = uiState.rootItem.state,
                enabled = uiState.rootItem.enabled,
                onClick = {
                    viewModel.emitIntent(IndexUiIntent.ValidateRoot(context = context))
                }
            )
        }
        item {
            EnvCard(
                content = uiState.binItem.content,
                state = uiState.binItem.state,
                enabled = uiState.binItem.enabled,
                onClick = {
                    viewModel.emitIntent(IndexUiIntent.ValidateBin(context = context))
                }
            )
        }
        item {
            EnvCard(
                content = uiState.abiItem.content,
                state = uiState.abiItem.state,
                enabled = uiState.abiItem.enabled,
                onClick = {
                    viewModel.emitIntent(IndexUiIntent.ValidateAbi(context = context))
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(PaddingTokens.Level2))
        }
    }
}
