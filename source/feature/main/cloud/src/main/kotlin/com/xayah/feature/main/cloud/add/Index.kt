package com.xayah.feature.main.cloud.add

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.xayah.core.model.CloudType
import com.xayah.core.ui.component.Clickable
import com.xayah.core.ui.component.Title
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.route.MainRoutes
import com.xayah.core.ui.token.SizeTokens
import com.xayah.core.ui.util.LocalNavController
import com.xayah.core.ui.util.fromString
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.icon
import com.xayah.core.util.encodeURL
import com.xayah.core.util.navigateSingle
import com.xayah.feature.main.cloud.CloudScaffold
import com.xayah.feature.main.cloud.R

@ExperimentalLayoutApi
@ExperimentalAnimationApi
@ExperimentalMaterial3Api
@Composable
fun PageCloudAddAccount() {
    val navController = LocalNavController.current!!
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    CloudScaffold(
        scrollBehavior = scrollBehavior,
        title = StringResourceToken.fromStringId(R.string.add_account),
        actions = {}
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(SizeTokens.Level24)
        ) {
            Title(title = StringResourceToken.fromStringId(R.string.provider)) {
                Clickable(
                    icon = CloudType.FTP.icon,
                    title = StringResourceToken.fromString(CloudType.FTP.title),
                ) {
                    navController.navigateSingle(MainRoutes.FTPSetup.getRoute(" ".encodeURL()))
                }
                Clickable(
                    icon = CloudType.WEBDAV.icon,
                    title = StringResourceToken.fromString(CloudType.WEBDAV.title),
                ) {
                    navController.navigateSingle(MainRoutes.WebDAVSetup.getRoute(" ".encodeURL()))
                }
                Clickable(
                    icon = CloudType.SMB.icon,
                    title = StringResourceToken.fromString(CloudType.SMB.title),
                ) {
                    navController.navigateSingle(MainRoutes.SMBSetup.getRoute(" ".encodeURL()))
                }
                Clickable(
                    icon = CloudType.SFTP.icon,
                    title = StringResourceToken.fromString(CloudType.SFTP.title),
                ) {
                    navController.navigateSingle(MainRoutes.SFTPSetup.getRoute(" ".encodeURL()))
                }
            }
        }
    }
}
