package com.xayah.core.data.repository

import android.content.Context
import android.os.Build
import com.xayah.core.data.R
import com.xayah.core.database.dao.PackageBackupEntireDao
import com.xayah.core.database.model.OperationMask
import com.xayah.core.database.model.PackageBackupActivate
import com.xayah.core.database.model.PackageBackupEntire
import com.xayah.core.database.model.PackageBackupUpdate
import com.xayah.core.database.model.StorageStats
import com.xayah.core.datastore.readBackupFilterFlagIndex
import com.xayah.core.datastore.readBackupSortType
import com.xayah.core.datastore.readBackupSortTypeIndex
import com.xayah.core.datastore.readBackupUserId
import com.xayah.core.datastore.readIconUpdateTime
import com.xayah.core.datastore.saveBackupFilterFlagIndex
import com.xayah.core.datastore.saveBackupSortType
import com.xayah.core.datastore.saveBackupSortTypeIndex
import com.xayah.core.datastore.saveIconUpdateTime
import com.xayah.core.model.SortType
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.rootservice.util.withIOContext
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.model.TopBarState
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.util.DateUtil
import com.xayah.core.util.PathUtil
import com.xayah.core.util.command.BaseUtil
import com.xayah.core.util.iconDir
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.Collator
import javax.inject.Inject

class PackageBackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val rootService: RemoteRootService,
    private val packageBackupDao: PackageBackupEntireDao,
    private val pathUtil: PathUtil,
) {
    val packages = packageBackupDao.queryActivePackages().distinctUntilChanged()
    val packagesApkOnly = packages.map { packages -> packages.filter { it.operationCode == OperationMask.Apk } }.distinctUntilChanged()
    val packagesDataOnly = packages.map { packages -> packages.filter { it.operationCode == OperationMask.Data } }.distinctUntilChanged()
    val packagesBoth = packages.map { packages -> packages.filter { it.operationCode == OperationMask.Both } }.distinctUntilChanged()
    val selectedPackages = packageBackupDao.querySelectedPackagesFlow().distinctUntilChanged()
    val selectedAPKsCount = packageBackupDao.countSelectedAPKs().distinctUntilChanged()
    val selectedDataCount = packageBackupDao.countSelectedData().distinctUntilChanged()

    val backupFilterFlagIndex = context.readBackupFilterFlagIndex()
    val backupSortTypeIndex = context.readBackupSortTypeIndex()
    val backupSortType = context.readBackupSortType()

    suspend fun saveBackupSortType(value: SortType) = context.saveBackupSortType(value = value)
    suspend fun saveBackupSortTypeIndex(value: Int) = context.saveBackupSortTypeIndex(value = value)
    suspend fun saveBackupFilterFlagIndex(value: Int) = context.saveBackupFilterFlagIndex(value = value)

    private suspend fun getInstalledPackages(userId: Int) = rootService.getInstalledPackagesAsUser(0, userId).filter {
        // Filter itself
        it.packageName != context.packageName
    }

    suspend fun activate() = withIOContext {
        val userId = context.readBackupUserId().first()

        // Inactivate all packages and activate installed only.
        packageBackupDao.updateActive(false)
        val installedPackages = getInstalledPackages(userId)
        val activePackages = mutableListOf<PackageBackupActivate>()
        installedPackages.forEach { packageInfo ->
            activePackages.add(PackageBackupActivate(packageName = packageInfo.packageName, active = true))
        }
        packageBackupDao.update(activePackages)
    }

    suspend fun update(topBarState: MutableStateFlow<TopBarState>) = run {
        val pm = context.packageManager
        val userId = context.readBackupUserId().first()
        val userHandle = rootService.getUserHandle(userId)
        val installedPackages = getInstalledPackages(userId)
        val installedPackagesCount = (installedPackages.size - 1).coerceAtLeast(1)

        val newPackages = mutableListOf<PackageBackupUpdate>()
        BaseUtil.mkdirs(context.iconDir())
        // Update packages' info.
        val iconUpdateTime = context.readIconUpdateTime().first()
        val now = DateUtil.getTimestamp()
        val hasPassedOneDay = DateUtil.getNumberOfDaysPassed(iconUpdateTime, now) >= 1
        if (hasPassedOneDay) context.saveIconUpdateTime(now)

        // Get 1/10 of total count.
        val epoch: Int = ((installedPackagesCount + 1) / 10).coerceAtLeast(1)

        installedPackages.forEachIndexed { index, packageInfo ->
            val iconPath = pathUtil.getPackageIconPath(packageInfo.packageName)
            val iconExists = rootService.exists(iconPath)
            if (iconExists.not() || (iconExists && hasPassedOneDay)) {
                val icon = packageInfo.applicationInfo.loadIcon(pm)
                BaseUtil.writeIcon(icon = icon, dst = iconPath)
            }
            val storageStats = StorageStats()
            if (userHandle != null) {
                rootService.queryStatsForPackage(packageInfo, userHandle).also { stats ->
                    if (stats != null) {
                        storageStats.appBytes = stats.appBytes
                        storageStats.cacheBytes = stats.cacheBytes
                        storageStats.dataBytes = stats.dataBytes
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) storageStats.externalCacheBytes = stats.externalCacheBytes
                    }
                }
            }

            newPackages.add(
                PackageBackupUpdate(
                    packageName = packageInfo.packageName,
                    label = packageInfo.applicationInfo.loadLabel(pm).toString(),
                    versionName = packageInfo.versionName ?: "",
                    versionCode = packageInfo.longVersionCode,
                    storageStats = storageStats,
                    flags = packageInfo.applicationInfo.flags,
                    firstInstallTime = packageInfo.firstInstallTime,
                    active = true
                )
            )
            if (index % epoch == 0)
                topBarState.emit(
                    TopBarState(
                        progress = index.toFloat() / installedPackagesCount,
                        title = StringResourceToken.fromStringId(R.string.updating)
                    )
                )
        }
        packageBackupDao.upsert(newPackages)
        topBarState.emit(TopBarState(progress = 1f, title = StringResourceToken.fromStringId(R.string.backup_list)))
    }

    suspend fun updatePackage(entity: PackageBackupEntire) = packageBackupDao.update(entity)

    suspend fun andOpCodeByMask(mask: Int, packageNames: List<String>) = packageBackupDao.andOpCodeByMask(mask, packageNames)
    suspend fun orOpCodeByMask(mask: Int, packageNames: List<String>) = packageBackupDao.orOpCodeByMask(mask, packageNames)

    fun getFlagPredicate(index: Int): (PackageBackupEntire) -> Boolean = { packageBackup ->
        when (index) {
            1 -> packageBackup.isSystemApp.not()
            2 -> packageBackup.isSystemApp
            else -> true
        }
    }

    fun getKeyPredicate(key: String): (PackageBackupEntire) -> Boolean = { packageBackup ->
        packageBackup.label.lowercase().contains(key.lowercase()) || packageBackup.packageName.lowercase().contains(key.lowercase())
    }

    fun getSortComparator(sortIndex: Int, sortType: SortType): Comparator<in PackageBackupEntire> = when (sortIndex) {
        1 -> sortByInstallTime(sortType)
        2 -> sortByDataSize(sortType)
        else -> sortByAlphabet(sortType)
    }

    private fun sortByAlphabet(type: SortType): Comparator<PackageBackupEntire> = Comparator { entity1, entity2 ->
        if (entity1 != null && entity2 != null) {
            when (type) {
                SortType.ASCENDING -> {
                    Collator.getInstance().let { collator -> collator.getCollationKey(entity1.label).compareTo(collator.getCollationKey(entity2.label)) }
                }

                SortType.DESCENDING -> {
                    Collator.getInstance().let { collator -> collator.getCollationKey(entity2.label).compareTo(collator.getCollationKey(entity1.label)) }
                }
            }
        } else {
            0
        }
    }

    private fun sortByInstallTime(type: SortType): Comparator<PackageBackupEntire> = when (type) {
        SortType.ASCENDING -> {
            compareBy { entity -> entity.firstInstallTime }
        }

        SortType.DESCENDING -> {
            compareByDescending { entity -> entity.firstInstallTime }
        }
    }

    private fun sortByDataSize(type: SortType): Comparator<PackageBackupEntire> = when (type) {
        SortType.ASCENDING -> {
            compareBy { entity -> entity.sizeBytes }
        }

        SortType.DESCENDING -> {
            compareByDescending { entity -> entity.sizeBytes }
        }
    }
}
