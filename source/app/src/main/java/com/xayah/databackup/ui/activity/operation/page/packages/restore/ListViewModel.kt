package com.xayah.databackup.ui.activity.operation.page.packages.restore

import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.xayah.databackup.data.PackageRestoreEntire
import com.xayah.databackup.data.PackageRestoreEntireDao
import com.xayah.librootservice.util.withIOContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

data class ListUiState(
    val packageRestoreEntireDao: PackageRestoreEntireDao,
    val timestamps: List<Long>,
    val selectedIndex: Int,
) {
    val timestamp: Long = timestamps.getOrElse(selectedIndex) { 0 }
    val packages: Flow<List<PackageRestoreEntire>> =
        packageRestoreEntireDao.queryPackages(timestamp).distinctUntilChanged()
    val selectedAPKs: Flow<Int> = packageRestoreEntireDao.countSelectedAPKs().distinctUntilChanged()
    val selectedData: Flow<Int> = packageRestoreEntireDao.countSelectedData().distinctUntilChanged()
}

@HiltViewModel
class ListViewModel @Inject constructor(
    private val packageRestoreEntireDao: PackageRestoreEntireDao,
) : ViewModel() {
    private val _uiState = mutableStateOf(ListUiState(packageRestoreEntireDao = packageRestoreEntireDao, timestamps = listOf(), selectedIndex = 0))
    val uiState: State<ListUiState>
        get() = _uiState

    suspend fun updatePackage(item: PackageRestoreEntire) = packageRestoreEntireDao.update(item)
    suspend fun updatePackages(items: List<PackageRestoreEntire>) = packageRestoreEntireDao.upsert(items)
    suspend fun delete(items: List<PackageRestoreEntire>) = packageRestoreEntireDao.delete(items)

    private fun setTimestamps(timestamps: List<Long>) {
        _uiState.value = uiState.value.copy(timestamps = timestamps)
    }

     suspend fun setSelectedIndex(index: Int) {
         val uiState by uiState
         _uiState.value = uiState.copy(selectedIndex = index)

         val dao = uiState.packageRestoreEntireDao
         val timestamp = uiState.timestamp
         // Inactivate all packages then activate displayed ones.
         dao.updateActive(active = false)
         dao.updateActive(timestamp = timestamp, active = true)
     }

    suspend fun initializeUiState() = withIOContext {
        val uiState by uiState
        val dao = uiState.packageRestoreEntireDao

        val timestamps = dao.queryTimestamps()
        setTimestamps(timestamps)
        setSelectedIndex(timestamps.lastIndex)
    }
}
