package com.xayah.core.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.os.UserHandle
import androidx.appcompat.content.res.AppCompatResources
import com.xayah.core.database.dao.PackageDao
import com.xayah.core.datastore.di.DbDispatchers.Default
import com.xayah.core.datastore.di.Dispatcher
import com.xayah.core.datastore.readCustomSUFile
import com.xayah.core.datastore.readLoadedIconMD5
import com.xayah.core.datastore.saveLoadedIconMD5
import com.xayah.core.hiddenapi.castTo
import com.xayah.core.model.App
import com.xayah.core.model.CompressionType
import com.xayah.core.model.DataState
import com.xayah.core.model.DefaultPreserveId
import com.xayah.core.model.OpType
import com.xayah.core.model.SettingsData
import com.xayah.core.model.database.PackageDataStates
import com.xayah.core.model.database.PackageDataStatesEntity
import com.xayah.core.model.database.PackageDataStats
import com.xayah.core.model.database.PackageEntity
import com.xayah.core.model.database.PackageExtraInfo
import com.xayah.core.model.database.PackageIndexInfo
import com.xayah.core.model.database.PackageInfo
import com.xayah.core.model.database.PackageStorageStats
import com.xayah.core.model.database.PackageUpdateEntity
import com.xayah.core.model.database.asExternalModel
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.util.ConfigsPackageRestoreName
import com.xayah.core.util.IconRelativeDir
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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class AppsRepo @Inject constructor(
    @ApplicationContext private val context: Context,
    @Dispatcher(Default) private val defaultDispatcher: CoroutineDispatcher,
    private val appsDao: PackageDao,
    private val packageRepo: PackageRepository,
    private val rootService: RemoteRootService,
    private val settingsDataRepo: SettingsDataRepo,
    private val pathUtil: PathUtil,
    private val cloudRepo: CloudRepository
) {
    fun getApps(
        opType: OpType,
        listData: Flow<ListData>,
        cloudName: String,
        backupDir: String
    ): Flow<List<App>> = combine(
        listData,
        when (opType) {
            OpType.BACKUP -> appsDao.queryPackagesFlow(opType = opType, blocked = false)
            OpType.RESTORE -> appsDao.queryPackagesFlow(opType = opType, cloud = cloudName, backupDir = backupDir)
        }
    ) { lData, apps ->
        val data = lData.castTo<ListData.Apps>()
        apps.asSequence()
            .filter(packageRepo.getKeyPredicateNew(key = data.searchQuery))
            .filter(packageRepo.getShowSystemAppsPredicate(value = data.showSystemApps))
            .filter(packageRepo.getUserIdPredicateNew(userId = data.userList.getOrNull(data.userIndex)?.id))
            .sortedWith(packageRepo.getSortComparatorNew(sortIndex = data.sortIndex, sortType = data.sortType))
            .sortedByDescending { p -> p.extraInfo.activated }.toList()
            .map(PackageEntity::asExternalModel)
    }.flowOn(defaultDispatcher)

    fun countApps(opType: OpType) = appsDao.countPackagesFlow(opType = opType, blocked = false)
    fun countSelectedApps(opType: OpType) = appsDao.countActivatedPackagesFlow(opType = opType, blocked = false)

    suspend fun selectApp(id: Long, selected: Boolean) {
        appsDao.activateById(id, selected)
    }

    suspend fun selectDataItems(id: Long, apk: DataState, user: DataState, userDe: DataState, data: DataState, obb: DataState, media: DataState) {
        appsDao.selectDataItemsById(id, apk.name, user.name, userDe.name, data.name, obb.name, media.name)
    }

    suspend fun selectAll(ids: List<Long>) {
        appsDao.activateByIds(ids, true)
    }

    suspend fun unselectAll(ids: List<Long>) {
        appsDao.activateByIds(ids, false)
    }

    suspend fun reverseAll(ids: List<Long>) {
        appsDao.reverseActivatedByIds(ids)
    }

    suspend fun blockSelected(ids: List<Long>) {
        appsDao.blockByIds(ids)
    }

    suspend fun deleteSelected(ids: List<Long>) {
        val appsDir = pathUtil.getLocalBackupAppsDir()
        val deletedIds = mutableListOf<Long>()
        ids.forEach {
            val app = appsDao.queryById(it)
            if (app != null) {
                val isSuccess = if (app.indexInfo.cloud.isEmpty()) {
                    val src = "${appsDir}/${app.archivesRelativeDir}"
                    rootService.deleteRecursively(src)
                } else {
                    runCatching {
                        cloudRepo.withClient(app.indexInfo.cloud) { client, entity ->
                            val remote = entity.remote
                            val remoteArchivesPackagesDir = pathUtil.getCloudRemoteAppsDir(remote)
                            val src = "${remoteArchivesPackagesDir}/${app.archivesRelativeDir}"
                            if (client.exists(src)) client.deleteRecursively(src)
                        }
                    }.withLog().isSuccess
                }
                if (isSuccess) deletedIds.add(app.id)
            }
        }
        appsDao.deleteByIds(deletedIds)
    }

    suspend fun setDataItems(ids: List<Long>, selections: PackageDataStates) {
        appsDao.updatePackageDataStates(ids.map { PackageDataStatesEntity(it, selections) })
    }

    /**
     * Initialize only newly installed apps or remove uninstalled apps.
     *
     * Faster than [fastInitialize] if there are too many newly installed apps.
     */
    suspend fun fullInitialize(onInit: suspend (cur: Int, max: Int, content: String) -> Unit) {
        val settings = settingsDataRepo.settingsData.first()
        val pm = context.packageManager
        val userInfoList = rootService.getUsers()
        for (userInfo in userInfoList) {
            val userId = userInfo.id
            val installedPackages = getInstalledPackages(userId)
            val storedSet = appsDao.queryPkgSetByUserId(OpType.BACKUP, userId).toSet()

            // Remove uninstalled apps
            val outdatedPackages = storedSet.subtract(installedPackages.map { it.packageName }.toSet())
            appsDao.deleteByPkgNames(opType = OpType.BACKUP, userId = userId, packageNames = outdatedPackages)

            val apps = mutableListOf<PackageEntity>()
            installedPackages.forEachIndexed { index, info ->
                onInit(index, installedPackages.size, info.packageName)
                if (storedSet.contains(info.packageName).not()) {
                    apps.add(initializeApp(settings, pm, userId, info))
                }
            }
            appsDao.upsert(apps)
        }
    }

    /**
     * Initialize only newly installed apps or remove uninstalled apps.
     */
    suspend fun fastInitialize(onInit: suspend (cur: Int, max: Int, content: String) -> Unit) {
        val settings = settingsDataRepo.settingsData.first()
        val pm = context.packageManager
        val userInfoList = rootService.getUsers()
        for (userInfo in userInfoList) {
            val userId = userInfo.id
            val installedPackages = getInstalledPackages(userId).map { it.packageName }.toSet()
            val storedSet = appsDao.queryPkgSetByUserId(OpType.BACKUP, userId).toSet()

            // Remove uninstalled apps
            val outdatedPackages = storedSet.subtract(installedPackages)
            appsDao.deleteByPkgNames(opType = OpType.BACKUP, userId = userId, packageNames = outdatedPackages)

            // Add newly install apps
            val apps = mutableListOf<PackageEntity>()
            val missingPackages = installedPackages.subtract(storedSet)
            missingPackages.forEachIndexed { index, pkg ->
                onInit(index, missingPackages.size, pkg)
                val info = rootService.getPackageInfoAsUser(pkg, 0, userId)
                if (info != null) {
                    apps.add(initializeApp(settings, pm, userId, info))
                }
            }
            appsDao.upsert(apps)
        }
    }

    private fun initializeApp(settings: SettingsData, pm: PackageManager, userId: Int, info: android.content.pm.PackageInfo): PackageEntity {
        return PackageEntity(
            id = 0,
            indexInfo = PackageIndexInfo(
                opType = OpType.BACKUP,
                packageName = info.packageName,
                userId = userId,
                compressionType = settings.compressionType,
                preserveId = DefaultPreserveId,
                cloud = "",
                backupDir = "",
            ),
            packageInfo = PackageInfo(
                info.applicationInfo.loadLabel(pm).toString(),
                versionName = info.versionName ?: "",
                versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    info.longVersionCode
                } else {
                    info.versionCode.toLong()
                },
                flags = info.applicationInfo.flags,
                firstInstallTime = info.firstInstallTime,
            ),
            extraInfo = PackageExtraInfo(
                uid = info.applicationInfo.uid,
                labels = listOf(),
                hasKeystore = false,
                permissions = listOf(),
                ssaid = "",
                blocked = false,
                activated = false,
                firstUpdated = false,
            ),
            dataStates = PackageDataStates(),
            storageStats = PackageStorageStats(),
            dataStats = PackageDataStats(),
            displayStats = PackageDataStats(),
        )
    }

    suspend fun fullUpdate(onUpdate: suspend (cur: Int, max: Int, content: String) -> Unit) {
        val pm = context.packageManager
        val userInfoList = rootService.getUsers()
        BaseUtil.mkdirs(context.iconDir())
        for (userInfo in userInfoList) {
            val userId = userInfo.id
            val userHandle = rootService.getUserHandle(userId)
            val apps = appsDao.queryPkgEntitiesByUserId(OpType.BACKUP, userId)
            val updateList = mutableListOf<PackageUpdateEntity>()

            apps.forEachIndexed { index, pkg ->
                onUpdate(index, apps.size, pkg.packageName)
                val updateEntity = updateApp(pm, pkg, userId, userHandle)
                if (updateEntity != null) {
                    updateList.add(updateEntity)
                }
            }
            appsDao.update(updateList)
        }
    }

    suspend fun fastUpdate(onUpdate: suspend (cur: Int, max: Int, content: String) -> Unit) {
        val pm = context.packageManager
        val apps = appsDao.queryFirstUpdatedApps(opType = OpType.BACKUP, firstUpdated = false)
        val updateList = mutableListOf<PackageUpdateEntity>()
        BaseUtil.mkdirs(context.iconDir())
        apps.forEachIndexed { index, pkg ->
            onUpdate(index, apps.size, pkg.packageName)
            val userId = pkg.userId
            val userHandle = rootService.getUserHandle(userId)
            val updateEntity = updateApp(pm, pkg, userId, userHandle)
            if (updateEntity != null) {
                updateList.add(updateEntity)
            }
        }
        appsDao.update(updateList)
    }

    private suspend fun updateApp(pm: PackageManager, pkg: PackageEntity, userId: Int, userHandle: UserHandle?): PackageUpdateEntity? {
        val info = rootService.getPackageInfoAsUser(pkg.packageName, PackageManager.GET_PERMISSIONS, userId)
        val updateEntity = PackageUpdateEntity(pkg.id, pkg.extraInfo, pkg.storageStats)
        if (info != null) {
            runCatching {
                val iconPath: String
                val icon: Drawable?
                val iconDrawable = runCatching { context.packageManager.getApplicationIcon(pkg.packageName) }.getOrElse { AppCompatResources.getDrawable(context, android.R.drawable.sym_def_app_icon) }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && iconDrawable is AdaptiveIconDrawable) {
                    iconPath = pathUtil.getPackageIconPath(info.packageName, true)
                    icon = LayerDrawable(arrayOf(iconDrawable.background, iconDrawable.foreground))
                } else {
                    iconPath = pathUtil.getPackageIconPath(info.packageName, false)
                    icon = iconDrawable
                }
                if (icon != null) {
                    BaseUtil.writeIcon(icon = icon, dst = iconPath)
                }
            }.withLog()

            updateEntity.extraInfo.firstUpdated = true
            val uid = info.applicationInfo.uid
            updateEntity.extraInfo.uid = uid
            updateEntity.extraInfo.permissions = PermissionUtil.getPermission(packageManager = pm, packageInfo = info)
            updateEntity.extraInfo.hasKeystore = PackageUtil.hasKeystore(context.readCustomSUFile().first(), uid)
            updateEntity.extraInfo.ssaid = rootService.getPackageSsaidAsUser(packageName = info.packageName, uid = uid, userId = userId)

            if (userHandle != null) {
                rootService.queryStatsForPackage(info, userHandle).also { stats ->
                    if (stats != null) {
                        updateEntity.storageStats.appBytes = stats.appBytes
                        updateEntity.storageStats.cacheBytes = stats.cacheBytes
                        updateEntity.storageStats.dataBytes = stats.dataBytes
                        updateEntity.storageStats.externalCacheBytes = stats.externalCacheBytes
                    }
                }
            }
            return updateEntity
        } else {
            appsDao.delete(updateEntity.id)
            return null
        }
    }

    private suspend fun getInstalledPackages(userId: Int) = rootService.getInstalledPackagesAsUser(0, userId).filter {
        // Filter itself
        it.packageName != context.packageName
    }

    suspend fun load(cloudName: String?, onLoad: suspend (cur: Int, max: Int, content: String) -> Unit) {
        if (cloudName.isNullOrEmpty().not()) {
            cloudName?.apply {
                loadCloudIcons(this)
                loadCloudApps(this, onLoad)
            }
        } else {
            loadLocalIcons()
            loadLocalApps(onLoad)
        }
    }

    private suspend fun loadLocalIcons() {
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

    private suspend fun loadCloudIcons(cloudName: String) = runCatching {
        cloudRepo.withClient(cloudName) { client, entity ->
            val archivePath = "${pathUtil.getCloudRemoteConfigsDir(entity.remote)}/$IconRelativeDir.${CompressionType.TAR.suffix}"
            if (client.exists(archivePath)) {
                val tmpDir = pathUtil.getCloudTmpDir()
                cloudRepo.download(client = client, src = archivePath, dstDir = tmpDir) { path ->
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
    }.withLog()

    private suspend fun loadLocalApps(onLoad: suspend (cur: Int, max: Int, content: String) -> Unit) {
        val path = pathUtil.getLocalBackupAppsDir()
        val paths = rootService.walkFileTree(path)
        paths.forEachIndexed { index, pathParcelable ->
            val fileName = PathUtil.getFileName(pathParcelable.pathString)
            onLoad(index, paths.size, fileName)
            if (fileName == ConfigsPackageRestoreName) {
                runCatching {
                    rootService.readJson<PackageEntity>(pathParcelable.pathString).also { p ->
                        p?.id = 0
                        p?.extraInfo?.activated = false
                        p?.indexInfo?.cloud = ""
                        p?.indexInfo?.backupDir = context.localBackupSaveDir()
                    }?.apply {
                        if (appsDao.query(packageName, indexInfo.opType, userId, preserveId, indexInfo.compressionType, indexInfo.cloud, indexInfo.backupDir) == null) {
                            appsDao.upsert(this)
                        }
                    }
                }
            }
        }
    }

    private suspend fun loadCloudApps(cloudName: String, onLoad: suspend (cur: Int, max: Int, content: String) -> Unit) = runCatching {
        cloudRepo.withClient(cloudName) { client, entity ->
            val remote = entity.remote
            val src = pathUtil.getCloudRemoteAppsDir(remote)
            if (client.exists(src)) {
                val paths = client.walkFileTree(src)
                val tmpDir = pathUtil.getCloudTmpDir()
                paths.forEachIndexed { index, pathParcelable ->
                    val fileName = PathUtil.getFileName(pathParcelable.pathString)
                    onLoad(index, paths.size, fileName)
                    if (fileName == ConfigsPackageRestoreName) {
                        runCatching {
                            cloudRepo.download(client = client, src = pathParcelable.pathString, dstDir = tmpDir) { path ->
                                rootService.readJson<PackageEntity>(path).also { p ->
                                    p?.id = 0
                                    p?.extraInfo?.activated = false
                                    p?.indexInfo?.cloud = entity.name
                                    p?.indexInfo?.backupDir = remote
                                }?.apply {
                                    if (appsDao.query(packageName, indexInfo.opType, userId, preserveId, indexInfo.compressionType, indexInfo.cloud, indexInfo.backupDir) == null) {
                                        appsDao.upsert(this)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }.withLog()
}
