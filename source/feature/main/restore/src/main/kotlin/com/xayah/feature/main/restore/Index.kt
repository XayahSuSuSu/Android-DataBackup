package com.xayah.feature.main.restore

import android.annotation.SuppressLint
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ManageSearch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.ui.component.Title
import com.xayah.core.ui.component.paddingTop
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.route.MainRoutes
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.LocalNavController
import com.xayah.core.ui.util.fromDrawable
import com.xayah.core.ui.util.fromString
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.fromVector

@SuppressLint("StringFormatInvalid")
@ExperimentalFoundationApi
@ExperimentalLayoutApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun PageRestore() {
    val navController = LocalNavController.current!!
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val viewModel = hiltViewModel<IndexViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lastBackupTime by viewModel.lastBackupTimeState.collectAsStateWithLifecycle()

    LaunchedEffect(null) {
        viewModel.emitIntent(IndexUiIntent.UpdateApps)
    }

    RestoreScaffold(
        scrollBehavior = scrollBehavior,
        title = StringResourceToken.fromStringId(R.string.restore),
        actions = {}
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            OverviewLastBackupCard(
                modifier = Modifier.padding(SizeTokens.Level16),
                lastBackupTime = lastBackupTime
            )

            Clickable(
                title = StringResourceToken.fromStringId(R.string.apps),
                value = StringResourceToken.fromString(
                    "${context.getString(R.string.args_apps_backed_up, uiState.packages.size)}${if (uiState.packagesSize.isNotEmpty()) " (${uiState.packagesSize})" else ""}"
                ),
                leadingIcon = ImageVectorToken.fromDrawable(R.drawable.ic_rounded_apps),
                content = {
                    PackageIcons(modifier = Modifier.paddingTop(SizeTokens.Level8), packages = uiState.packages)
                }
            ) {
                navController.navigate(MainRoutes.PackagesRestoreList.route)
            }

            Title(title = StringResourceToken.fromStringId(R.string.advanced)) {
                Clickable(
                    title = StringResourceToken.fromStringId(R.string.reload),
                    value = StringResourceToken.fromStringId(R.string.reload_desc),
                    leadingIcon = ImageVectorToken.fromVector(Icons.Rounded.ManageSearch),
                )
            }
        }
    }
}
