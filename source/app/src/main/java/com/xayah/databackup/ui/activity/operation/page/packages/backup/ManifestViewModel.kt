package com.xayah.databackup.ui.activity.operation.page.packages.backup

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.xayah.databackup.data.CloudDao
import com.xayah.databackup.data.PackageBackupEntireDao
import com.xayah.databackup.data.PackageBackupManifest
import com.xayah.databackup.util.LogUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

data class ManifestUiState(
    val packageBackupEntireDao: PackageBackupEntireDao,
    val cloudDao: CloudDao,
    val logUtil: LogUtil,
) {
    val bothPackages: Flow<List<PackageBackupManifest>> = packageBackupEntireDao.queryActiveBothPackages().distinctUntilChanged()
    val apkOnlyPackages: Flow<List<PackageBackupManifest>> = packageBackupEntireDao.queryActiveAPKOnlyPackages().distinctUntilChanged()
    val dataOnlyPackages: Flow<List<PackageBackupManifest>> = packageBackupEntireDao.queryActiveDataOnlyPackages().distinctUntilChanged()
    val selectedBoth: Flow<Int> = packageBackupEntireDao.countSelectedBoth().distinctUntilChanged()
    val selectedAPKs: Flow<Int> = packageBackupEntireDao.countSelectedAPKs().distinctUntilChanged()
    val selectedData: Flow<Int> = packageBackupEntireDao.countSelectedData().distinctUntilChanged()
}

@HiltViewModel
class ManifestViewModel @Inject constructor(packageBackupEntireDao: PackageBackupEntireDao, cloudDao: CloudDao, logUtil: LogUtil) : ViewModel() {
    private val _uiState = mutableStateOf(ManifestUiState(packageBackupEntireDao = packageBackupEntireDao, cloudDao = cloudDao, logUtil = logUtil))
    val uiState: State<ManifestUiState>
        get() = _uiState
}
