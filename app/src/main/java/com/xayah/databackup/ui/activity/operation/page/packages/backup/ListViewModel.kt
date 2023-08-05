package com.xayah.databackup.ui.activity.operation.page.packages.backup

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

data class ListUiState(
    val packages: Flow<List<PackageBackupEntire>>,
    val selectedAPKs: Flow<Int>,
    val selectedData: Flow<Int>,
)

@HiltViewModel
class ListViewModel @Inject constructor(private val packageBackupDao: PackageBackupDao) : ViewModel() {
    private val _uiState = mutableStateOf(
        ListUiState(
            packages = packageBackupDao.queryActivePackages(),
            selectedAPKs = packageBackupDao.countSelectedAPKs(),
            selectedData = packageBackupDao.countSelectedData(),
        )
    )
    val uiState: State<ListUiState>
        get() = _uiState

    suspend fun inactivatePackages() = packageBackupDao.updateActive(false)
    suspend fun activatePackages(items: List<PackageBackupActivate>) = packageBackupDao.update(items)
    suspend fun updatePackages(items: List<PackageBackupUpdate>) = packageBackupDao.insert(items)
    suspend fun updatePackage(item: PackageBackupEntire) = packageBackupDao.update(item)
}
