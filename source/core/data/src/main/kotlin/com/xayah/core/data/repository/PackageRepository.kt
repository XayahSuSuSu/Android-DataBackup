package com.xayah.core.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.xayah.core.data.R
import com.xayah.core.data.util.srcDir
import com.xayah.core.database.dao.PackageDao
import com.xayah.core.datastore.readCheckKeystore
import com.xayah.core.datastore.readCompressionType
import com.xayah.core.datastore.readIconUpdateTime
import com.xayah.core.datastore.readLoadedIconMD5
import com.xayah.core.datastore.saveIconUpdateTime
import com.xayah.core.datastore.saveLoadedIconMD5
import com.xayah.core.model.CompressionType
import com.xayah.core.model.DataType
import com.xayah.core.model.DefaultPreserveId
import com.xayah.core.model.ModeState
import com.xayah.core.model.OpType
import com.xayah.core.model.SortType
import com.xayah.core.model.database.CloudEntity
import com.xayah.core.model.database.PackageDataStates
import com.xayah.core.model.database.PackageDataStats
import com.xayah.core.model.database.PackageEntity
import com.xayah.core.model.database.PackageEntityWithCount
import com.xayah.core.model.database.PackageExtraInfo
import com.xayah.core.model.database.PackageIndexInfo
import com.xayah.core.model.database.PackageInfo
import com.xayah.core.model.database.PackageStorageStats
import com.xayah.core.network.client.CloudClient
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
import com.xayah.core.util.localBackupSaveDir
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
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
    fun getPackages() = packageDao.queryFlow().distinctUntilChanged()
    fun getPackages(opType: OpType) = packageDao.queryFlow(opType).distinctUntilChanged()
    val activatedCount = packageDao.countActivatedFlow().distinctUntilChanged()
    private val localBackupSaveDir get() = context.localBackupSaveDir()
    private val archivesPackagesDir get() = pathUtil.getLocalBackupArchivesPackagesDir()

    fun getArchiveDst(dstDir: String, dataType: DataType, ct: CompressionType) = "${dstDir}/${dataType.type}.${ct.suffix}"

    private suspend fun getPackage(packageName: String, opType: OpType, userId: Int, preserveId: Long, cloud: String, backupDir: String) =
        packageDao.query(packageName, opType, userId, preserveId, cloud, backupDir)

    suspend fun getPackage(packageName: String, opType: OpType, userId: Int, preserveId: Long, ct: CompressionType, cloud: String, backupDir: String) =
        packageDao.query(packageName, opType, userId, preserveId, ct, cloud, backupDir)

    private suspend fun queryPackages(packageName: String, opType: OpType, userId: Int, cloud: String, backupDir: String) =
        packageDao.query(packageName, opType, userId, cloud, backupDir)

    suspend fun updateLocalPackageDataSize(packageName: String, opType: OpType, userId: Int, preserveId: Long) {
        getPackage(packageName, opType, userId, preserveId, "", "").also {
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

    suspend fun updateCloudPackageArchivesSize(packageName: String, opType: OpType, userId: Int) {
        cloudRepository.withActivatedClients { clients ->
            clients.forEach { (client, entity) ->
                updateCloudPackageArchivesSize(packageName, opType, userId, client, entity)
            }
        }
    }

    suspend fun updateCloudPackageArchivesSize(packageName: String, opType: OpType, userId: Int, client: CloudClient, entity: CloudEntity) {
        val remote = entity.remote
        val remoteArchivesPackagesDir = pathUtil.getCloudRemoteArchivesPackagesDir(remote)
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

    fun getLocationPredicate(index: Int, accountList: List<CloudEntity>): (PackageEntityWithCount) -> Boolean = { p ->
        when (index) {
            0 -> p.entity.indexInfo.cloud.isEmpty()
            else -> p.entity.indexInfo.cloud == accountList.getOrNull(index - 1)?.name
        }
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

    suspend fun refresh(topBarState: MutableStateFlow<TopBarState>, modeState: ModeState, cloud: String) = run {
        val title = topBarState.value.title
        if (modeState != ModeState.BATCH_RESTORE) {
            packageDao.clearExisted(opType = OpType.BACKUP)
            val checkKeystore = context.readCheckKeystore().first()
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
                    val hasKeystore = if (checkKeystore) PackageUtil.hasKeystore(uid) else false
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
        }
        topBarState.emit(TopBarState(progress = 1f, title = StringResourceToken.fromStringId(R.string.updating), indeterminate = true))
        if (cloud.isEmpty()) {
            refreshFromLocalPackage()
            loadLocalIcon()
        } else {
            refreshFromCloudPackage(cloud = cloud)
            loadCloudIcon(cloud = cloud)
        }
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

    private suspend fun loadCloudIcon(cloud: String) = runCatching {
        val load: suspend (CloudClient, CloudEntity) -> Unit = { client: CloudClient, entity: CloudEntity ->
            val remote = entity.remote
            val remoteConfigsDir = pathUtil.getCloudRemoteConfigsDir(remote)
            val archivePath = "$remoteConfigsDir/$IconRelativeDir.${CompressionType.TAR.suffix}"
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
        cloudRepository.withClient(cloud) { client, entity ->
            load(client, entity)
        }
    }.onFailure(rootService.onFailure)

    private suspend fun refreshFromLocal(packageName: String) {
        val path = if (packageName.isEmpty()) archivesPackagesDir else "$archivesPackagesDir/$packageName"
        val paths = rootService.walkFileTree(path)
        paths.forEach {
            val fileName = PathUtil.getFileName(it.pathString)
            val preserveId = PathUtil.getFileName(PathUtil.getParentPath(it.pathString))
            if (fileName == ConfigsPackageRestoreName) {
                runCatching {
                    val stored = rootService.readProtoBuf<PackageEntity>(it.pathString).also { p ->
                        p?.extraInfo?.existed = true
                        p?.extraInfo?.activated = false
                        p?.indexInfo?.cloud = ""
                        p?.indexInfo?.backupDir = localBackupSaveDir
                        p?.indexInfo?.preserveId = preserveId.toLongOrNull() ?: 0
                    }
                    if (stored != null) {
                        getPackage(stored.packageName, stored.indexInfo.opType, stored.userId, stored.preserveId, stored.indexInfo.compressionType, "", localBackupSaveDir).also { p ->
                            if (p == null)
                                packageDao.upsert(stored)
                        }
                    }
                }
            }
        }
    }

    private suspend fun refreshFromCloud(packageName: String, cloud: String) {
        val refresh: suspend (CloudClient, CloudEntity) -> Unit = { client: CloudClient, entity: CloudEntity ->
            val remote = entity.remote
            val remoteArchivesPackagesDir = pathUtil.getCloudRemoteArchivesPackagesDir(remote)
            val src = if (packageName.isEmpty()) remoteArchivesPackagesDir else "$remoteArchivesPackagesDir/$packageName"
            if (client.exists(src)) {
                val paths = client.walkFileTree(src)
                val tmpDir = pathUtil.getCloudTmpDir()
                paths.forEach {
                    val fileName = PathUtil.getFileName(it.pathString)
                    val preserveId = PathUtil.getFileName(PathUtil.getParentPath(it.pathString))
                    if (fileName == ConfigsPackageRestoreName) {
                        runCatching {
                            cloudRepository.download(client = client, src = it.pathString, dstDir = tmpDir) { path ->
                                val stored = rootService.readProtoBuf<PackageEntity>(path).also { p ->
                                    p?.extraInfo?.existed = true
                                    p?.extraInfo?.activated = false
                                    p?.indexInfo?.cloud = entity.name
                                    p?.indexInfo?.backupDir = remote
                                    p?.indexInfo?.preserveId = preserveId.toLongOrNull() ?: 0
                                }
                                if (stored != null) {
                                    getPackage(stored.packageName, stored.indexInfo.opType, stored.userId, stored.preserveId, stored.indexInfo.compressionType, entity.name, remote).also { p ->
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
        if (cloud.isNotEmpty()) {
            cloudRepository.withClient(cloud) { client, entity ->
                refresh(client, entity)
            }
        } else {
            cloudRepository.withActivatedClients { clients ->
                clients.forEach { (client, entity) ->
                    refresh(client, entity)
                }
            }
        }
    }

    suspend fun refreshFromLocalPackage(packageName: String = "") {
        refreshFromLocal(packageName = packageName)
    }

    suspend fun refreshFromCloudPackage(packageName: String = "", cloud: String = "") {
        runCatching { refreshFromCloud(packageName = packageName, cloud = cloud) }.onFailure(rootService.onFailure)
    }

    private suspend fun getPackageSourceDir(packageName: String, userId: Int) = rootService.getPackageSourceDir(packageName, userId).let { list ->
        if (list.isNotEmpty()) PathUtil.getParentPath(list[0]) else ""
    }

    fun getDataSrcDir(dataType: DataType, userId: Int) = dataType.srcDir(userId)

    fun getDataSrc(srcDir: String, packageName: String) = "$srcDir/$packageName"

    suspend fun deleteLocalArchive(p: PackageEntity) = run {
        if (rootService.deleteRecursively("${archivesPackagesDir}/${p.archivesPreserveRelativeDir}")) packageDao.delete(p)
    }

    suspend fun deleteRemoteArchive(cloud: String, p: PackageEntity) = runCatching {
        cloudRepository.withClient(cloud) { client, entity ->
            val remote = entity.remote
            val remoteArchivesPackagesDir = pathUtil.getCloudRemoteArchivesPackagesDir(remote)
            val src = "${remoteArchivesPackagesDir}/${p.archivesPreserveRelativeDir}"
            client.deleteRecursively(src)
        }
    }.onSuccess { packageDao.delete(p) }

    private suspend fun calculateLocalArchiveSize(p: PackageEntity, dataType: DataType) = rootService.calculateSize(
        getArchiveDst("${archivesPackagesDir}/${p.archivesPreserveRelativeDir}", dataType, p.indexInfo.compressionType)
    )

    private fun calculateCloudArchiveSize(client: CloudClient, p: PackageEntity, dataType: DataType, archivesPackagesDir: String) = run {
        val src = getArchiveDst("${archivesPackagesDir}/${p.archivesPreserveRelativeDir}", dataType, p.indexInfo.compressionType)
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
    suspend fun clearActivated() = packageDao.clearActivated()

    suspend fun preserve(p: PackageEntity) {
        val preserveId = DateUtil.getTimestamp()
        val isSuccess = if (p.indexInfo.cloud.isEmpty()) {
            val src = "${archivesPackagesDir}/${p.archivesPreserveRelativeDir}"
            val dst = "${archivesPackagesDir}/${p.archivesRelativeDir}/${preserveId}"
            rootService.renameTo(src, dst)
        } else {
            runCatching {
                cloudRepository.withClient(p.indexInfo.cloud) { client, entity ->
                    val remote = entity.remote
                    val remoteArchivesPackagesDir = pathUtil.getCloudRemoteArchivesPackagesDir(remote)
                    val src = "${remoteArchivesPackagesDir}/${p.archivesPreserveRelativeDir}"
                    val dst = "${remoteArchivesPackagesDir}/${p.archivesRelativeDir}/${preserveId}"
                    client.renameTo(src, dst)
                }
            }.onFailure(rootService.onFailure).isSuccess
        }

        if (isSuccess) upsert(p.copy(indexInfo = p.indexInfo.copy(preserveId = preserveId)))
    }

    suspend fun delete(p: PackageEntity) {
        val isSuccess = if (p.indexInfo.cloud.isEmpty()) {
            val src = "${archivesPackagesDir}/${p.archivesPreserveRelativeDir}"
            rootService.deleteRecursively(src)
        } else {
            runCatching {
                cloudRepository.withClient(p.indexInfo.cloud) { client, entity ->
                    val remote = entity.remote
                    val remoteArchivesPackagesDir = pathUtil.getCloudRemoteArchivesPackagesDir(remote)
                    val src = "${remoteArchivesPackagesDir}/${p.archivesPreserveRelativeDir}"
                    client.deleteRecursively(src)
                }
            }.onFailure(rootService.onFailure).isSuccess
        }

        if (isSuccess) packageDao.delete(p)
    }
}
