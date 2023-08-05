package com.xayah.databackup.ui.activity.operation.page.packages.backup

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.xayah.databackup.data.PackageBackupDao
import com.xayah.databackup.data.PackageBackupManifest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

data class ManifestUiState(
    val bothPackages: Flow<List<PackageBackupManifest>>,
    val apkOnlyPackages: Flow<List<PackageBackupManifest>>,
    val dataOnlyPackages: Flow<List<PackageBackupManifest>>,
    val selectedAPKs: Flow<Int>,
    val selectedData: Flow<Int>,
)

@HiltViewModel
class ManifestViewModel @Inject constructor(packageBackupDao: PackageBackupDao) : ViewModel() {
    private val _uiState = mutableStateOf(
        ManifestUiState(
            bothPackages = packageBackupDao.queryActiveBothPackages(),
            apkOnlyPackages = packageBackupDao.queryActiveAPKOnlyPackages(),
            dataOnlyPackages = packageBackupDao.queryActiveDataOnlyPackages(),
            selectedAPKs = packageBackupDao.countSelectedAPKs(),
            selectedData = packageBackupDao.countSelectedData(),
        )
    )
    val uiState: State<ManifestUiState>
        get() = _uiState
}
