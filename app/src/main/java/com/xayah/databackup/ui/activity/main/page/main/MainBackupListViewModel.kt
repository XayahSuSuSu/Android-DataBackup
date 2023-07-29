package com.xayah.databackup.ui.activity.main.page.main

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

data class MainBackupListUiState(
    val packages: Flow<List<PackageBackupEntire>>,
)

@HiltViewModel
class MainBackupListViewModel @Inject constructor(val packageBackupDao: PackageBackupDao) : ViewModel() {
    private val _uiState = mutableStateOf(MainBackupListUiState(packages = packageBackupDao.queryActivePackages()))
    val uiState: State<MainBackupListUiState>
        get() = _uiState
    val packages = uiState.value.packages

    suspend fun inactivatePackages() = packageBackupDao.updateActive(false)
    suspend fun activatePackages(items: List<PackageBackupActivate>) = packageBackupDao.update(items)
    suspend fun updatePackages(items: List<PackageBackupUpdate>) = packageBackupDao.insert(items)
    suspend fun updatePackage(item: PackageBackupEntire) = packageBackupDao.update(item)

}
