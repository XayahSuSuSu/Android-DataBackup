package com.xayah.core.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.UserHandle
import com.xayah.core.data.util.srcDir
import com.xayah.core.database.dao.PackageDao
import com.xayah.core.datastore.readBackupFilterFlagIndex
import com.xayah.core.datastore.readCheckKeystore
import com.xayah.core.datastore.readCompressionType
import com.xayah.core.datastore.readCustomSUFile
import com.xayah.core.datastore.readIconUpdateTime
import com.xayah.core.datastore.readLoadSystemApps
import com.xayah.core.datastore.readLoadedIconMD5
import com.xayah.core.datastore.readReloadDumpApk
import com.xayah.core.datastore.readRestoreFilterFlagIndex
import com.xayah.core.datastore.readRestoreUserIdIndex
import com.xayah.core.datastore.saveIconUpdateTime
import com.xayah.core.datastore.saveLoadedIconMD5
import com.xayah.core.model.CompressionType
import com.xayah.core.model.DataState
import com.xayah.core.model.DataType
import com.xayah.core.model.DefaultPreserveId
import com.xayah.core.model.OpType
import com.xayah.core.model.SortType
import com.xayah.core.model.database.CloudEntity
import com.xayah.core.model.database.MediaEntity
import com.xayah.core.model.database.MediaExtraInfo
import com.xayah.core.model.database.MediaIndexInfo
import com.xayah.core.model.database.MediaInfo
import com.xayah.core.model.database.PackageDataStates
import com.xayah.core.model.database.PackageDataStats
import com.xayah.core.model.database.PackageEntity
import com.xayah.core.model.database.PackageExtraInfo
import com.xayah.core.model.database.PackageIndexInfo
import com.xayah.core.model.database.PackageInfo
import com.xayah.core.model.database.PackageStorageStats
import com.xayah.core.model.util.suffixOf
import com.xayah.core.network.client.CloudClient
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.util.ConfigsPackageRestoreName
import com.xayah.core.util.DateUtil
import com.xayah.core.util.IconRelativeDir
import com.xayah.core.util.LogUtil
import com.xayah.core.util.PathUtil
import com.xayah.core.util.PermissionUtil
import com.xayah.core.util.command.BaseUtil
import com.xayah.core.util.command.PackageUtil
import com.xayah.core.util.command.Tar
import com.xayah.core.util.filesDir
import com.xayah.core.util.iconDir
import com.xayah.core.util.localBackupSaveDir
import com.xayah.core.util.withLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import java.text.Collator
import javax.inject.Inject

class PackageRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val rootService: RemoteRootService,
    private val cloudRepository: CloudRepository,
    private val packageDao: PackageDao,
    private val pathUtil: PathUtil,
) {
    companion object {
        private const val TAG = "PackageRepository"
    }

    private fun log(onMsg: () -> String): String = run {
        val msg = onMsg()
        LogUtil.log { TAG to msg }
        msg
    }

    fun getPackage(packageName: String, opType: OpType, userId: Int, preserveId: Long) = packageDao.queryFlow(packageName, opType, userId, preserveId).distinctUntilChanged()
    suspend fun getPackage(packageName: String, opType: OpType, userId: Int) = packageDao.query(packageName, opType, userId)
    fun queryPackagesFlow(opType: OpType, existed: Boolean, blocked: Boolean) = packageDao.queryPackagesFlow(opType, blocked).distinctUntilChanged()
    fun queryPackagesFlow(opType: OpType, blocked: Boolean) = packageDao.queryPackagesFlow(opType, blocked).distinctUntilChanged()
    fun queryPackagesFlow(opType: OpType, cloud: String, backupDir: String) = packageDao.queryPackagesFlow(opType, cloud, backupDir).distinctUntilChanged()
    suspend fun queryUserIds(opType: OpType) = packageDao.queryUserIds(opType)
    suspend fun queryPackages(opType: OpType, cloud: String, backupDir: String) = packageDao.queryPackages(opType, cloud, backupDir)
    suspend fun queryActivated(opType: OpType) = packageDao.queryActivated(opType)
    suspend fun queryActivated(opType: OpType, cloud: String, backupDir: String) = packageDao.queryActivated(opType, cloud, backupDir)
    suspend fun setBlocked(id: Long, blocked: Boolean) = packageDao.setBlocked(id, blocked)
    suspend fun clearBlocked() = packageDao.clearBlocked()
    private val localBackupSaveDir get() = context.localBackupSaveDir()
    val backupAppsDir get() = pathUtil.getLocalBackupAppsDir()

    fun getArchiveDst(dstDir: String, dataType: DataType, ct: CompressionType) = "${dstDir}/${dataType.type}.${ct.suffix}"

    private suspend fun getPackage(packageName: String, opType: OpType, userId: Int, preserveId: Long, cloud: String, backupDir: String) =
        packageDao.query(packageName, opType, userId, preserveId, cloud, backupDir)

    suspend fun getPackage(packageName: String, opType: OpType, userId: Int, preserveId: Long, ct: CompressionType, cloud: String, backupDir: String) =
        packageDao.query(packageName, opType, userId, preserveId, ct, cloud, backupDir)

    private suspend fun queryPackages(packageName: String, opType: OpType, userId: Int, cloud: String, backupDir: String) =
        packageDao.query(packageName, opType, userId, cloud, backupDir)

    suspend fun updateLocalPackageDataSize(packageName: String, opType: OpType, userId: Int, preserveId: Long) {
        getPackage(packageName, opType, userId, preserveId, "", "").also {
//            if (it != null) {
//                it.extraInfo.existed = rootService.queryInstalled(packageName = packageName, userId = userId)
//                if (it.extraInfo.existed) {
//                    it.displayStats.apkBytes = calculateDataSize(it, DataType.PACKAGE_APK)
//                    it.displayStats.userBytes = calculateDataSize(it, DataType.PACKAGE_USER)
//                    it.displayStats.userDeBytes = calculateDataSize(it, DataType.PACKAGE_USER_DE)
//                    it.displayStats.dataBytes = calculateDataSize(it, DataType.PACKAGE_DATA)
//                    it.displayStats.obbBytes = calculateDataSize(it, DataType.PACKAGE_OBB)
//                    it.displayStats.mediaBytes = calculateDataSize(it, DataType.PACKAGE_MEDIA)
//                }
//                upsert(it)
//            }
        }
    }

    suspend fun updateLocalPackageArchivesSize(packageName: String, opType: OpType, userId: Int) {
        queryPackages(packageName, opType, userId, "", localBackupSaveDir).onEach {
            it.displayStats.apkBytes = calculateLocalArchiveSize(it, DataType.PACKAGE_APK)
            it.displayStats.userBytes = calculateLocalArchiveSize(it, DataType.PACKAGE_USER)
            it.displayStats.userDeBytes = calculateLocalArchiveSize(it, DataType.PACKAGE_USER_DE)
            it.displayStats.dataBytes = calculateLocalArchiveSize(it, DataType.PACKAGE_DATA)
            it.displayStats.obbBytes = calculateLocalArchiveSize(it, DataType.PACKAGE_OBB)
            it.displayStats.mediaBytes = calculateLocalArchiveSize(it, DataType.PACKAGE_MEDIA)
            upsert(it)
        }
    }

    suspend fun updateCloudPackageArchivesSize(packageName: String, opType: OpType, userId: Int, client: CloudClient, entity: CloudEntity) {
        val remote = entity.remote
        val remoteArchivesPackagesDir = pathUtil.getCloudRemoteAppsDir(remote)
        queryPackages(packageName, opType, userId, entity.name, entity.remote).onEach {
            runCatching { it.displayStats.apkBytes = calculateCloudArchiveSize(client, it, DataType.PACKAGE_APK, remoteArchivesPackagesDir) }.onFailure(rootService.onFailure)
            runCatching { it.displayStats.userBytes = calculateCloudArchiveSize(client, it, DataType.PACKAGE_USER, remoteArchivesPackagesDir) }.onFailure(rootService.onFailure)
            runCatching { it.displayStats.userDeBytes = calculateCloudArchiveSize(client, it, DataType.PACKAGE_USER_DE, remoteArchivesPackagesDir) }.onFailure(rootService.onFailure)
            runCatching { it.displayStats.dataBytes = calculateCloudArchiveSize(client, it, DataType.PACKAGE_DATA, remoteArchivesPackagesDir) }.onFailure(rootService.onFailure)
            runCatching { it.displayStats.obbBytes = calculateCloudArchiveSize(client, it, DataType.PACKAGE_OBB, remoteArchivesPackagesDir) }.onFailure(rootService.onFailure)
            runCatching { it.displayStats.mediaBytes = calculateCloudArchiveSize(client, it, DataType.PACKAGE_MEDIA, remoteArchivesPackagesDir) }.onFailure(rootService.onFailure)
            upsert(it)
        }
    }

    private suspend fun getInstalledPackages(userId: Int) = rootService.getInstalledPackagesAsUser(PackageManager.GET_PERMISSIONS, userId).filter {
        // Filter itself
        it.packageName != context.packageName
    }

    fun getKeyPredicateNew(key: String): (PackageEntity) -> Boolean = { p ->
        p.packageInfo.label.lowercase().contains(key.lowercase()) || p.packageName.lowercase().contains(key.lowercase())
    }

    fun getFlagPredicateNew(index: Int): (PackageEntity) -> Boolean = { p ->
        when (index) {
            1 -> p.isSystemApp.not()
            2 -> p.isSystemApp
            else -> true
        }
    }

    fun getShowSystemAppsPredicate(value: Boolean): (PackageEntity) -> Boolean = { p ->
        value || p.isSystemApp.not()
    }

    fun getUserIdPredicateNew(indexList: List<Int>, userIdList: List<Int>): (PackageEntity) -> Boolean = { p ->
        runCatching { p.userId in indexList.map { userIdList[it] } }.getOrDefault(p.userId == 0)
    }

    fun getUserIdPredicateNew(userId: Int?): (PackageEntity) -> Boolean = { p ->
        runCatching { p.userId == userId }.getOrDefault(false)
    }

    suspend fun filterBackup(packages: List<PackageEntity>) = packages.filter(getFlagPredicateNew(index = context.readBackupFilterFlagIndex().first()))

    suspend fun filterRestore(packages: List<PackageEntity>) = packages.filter(getFlagPredicateNew(index = context.readRestoreFilterFlagIndex().first()))
        .filter(getUserIdPredicateNew(indexList = context.readRestoreUserIdIndex().first(), userIdList = queryUserIds(OpType.RESTORE)))

    private fun sortByInstallTimeNew(type: SortType): Comparator<PackageEntity> = when (type) {
        SortType.ASCENDING -> {
            compareBy { p -> p.packageInfo.firstInstallTime }
        }

        SortType.DESCENDING -> {
            compareByDescending { p -> p.packageInfo.firstInstallTime }
        }
    }

    private fun sortByDataSizeNew(type: SortType): Comparator<PackageEntity> = when (type) {
        SortType.ASCENDING -> {
            compareBy { p -> p.storageStatsBytes }
        }

        SortType.DESCENDING -> {
            compareByDescending { p -> p.storageStatsBytes }
        }
    }

    private fun sortByAlphabetNew(type: SortType): Comparator<PackageEntity> = Comparator { p1, p2 ->
        if (p1 != null && p2 != null) {
            when (type) {
                SortType.ASCENDING -> {
                    Collator.getInstance().let { collator ->
                        collator.getCollationKey(p1.packageInfo.label).compareTo(collator.getCollationKey(p2.packageInfo.label))
                    }
                }

                SortType.DESCENDING -> {
                    Collator.getInstance().let { collator ->
                        collator.getCollationKey(p2.packageInfo.label).compareTo(collator.getCollationKey(p1.packageInfo.label))
                    }
                }
            }
        } else {
            0
        }
    }

    fun getSortComparatorNew(sortIndex: Int, sortType: SortType): Comparator<in PackageEntity> = when (sortIndex) {
        1 -> sortByInstallTimeNew(sortType)
        2 -> sortByDataSizeNew(sortType)
        else -> sortByAlphabetNew(sortType)
    }

    private suspend fun handlePackage(
        pm: PackageManager,
        info: android.content.pm.PackageInfo,
        checkKeystore: Boolean, userId: Int,
        userHandle: UserHandle?,
        hasPassedOneDay: Boolean
    ): PackageEntity {
        val permissions = PermissionUtil.getPermission(packageManager = pm, packageInfo = info)
        val uid = info.applicationInfo.uid
        val hasKeystore = if (checkKeystore) PackageUtil.hasKeystore(context.readCustomSUFile().first(), uid) else false
        val ssaid = rootService.getPackageSsaidAsUser(packageName = info.packageName, uid = uid, userId = userId)
        val iconPath = pathUtil.getPackageIconPath(info.packageName, false)
        val iconExists = rootService.exists(iconPath)
        if (iconExists.not() || (iconExists && hasPassedOneDay)) {
            runCatching {
                val icon = info.applicationInfo.loadIcon(pm)
                BaseUtil.writeIcon(icon = icon, dst = iconPath)
            }.withLog()
        }
        val packageInfo = PackageInfo(
            label = info.applicationInfo.loadLabel(pm).toString(),
            versionName = info.versionName ?: "",
            versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.longVersionCode
            } else {
                info.versionCode.toLong()
            },
            flags = info.applicationInfo.flags,
            firstInstallTime = info.firstInstallTime,
        )
        val extraInfo = PackageExtraInfo(
            uid = uid,
            hasKeystore = hasKeystore,
            permissions = permissions,
            ssaid = ssaid,
            blocked = false,
            activated = false,
            firstUpdated = true,
            enabled = true,
        )
        val indexInfo = PackageIndexInfo(
            opType = OpType.BACKUP,
            packageName = info.packageName,
            userId = userId,
            compressionType = context.readCompressionType().first(),
            preserveId = DefaultPreserveId,
            cloud = "",
            backupDir = ""
        )
        val packageEntity =
            getPackage(packageName = info.packageName, opType = OpType.BACKUP, userId = userId, preserveId = DefaultPreserveId, cloud = "", backupDir = "")
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
        }
        if (userHandle != null) {
            rootService.queryStatsForPackage(info, userHandle).also { stats ->
                if (stats != null) {
                    packageEntity.apply {
                        this.storageStats.appBytes = stats.appBytes
                        this.storageStats.cacheBytes = stats.cacheBytes
                        this.storageStats.dataBytes = stats.dataBytes
                        this.storageStats.externalCacheBytes = stats.externalCacheBytes
                    }
                }
            }
        }
        return packageEntity
    }

    @SuppressLint("StringFormatInvalid")
    suspend fun refresh() = run {
        val checkKeystore = context.readCheckKeystore().first()
        val loadSystemApps = context.readLoadSystemApps().first()
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
                val isSystemApp = (info.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                if (loadSystemApps || isSystemApp.not()) {
                    val packageEntity = handlePackage(pm, info, checkKeystore, userId, userHandle, hasPassedOneDay)
                    upsert(packageEntity)
                }
            }
        }
    }

    @SuppressLint("StringFormatInvalid")
    suspend fun fastRefresh() = run {
        val checkKeystore = context.readCheckKeystore().first()
        val loadSystemApps = context.readLoadSystemApps().first()
        val pm = context.packageManager
        val userInfoList = rootService.getUsers()
        for (userInfo in userInfoList) {
            val userId = userInfo.id
            val userHandle = rootService.getUserHandle(userId)
            val installedPackages = getInstalledPackages(userId).filter {
                val isSystemApp = (it.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                loadSystemApps || isSystemApp.not()
            }.map { it.packageName!! }.toSet()
            val storedPackages = packageDao.queryPackageNamesByUserId(OpType.BACKUP, userId).toSet()
            val missingPackages = installedPackages.subtract(storedPackages)
            val outdatedPackages = storedPackages.subtract(installedPackages)
            log { "Missing packages: $missingPackages" }
            log { "Outdated packages: $outdatedPackages" }

            BaseUtil.mkdirs(context.iconDir())
            val iconUpdateTime = context.readIconUpdateTime().first()
            val now = DateUtil.getTimestamp()
            val hasPassedOneDay = DateUtil.getNumberOfDaysPassed(iconUpdateTime, now) >= 1
            if (hasPassedOneDay) context.saveIconUpdateTime(now)
            // For missing packages, we query info.
            missingPackages.forEach {
                // Update packages' info.
                val info = rootService.getPackageInfoAsUser(it, PackageManager.GET_PERMISSIONS, userId)
                if (info != null) {
                    val isSystemApp = (info.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    if (loadSystemApps || isSystemApp.not()) {
                        val packageEntity = handlePackage(pm, info, checkKeystore, userId, userHandle, hasPassedOneDay)
                        upsert(packageEntity)
                    }
                }
            }

            // For those uninstalled packages, we hide them.
            outdatedPackages.forEach {
//                packageDao.setExisted(OpType.BACKUP, it, userId, false)
            }
        }
    }

    suspend fun loadPackagesFromLocal() {
        val path = backupAppsDir
        val paths = rootService.walkFileTree(path)
        paths.forEach {
            val fileName = PathUtil.getFileName(it.pathString)
            if (fileName == ConfigsPackageRestoreName) {
                runCatching {
                    val stored = rootService.readJson<PackageEntity>(it.pathString).also { p ->
                        p?.id = 0
                        p?.extraInfo?.activated = false
                        p?.indexInfo?.cloud = ""
                        p?.indexInfo?.backupDir = localBackupSaveDir
                    }
                    if (stored != null) {
                        getPackage(
                            packageName = stored.packageName,
                            opType = stored.indexInfo.opType,
                            userId = stored.userId,
                            preserveId = stored.preserveId,
                            ct = stored.indexInfo.compressionType,
                            cloud = stored.indexInfo.cloud,
                            backupDir = localBackupSaveDir
                        ).also { p ->
                            if (p == null)
                                packageDao.upsert(stored)
                        }
                    }
                }
            }
        }
    }

    suspend fun loadPackagesFromCloud(cloud: String) = runCatching {
        cloudRepository.withClient(cloud) { client, entity ->
            val remote = entity.remote
            val src = pathUtil.getCloudRemoteAppsDir(remote)
            if (client.exists(src)) {
                val paths = client.walkFileTree(src)
                val tmpDir = pathUtil.getCloudTmpDir()
                paths.forEach {
                    val fileName = PathUtil.getFileName(it.pathString)
                    if (fileName == ConfigsPackageRestoreName) {
                        runCatching {
                            cloudRepository.download(client = client, src = it.pathString, dstDir = tmpDir) { path ->
                                val stored = rootService.readJson<PackageEntity>(path).also { p ->
                                    p?.id = 0
                                    p?.extraInfo?.activated = false
                                    p?.indexInfo?.cloud = entity.name
                                    p?.indexInfo?.backupDir = remote
                                }
                                if (stored != null) {
                                    getPackage(
                                        packageName = stored.packageName,
                                        opType = stored.indexInfo.opType,
                                        userId = stored.userId,
                                        preserveId = stored.preserveId,
                                        ct = stored.indexInfo.compressionType,
                                        cloud = entity.name,
                                        backupDir = remote
                                    ).also { p ->
                                        if (p == null)
                                            packageDao.upsert(stored)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }.onFailure(rootService.onFailure)

    suspend fun loadIconsFromLocal() {
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

    suspend fun loadIconsFromCloud(cloud: String) = runCatching {
        cloudRepository.withClient(cloud) { client, entity ->
            val archivePath = "${pathUtil.getCloudRemoteConfigsDir(entity.remote)}/$IconRelativeDir.${CompressionType.TAR.suffix}"
            if (client.exists(archivePath)) {
                val tmpDir = pathUtil.getCloudTmpDir()
                cloudRepository.download(client = client, src = archivePath, dstDir = tmpDir) { path ->
                    val loadedIconMD5 = context.readLoadedIconMD5().first()
                    val iconMD5 = rootService.calculateMD5(path) ?: ""
                    if (loadedIconMD5 != iconMD5) {
                        Tar.decompress(src = path, dst = context.filesDir(), extra = CompressionType.TAR.decompressPara)
                        PathUtil.setFilesDirSELinux(context)
                        context.saveLoadedIconMD5(iconMD5)
                    }
                }
            }
        }
    }.onFailure(rootService.onFailure)

    private suspend fun getPackageSourceDir(packageName: String, userId: Int) = rootService.getPackageSourceDir(packageName, userId).let { list ->
        if (list.isNotEmpty()) PathUtil.getParentPath(list[0]) else ""
    }

    fun getDataSrcDir(dataType: DataType, userId: Int) = dataType.srcDir(userId)

    fun getDataSrc(srcDir: String, packageName: String) = "$srcDir/$packageName"

    private suspend fun calculateLocalArchiveSize(p: PackageEntity, dataType: DataType) = rootService.calculateSize(
        getArchiveDst("${backupAppsDir}/${p.archivesRelativeDir}", dataType, p.indexInfo.compressionType)
    )

    private fun calculateCloudArchiveSize(client: CloudClient, p: PackageEntity, dataType: DataType, archivesPackagesDir: String) = run {
        val src = getArchiveDst("${archivesPackagesDir}/${p.archivesRelativeDir}", dataType, p.indexInfo.compressionType)
        if (client.exists(src)) client.size(src)
        else 0
    }

    private suspend fun calculateDataSize(p: PackageEntity, dataType: DataType) = run {
        val src = if (dataType == DataType.PACKAGE_APK)
            getPackageSourceDir(packageName = p.packageName, userId = p.userId)
        else
            getDataSrc(srcDir = getDataSrcDir(dataType = dataType, userId = p.userId), packageName = p.packageName)
        if (rootService.exists(src)) rootService.calculateSize(src)
        else 0
    }

    suspend fun upsert(item: PackageEntity) = packageDao.upsert(item)
    suspend fun upsert(items: List<PackageEntity>) = packageDao.upsert(items)

    suspend fun preserve(p: PackageEntity) {
        val pkgEntity = p.copy(id = 0, indexInfo = p.indexInfo.copy(preserveId = DateUtil.getTimestamp()))
        val appsDir = pathUtil.getLocalBackupAppsDir()
        val isSuccess = if (pkgEntity.indexInfo.cloud.isEmpty()) {
            val src = "${appsDir}/${p.archivesRelativeDir}"
            val dst = "${appsDir}/${pkgEntity.archivesRelativeDir}"
            rootService.writeJson(data = pkgEntity, dst = PathUtil.getPackageRestoreConfigDst(src))
            rootService.renameTo(src, dst)
        } else {
            runCatching {
                cloudRepository.withClient(pkgEntity.indexInfo.cloud) { client, entity ->
                    val remote = entity.remote
                    val remoteArchivesPackagesDir = pathUtil.getCloudRemoteAppsDir(remote)
                    val src = "${remoteArchivesPackagesDir}/${p.archivesRelativeDir}"
                    val dst = "${remoteArchivesPackagesDir}/${pkgEntity.archivesRelativeDir}"
                    val tmpDir = pathUtil.getCloudTmpDir()
                    val tmpJsonPath = PathUtil.getPackageRestoreConfigDst(tmpDir)
                    rootService.writeJson(data = pkgEntity, dst = tmpJsonPath)
                    cloudRepository.upload(client = client, src = tmpJsonPath, dstDir = src)
                    rootService.deleteRecursively(tmpDir)
                    client.renameTo(src, dst)
                }
            }.onFailure(rootService.onFailure).isSuccess
        }
        if (isSuccess) {
            packageDao.delete(p.id)
            upsert(pkgEntity)
        }
    }

    suspend fun delete(p: PackageEntity) {
        val appsDir = pathUtil.getLocalBackupAppsDir()
        val isSuccess = if (p.indexInfo.cloud.isEmpty()) {
            val src = "${appsDir}/${p.archivesRelativeDir}"
            rootService.deleteRecursively(src)
        } else {
            runCatching {
                cloudRepository.withClient(p.indexInfo.cloud) { client, entity ->
                    val remote = entity.remote
                    val remoteArchivesPackagesDir = pathUtil.getCloudRemoteAppsDir(remote)
                    val src = "${remoteArchivesPackagesDir}/${p.archivesRelativeDir}"
                    if (client.exists(src)) client.deleteRecursively(src)
                }
            }.onFailure(rootService.onFailure).isSuccess
        }

        if (isSuccess) packageDao.delete(p.id)
    }

    /**
     * Modify directory structure from local.
     *        /.../DataBackup/backup/$userId/data/$packageName/$coverOrTimestamp/${dataType}.tar.*
     * --->   /.../DataBackup/apps/$packageName/user_$userId@$timestamp/${dataType}.tar.*
     */
    suspend fun modifyAppsStructureFromLocal10x(onMsgUpdate: suspend (String) -> Unit) {
        onMsgUpdate(log { "Modifying directory structure..." })
        val backupDir = "${context.localBackupSaveDir()}/backup"
        val dstDir = pathUtil.getLocalBackupAppsDir()
        var serialTimestamp: Long
        BaseUtil.mkdirs(context.iconDir())

        rootService.listFilePaths(backupDir).forEach { userPath ->
            // Timestamp serial for "Cover".
            serialTimestamp = DateUtil.getTimestamp()
            val userId = userPath.split("/").lastOrNull()?.toIntOrNull() ?: 0
            val packagesDir = "${userPath}/data"
            rootService.listFilePaths(packagesDir).forEach { pkg ->
                rootService.listFilePaths(pkg).forEach { path ->
                    val list = path.split("/")
                    runCatching {
                        // Skip icon.png
                        if (list.last() != "icon.png") {
                            val pathListSize = list.size
                            val packageName = list[pathListSize - 2]
                            val timestampName = list[pathListSize - 1]

                            /**
                             * In old versions, the timestamp may be named as "Cover" or something else,
                             * we need to convert it to number.
                             */
                            val timestamp = runCatching { timestampName.toLong() }.getOrElse { serialTimestamp }
                            val dst = "${dstDir}/${packageName}/user_${userId}@${timestamp}"
                            onMsgUpdate(log { "Trying to move $path to $dst." })
                            rootService.mkdirs(path = PathUtil.getParentPath(dst))
                            rootService.renameTo(src = path, dst = dst)
                        }
                    }.withLog()
                }
            }
        }
    }

    /**
     * Modify directory structure from local.
     *        /.../DataBackup/backup/$userId/media/$name/$coverOrTimestamp/${dataType}.tar.*
     * --->   /.../DataBackup/files/$name@$timestamp/${dataType}.tar.*
     */
    suspend fun modifyFilesStructureFromLocal10x(onMsgUpdate: suspend (String) -> Unit) {
        onMsgUpdate(log { "Modifying directory structure..." })
        val backupDir = "${context.localBackupSaveDir()}/backup"
        val dstDir = pathUtil.getLocalBackupFilesDir()
        var serialTimestamp: Long

        rootService.listFilePaths(backupDir).forEach { userPath ->
            // Timestamp serial for "Cover".
            serialTimestamp = DateUtil.getTimestamp()
            val userId = userPath.split("/").lastOrNull()?.toIntOrNull() ?: 0
            val mediumDir = "${userPath}/media"
            rootService.listFilePaths(mediumDir).forEach { media ->
                rootService.listFilePaths(media).forEach { path ->
                    val list = path.split("/")
                    runCatching {
                        val pathListSize = list.size
                        val name = list[pathListSize - 2]
                        val timestampName = list[pathListSize - 1]

                        /**
                         * In old versions, the timestamp may be named as "Cover" or something else,
                         * we need to convert it to number.
                         */
                        val timestamp = runCatching { timestampName.toLong() }.getOrElse { serialTimestamp }
                        val dst = "${dstDir}/${name}@${timestamp}"
                        onMsgUpdate(log { "Trying to move $path to $dst." })
                        rootService.mkdirs(path = PathUtil.getParentPath(dst))
                        rootService.renameTo(src = path, dst = dst)
                    }.withLog()
                }
            }
        }
    }

    /**
     * Modify directory structure from cloud.
     *        /.../DataBackup/backup/$userId/data/$packageName/$coverOrTimestamp/${dataType}.tar.*
     * --->   /.../DataBackup/apps/$packageName/user_$userId@$timestamp/${dataType}.tar.*
     */
    suspend fun modifyAppsStructureFromCloud10x(cloud: String, onMsgUpdate: suspend (String) -> Unit) {
        runCatching {
            cloudRepository.withClient(cloud) { client, entity ->
                onMsgUpdate(log { "Modifying directory structure..." })
                val backupDir = "${entity.remote}/backup"
                val dstDir = pathUtil.getCloudRemoteAppsDir(entity.remote)
                var serialTimestamp: Long
                BaseUtil.mkdirs(context.iconDir())

                client.listFiles(backupDir).directories.forEach { fileParcelable ->
                    val userPath = "${backupDir}/${fileParcelable.name}"
                    // Timestamp serial for "Cover".
                    serialTimestamp = DateUtil.getTimestamp()
                    val userId = userPath.split("/").lastOrNull()?.toIntOrNull() ?: 0
                    val packagesDir = "${userPath}/data"
                    client.listFiles(packagesDir).directories.forEach { pkg ->
                        val pkgPath = "${packagesDir}/${pkg.name}"
                        client.listFiles(pkgPath).directories.forEach { pathParcelable ->
                            val path = "${pkgPath}/${pathParcelable.name}"
                            val list = path.split("/")
                            runCatching {
                                // Skip icon.png
                                if (list.last() != "icon.png") {
                                    val pathListSize = list.size
                                    val packageName = list[pathListSize - 2]
                                    val timestampName = list[pathListSize - 1]

                                    /**
                                     * In old versions, the timestamp may be named as "Cover" or something else,
                                     * we need to convert it to number.
                                     */
                                    val timestamp = runCatching { timestampName.toLong() }.getOrElse { serialTimestamp }
                                    val dst = "${dstDir}/${packageName}/user_${userId}@${timestamp}"
                                    onMsgUpdate(log { "Trying to move $path to $dst." })
                                    client.mkdirRecursively(dst = PathUtil.getParentPath(dst))
                                    client.renameTo(src = path, dst = dst)
                                }
                            }.withLog()
                        }
                    }
                }
            }
        }.onFailure(rootService.onFailure)
    }

    /**
     * Modify directory structure from cloud.
     *        /.../DataBackup/backup/$userId/data/$packageName/$coverOrTimestamp/${dataType}.tar.*
     * --->   /.../DataBackup/apps/$packageName/user_$userId@$timestamp/${dataType}.tar.*
     */
    suspend fun modifyFilesStructureFromCloud10x(cloud: String, onMsgUpdate: suspend (String) -> Unit) {
        runCatching {
            cloudRepository.withClient(cloud) { client, entity ->
                onMsgUpdate(log { "Modifying directory structure..." })
                val backupDir = "${entity.remote}/backup"
                val dstDir = pathUtil.getCloudRemoteFilesDir(entity.remote)
                var serialTimestamp: Long

                client.listFiles(backupDir).directories.forEach { fileParcelable ->
                    val userPath = "${backupDir}/${fileParcelable.name}"
                    // Timestamp serial for "Cover".
                    serialTimestamp = DateUtil.getTimestamp()
                    val userId = userPath.split("/").lastOrNull()?.toIntOrNull() ?: 0
                    val mediumDir = "${userPath}/media"
                    client.listFiles(mediumDir).directories.forEach { media ->
                        val mediaPath = "${mediumDir}/${media.name}"
                        client.listFiles(mediaPath).directories.forEach { pathParcelable ->
                            val path = "${mediaPath}/${pathParcelable.name}"
                            val list = path.split("/")
                            runCatching {
                                val pathListSize = list.size
                                val name = list[pathListSize - 2]
                                val timestampName = list[pathListSize - 1]

                                /**
                                 * In old versions, the timestamp may be named as "Cover" or something else,
                                 * we need to convert it to number.
                                 */
                                val timestamp = runCatching { timestampName.toLong() }.getOrElse { serialTimestamp }
                                val dst = "${dstDir}/${name}@${timestamp}"
                                onMsgUpdate(log { "Trying to move $path to $dst." })
                                client.mkdirRecursively(dst = PathUtil.getParentPath(dst))
                                client.renameTo(src = path, dst = dst)
                            }.withLog()
                        }
                    }
                }
            }
        }.onFailure(rootService.onFailure)
    }

    /**
     * Modify directory structure from local.
     *        /.../DataBackup/archives/packages/$packageName/$timestamp/${dataType}.tar.*
     * --->   /.../DataBackup/apps/$packageName/user_$userId@$timestamp/${dataType}.tar.*
     */
    suspend fun modifyAppsStructureFromLocal11x(onMsgUpdate: suspend (String) -> Unit) {
        onMsgUpdate(log { "Modifying directory structure..." })
        val packagesDir = "${context.localBackupSaveDir()}/archives/packages"
        val dstDir = pathUtil.getLocalBackupAppsDir()
        val serialTimestamp: Long = DateUtil.getTimestamp()
        val userId = 0
        BaseUtil.mkdirs(context.iconDir())

        rootService.listFilePaths(packagesDir).forEach { pkgPath ->
            rootService.listFilePaths(pkgPath).forEach { path ->
                val list = path.split("/")
                runCatching {
                    val pathListSize = list.size
                    val packageName = list[pathListSize - 2]
                    val timestampName = list[pathListSize - 1]
                    val timestamp = runCatching { timestampName.toLong() }.getOrElse { serialTimestamp }
                    val dst = "${dstDir}/${packageName}/user_${userId}@${timestamp}"
                    onMsgUpdate(log { "Trying to move $path to $dst." })
                    rootService.mkdirs(path = PathUtil.getParentPath(dst))
                    rootService.renameTo(src = path, dst = dst)
                }.withLog()
            }
        }
    }

    /**
     * Modify directory structure from local.
     *        /.../DataBackup/archives/medium/$name/$timestamp/${dataType}.tar.*
     * --->   /.../DataBackup/files/$name@$timestamp/${dataType}.tar.*
     */
    suspend fun modifyFilesStructureFromLocal11x(onMsgUpdate: suspend (String) -> Unit) {
        onMsgUpdate(log { "Modifying directory structure..." })
        val mediumDir = "${context.localBackupSaveDir()}/archives/medium"
        val dstDir = pathUtil.getLocalBackupFilesDir()
        val serialTimestamp: Long = DateUtil.getTimestamp()

        rootService.listFilePaths(mediumDir).forEach { mediaPath ->
            rootService.listFilePaths(mediaPath).forEach { path ->
                val list = path.split("/")
                runCatching {
                    val pathListSize = list.size
                    val name = list[pathListSize - 2]
                    val timestampName = list[pathListSize - 1]
                    val timestamp = runCatching { timestampName.toLong() }.getOrElse { serialTimestamp }
                    val dst = "${dstDir}/${name}@${timestamp}"
                    onMsgUpdate(log { "Trying to move $path to $dst." })
                    rootService.mkdirs(path = PathUtil.getParentPath(dst))
                    rootService.renameTo(src = path, dst = dst)
                }.withLog()
            }
        }
    }

    /**
     * Modify directory structure from cloud.
     *        /.../DataBackup/archives/packages/$packageName/$timestamp/${dataType}.tar.*
     * --->   /.../DataBackup/apps/$packageName/user_$userId@$timestamp/${dataType}.tar.*
     */
    suspend fun modifyAppsStructureFromCloud11x(cloud: String, onMsgUpdate: suspend (String) -> Unit) {
        runCatching {
            cloudRepository.withClient(cloud) { client, entity ->
                onMsgUpdate(log { "Modifying directory structure..." })
                val packagesDir = "${entity.remote}/archives/packages"
                val dstDir = pathUtil.getCloudRemoteAppsDir(entity.remote)
                val serialTimestamp: Long = DateUtil.getTimestamp()
                val userId = 0
                BaseUtil.mkdirs(context.iconDir())

                client.listFiles(packagesDir).directories.forEach { pkg ->
                    val pkgPath = "${packagesDir}/${pkg.name}"
                    client.listFiles(pkgPath).directories.forEach { pathParcelable ->
                        val path = "${pkgPath}/${pathParcelable.name}"
                        val list = path.split("/")
                        runCatching {
                            val pathListSize = list.size
                            val packageName = list[pathListSize - 2]
                            val timestampName = list[pathListSize - 1]
                            val timestamp = runCatching { timestampName.toLong() }.getOrElse { serialTimestamp }
                            val dst = "${dstDir}/${packageName}/user_${userId}@${timestamp}"
                            onMsgUpdate(log { "Trying to move $path to $dst." })
                            client.mkdirRecursively(dst = PathUtil.getParentPath(dst))
                            client.renameTo(src = path, dst = dst)
                        }.withLog()
                    }
                }
            }
        }.onFailure(rootService.onFailure)
    }

    /**
     * Modify directory structure from cloud.
     *        /.../DataBackup/archives/medium/$name/$timestamp/${dataType}.tar.*
     * --->   /.../DataBackup/files/$name@$timestamp/${dataType}.tar.*
     */
    suspend fun modifyFilesStructureFromCloud11x(cloud: String, onMsgUpdate: suspend (String) -> Unit) {
        runCatching {
            cloudRepository.withClient(cloud) { client, entity ->
                onMsgUpdate(log { "Modifying directory structure..." })
                val mediumDir = "${entity.remote}/archives/medium"
                val dstDir = pathUtil.getCloudRemoteFilesDir(entity.remote)
                val serialTimestamp: Long = DateUtil.getTimestamp()

                client.listFiles(mediumDir).directories.forEach { media ->
                    val mediaPath = "${mediumDir}/${media.name}"
                    client.listFiles(mediaPath).directories.forEach { pathParcelable ->
                        val path = "${mediaPath}/${pathParcelable.name}"
                        val list = path.split("/")
                        runCatching {
                            val pathListSize = list.size
                            val name = list[pathListSize - 2]
                            val timestampName = list[pathListSize - 1]
                            val timestamp = runCatching { timestampName.toLong() }.getOrElse { serialTimestamp }
                            val dst = "${dstDir}/${name}@${timestamp}"
                            onMsgUpdate(log { "Trying to move $path to $dst." })
                            client.mkdirRecursively(dst = PathUtil.getParentPath(dst))
                            client.renameTo(src = path, dst = dst)
                        }.withLog()
                    }
                }
            }
        }.onFailure(rootService.onFailure)
    }

    /**
     * Reload directory structure from local.
     *        /.../DataBackup/apps/$packageName/user_$userId@$timestamp/${dataType}.tar.*
     */
    suspend fun reloadAppsFromLocal12x(onMsgUpdate: suspend (String) -> Unit) {
        onMsgUpdate(log { "Reloading..." })
        val packageManager = context.packageManager
        val appsDir = pathUtil.getLocalBackupAppsDir()
        val pathList = rootService.walkFileTree(appsDir)
        val typedPathSet = mutableSetOf<String>()
        BaseUtil.mkdirs(context.iconDir())
        log { "Total paths count: ${pathList.size}" }

        // Classify the paths
        pathList.forEach { path ->
            log { "Classifying: ${path.pathString}" }
            runCatching {
                val pathListSize = path.pathList.size
                val packageName = path.pathList[pathListSize - 3]
                val preserveId: Long
                val userId: Int
                if (path.pathList[pathListSize - 2].contains("@")) {
                    val userIdWithPreserveId = path.pathList[pathListSize - 2].split("@")
                    preserveId = userIdWithPreserveId.lastOrNull()?.toLongOrNull() ?: 0
                    userId = userIdWithPreserveId.first().split("_").lastOrNull()?.toIntOrNull() ?: 0
                } else {
                    // Main backup
                    preserveId = -1L
                    userId = path.pathList[pathListSize - 2].split("_").lastOrNull()?.toIntOrNull() ?: 0
                }
                typedPathSet.add("$packageName@$preserveId@$userId")
                log { "packageName: $packageName, preserveId: $preserveId, userId: $userId" }
            }.withLog()
        }

        log { "Apps count: ${typedPathSet.size}" }
        typedPathSet.forEach { typed ->
            runCatching {
                // For each $packageName@$preserveId@$userId
                val split = typed.split("@")
                val packageName = split[0]
                var preserveId = split[1].toLong()
                val mainBackup: Boolean = preserveId == -1L
                val userId = split[2].toInt()
                var dir = if (mainBackup) "${appsDir}/${packageName}/user_${userId}" else "${appsDir}/${packageName}/user_${userId}@${preserveId}"
                onMsgUpdate(log { "packageName: $packageName, preserveId: $preserveId, userId: $userId" })
                if (mainBackup.not() && preserveId < 1000000000000) {
                    // Diff from main backup (without preserveId)
                    val timestamp = DateUtil.getTimestamp()
                    val newDir = "${appsDir}/${packageName}/user_${userId}@${timestamp}"
                    onMsgUpdate(log { "$dir move to $newDir" })
                    rootService.mkdirs(path = PathUtil.getParentPath(newDir))
                    rootService.renameTo(dir, newDir)
                    preserveId = timestamp
                    dir = newDir
                }
                val jsonPath = PathUtil.getPackageRestoreConfigDst(dir)

                val dataStates = PackageDataStates(
                    apkState = DataState.Disabled,
                    userState = DataState.Disabled,
                    userDeState = DataState.Disabled,
                    dataState = DataState.Disabled,
                    obbState = DataState.Disabled,
                    mediaState = DataState.Disabled,
                    permissionState = DataState.Disabled,
                    ssaidState = DataState.Disabled
                )
                val packageEntity = runCatching {
                    val entity = rootService.readJson<PackageEntity>(jsonPath).also { p ->
                        p?.indexInfo?.packageName = packageName
                        p?.indexInfo?.userId = userId
                        p?.indexInfo?.preserveId = if (mainBackup) 0L else preserveId
                        p?.extraInfo?.activated = false
                        p?.indexInfo?.cloud = ""
                        p?.indexInfo?.backupDir = localBackupSaveDir
                        p?.dataStates = dataStates
                    }
                    onMsgUpdate(log { "Config is reloaded from json." })
                    entity
                }.getOrNull() ?: PackageEntity(
                    id = 0,
                    indexInfo = PackageIndexInfo(
                        opType = OpType.RESTORE,
                        packageName = packageName,
                        userId = userId,
                        compressionType = context.readCompressionType().first(),
                        preserveId = if (mainBackup) 0L else preserveId,
                        cloud = "",
                        backupDir = localBackupSaveDir
                    ),
                    packageInfo = PackageInfo(
                        label = "",
                        versionName = "",
                        versionCode = 0,
                        flags = 0,
                        firstInstallTime = 0,
                    ),
                    extraInfo = PackageExtraInfo(
                        uid = -1,
                        hasKeystore = false,
                        permissions = listOf(),
                        ssaid = "",
                        blocked = false,
                        activated = false,
                        firstUpdated = true,
                        enabled = true,
                    ),
                    dataStates = dataStates,
                    storageStats = PackageStorageStats(),
                    dataStats = PackageDataStats(),
                    displayStats = PackageDataStats()
                )

                val archives = rootService.walkFileTree(dir)

                archives.forEach { archivePath ->
                    // For each archive
                    log { "Package archive: ${archivePath.pathString}" }
                    runCatching {
                        when (archivePath.nameWithoutExtension) {
                            DataType.PACKAGE_APK.type -> {
                                onMsgUpdate(log { "Dumping apk..." })
                                val type = CompressionType.suffixOf(archivePath.extension)
                                if (type != null) {
                                    log { "Archive compression type: ${type.type}" }
                                    packageEntity.indexInfo.compressionType = type
                                    packageEntity.dataStates.apkState = DataState.Selected

                                    if (context.readReloadDumpApk().first()) {
                                        val tmpApkPath = pathUtil.getTmpApkPath(packageName = packageName)
                                        rootService.deleteRecursively(tmpApkPath)
                                        rootService.mkdirs(tmpApkPath)
                                        Tar.decompress(src = archivePath.pathString, dst = tmpApkPath, extra = type.decompressPara)
                                        rootService.listFilePaths(tmpApkPath).also { pathList ->
                                            if (pathList.isNotEmpty()) {
                                                rootService.getPackageArchiveInfo(pathList.first())?.apply {
                                                    packageEntity.packageInfo.label = applicationInfo.loadLabel(packageManager).toString()
                                                    packageEntity.packageInfo.versionName = versionName ?: ""
                                                    packageEntity.packageInfo.versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                                        longVersionCode
                                                    } else {
                                                        versionCode.toLong()
                                                    }
                                                    packageEntity.packageInfo.flags = applicationInfo.flags
                                                    val iconPath = pathUtil.getPackageIconPath(packageName, false)
                                                    val iconExists = rootService.exists(iconPath)
                                                    if (iconExists.not()) {
                                                        val icon = applicationInfo.loadIcon(packageManager)
                                                        BaseUtil.writeIcon(icon = icon, dst = iconPath)
                                                    }
                                                    log { "Icon and config updated." }
                                                }
                                            } else {
                                                log { "Archive is empty." }
                                            }
                                        }
                                        rootService.deleteRecursively(tmpApkPath)
                                    } else {
                                        log { "Skip dumping." }
                                    }
                                } else {
                                    log { "Failed to parse compression type: ${archivePath.extension}" }
                                }
                            }

                            DataType.PACKAGE_USER.type -> {
                                onMsgUpdate(log { "Dumping user..." })
                                val type = CompressionType.suffixOf(archivePath.extension)
                                if (type != null) {
                                    log { "Archive compression type: ${type.type}" }
                                    packageEntity.indexInfo.compressionType = type
                                    packageEntity.dataStates.userState = DataState.Selected
                                } else {
                                    log { "Failed to parse compression type: ${archivePath.extension}" }
                                }
                            }

                            DataType.PACKAGE_USER_DE.type, DataType.PACKAGE_DATA.type, DataType.PACKAGE_OBB.type, DataType.PACKAGE_MEDIA.type -> {
                                onMsgUpdate(log { "Dumping ${archivePath.nameWithoutExtension}..." })
                                when (archivePath.nameWithoutExtension) {
                                    DataType.PACKAGE_USER_DE.type -> {
                                        dataStates.userDeState = DataState.Selected
                                    }

                                    DataType.PACKAGE_DATA.type -> {
                                        dataStates.dataState = DataState.Selected
                                    }

                                    DataType.PACKAGE_OBB.type -> {
                                        dataStates.obbState = DataState.Selected
                                    }

                                    DataType.PACKAGE_MEDIA.type -> {
                                        dataStates.mediaState = DataState.Selected
                                    }

                                    else -> {}
                                }
                            }

                            else -> {}
                        }
                    }.withLog()
                }

                // Write config
                rootService.writeJson(data = packageEntity, dst = jsonPath)
            }.withLog()
        }
    }

    /**
     * Reload directory structure from local.
     *        /.../DataBackup/files/$name@$timestamp/${dataType}.tar.*
     */
    suspend fun reloadFilesFromLocal12x(onMsgUpdate: suspend (String) -> Unit) {
        onMsgUpdate(log { "Reloading..." })
        val filesDir = pathUtil.getLocalBackupFilesDir()
        val pathList = rootService.walkFileTree(filesDir)
        val typedPathSet = mutableSetOf<String>()
        log { "Total paths count: ${pathList.size}" }

        // Classify the paths
        pathList.forEach { path ->
            log { "Classifying: ${path.pathString}" }
            runCatching {
                val pathListSize = path.pathList.size
                val name: String
                val preserveId: Long
                if (path.pathList[pathListSize - 2].contains("@")) {
                    val nameWithPreserveId = path.pathList[pathListSize - 2].split("@")
                    preserveId = nameWithPreserveId.lastOrNull()?.toLongOrNull() ?: 0
                    name = nameWithPreserveId.first()
                } else {
                    // Main backup
                    preserveId = -1L
                    name = path.pathList[pathListSize - 2]
                }
                typedPathSet.add("$name@$preserveId")
                log { "name: $name, preserveId: $preserveId" }
            }.withLog()
        }

        log { "Files count: ${typedPathSet.size}" }
        typedPathSet.forEach { typed ->
            runCatching {
                // For each $packageName@$preserveId
                val split = typed.split("@")
                val name = split[0]
                var preserveId = split[1].toLong()
                val mainBackup: Boolean = preserveId == -1L
                var dir = if (mainBackup) "${filesDir}/${name}" else "${filesDir}/${name}@${preserveId}"
                onMsgUpdate(log { "name: $name, preserveId: $preserveId" })
                if (mainBackup.not() && preserveId < 1000000000000) {
                    // Diff from main backup (without preserveId)
                    val timestamp = DateUtil.getTimestamp()
                    val newDir = "${filesDir}/${name}@${timestamp}"
                    onMsgUpdate(log { "$dir move to $newDir" })
                    rootService.mkdirs(path = PathUtil.getParentPath(newDir))
                    rootService.renameTo(dir, newDir)
                    preserveId = timestamp
                    dir = newDir
                }
                val jsonPath = PathUtil.getMediaRestoreConfigDst(dir)

                val mediaEntity = runCatching {
                    val entity = rootService.readJson<MediaEntity>(jsonPath).also { m ->
                        m?.indexInfo?.name = name
                        m?.indexInfo?.preserveId = if (mainBackup) 0L else preserveId
                        m?.extraInfo?.existed = true
                        m?.extraInfo?.activated = false
                        m?.indexInfo?.cloud = ""
                        m?.indexInfo?.backupDir = localBackupSaveDir
                    }
                    onMsgUpdate(log { "Config is reloaded from json." })
                    entity
                }.getOrNull() ?: MediaEntity(
                    id = 0,
                    indexInfo = MediaIndexInfo(
                        opType = OpType.RESTORE,
                        name = name,
                        compressionType = CompressionType.TAR,
                        preserveId = if (mainBackup) 0L else preserveId,
                        cloud = "",
                        backupDir = localBackupSaveDir
                    ),
                    mediaInfo = MediaInfo(
                        path = "",
                        dataBytes = 0L,
                        displayBytes = 0L,
                    ),
                    extraInfo = MediaExtraInfo(
                        blocked = false,
                        activated = false,
                        existed = true,
                    ),
                )

                val archives = rootService.walkFileTree(dir)

                archives.forEach { archivePath ->
                    // For each archive
                    log { "Media archive: ${archivePath.pathString}" }
                    runCatching {
                        when (archivePath.nameWithoutExtension) {
                            DataType.MEDIA_MEDIA.type -> {
                                onMsgUpdate(log { "Dumping media..." })
                                val type = CompressionType.suffixOf(archivePath.extension)
                                if (type != null) {
                                    log { "Archive compression type: ${type.type}" }
                                    mediaEntity.indexInfo.compressionType = type
                                    mediaEntity.extraInfo.existed = true
                                } else {
                                    log { "Failed to parse compression type: ${archivePath.extension}" }
                                }
                            }

                            else -> {}
                        }
                    }.withLog()
                }

                // Write config
                rootService.writeJson(data = mediaEntity, dst = jsonPath)
            }.withLog()
        }
    }

    /**
     * Reload directory structure from cloud.
     *        /.../DataBackup/apps/$packageName/user_$userId@$timestamp/${dataType}.tar.*
     */
    suspend fun reloadAppsFromCloud12x(cloud: String, onMsgUpdate: suspend (String) -> Unit) {
        runCatching {
            cloudRepository.withClient(cloud) { client, cloudEntity ->
                onMsgUpdate(log { "Reloading..." })
                val packageManager = context.packageManager
                val appsDir = pathUtil.getCloudRemoteAppsDir(cloudEntity.remote)
                val pathList = client.walkFileTree(appsDir)
                val typedPathSet = mutableSetOf<String>()
                BaseUtil.mkdirs(context.iconDir())
                log { "Total paths count: ${pathList.size}" }

                // Classify the paths
                pathList.forEach { path ->
                    log { "Classifying: ${path.pathString}" }
                    runCatching {
                        val pathListSize = path.pathList.size
                        val packageName = path.pathList[pathListSize - 3]
                        val preserveId: Long
                        val userId: Int
                        if (path.pathList[pathListSize - 2].contains("@")) {
                            val userIdWithPreserveId = path.pathList[pathListSize - 2].split("@")
                            preserveId = userIdWithPreserveId.lastOrNull()?.toLongOrNull() ?: 0
                            userId = userIdWithPreserveId.first().split("_").lastOrNull()?.toIntOrNull() ?: 0
                        } else {
                            // Main backup
                            preserveId = -1L
                            userId = path.pathList[pathListSize - 2].split("_").lastOrNull()?.toIntOrNull() ?: 0
                        }
                        typedPathSet.add("$packageName@$preserveId@$userId")
                        log { "packageName: $packageName, preserveId: $preserveId, userId: $userId" }
                    }.withLog()
                }

                log { "Apps count: ${typedPathSet.size}" }
                typedPathSet.forEach { typed ->
                    runCatching {
                        // For each $packageName@$preserveId@$userId
                        val split = typed.split("@")
                        val packageName = split[0]
                        var preserveId = split[1].toLong()
                        val mainBackup: Boolean = preserveId == -1L
                        val userId = split[2].toInt()
                        var dir = if (mainBackup) "${appsDir}/${packageName}/user_${userId}" else "${appsDir}/${packageName}/user_${userId}@${preserveId}"
                        onMsgUpdate(log { "packageName: $packageName, preserveId: $preserveId, userId: $userId" })
                        if (mainBackup.not() && preserveId < 1000000000000) {
                            // Diff from main backup (without preserveId)
                            val timestamp = DateUtil.getTimestamp()
                            val newDir = "${appsDir}/${packageName}/user_${userId}@${timestamp}"
                            onMsgUpdate(log { "$dir move to $newDir" })
                            client.mkdirRecursively(dst = PathUtil.getParentPath(newDir))
                            client.renameTo(dir, newDir)
                            preserveId = timestamp
                            dir = newDir
                        }
                        val jsonPath = PathUtil.getPackageRestoreConfigDst(dir)

                        val dataStates = PackageDataStates(
                            apkState = DataState.Disabled,
                            userState = DataState.Disabled,
                            userDeState = DataState.Disabled,
                            dataState = DataState.Disabled,
                            obbState = DataState.Disabled,
                            mediaState = DataState.Disabled,
                            permissionState = DataState.Disabled,
                            ssaidState = DataState.Disabled
                        )
                        val packageEntity = runCatching {
                            val tmpDir = pathUtil.getCloudTmpDir()
                            var entity: PackageEntity? = null
                            cloudRepository.download(client = client, src = jsonPath, dstDir = tmpDir) { path ->
                                entity = rootService.readJson<PackageEntity>(path).also { p ->
                                    p?.indexInfo?.packageName = packageName
                                    p?.indexInfo?.userId = userId
                                    p?.indexInfo?.preserveId = if (mainBackup) 0L else preserveId
                                    p?.extraInfo?.activated = false
                                    p?.indexInfo?.cloud = cloud
                                    p?.indexInfo?.backupDir = cloudEntity.remote
                                    p?.dataStates = dataStates
                                }
                                onMsgUpdate(log { "Config is reloaded from json." })
                            }
                            entity!!
                        }.getOrNull() ?: PackageEntity(
                            id = 0,
                            indexInfo = PackageIndexInfo(
                                opType = OpType.RESTORE,
                                packageName = packageName,
                                userId = userId,
                                compressionType = context.readCompressionType().first(),
                                preserveId = if (mainBackup) 0L else preserveId,
                                cloud = cloud,
                                backupDir = cloudEntity.remote
                            ),
                            packageInfo = PackageInfo(
                                label = "",
                                versionName = "",
                                versionCode = 0,
                                flags = 0,
                                firstInstallTime = 0,
                            ),
                            extraInfo = PackageExtraInfo(
                                uid = -1,
                                hasKeystore = false,
                                permissions = listOf(),
                                ssaid = "",
                                blocked = false,
                                activated = false,
                                firstUpdated = true,
                                enabled = true
                            ),
                            dataStates = dataStates,
                            storageStats = PackageStorageStats(),
                            dataStats = PackageDataStats(),
                            displayStats = PackageDataStats()
                        )

                        val archives = client.walkFileTree(dir)

                        archives.forEach { archivePath ->
                            // For each archive
                            log { "Package archive: ${archivePath.pathString}" }
                            runCatching {
                                when (archivePath.nameWithoutExtension) {
                                    DataType.PACKAGE_APK.type -> {
                                        onMsgUpdate(log { "Dumping apk..." })
                                        val type = CompressionType.suffixOf(archivePath.extension)
                                        if (type != null) {
                                            log { "Archive compression type: ${type.type}" }
                                            packageEntity.indexInfo.compressionType = type
                                            packageEntity.dataStates.apkState = DataState.Selected

                                            if (context.readReloadDumpApk().first()) {
                                                val tmpApkPath = pathUtil.getTmpApkPath(packageName = packageName)
                                                rootService.deleteRecursively(tmpApkPath)
                                                rootService.mkdirs(tmpApkPath)
                                                val tmpDir = pathUtil.getCloudTmpDir()
                                                cloudRepository.download(client = client, src = archivePath.pathString, dstDir = tmpDir) { path ->
                                                    Tar.decompress(src = path, dst = tmpApkPath, extra = type.decompressPara)
                                                    rootService.listFilePaths(tmpApkPath).also { pathList ->
                                                        if (pathList.isNotEmpty()) {
                                                            rootService.getPackageArchiveInfo(pathList.first())?.apply {
                                                                packageEntity.packageInfo.label = applicationInfo.loadLabel(packageManager).toString()
                                                                packageEntity.packageInfo.versionName = versionName ?: ""
                                                                packageEntity.packageInfo.versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                                                    longVersionCode
                                                                } else {
                                                                    versionCode.toLong()
                                                                }
                                                                packageEntity.packageInfo.flags = applicationInfo.flags
                                                                val iconPath = pathUtil.getPackageIconPath(packageName, false)
                                                                val iconExists = rootService.exists(iconPath)
                                                                if (iconExists.not()) {
                                                                    val icon = applicationInfo.loadIcon(packageManager)
                                                                    BaseUtil.writeIcon(icon = icon, dst = iconPath)
                                                                }
                                                                log { "Icon and config updated." }
                                                            }
                                                        } else {
                                                            log { "Archive is empty." }
                                                        }
                                                    }
                                                }
                                                rootService.deleteRecursively(tmpApkPath)
                                            } else {
                                                log { "Skip dumping." }
                                            }
                                        } else {
                                            log { "Failed to parse compression type: ${archivePath.extension}" }
                                        }
                                    }

                                    DataType.PACKAGE_USER.type -> {
                                        onMsgUpdate(log { "Dumping user..." })
                                        val type = CompressionType.suffixOf(archivePath.extension)
                                        if (type != null) {
                                            log { "Archive compression type: ${type.type}" }
                                            packageEntity.indexInfo.compressionType = type
                                            packageEntity.dataStates.userState = DataState.Selected
                                        } else {
                                            log { "Failed to parse compression type: ${archivePath.extension}" }
                                        }
                                    }

                                    DataType.PACKAGE_USER_DE.type, DataType.PACKAGE_DATA.type, DataType.PACKAGE_OBB.type, DataType.PACKAGE_MEDIA.type -> {
                                        onMsgUpdate(log { "Dumping ${archivePath.nameWithoutExtension}..." })

                                        when (archivePath.nameWithoutExtension) {
                                            DataType.PACKAGE_USER_DE.type -> {
                                                dataStates.userDeState = DataState.Selected
                                            }

                                            DataType.PACKAGE_DATA.type -> {
                                                dataStates.dataState = DataState.Selected
                                            }

                                            DataType.PACKAGE_OBB.type -> {
                                                dataStates.obbState = DataState.Selected
                                            }

                                            DataType.PACKAGE_MEDIA.type -> {
                                                dataStates.mediaState = DataState.Selected
                                            }

                                            else -> {}
                                        }
                                    }

                                    else -> {}
                                }
                            }.withLog()
                        }

                        // Write config
                        val tmpDir = pathUtil.getCloudTmpDir()
                        val tmpJsonPath = PathUtil.getPackageRestoreConfigDst(tmpDir)
                        rootService.writeJson(data = packageEntity, dst = tmpJsonPath)
                        cloudRepository.upload(client = client, src = tmpJsonPath, dstDir = PathUtil.getParentPath(jsonPath))
                        rootService.deleteRecursively(tmpDir)
                    }.withLog()
                }
            }
        }.onFailure(rootService.onFailure)
    }

    /**
     * Reload directory structure from cloud.
     *        /.../DataBackup/files/$name@$timestamp/${dataType}.tar.*
     */
    suspend fun reloadFilesFromCloud12x(cloud: String, onMsgUpdate: suspend (String) -> Unit) {
        runCatching {
            cloudRepository.withClient(cloud) { client, cloudEntity ->
                onMsgUpdate(log { "Reloading..." })
                val filesDir = pathUtil.getCloudRemoteFilesDir(cloudEntity.remote)
                val pathList = client.walkFileTree(filesDir)
                val typedPathSet = mutableSetOf<String>()
                log { "Total paths count: ${pathList.size}" }

                // Classify the paths
                pathList.forEach { path ->
                    log { "Classifying: ${path.pathString}" }
                    runCatching {
                        val pathListSize = path.pathList.size
                        val name: String
                        val preserveId: Long
                        if (path.pathList[pathListSize - 2].contains("@")) {
                            val nameWithPreserveId = path.pathList[pathListSize - 2].split("@")
                            preserveId = nameWithPreserveId.lastOrNull()?.toLongOrNull() ?: 0
                            name = nameWithPreserveId.first()
                        } else {
                            // Main backup
                            preserveId = -1L
                            name = path.pathList[pathListSize - 2]
                        }
                        typedPathSet.add("$name@$preserveId")
                        log { "name: $name, preserveId: $preserveId" }
                    }.withLog()
                }

                log { "Files count: ${typedPathSet.size}" }
                typedPathSet.forEach { typed ->
                    runCatching {
                        // For each $packageName@$preserveId
                        val split = typed.split("@")
                        val name = split[0]
                        var preserveId = split[1].toLong()
                        val mainBackup: Boolean = preserveId == -1L
                        var dir = if (mainBackup) "${filesDir}/${name}" else "${filesDir}/${name}@${preserveId}"
                        onMsgUpdate(log { "name: $name, preserveId: $preserveId" })
                        if (mainBackup.not() && preserveId < 1000000000000) {
                            // Diff from main backup (without preserveId)
                            val timestamp = DateUtil.getTimestamp()
                            val newDir = "${filesDir}/${name}@${timestamp}"
                            onMsgUpdate(log { "$dir move to $newDir" })
                            client.mkdirRecursively(dst = PathUtil.getParentPath(newDir))
                            client.renameTo(dir, newDir)
                            preserveId = timestamp
                            dir = newDir
                        }
                        val jsonPath = PathUtil.getMediaRestoreConfigDst(dir)

                        val mediaEntity = runCatching {
                            val tmpDir = pathUtil.getCloudTmpDir()
                            var entity: MediaEntity? = null
                            cloudRepository.download(client = client, src = jsonPath, dstDir = tmpDir) { path ->
                                entity = rootService.readJson<MediaEntity>(path).also { p ->
                                    p?.indexInfo?.name = name
                                    p?.indexInfo?.preserveId = if (mainBackup) 0L else preserveId
                                    p?.extraInfo?.existed = true
                                    p?.extraInfo?.activated = false
                                    p?.indexInfo?.cloud = cloud
                                    p?.indexInfo?.backupDir = cloudEntity.remote
                                }
                                onMsgUpdate(log { "Config is reloaded from json." })
                            }
                            entity!!
                        }.getOrNull() ?: MediaEntity(
                            id = 0,
                            indexInfo = MediaIndexInfo(
                                opType = OpType.RESTORE,
                                name = name,
                                compressionType = CompressionType.TAR,
                                preserveId = if (mainBackup) 0L else preserveId,
                                cloud = cloud,
                                backupDir = cloudEntity.remote
                            ),
                            mediaInfo = MediaInfo(
                                path = "",
                                dataBytes = 0L,
                                displayBytes = 0L,
                            ),
                            extraInfo = MediaExtraInfo(
                                blocked = false,
                                activated = false,
                                existed = true,
                            ),
                        )

                        val archives = client.walkFileTree(dir)

                        archives.forEach { archivePath ->
                            // For each archive
                            log { "Media archive: ${archivePath.pathString}" }
                            runCatching {
                                when (archivePath.nameWithoutExtension) {
                                    DataType.PACKAGE_USER.type -> {
                                        onMsgUpdate(log { "Dumping media..." })
                                        val type = CompressionType.suffixOf(archivePath.extension)
                                        if (type != null) {
                                            log { "Archive compression type: ${type.type}" }
                                            mediaEntity.indexInfo.compressionType = type
                                            mediaEntity.extraInfo.existed = true
                                        } else {
                                            log { "Failed to parse compression type: ${archivePath.extension}" }
                                        }
                                    }

                                    else -> {}
                                }
                            }.withLog()
                        }

                        // Write config
                        val tmpDir = pathUtil.getCloudTmpDir()
                        val tmpJsonPath = PathUtil.getMediaRestoreConfigDst(tmpDir)
                        rootService.writeJson(data = mediaEntity, dst = tmpJsonPath)
                        cloudRepository.upload(client = client, src = tmpJsonPath, dstDir = PathUtil.getParentPath(jsonPath))
                        rootService.deleteRecursively(tmpDir)
                    }.withLog()
                }
            }
        }.onFailure(rootService.onFailure)
    }
}
