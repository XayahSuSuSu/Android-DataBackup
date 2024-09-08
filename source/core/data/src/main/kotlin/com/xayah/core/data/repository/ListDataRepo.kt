package com.xayah.core.data.repository

import com.xayah.core.model.App
import com.xayah.core.model.File
import com.xayah.core.model.OpType
import com.xayah.core.model.SortType
import com.xayah.core.model.Target
import com.xayah.core.model.UserInfo
import com.xayah.core.util.module.combine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListDataRepo @Inject constructor(
    private val usersRepo: UsersRepo,
    private val appsRepo: AppsRepo,
    private val filesRepo: FilesRepo,
    private val workRepo: WorkRepo,
) {
    private lateinit var listData: Flow<ListData>

    private lateinit var selected: Flow<Long>
    private lateinit var total: Flow<Long>
    private lateinit var searchQuery: MutableStateFlow<String>
    private lateinit var showFilterSheet: MutableStateFlow<Boolean>
    private lateinit var sortIndex: MutableStateFlow<Int>
    private lateinit var sortType: MutableStateFlow<SortType>
    private lateinit var isUpdating: Flow<Boolean>

    // Apps
    private lateinit var showDataItemsSheet: MutableStateFlow<Boolean>
    private lateinit var showSystemApps: MutableStateFlow<Boolean>
    private lateinit var userIndex: MutableStateFlow<Int>
    private lateinit var userList: Flow<List<UserInfo>>
    private lateinit var userMap: Flow<Map<Int, Long>>
    private lateinit var appList: Flow<List<App>>

    // Files
    private lateinit var fileList: Flow<List<File>>

    fun initialize(target: Target, opType: OpType, cloudName: String, backupDir: String) {
        when (target) {
            Target.Apps -> {
                selected = appsRepo.countSelectedApps(opType)
                total = appsRepo.countApps(opType)
                searchQuery = MutableStateFlow("")
                showFilterSheet = MutableStateFlow(false)
                sortIndex = MutableStateFlow(0)
                sortType = MutableStateFlow(SortType.ASCENDING)
                isUpdating = when (opType) {
                    OpType.BACKUP -> combine(workRepo.isFullInitRunning(), workRepo.isFullInitAndUpdateAppsRunning(), workRepo.isFastInitAndUpdateAppsRunning()) { fInit, full, fast -> fInit || full || fast }
                    OpType.RESTORE -> combine(workRepo.isFullInitRunning(), workRepo.isLoadAppBackupsRunning()) { fInit, lAppBackups -> fInit || lAppBackups }
                }

                showDataItemsSheet = MutableStateFlow(false)
                showSystemApps = MutableStateFlow(false)
                userIndex = MutableStateFlow(0)
                userList = usersRepo.getUsers(opType)
                userMap = usersRepo.getUsersMap(opType)

                listData = getAppListData()
                appList = appsRepo.getApps(opType = opType, listData = listData, cloudName = cloudName, backupDir = backupDir)
            }

            Target.Files -> {
                selected = filesRepo.countSelectedFiles(opType)
                total = filesRepo.countFiles(opType)
                searchQuery = MutableStateFlow("")
                showFilterSheet = MutableStateFlow(false)
                sortIndex = MutableStateFlow(0)
                sortType = MutableStateFlow(SortType.ASCENDING)
                isUpdating = when (opType) {
                    OpType.BACKUP -> combine(workRepo.isFullInitRunning(), workRepo.isFastInitAndUpdateFilesRunning()) { fInit, fast -> fInit || fast }
                    OpType.RESTORE -> combine(workRepo.isFullInitRunning(), workRepo.isLoadFileBackupsRunning()) { fInit, lFileBackups -> fInit || lFileBackups }
                }

                listData = getFileListData()
                fileList = filesRepo.getFiles(opType = opType, listData = listData, cloudName = cloudName, backupDir = backupDir)
            }
        }
    }

    private fun getAppListData(): Flow<ListData.Apps> = combine(
        selected,
        total,
        searchQuery,
        showFilterSheet,
        sortIndex,
        sortType,
        isUpdating,
        showDataItemsSheet,
        showSystemApps,
        userIndex,
        userList,
        userMap,
    ) { s, t, sQuery, sFSheet, sIndex, sType, iUpdating, sDISheet, sSystemApps, uIndex, uList, uMap ->
        ListData.Apps(s, t, sQuery, sFSheet, sIndex, sType, iUpdating, sDISheet, sSystemApps, uIndex, uList, uMap)
    }

    private fun getFileListData(): Flow<ListData.Files> = combine(
        selected,
        total,
        searchQuery,
        showFilterSheet,
        sortIndex,
        sortType,
        isUpdating,
    ) { s, t, sQuery, sFSheet, sIndex, sType, iUpdating ->
        ListData.Files(s, t, sQuery, sFSheet, sIndex, sType, iUpdating)
    }

    fun getListData(): Flow<ListData> = listData

    fun getAppList(): Flow<List<App>> = appList

    fun getFileList(): Flow<List<File>> = fileList

    suspend fun setShowSystemApps(block: (Boolean) -> Boolean) {
        showSystemApps.emit(block(showSystemApps.value))
    }

    suspend fun setSortIndex(block: (Int) -> Int) {
        sortIndex.emit(block(sortIndex.value))
    }

    suspend fun setSortType(block: (SortType) -> SortType) {
        sortType.emit(block(sortType.value))
    }

    suspend fun setSearchQuery(value: String) {
        searchQuery.emit(value)
    }

    suspend fun setUserIndex(value: Int) {
        userIndex.emit(value)
    }

    suspend fun setShowFilterSheet(value: Boolean) {
        showFilterSheet.emit(value)
    }

    suspend fun setShowDataItemsSheet(value: Boolean) {
        showDataItemsSheet.emit(value)
    }
}

sealed class ListData(
    open val selected: Long,
    open val total: Long,
    open val searchQuery: String,
    open val showFilterSheet: Boolean,
    open val sortIndex: Int,
    open val sortType: SortType,
    open val isUpdating: Boolean,
) {
    data class Apps(
        override val selected: Long,
        override val total: Long,
        override val searchQuery: String,
        override val showFilterSheet: Boolean,
        override val sortIndex: Int,
        override val sortType: SortType,
        override val isUpdating: Boolean,
        val showDataItemsSheet: Boolean,
        val showSystemApps: Boolean,
        val userIndex: Int,
        val userList: List<UserInfo>,
        val userMap: Map<Int, Long>,
    ) : ListData(selected, total, searchQuery, showFilterSheet, sortIndex, sortType, isUpdating)

    data class Files(
        override val selected: Long,
        override val total: Long,
        override val searchQuery: String,
        override val showFilterSheet: Boolean,
        override val sortIndex: Int,
        override val sortType: SortType,
        override val isUpdating: Boolean,
    ) : ListData(selected, total, searchQuery, showFilterSheet, sortIndex, sortType, isUpdating)
}
