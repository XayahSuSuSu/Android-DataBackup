package com.xayah.feature.main.cloud

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xayah.core.model.CloudType
import com.xayah.core.ui.component.Clickable
import com.xayah.core.ui.component.Divider
import com.xayah.core.ui.component.IconButton
import com.xayah.core.ui.component.Title
import com.xayah.core.ui.material3.DisabledAlpha
import com.xayah.core.ui.material3.toColor
import com.xayah.core.ui.material3.tokens.ColorSchemeKeyTokens
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.route.MainRoutes
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.LocalNavController
import com.xayah.core.ui.util.fromString
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.fromVector
import com.xayah.core.ui.util.icon
import com.xayah.core.ui.util.value

@ExperimentalLayoutApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun PageCloud() {
    val navController = LocalNavController.current!!
    val viewModel = hiltViewModel<IndexViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()

    CloudScaffold(
        scrollBehavior = scrollBehavior,
        snackbarHostState = viewModel.snackbarHostState,
        title = StringResourceToken.fromStringId(R.string.cloud),
        actions = {}
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(SizeTokens.Level24)
        ) {
            Title(title = StringResourceToken.fromStringId(R.string.account)) {
                accounts.forEach {
                    Clickable(
                        enabled = uiState.isProcessing.not(),
                        title = StringResourceToken.fromString(it.name),
                        value = StringResourceToken.fromString(it.user),
                        leadingContent = {
                            Icon(
                                imageVector = it.type.icon.value,
                                contentDescription = null,
                                tint = if (uiState.isProcessing.not()) LocalContentColor.current else LocalContentColor.current.copy(alpha = DisabledAlpha)
                            )
                        },
                        trailingContent = {
                            Divider(
                                modifier = Modifier
                                    .height(SizeTokens.Level36)
                                    .width(SizeTokens.Level1)
                                    .fillMaxHeight()
                            )
                            IconButton(
                                icon = ImageVectorToken.fromVector(Icons.Outlined.Settings),
                                tint = ColorSchemeKeyTokens.Primary.toColor(),
                                onClick = {
                                    navController.navigate(
                                        when (it.type) {
                                            CloudType.FTP -> MainRoutes.FTPSetup.getRoute(it.name)
                                            CloudType.WEBDAV -> MainRoutes.WebDAVSetup.getRoute(it.name)
                                            CloudType.SMB -> MainRoutes.SMBSetup.getRoute(it.name)
                                        }
                                    )
                                }
                            )
                        },
                        onClick = {
                            viewModel.emitIntentOnIO(IndexUiIntent.TestConnection(it))
                        }
                    )
                }

                Clickable(
                    icon = ImageVectorToken.fromVector(Icons.Rounded.Add),
                    title = StringResourceToken.fromStringId(R.string.add_account),
                ) {
                    navController.navigate(MainRoutes.CloudAddAccount.route)
                }
            }
        }
    }
}
