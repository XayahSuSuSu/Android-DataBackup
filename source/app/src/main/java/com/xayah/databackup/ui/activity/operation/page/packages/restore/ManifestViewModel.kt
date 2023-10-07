package com.xayah.databackup.ui.activity.operation.page.packages.restore

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.xayah.databackup.data.PackageRestoreEntireDao
import com.xayah.databackup.data.PackageRestoreManifest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

data class ManifestUiState(
    val packageRestoreEntireDao: PackageRestoreEntireDao,
) {
    val bothPackages: Flow<List<PackageRestoreManifest>> = packageRestoreEntireDao.queryActiveBothPackages().distinctUntilChanged()
    val apkOnlyPackages: Flow<List<PackageRestoreManifest>> = packageRestoreEntireDao.queryActiveAPKOnlyPackages().distinctUntilChanged()
    val dataOnlyPackages: Flow<List<PackageRestoreManifest>> = packageRestoreEntireDao.queryActiveDataOnlyPackages().distinctUntilChanged()
    val selectedBoth: Flow<Int> = packageRestoreEntireDao.countSelectedBoth().distinctUntilChanged()
    val selectedAPKs: Flow<Int> = packageRestoreEntireDao.countSelectedAPKs().distinctUntilChanged()
    val selectedData: Flow<Int> = packageRestoreEntireDao.countSelectedData().distinctUntilChanged()
}

@HiltViewModel
class ManifestViewModel @Inject constructor(packageRestoreEntireDao: PackageRestoreEntireDao) : ViewModel() {
    private val _uiState = mutableStateOf(ManifestUiState(packageRestoreEntireDao = packageRestoreEntireDao))
    val uiState: State<ManifestUiState>
        get() = _uiState
}
