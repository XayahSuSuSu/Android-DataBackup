package com.xayah.core.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.xayah.core.data.R
import com.xayah.core.data.util.srcDir
import com.xayah.core.database.dao.PackageDao
import com.xayah.core.datastore.readCompressionType
import com.xayah.core.datastore.readIconUpdateTime
import com.xayah.core.datastore.readLoadedIconMD5
import com.xayah.core.datastore.saveIconUpdateTime
import com.xayah.core.datastore.saveLoadedIconMD5
import com.xayah.core.model.CompressionType
import com.xayah.core.model.DataType
import com.xayah.core.model.DefaultPreserveId
import com.xayah.core.model.OpType
import com.xayah.core.model.SortType
import com.xayah.core.model.database.PackageDataStates
import com.xayah.core.model.database.PackageDataStats
import com.xayah.core.model.database.PackageEntity
import com.xayah.core.model.database.PackageEntityWithCount
import com.xayah.core.model.database.PackageExtraInfo
import com.xayah.core.model.database.PackageIndexInfo
import com.xayah.core.model.database.PackageInfo
import com.xayah.core.model.database.PackageStorageStats
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.model.TopBarState
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.util.ConfigsPackageRestoreName
import com.xayah.core.util.DateUtil
import com.xayah.core.util.IconRelativeDir
import com.xayah.core.util.PathUtil
import com.xayah.core.util.PermissionUtil
import com.xayah.core.util.command.BaseUtil
import com.xayah.core.util.command.PackageUtil
import com.xayah.core.util.command.Tar
import com.xayah.core.util.filesDir
import com.xayah.core.util.iconDir
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import java.text.Collator
import javax.inject.Inject

class PackageRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val rootService: RemoteRootService,
    private val packageDao: PackageDao,
    private val pathUtil: PathUtil,
) {
    val packages = packageDao.queryFlow().distinctUntilChanged()
    val activatedCount = packageDao.countActivatedFlow().distinctUntilChanged()
    private val archivesPackagesDir by lazy { pathUtil.getLocalBackupArchivesPackagesDir() }

    fun getArchiveDst(dstDir: String, dataType: DataType, ct: CompressionType) = "${dstDir}/${dataType.type}.${ct.suffix}"

    private suspend fun queryPackage(packageName: String, opType: OpType, userId: Int, preserveId: Long) =
        packageDao.query(packageName, opType, userId, preserveId)

    private suspend fun queryPackages(packageName: String, opType: OpType, userId: Int) = packageDao.query(packageName, opType, userId)

    suspend fun updatePackageDataSize(packageName: String, opType: OpType, userId: Int, preserveId: Long) {
        queryPackage(packageName, opType, userId, preserveId).also {
            if (it != null) {
                it.extraInfo.existed = rootService.queryInstalled(packageName = packageName, userId = userId)
                if (it.extraInfo.existed) {
                    it.displayStats.apkBytes = calculateDataSize(it, DataType.PACKAGE_APK)
                    it.displayStats.userBytes = calculateDataSize(it, DataType.PACKAGE_USER)
                    it.displayStats.userDeBytes = calculateDataSize(it, DataType.PACKAGE_USER_DE)
                    it.displayStats.dataBytes = calculateDataSize(it, DataType.PACKAGE_DATA)
                    it.displayStats.obbBytes = calculateDataSize(it, DataType.PACKAGE_OBB)
                    it.displayStats.mediaBytes = calculateDataSize(it, DataType.PACKAGE_MEDIA)
                }
                upsert(it)
            }
        }
    }

    suspend fun updatePackageArchivesSize(packageName: String, opType: OpType, userId: Int) {
        queryPackages(packageName, opType, userId).onEach {
            it.displayStats.apkBytes = calculateArchiveSize(it, DataType.PACKAGE_APK)
            it.displayStats.userBytes = calculateArchiveSize(it, DataType.PACKAGE_USER)
            it.displayStats.userDeBytes = calculateArchiveSize(it, DataType.PACKAGE_USER_DE)
            it.displayStats.dataBytes = calculateArchiveSize(it, DataType.PACKAGE_DATA)
            it.displayStats.obbBytes = calculateArchiveSize(it, DataType.PACKAGE_OBB)
            it.displayStats.mediaBytes = calculateArchiveSize(it, DataType.PACKAGE_MEDIA)
            upsert(it)
        }
    }

    fun queryPackageFlow(packageName: String, opType: OpType, userId: Int, preserveId: Long) =
        packageDao.queryFlow(packageName, opType, userId, preserveId).distinctUntilChanged()

    fun queryPackagesFlow(packageName: String, opType: OpType, userId: Int) =
        packageDao.queryFlow(packageName, opType, userId).distinctUntilChanged()

    private suspend fun getInstalledPackages(userId: Int) = rootService.getInstalledPackagesAsUser(PackageManager.GET_PERMISSIONS, userId).filter {
        // Filter itself
        it.packageName != context.packageName
    }

    fun getKeyPredicate(key: String): (PackageEntityWithCount) -> Boolean = { p ->
        p.entity.packageInfo.label.lowercase().contains(key.lowercase()) || p.entity.packageName.lowercase().contains(key.lowercase())
    }

    fun getFlagPredicate(index: Int): (PackageEntityWithCount) -> Boolean = { p ->
        when (index) {
            1 -> p.entity.isSystemApp.not()
            2 -> p.entity.isSystemApp
            else -> true
        }
    }

    fun getUserIdPredicate(indexList: List<Int>, userIdList: List<Int>): (PackageEntityWithCount) -> Boolean = { p ->
        runCatching { p.entity.userId in indexList.map { userIdList[it] } }.getOrDefault(p.entity.userId == 0)
    }

    private fun sortByInstallTime(type: SortType): Comparator<PackageEntityWithCount> = when (type) {
        SortType.ASCENDING -> {
            compareBy { p -> p.entity.packageInfo.firstInstallTime }
        }

        SortType.DESCENDING -> {
            compareByDescending { p -> p.entity.packageInfo.firstInstallTime }
        }
    }

    private fun sortByDataSize(type: SortType): Comparator<PackageEntityWithCount> = when (type) {
        SortType.ASCENDING -> {
            compareBy { p -> p.entity.storageStatsBytes }
        }

        SortType.DESCENDING -> {
            compareByDescending { p -> p.entity.storageStatsBytes }
        }
    }

    private fun sortByAlphabet(type: SortType): Comparator<PackageEntityWithCount> = Comparator { p1, p2 ->
        if (p1 != null && p2 != null) {
            when (type) {
                SortType.ASCENDING -> {
                    Collator.getInstance().let { collator ->
                        collator.getCollationKey(p1.entity.packageInfo.label).compareTo(collator.getCollationKey(p2.entity.packageInfo.label))
                    }
                }

                SortType.DESCENDING -> {
                    Collator.getInstance().let { collator ->
                        collator.getCollationKey(p2.entity.packageInfo.label).compareTo(collator.getCollationKey(p1.entity.packageInfo.label))
                    }
                }
            }
        } else {
            0
        }
    }

    fun getSortComparator(sortIndex: Int, sortType: SortType): Comparator<in PackageEntityWithCount> = when (sortIndex) {
        1 -> sortByInstallTime(sortType)
        2 -> sortByDataSize(sortType)
        else -> sortByAlphabet(sortType)
    }

    suspend fun refresh(topBarState: MutableStateFlow<TopBarState>) = run {
        packageDao.clearExisted(opType = OpType.BACKUP)
        val title = topBarState.value.title
        val pm = context.packageManager
        val userInfoList = rootService.getUsers()
        for (userInfo in userInfoList) {
            val userId = userInfo.id
            val userHandle = rootService.getUserHandle(userId)
            val installedPackages = getInstalledPackages(userId)
            val installedPackagesCount = (installedPackages.size - 1).coerceAtLeast(1)

            // Get 1/10 of total count.
            val epoch: Int = ((installedPackagesCount + 1) / 10).coerceAtLeast(1)

            // Update packages' info.
            BaseUtil.mkdirs(context.iconDir())
            val iconUpdateTime = context.readIconUpdateTime().first()
            val now = DateUtil.getTimestamp()
            val hasPassedOneDay = DateUtil.getNumberOfDaysPassed(iconUpdateTime, now) >= 1
            if (hasPassedOneDay) context.saveIconUpdateTime(now)
            installedPackages.forEachIndexed { index, info ->
                val permissions = PermissionUtil.getPermission(packageManager = pm, packageInfo = info)
                val uid = info.applicationInfo.uid
                val hasKeystore = PackageUtil.hasKeystore(uid)
                val ssaid = rootService.getPackageSsaidAsUser(packageName = info.packageName, uid = uid, userId = userId)
                val iconPath = pathUtil.getPackageIconPath(info.packageName)
                val iconExists = rootService.exists(iconPath)
                if (iconExists.not() || (iconExists && hasPassedOneDay)) {
                    val icon = info.applicationInfo.loadIcon(pm)
                    BaseUtil.writeIcon(icon = icon, dst = iconPath)
                }
                val packageInfo = PackageInfo(
                    label = info.applicationInfo.loadLabel(pm).toString(),
                    versionName = info.versionName ?: "",
                    versionCode = info.longVersionCode,
                    flags = info.applicationInfo.flags,
                    firstInstallTime = info.firstInstallTime,
                )
                val extraInfo = PackageExtraInfo(
                    uid = uid,
                    labels = listOf(),
                    hasKeystore = hasKeystore,
                    permissions = permissions,
                    ssaid = ssaid,
                    activated = false,
                    existed = true,
                )
                val indexInfo = PackageIndexInfo(
                    opType = OpType.BACKUP,
                    packageName = info.packageName,
                    userId = userId,
                    compressionType = context.readCompressionType().first(),
                    preserveId = DefaultPreserveId,
                )
                val packageEntity =
                    packageDao.query(packageName = info.packageName, opType = OpType.BACKUP, userId = userId, preserveId = DefaultPreserveId)
                        ?: PackageEntity(
                            id = 0,
                            indexInfo = indexInfo,
                            packageInfo = packageInfo,
                            extraInfo = extraInfo,
                            dataStates = PackageDataStates(),
                            storageStats = PackageStorageStats(),
                            dataStats = PackageDataStats(),
                            displayStats = PackageDataStats(),
                        )
                // Update if exists.
                packageEntity.apply {
                    this.packageInfo = packageInfo
                    this.extraInfo.uid = uid
                    this.extraInfo.hasKeystore = hasKeystore
                    this.extraInfo.permissions = permissions
                    this.extraInfo.ssaid = ssaid
                    this.extraInfo.existed = true
                }
                if (userHandle != null) {
                    rootService.queryStatsForPackage(info, userHandle).also { stats ->
                        if (stats != null) {
                            packageEntity.apply {
                                this.storageStats.appBytes = stats.appBytes
                                this.storageStats.cacheBytes = stats.cacheBytes
                                this.storageStats.dataBytes = stats.dataBytes
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) this.storageStats.externalCacheBytes = stats.externalCacheBytes
                            }
                        }
                    }
                }
                upsert(packageEntity)

                if (index % epoch == 0)
                    topBarState.emit(
                        TopBarState(
                            progress = index.toFloat() / installedPackagesCount,
                            title = StringResourceToken.fromStringId(R.string.updating)
                        )
                    )
            }
            topBarState.emit(TopBarState(progress = 1f, title = title))
        }
        topBarState.emit(TopBarState(progress = 1f, title = StringResourceToken.fromStringId(R.string.updating), indeterminate = true))
        refreshFromLocal(path = archivesPackagesDir)
        loadLocalIcon()
        topBarState.emit(TopBarState(progress = 1f, title = title, indeterminate = false))
    }

    private suspend fun loadLocalIcon() {
        val archivePath = "${pathUtil.getLocalBackupConfigsDir()}/$IconRelativeDir.${CompressionType.TAR.suffix}"
        if (rootService.exists(archivePath)) {
            val loadedIconMD5 = context.readLoadedIconMD5().first()
            val iconMD5 = rootService.calculateMD5(archivePath) ?: ""
            if (loadedIconMD5 != iconMD5) {
                Tar.decompress(src = archivePath, dst = context.filesDir(), extra = CompressionType.TAR.decompressPara)
                PathUtil.setFilesDirSELinux(context)
                context.saveLoadedIconMD5(iconMD5)
            }
        }
    }

    private suspend fun refreshFromLocal(path: String) {
        val paths = rootService.walkFileTree(path)
        paths.forEach {
            val fileName = PathUtil.getFileName(it.pathString)
            if (fileName == ConfigsPackageRestoreName) {
                runCatching {
                    val stored = rootService.readProtoBuf<PackageEntity>(it.pathString).also { p ->
                        p?.extraInfo?.existed = true
                        p?.extraInfo?.activated = false
                    }
                    if (stored != null) {
                        packageDao.query(stored.packageName, stored.indexInfo.opType, stored.userId, stored.preserveId, stored.indexInfo.compressionType).also { p ->
                            if (p == null)
                                packageDao.upsert(stored)
                        }
                    }
                }
            }
        }
    }

    suspend fun refreshFromLocalPackage(packageName: String) {
        refreshFromLocal(path = "$archivesPackagesDir/$packageName")
    }


    private suspend fun getPackageSourceDir(packageName: String, userId: Int) = rootService.getPackageSourceDir(packageName, userId).let { list ->
        if (list.isNotEmpty()) PathUtil.getParentPath(list[0]) else ""
    }

    fun getDataSrcDir(dataType: DataType, userId: Int) = dataType.srcDir(userId)

    fun getDataSrc(srcDir: String, packageName: String) = "$srcDir/$packageName"

    private suspend fun calculateArchiveSize(p: PackageEntity, dataType: DataType) = rootService.calculateSize(
        getArchiveDst("${archivesPackagesDir}/${p.archivesPreserveRelativeDir}", dataType, p.indexInfo.compressionType)
    )

    private suspend fun calculateDataSize(p: PackageEntity, dataType: DataType) = run {
        val src = if (dataType == DataType.PACKAGE_APK)
            getPackageSourceDir(packageName = p.packageName, userId = p.userId)
        else
            getDataSrc(srcDir = getDataSrcDir(dataType = dataType, userId = p.userId), packageName = p.packageName)
        if (rootService.exists(src)) rootService.calculateSize(src)
        else 0
    }

    suspend fun upsert(item: PackageEntity) = packageDao.upsert(item)

    suspend fun preserve(p: PackageEntity) {
        val preserveId = DateUtil.getTimestamp()
        val src = "${archivesPackagesDir}/${p.archivesPreserveRelativeDir}"
        val dst = "${archivesPackagesDir}/${p.archivesRelativeDir}/${preserveId}"
        rootService.renameTo(src, dst)
        upsert(p.copy(indexInfo = p.indexInfo.copy(preserveId = preserveId)))
    }

    suspend fun delete(p: PackageEntity) {
        val src = "${archivesPackagesDir}/${p.archivesPreserveRelativeDir}"
        rootService.deleteRecursively(src)
        packageDao.delete(p)
    }
}
