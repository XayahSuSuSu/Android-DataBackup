package com.xayah.databackup.ui.activity.operation.page.packages.backup

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.xayah.databackup.data.PackageBackupActivate
import com.xayah.databackup.data.PackageBackupEntire
import com.xayah.databackup.data.PackageBackupEntireDao
import com.xayah.databackup.data.PackageBackupUpdate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

data class ListUiState(
    val packageBackupEntireDao: PackageBackupEntireDao,
) {
    val packages: Flow<List<PackageBackupEntire>> = packageBackupEntireDao.queryActivePackages()
    val selectedAPKs: Flow<Int> = packageBackupEntireDao.countSelectedAPKs()
    val selectedData: Flow<Int> = packageBackupEntireDao.countSelectedData()
}

@HiltViewModel
class ListViewModel @Inject constructor(private val packageBackupEntireDao: PackageBackupEntireDao) : ViewModel() {
    private val _uiState = mutableStateOf(ListUiState(packageBackupEntireDao = packageBackupEntireDao))
    val uiState: State<ListUiState>
        get() = _uiState

    suspend fun inactivatePackages() = packageBackupEntireDao.updateActive(false)
    suspend fun activatePackages(items: List<PackageBackupActivate>) = packageBackupEntireDao.update(items)
    suspend fun updatePackages(items: List<PackageBackupUpdate>) = packageBackupEntireDao.insert(items)
    suspend fun updatePackage(item: PackageBackupEntire) = packageBackupEntireDao.update(item)
}
