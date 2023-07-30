package com.xayah.databackup.ui.activity.operation.page.packageBackup

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.xayah.databackup.data.PackageBackupActivate
import com.xayah.databackup.data.PackageBackupDao
import com.xayah.databackup.data.PackageBackupEntire
import com.xayah.databackup.data.PackageBackupUpdate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

data class PackageBackupListUiState(
    val packages: Flow<List<PackageBackupEntire>>,
)

@HiltViewModel
class PackageBackupListViewModel @Inject constructor(private val packageBackupDao: PackageBackupDao) : ViewModel() {
    private val _uiState = mutableStateOf(PackageBackupListUiState(packages = packageBackupDao.queryActivePackages()))
    val uiState: State<PackageBackupListUiState>
        get() = _uiState
    val packages = uiState.value.packages

    suspend fun inactivatePackages() = packageBackupDao.updateActive(false)
    suspend fun activatePackages(items: List<PackageBackupActivate>) = packageBackupDao.update(items)
    suspend fun countActivePackages() = packageBackupDao.countActivePackages()
    suspend fun updatePackages(items: List<PackageBackupUpdate>) = packageBackupDao.insert(items)
    suspend fun updatePackage(item: PackageBackupEntire) = packageBackupDao.update(item)

}
