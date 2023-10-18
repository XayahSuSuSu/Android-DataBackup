package com.xayah.databackup.ui.activity.operation.page.packages.backup

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.xayah.databackup.data.CloudDao
import com.xayah.databackup.data.DirectoryDao
import com.xayah.databackup.data.DirectoryType
import com.xayah.databackup.data.PackageBackupEntireDao
import com.xayah.databackup.data.PackageBackupManifest
import com.xayah.databackup.data.formatSize
import com.xayah.databackup.util.LogUtil
import com.xayah.databackup.util.PathUtil
import com.xayah.librootservice.service.RemoteRootService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

data class ManifestUiState(
    val packageBackupEntireDao: PackageBackupEntireDao,
    val cloudDao: CloudDao,
    val logUtil: LogUtil,
    val rootService: RemoteRootService,
    val directoryType: DirectoryType,
    val directoryDao: DirectoryDao,
) {
    val bothPackages: Flow<List<PackageBackupManifest>> = packageBackupEntireDao.queryActiveBothPackages().distinctUntilChanged()
    val apkOnlyPackages: Flow<List<PackageBackupManifest>> = packageBackupEntireDao.queryActiveAPKOnlyPackages().distinctUntilChanged()
    val dataOnlyPackages: Flow<List<PackageBackupManifest>> = packageBackupEntireDao.queryActiveDataOnlyPackages().distinctUntilChanged()
    val selectedBoth: Flow<Int> = packageBackupEntireDao.countSelectedBoth().distinctUntilChanged()
    val selectedAPKs: Flow<Int> = packageBackupEntireDao.countSelectedAPKs().distinctUntilChanged()
    val selectedData: Flow<Int> = packageBackupEntireDao.countSelectedData().distinctUntilChanged()
    val totalSizeDisplay: Flow<String> = combine(bothPackages, apkOnlyPackages, dataOnlyPackages) { both, apk, data ->
        var total = 0.0
        both.forEach { total += it.storageStats.appBytes + it.storageStats.dataBytes }
        apk.forEach { total += it.storageStats.appBytes }
        data.forEach { total += it.storageStats.dataBytes }
        formatSize(total)
    }
    val availableBytesDisplay: Flow<String> = directoryDao.querySelectedByDirectoryTypeFlow(directoryType).map {
        val availableBytes =
            rootService.readStatFs(it?.parent ?: PathUtil.getParentPath(PathUtil.getBackupSavePath(false))).availableBytes.toDouble()
        formatSize(availableBytes)
    }
}

@HiltViewModel
class ManifestViewModel @Inject constructor(
    packageBackupEntireDao: PackageBackupEntireDao,
    cloudDao: CloudDao,
    logUtil: LogUtil,
    rootService: RemoteRootService,
    directoryDao: DirectoryDao,
) : ViewModel() {
    private val _uiState =
        mutableStateOf(
            ManifestUiState(
                packageBackupEntireDao = packageBackupEntireDao,
                cloudDao = cloudDao,
                logUtil = logUtil,
                rootService = rootService,
                directoryType = DirectoryType.BACKUP,
                directoryDao = directoryDao
            )
        )
    val uiState: State<ManifestUiState>
        get() = _uiState
}
