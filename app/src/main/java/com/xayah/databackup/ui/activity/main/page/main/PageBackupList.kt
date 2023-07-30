package com.xayah.databackup.ui.activity.main.page.main

import android.content.pm.PackageInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.xayah.databackup.data.PackageBackupActivate
import com.xayah.databackup.data.PackageBackupUpdate
import com.xayah.databackup.ui.component.ListItemPackage
import com.xayah.databackup.ui.component.SearchBar
import com.xayah.databackup.ui.component.Serial
import com.xayah.databackup.ui.component.paddingHorizontal
import com.xayah.databackup.ui.token.CommonTokens
import com.xayah.databackup.util.ExceptionUtil
import com.xayah.databackup.util.command.EnvUtil
import com.xayah.librootservice.service.RemoteRootService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@ExperimentalMaterial3Api
@Composable
fun PageBackupList() {
    val context = LocalContext.current
    val viewModel = hiltViewModel<MainBackupListViewModel>()
    val packages = viewModel.packages.collectAsState(initial = listOf())
    val packageManager = context.packageManager

    LaunchedEffect(null) {
        withContext(Dispatchers.IO) {
            val remoteRootService = RemoteRootService(context)

            // Inactivate all packages and activate installed only.
            viewModel.inactivatePackages()
            var installedPackages = listOf<PackageInfo>()
            ExceptionUtil.tryService {
                installedPackages = remoteRootService.getInstalledPackagesAsUser(0, 0)
            }
            val activePackages = mutableListOf<PackageBackupActivate>()
            installedPackages.forEach { packageInfo ->
                activePackages.add(PackageBackupActivate(packageName = packageInfo.packageName, active = true))
            }
            viewModel.activatePackages(activePackages)

            val newPackages = mutableListOf<PackageBackupUpdate>()
            EnvUtil.createIconDirectory(context)
            // Update packages' info.
            installedPackages.forEach { packageInfo ->
                val icon = packageInfo.applicationInfo.loadIcon(packageManager)
                EnvUtil.saveIcon(context, packageInfo.packageName, icon)

                newPackages.add(
                    PackageBackupUpdate(
                        packageName = packageInfo.packageName,
                        label = packageInfo.applicationInfo.loadLabel(packageManager).toString(),
                        versionName = packageInfo.versionName,
                        versionCode = packageInfo.longVersionCode,
                        apkSize = 0,
                        userSize = 0,
                        userDeSize = 0,
                        dataSize = 0,
                        obbSize = 0,
                        mediaSize = 0,
                        firstInstallTime = packageInfo.firstInstallTime,
                        active = true
                    )
                )
            }
            viewModel.updatePackages(newPackages)
        }
    }
    LazyColumn(
        modifier = Modifier.paddingHorizontal(CommonTokens.PaddingMedium),
        verticalArrangement = Arrangement.spacedBy(CommonTokens.PaddingMedium)
    ) {
        item {
            Spacer(modifier = Modifier.height(CommonTokens.PaddingMedium))
            SearchBar(onTextChange = {})
        }
        items(items = packages.value) { packageInfo ->
            ListItemPackage(
                packageInfo = packageInfo,
                chipGroup = {
                    Serial(serial = packageInfo.versionName)
                },
            )
        }
        item {
            Spacer(modifier = Modifier.height(CommonTokens.None))
        }
    }
}
