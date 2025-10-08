package com.xayah.databackup.feature.backup.apps

import android.content.pm.UserInfo
import androidx.compose.ui.state.ToggleableState
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.viewModelScope
import com.xayah.databackup.App
import com.xayah.databackup.data.AppRepository
import com.xayah.databackup.util.BaseViewModel
import com.xayah.databackup.util.DatabaseHelper
import com.xayah.databackup.util.DefStorageSize
import com.xayah.databackup.util.KeyFilterBackupUser
import com.xayah.databackup.util.KeySortsSequenceBackup
import com.xayah.databackup.util.SortsSequence
import com.xayah.databackup.util.SortsSequenceBackup
import com.xayah.databackup.util.SortsType
import com.xayah.databackup.util.SortsTypeBackup
import com.xayah.databackup.util.filterApp
import com.xayah.databackup.util.formatToStorageSize
import com.xayah.databackup.util.readEnum
import com.xayah.databackup.util.saveBoolean
import com.xayah.databackup.util.saveEnum
import com.xayah.databackup.util.saveInt
import com.xayah.databackup.util.sortByA2Z
import com.xayah.databackup.util.sortByDataSize
import com.xayah.databackup.util.sortByInstallTime
import com.xayah.databackup.util.sortByUpdateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data object UiState

open class AppsViewModel(
    appRepo: AppRepository,
) : BaseViewModel() {
    private val _uiState = MutableStateFlow(UiState)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()
    val apps = combine(
        appRepo.appsFiltered,
        _searchText,
        App.application.readEnum(SortsTypeBackup),
        App.application.readEnum(SortsSequenceBackup),
    ) { apps, searchText, sortType, sortSequence ->
        when (sortType) {
            SortsType.A2Z -> apps.sortByA2Z(sortSequence)
            SortsType.DATA_SIZE -> apps.sortByDataSize(sortSequence)
            SortsType.INSTALL_TIME -> apps.sortByInstallTime(sortSequence)
            SortsType.UPDATE_TIME -> apps.sortByUpdateTime(sortSequence)
        }.filterApp(searchText)
    }.stateIn(
        scope = viewModelScope,
        initialValue = listOf(),
        started = SharingStarted.WhileSubscribed(5_000),
    )

    val allSelected =
        apps.map { list -> list.count { it.isSelected } }.stateIn(
            scope = viewModelScope,
            initialValue = 0,
            started = SharingStarted.WhileSubscribed(5_000),
        )

    val selectedBytes =
        apps.map { list -> list.sumOf { it.selectedBytes }.formatToStorageSize }.stateIn(
            scope = viewModelScope,
            initialValue = DefStorageSize,
            started = SharingStarted.WhileSubscribed(5_000),
        )

    val apkAllSelected =
        apps.map { list -> list.count { it.option.apk } == list.size }.stateIn(
            scope = viewModelScope,
            initialValue = true,
            started = SharingStarted.WhileSubscribed(5_000),
        )

    val dataAllSelected =
        apps.map { list -> list.count { it.isDataAllSelected } == list.size }.stateIn(
            scope = viewModelScope,
            initialValue = true,
            started = SharingStarted.WhileSubscribed(5_000),
        )

    val intDataAllSelected =
        apps.map { list -> list.count { it.option.internalData } == list.size }.stateIn(
            scope = viewModelScope,
            initialValue = true,
            started = SharingStarted.WhileSubscribed(5_000),
        )

    val extDataAllSelected =
        apps.map { list -> list.count { it.option.externalData } == list.size }.stateIn(
            scope = viewModelScope,
            initialValue = true,
            started = SharingStarted.WhileSubscribed(5_000),
        )

    val addlDataAllSelected =
        apps.map { list -> list.count { it.option.additionalData } == list.size }.stateIn(
            scope = viewModelScope,
            initialValue = true,
            started = SharingStarted.WhileSubscribed(5_000),
        )

    fun selectApk(packageName: String, userId: Int, selected: Boolean) {
        withLock(Dispatchers.IO) {
            DatabaseHelper.appDao.selectApk(packageName, userId, selected)
        }
    }

    fun selectInternalData(packageName: String, userId: Int, selected: Boolean) {
        withLock(Dispatchers.IO) {
            DatabaseHelper.appDao.selectInternalData(packageName, userId, selected)
        }
    }

    fun selectExternalData(packageName: String, userId: Int, selected: Boolean) {
        withLock(Dispatchers.IO) {
            DatabaseHelper.appDao.selectExternalData(packageName, userId, selected)
        }
    }

    fun selectAdditionalData(packageName: String, userId: Int, selected: Boolean) {
        withLock(Dispatchers.IO) {
            DatabaseHelper.appDao.selectAdditionalData(packageName, userId, selected)
        }
    }

    fun selectAll(packageName: String, userId: Int, toggleableState: ToggleableState) {
        withLock(Dispatchers.IO) {
            val selected = when (toggleableState) {
                ToggleableState.On -> {
                    false
                }

                ToggleableState.Off -> {
                    true
                }

                ToggleableState.Indeterminate -> {
                    true
                }
            }
            DatabaseHelper.appDao.selectAll(packageName, userId, selected)
        }
    }

    fun selectAllApk() {
        withLock(Dispatchers.IO) {
            DatabaseHelper.appDao.selectAllApk(apps.value.map { it.pkgUserKey }, apkAllSelected.value.not())
        }
    }

    fun selectAllData() {
        withLock(Dispatchers.IO) {
            DatabaseHelper.appDao.selectAllData(apps.value.map { it.pkgUserKey }, dataAllSelected.value.not())
        }
    }

    fun selectAllIntData() {
        withLock(Dispatchers.IO) {
            DatabaseHelper.appDao.selectAllIntData(apps.value.map { it.pkgUserKey }, intDataAllSelected.value.not())
        }
    }

    fun selectAllExtData() {
        withLock(Dispatchers.IO) {
            DatabaseHelper.appDao.selectAllExtData(apps.value.map { it.pkgUserKey }, extDataAllSelected.value.not())
        }
    }

    fun selectAllAddlData() {
        withLock(Dispatchers.IO) {
            DatabaseHelper.appDao.selectAllAddlData(apps.value.map { it.pkgUserKey }, addlDataAllSelected.value.not())
        }
    }

    fun changeUser(filterUser: Int, userInfo: UserInfo) {
        withLock(Dispatchers.IO) {
            if (filterUser != userInfo.id) {
                App.application.saveInt(KeyFilterBackupUser, userInfo.id)
            }
        }
    }

    inline fun <reified T : Enum<T>> changeSort(selected: Boolean, key: Preferences.Key<String>, value: T) {
        withLock(Dispatchers.IO) {
            if (selected.not()) {
                App.application.saveEnum(key, value)
            }
        }
    }

    fun changeSequence(sequenceBackup: SortsSequence) {
        withLock(Dispatchers.IO) {
            if (sequenceBackup == SortsSequence.ASCENDING) {
                App.application.saveEnum(KeySortsSequenceBackup, SortsSequence.DESCENDING)
            } else {
                App.application.saveEnum(KeySortsSequenceBackup, SortsSequence.ASCENDING)
            }
        }
    }

    fun changeFilter(key: Preferences.Key<Boolean>, value: Boolean) {
        withLock(Dispatchers.IO) {
            App.application.saveBoolean(key, value)
        }
    }

    fun changeSearchText(text: String) {
        withLock(Dispatchers.Default) {
            _searchText.emit(text)
        }
    }
}
