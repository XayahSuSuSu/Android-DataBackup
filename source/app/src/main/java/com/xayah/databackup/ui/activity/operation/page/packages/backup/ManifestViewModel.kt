package com.xayah.databackup.ui.activity.operation.page.packages.backup

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.xayah.core.database.dao.CloudDao
import com.xayah.core.database.dao.DirectoryDao
import com.xayah.core.model.OpType
import com.xayah.core.database.dao.PackageBackupEntireDao
import com.xayah.core.database.model.PackageBackupManifest
import com.xayah.core.database.model.formatSize
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
    val opType: OpType,
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
    val availableBytesDisplay: Flow<String> = directoryDao.querySelectedByDirectoryTypeFlow(opType).map {
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
                opType = OpType.BACKUP,
                directoryDao = directoryDao
            )
        )
    val uiState: State<ManifestUiState>
        get() = _uiState
}
