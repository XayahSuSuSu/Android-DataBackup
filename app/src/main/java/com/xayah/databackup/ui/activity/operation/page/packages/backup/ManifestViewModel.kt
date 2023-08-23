package com.xayah.databackup.ui.activity.operation.page.packages.backup

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.xayah.databackup.data.PackageBackupEntireDao
import com.xayah.databackup.data.PackageBackupManifest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

data class ManifestUiState(
    val packageBackupEntireDao: PackageBackupEntireDao,
) {
    val bothPackages: Flow<List<PackageBackupManifest>> = packageBackupEntireDao.queryActiveBothPackages()
    val apkOnlyPackages: Flow<List<PackageBackupManifest>> = packageBackupEntireDao.queryActiveAPKOnlyPackages()
    val dataOnlyPackages: Flow<List<PackageBackupManifest>> = packageBackupEntireDao.queryActiveDataOnlyPackages()
    val selectedAPKs: Flow<Int> = packageBackupEntireDao.countSelectedAPKs()
    val selectedData: Flow<Int> = packageBackupEntireDao.countSelectedData()
}

@HiltViewModel
class ManifestViewModel @Inject constructor(packageBackupEntireDao: PackageBackupEntireDao) : ViewModel() {
    private val _uiState = mutableStateOf(ManifestUiState(packageBackupEntireDao = packageBackupEntireDao))
    val uiState: State<ManifestUiState>
        get() = _uiState
}
