package com.xayah.core.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.os.UserHandle
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import com.xayah.core.data.R
import com.xayah.core.data.util.srcDir
import com.xayah.core.database.dao.PackageDao
import com.xayah.core.datastore.di.DbDispatchers.Default
import com.xayah.core.datastore.di.Dispatcher
import com.xayah.core.datastore.readCustomSUFile
import com.xayah.core.datastore.readLoadSystemApps
import com.xayah.core.datastore.readLoadedIconMD5
import com.xayah.core.datastore.saveLoadedIconMD5
import com.xayah.core.hiddenapi.castTo
import com.xayah.core.model.App
import com.xayah.core.model.CompressionType
import com.xayah.core.model.DataState
import com.xayah.core.model.DataType
import com.xayah.core.model.DefaultPreserveId
import com.xayah.core.model.OpType
import com.xayah.core.model.SettingsData
import com.xayah.core.model.UserInfo
import com.xayah.core.model.database.LabelAppCrossRefEntity
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
import com.xayah.core.rootservice.parcelables.PathParcelable
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.util.ConfigsPackageRestoreName
import com.xayah.core.util.DateUtil
import com.xayah.core.util.IconRelativeDir
import com.xayah.core.util.PathUtil
import com.xayah.core.util.command.BaseUtil
import com.xayah.core.util.command.PackageUtil
import com.xayah.core.util.command.Tar
import com.xayah.core.util.EncryptionHelper
import com.xayah.core.util.LogUtil
import com.xayah.core.util.filesDir
import java.security.SecureRandom
import com.google.gson.Gson
import java.nio.charset.StandardCharsets // For UTF_8
import com.xayah.core.util.iconDir
import com.xayah.core.util.localBackupSaveDir
import com.xayah.core.util.withLog
import com.xayah.core.util.withMainContext
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
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
    fun getBackups(filters: Flow<Filters>): Flow<Set<String>> = combine(
        filters,
        appsDao.queryPackagesFlow(opType = OpType.RESTORE).flowOn(defaultDispatcher),
    ) { f, p ->
        p.filter { it.indexInfo.cloud == f.cloud && it.indexInfo.backupDir == f.backupDir }.map { it.pkgUserKey }.toSet()
    }

    fun getInstalledApps(users: Flow<List<UserInfo>>): Flow<Set<String>> = users.map { u ->
        val set = mutableSetOf<String>()
        u.forEach {
            set.addAll(rootService.getInstalledPackagesAsUser(0, it.id).map { p -> "${p.packageName}-${it.id}" }.toSet())
        }
        set
    }

    fun getApp(id: Long) = appsDao.queryPackageFlow(id).flowOn(defaultDispatcher)

    fun getApps(
        opType: OpType,
        listData: Flow<ListData>,
        pkgUserSet: Flow<Set<String>>,
        refs: Flow<List<LabelAppCrossRefEntity>>,
        labels: Flow<Set<String>>,
        cloudName: String,
        backupDir: String
    ): Flow<List<App>> = combine(
        listData,
        pkgUserSet,
        refs,
        labels,
        when (opType) {
            OpType.BACKUP -> appsDao.queryPackagesFlow(opType = opType, blocked = false)
            OpType.RESTORE -> appsDao.queryPackagesFlow(opType = opType, cloud = cloudName, backupDir = backupDir)
        }
    ) { lData, pSet, lRefs, lLabels, apps ->
        val data = lData.castTo<ListData.Apps>()
        apps.asSequence()
            .filter(packageRepo.getKeyPredicateNew(key = data.searchQuery))
            .filter(packageRepo.getShowSystemAppsPredicate(value = data.filters.showSystemApps))
            .filter(packageRepo.getHasBackupsPredicate(value = data.filters.hasBackups, pkgUserSet = pSet))
            .filter(packageRepo.getHasNoBackupsPredicate(value = data.filters.hasNoBackups, pkgUserSet = pSet))
            .filter(packageRepo.getInstalledPredicate(value = data.filters.installedApps, pkgUserSet = pSet))
            .filter(packageRepo.getNotInstalledPredicate(value = data.filters.notInstalledApps, pkgUserSet = pSet))
            .filter(packageRepo.getUserIdPredicateNew(userId = data.userList.getOrNull(data.userIndex)?.id))
            .filter { if (lLabels.isNotEmpty()) lRefs.find { ref -> it.packageName == ref.packageName && it.userId == ref.userId && it.preserveId == ref.preserveId } != null else true }
            .sortedWith(packageRepo.getSortComparatorNew(sortIndex = data.sortIndex, sortType = data.sortType))
            .sortedByDescending { p -> p.extraInfo.activated }.toList()
            .map(PackageEntity::asExternalModel)
    }.flowOn(defaultDispatcher)

    fun countApps(opType: OpType) = appsDao.countPackagesFlow(opType = opType, blocked = false)
    fun countSelectedApps(opType: OpType) = appsDao.countActivatedPackagesFlow(opType = opType, blocked = false)

    suspend fun getLoadSystemApps() = context.readLoadSystemApps().first()

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

    suspend fun setBlocked(id: Long, blocked: Boolean) {
        appsDao.setBlocked(id, blocked)
    }

    suspend fun setEnabled(id: Long, enabled: Boolean) {
        appsDao.setEnabled(id, enabled)
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
        val loadSystemApps = context.readLoadSystemApps().first()
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
                    val isSystemApp = ((info.applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_SYSTEM) != 0
                    if (loadSystemApps || isSystemApp.not()) {
                        apps.add(initializeApp(settings, pm, userId, info))
                    }
                }
            }
            appsDao.upsert(apps)
        }
    }

    /**
     * Initialize only newly installed apps or remove uninstalled apps.
     */
    suspend fun fastInitialize(onInit: suspend (cur: Int, max: Int, content: String) -> Unit) {
        val loadSystemApps = context.readLoadSystemApps().first()
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
                    val isSystemApp = ((info.applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_SYSTEM) != 0
                    if (loadSystemApps || isSystemApp.not()) {
                        apps.add(initializeApp(settings, pm, userId, info))
                    }
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
                info.applicationInfo?.loadLabel(pm).toString(),
                versionName = info.versionName ?: "",
                versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    info.longVersionCode
                } else {
                    info.versionCode.toLong()
                },
                flags = info.applicationInfo?.flags ?: 0,
                firstInstallTime = info.firstInstallTime,
                lastUpdateTime = info.lastUpdateTime,
            ),
            extraInfo = PackageExtraInfo(
                uid = info.applicationInfo?.uid ?: -1,
                hasKeystore = false,
                permissions = listOf(),
                ssaid = "",
                lastBackupTime = 0L,
                blocked = false,
                activated = false,
                firstUpdated = false,
                enabled = true,
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

    suspend fun updateApp(pkg: PackageEntity, userId: Int) {
        val pm = context.packageManager
        val userHandle = rootService.getUserHandle(userId)
        val updateEntity = updateApp(pm, pkg, userId, userHandle)
        if (updateEntity != null) {
            appsDao.update(updateEntity)
        }
    }

    private suspend fun updateApp(pm: PackageManager, pkg: PackageEntity, userId: Int, userHandle: UserHandle?): PackageUpdateEntity? {
        val info = rootService.getPackageInfoAsUser(pkg.packageName, PackageManager.GET_PERMISSIONS, userId)
        val updateEntity = PackageUpdateEntity(pkg.id, pkg.packageInfo, pkg.extraInfo, pkg.storageStats)
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

            updateEntity.packageInfo.label = info.applicationInfo?.loadLabel(pm).toString()
            updateEntity.packageInfo.versionName = info.versionName ?: ""
            updateEntity.packageInfo.versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.longVersionCode
            } else {
                info.versionCode.toLong()
            }
            updateEntity.packageInfo.flags = info.applicationInfo?.flags ?: 0
            updateEntity.packageInfo.firstInstallTime = info.firstInstallTime
            updateEntity.packageInfo.lastUpdateTime = info.lastUpdateTime

            updateEntity.extraInfo.firstUpdated = true
            val uid = info.applicationInfo?.uid ?: -1
            updateEntity.extraInfo.uid = uid
            updateEntity.extraInfo.permissions = rootService.getPermissions(packageInfo = info)
            updateEntity.extraInfo.hasKeystore = PackageUtil.hasKeystore(context.readCustomSUFile().first(), uid)
            updateEntity.extraInfo.ssaid = rootService.getPackageSsaidAsUser(packageName = info.packageName, uid = uid, userId = userId)
            updateEntity.extraInfo.enabled = info.applicationInfo?.enabled ?: false

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

    private fun parsePreserveAndUserId(pathParcelable: PathParcelable): Pair<Long, Int>? {
        runCatching {
            val userPath = pathParcelable.pathList[pathParcelable.pathList.size - 2]
            if (userPath.contains("@")) {
                val userIdWithPreserveId = userPath.split("@")
                val preserveId = userIdWithPreserveId.lastOrNull()?.toLongOrNull() ?: 0L
                val userId = userIdWithPreserveId.first().split("_").lastOrNull()?.toIntOrNull() ?: 0
                return preserveId to userId
            } else {
                // Main backup
                val preserveId = 0L
                val userId = userPath.split("_").lastOrNull()?.toIntOrNull() ?: 0
                return preserveId to userId
            }
        }
        return null
    }

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
                        parsePreserveAndUserId(pathParcelable).also { result ->
                            result?.also { (pId, uId) ->
                                p?.indexInfo?.preserveId = pId
                                p?.indexInfo?.userId = uId
                            }
                        }
                    }?.apply {
                        if (appsDao.query(packageName, indexInfo.opType, userId, preserveId, indexInfo.compressionType, indexInfo.cloud, indexInfo.backupDir) == null) {
                            appsDao.upsert(this)
                        }
                    }
                }
            }
        }
        rootService.clearEmptyDirectoriesRecursively(path)
        appsDao.queryPackages(OpType.RESTORE, "", context.localBackupSaveDir()).forEach {
            val src = "${path}/${it.archivesRelativeDir}"
            if (rootService.exists(src).not()) {
                appsDao.delete(it.id)
            }
        }
    }

    private suspend fun loadCloudApps(cloudName: String, onLoad: suspend (cur: Int, max: Int, content: String) -> Unit) = runCatching {
        cloudRepo.withClient(cloudName) { client, entity ->
            val remote = entity.remote // Base remote path for this cloud account
            val remoteAppsPath = pathUtil.getCloudRemoteAppsDir(remote) // e.g., /<user-defined-path>/DataBackup/apps
            if (client.exists(remoteAppsPath)) {
                val paths = client.walkFileTree(remoteAppsPath) // List all files/dirs under .../apps/
                val tmpDir = pathUtil.getCloudTmpDir() // Local temporary directory for downloads
                val gson = Gson()
                val settings = settingsDataRepo.settingsData.first() // For password placeholder

                paths.forEachIndexed { index, pathParcelable -> // pathParcelable here refers to a remote path
                    val remoteConfigFilePathString = pathParcelable.pathString
                    val fileName = PathUtil.getFileName(remoteConfigFilePathString)
                    onLoad(index, paths.size, fileName) // Progress update

                    if (fileName == ConfigsPackageRestoreName) { // "package.json"
                        var packageEntity: PackageEntity? = null
                        val remoteSaltFilePathString = "$remoteConfigFilePathString.salt"

                        // Determine local temporary paths for downloaded config and salt
                        val localTmpConfigFilePath = File(tmpDir, "${PathUtil.getFileNameWithoutExtension(fileName)}_${DateUtil.getTimestamp()}.${PathUtil.getExtension(fileName)}").absolutePath
                        val localTmpSaltFilePath = "$localTmpConfigFilePath.salt"

                        try {
                            // Attempt to download salt file first, if it exists remotely
                            var salt: ByteArray? = null
                            if (client.exists(remoteSaltFilePathString)) {
                                LogUtil.log { "AppsRepo" to "Found remote salt file for $remoteConfigFilePathString, downloading." }
                                val downloadSaltSuccess = cloudRepo.download(client = client, src = remoteSaltFilePathString, dst = localTmpSaltFilePath)
                                if (downloadSaltSuccess) {
                                    salt = rootService.readBytes(localTmpSaltFilePath) // Read the downloaded salt file
                                    rootService.deleteRecursively(localTmpSaltFilePath) // Clean up tmp salt file
                                } else {
                                    LogUtil.log { "AppsRepo" to "Failed to download salt file $remoteSaltFilePathString" }
                                }
                            }

                            // Download the main config file (package.json)
                            val downloadConfigSuccess = cloudRepo.download(client = client, src = remoteConfigFilePathString, dst = localTmpConfigFilePath)
                            if (downloadConfigSuccess) {
                                val configFileBytes = rootService.readBytes(localTmpConfigFilePath) // Read downloaded config
                                rootService.deleteRecursively(localTmpConfigFilePath) // Clean up tmp config file

                                if (configFileBytes.isNotEmpty()) {
                                    if (salt != null && salt.isNotEmpty()) {
                                        // Encrypted
                                        LogUtil.log { "AppsRepo" to "Attempting decryption for downloaded $remoteConfigFilePathString" }
                                        if (settings.encryptionPasswordRaw.isNotEmpty()) {
                                            try {
                                                val decryptedConfigBytes = EncryptionHelper.decrypt(configFileBytes, settings.encryptionPasswordRaw.toCharArray(), salt)
                                                packageEntity = gson.fromJson(String(decryptedConfigBytes, StandardCharsets.UTF_8), PackageEntity::class.java)
                                                LogUtil.log { "AppsRepo" to "Decryption successful for downloaded $remoteConfigFilePathString" }
                                            } catch (e: Exception) {
                                                LogUtil.log { "AppsRepo" to "Decryption failed for downloaded $remoteConfigFilePathString: ${e.message}" }
                                                e.printStackTrace()
                                            }
                                        } else {
                                            LogUtil.log { "AppsRepo" to "Salt found but no password available for $remoteConfigFilePathString. Skipping." }
                                        }
                                    } else {
                                        // Not encrypted
                                        LogUtil.log { "AppsRepo" to "No salt file or empty salt for $remoteConfigFilePathString, reading as unencrypted." }
                                        try {
                                            packageEntity = gson.fromJson(String(configFileBytes, StandardCharsets.UTF_8), PackageEntity::class.java)
                                        } catch (e: Exception) {
                                            LogUtil.log { "AppsRepo" to "Failed to parse unencrypted downloaded $remoteConfigFilePathString: ${e.message}" }
                                        }
                                    }
                                } else {
                                    LogUtil.log { "AppsRepo" to "Downloaded config file $remoteConfigFilePathString is empty."}
                                }
                            } else {
                                LogUtil.log { "AppsRepo" to "Failed to download config file $remoteConfigFilePathString" }
                            }
                        } finally {
                            // Ensure cleanup of temp files even if errors occur before reading them
                            if (rootService.exists(localTmpConfigFilePath)) rootService.deleteRecursively(localTmpConfigFilePath)
                            if (rootService.exists(localTmpSaltFilePath)) rootService.deleteRecursively(localTmpSaltFilePath)
                        }

                        // Process valid packageEntity
                        packageEntity?.also { p ->
                            p.id = 0
                            p.extraInfo.activated = false
                            p.indexInfo.cloud = entity.name // Set cloud name from the current cloud entity
                            p.indexInfo.backupDir = remote // Set backupDir to the base remote path for this cloud

                            parsePreserveAndUserId(pathParcelable).also { result -> // pathParcelable here is remote
                                result?.also { (pId, uId) ->
                                    p.indexInfo.preserveId = pId
                                    p.indexInfo.userId = uId
                                } ?: run {
                                    LogUtil.log { "AppsRepo" to "Failed to parse preserveId/userId from remote path $remoteConfigFilePathString. Skipping DB upsert." }
                                    return@forEachIndexed
                                }
                            }

                            if (p.packageName.isNotBlank() && p.indexInfo.backupDir.isNotBlank()) {
                                val existingEntry = appsDao.query(p.packageName, p.indexInfo.opType, p.userId, p.indexInfo.preserveId, p.indexInfo.compressionType, p.indexInfo.cloud, p.indexInfo.backupDir)
                                if (existingEntry == null) {
                                    appsDao.upsert(p)
                                } else {
                                    p.id = existingEntry.id
                                    appsDao.updatePackage(p)
                                }
                            } else {
                                LogUtil.log { "AppsRepo" to "Skipping DB upsert for $remoteConfigFilePathString due to missing critical info." }
                            }
                        }
                    }
                }
                // Validate database entries against remote file system (more complex, might need selective validation)
                appsDao.queryPackages(OpType.RESTORE, entity.name, remote).forEach {
                    val remotePackageDir = "${remoteAppsPath}/${it.archivesRelativeDir}" // e.g., /<user-path>/DataBackup/apps/com.example.app/0
                    val remoteConfigFile = PathUtil.getPackageRestoreConfigDst(remotePackageDir)
                    if (!client.exists(remoteConfigFile)) {
                         LogUtil.log { "AppsRepo" to "Cloud DB entry for ${it.packageName}/${it.indexInfo.preserveId} points to non-existent remote config ($remoteConfigFile). Deleting from DB." }
                        appsDao.delete(it.id)
                    }
                }
            } else {
                LogUtil.log { "AppsRepo" to "Remote apps path $remoteAppsPath does not exist for cloud ${entity.name}."}
            }
        }
    }.withLog()

    suspend fun calculateLocalAppSize(app: PackageEntity) {
        app.displayStats.apkBytes = calculateLocalAppDataSize(app, DataType.PACKAGE_APK)
        app.displayStats.userBytes = calculateLocalAppDataSize(app, DataType.PACKAGE_USER)
        app.displayStats.userDeBytes = calculateLocalAppDataSize(app, DataType.PACKAGE_USER_DE)
        app.displayStats.dataBytes = calculateLocalAppDataSize(app, DataType.PACKAGE_DATA)
        app.displayStats.obbBytes = calculateLocalAppDataSize(app, DataType.PACKAGE_OBB)
        app.displayStats.mediaBytes = calculateLocalAppDataSize(app, DataType.PACKAGE_MEDIA)
        appsDao.upsert(app)
    }

    private suspend fun calculateLocalAppDataSize(p: PackageEntity, dataType: DataType): Long {
        val src = getLocalAppDataSrcDir(p, dataType)
        return if (rootService.exists(src)) rootService.calculateSize(src) else 0
    }

    private fun getDataSrcDir(dataType: DataType, userId: Int) = dataType.srcDir(userId)

    private fun getDataSrc(srcDir: String, packageName: String) = "$srcDir/$packageName"

    private suspend fun getPackageSourceDir(packageName: String, userId: Int) = rootService.getPackageSourceDir(packageName, userId).let { list ->
        if (list.isNotEmpty()) PathUtil.getParentPath(list[0]) else ""
    }

    private suspend fun getLocalAppDataSrcDir(p: PackageEntity, dataType: DataType) =
        if (dataType == DataType.PACKAGE_APK) getPackageSourceDir(packageName = p.packageName, userId = p.userId) else getDataSrc(srcDir = getDataSrcDir(dataType = dataType, userId = p.userId), packageName = p.packageName)

    /**
     * @author <a href="https://github.com/MuntashirAkon">@MuntashirAkon</a>
     */
    suspend fun launchApp(packageName: String, userId: Int) {
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val user = rootService.getUserHandle(userId)
        if (launcherApps.isPackageEnabled(packageName, user).not()) {
            // Package not enabled
            withMainContext {
                Toast.makeText(context, context.getString(R.string.app_is_frozen), Toast.LENGTH_SHORT).show()
            }
            return
        }
        val activityInfoList = launcherApps.getActivityList(packageName, user)
        if (activityInfoList.isEmpty()) {
            // No activities
            withMainContext {
                Toast.makeText(context, context.getString(R.string.no_activities_found), Toast.LENGTH_SHORT).show()
            }
            return
        }

        // Return the first openable activity
        val info = activityInfoList[0]
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            .setComponent(info.componentName)

        context.startActivity(intent)
    }

    private fun getArchiveSrc(dstDir: String, dataType: DataType, ct: CompressionType) = "${dstDir}/${dataType.type}.${ct.suffix}"

    private suspend fun calculateLocalAppArchiveSize(p: PackageEntity, dataType: DataType) = rootService.calculateSize(
        getArchiveSrc("${pathUtil.getLocalBackupAppsDir()}/${p.archivesRelativeDir}", dataType, p.indexInfo.compressionType)
    )

    suspend fun calculateLocalAppArchiveSize(app: PackageEntity) {
        app.displayStats.apkBytes = calculateLocalAppArchiveSize(app, DataType.PACKAGE_APK)
        app.displayStats.userBytes = calculateLocalAppArchiveSize(app, DataType.PACKAGE_USER)
        app.displayStats.userDeBytes = calculateLocalAppArchiveSize(app, DataType.PACKAGE_USER_DE)
        app.displayStats.dataBytes = calculateLocalAppArchiveSize(app, DataType.PACKAGE_DATA)
        app.displayStats.obbBytes = calculateLocalAppArchiveSize(app, DataType.PACKAGE_OBB)
        app.displayStats.mediaBytes = calculateLocalAppArchiveSize(app, DataType.PACKAGE_MEDIA)
        appsDao.upsert(app)
    }

    suspend fun protectApp(cloudName: String?, app: PackageEntity) {
        if (cloudName.isNullOrEmpty().not()) {
            cloudName?.apply {
                protectCloudApp(this, app)
            }
        } else {
            protectLocalApp(app)
        }
    }

    private suspend fun protectLocalApp(app: PackageEntity) {
        val protectedApp = app.copy(indexInfo = app.indexInfo.copy(preserveId = DateUtil.getTimestamp()))
        val localAppsDir = pathUtil.getLocalBackupAppsDir()
        // Directory for the original backup (e.g., .../apps/com.example.app/0/)
        // This is the source directory for the rename operation later.
        val originalAppBackupDir = "${localAppsDir}/${app.archivesRelativeDir}"
        // Directory for the new protected backup (e.g., .../apps/com.example.app/1678886400000/)
        // This is the target directory for the rename operation.
        val protectedAppBackupDir = "${localAppsDir}/${protectedApp.archivesRelativeDir}"
        // Config file path within the *original* app backup directory.
        // We modify/create the package.json here, then rename its parent directory.
        val configFilePath = PathUtil.getPackageRestoreConfigDst(originalAppBackupDir)
        val saltFilePath = "$configFilePath.salt"
        val gson = Gson()

        val settings = settingsDataRepo.settingsData.first()

        // Ensure the directory where package.json will be written exists.
        // For a "protect" operation, originalAppBackupDir (e.g., .../0/) should already exist.
        if (!rootService.exists(originalAppBackupDir)) {
            // This case should ideally not be hit if we are "protecting" an existing backup.
            // If it's a brand new backup being saved directly as "protected", then mkdirs makes sense.
            // However, "protectLocalApp" implies an existing 'app' entity.
            LogUtil.log { "AppsRepo" to "Warning: Original backup directory $originalAppBackupDir not found during protect operation. Creating." }
            rootService.mkdirs(originalAppBackupDir)
        }

        if (settings.encryptionEnabled && settings.encryptionPasswordRaw.isNotEmpty()) {
            try {
                LogUtil.log { "AppsRepo" to "Encryption enabled for ${protectedApp.packageName}. Encrypting metadata." }
                val configJsonBytes = gson.toJson(protectedApp).toByteArray(Charsets.UTF_8)
                val salt = ByteArray(16).apply { SecureRandom().nextBytes(this) } // Generate 16-byte salt

                // EncryptionHelper.encrypt now expects salt and returns IV+ciphertext
                val encryptedConfigDataWithIv = EncryptionHelper.encrypt(configJsonBytes, settings.encryptionPasswordRaw.toCharArray(), salt)

                val writeConfigSuccess = rootService.writeBytes(bytes = encryptedConfigDataWithIv, dst = configFilePath)
                if (!writeConfigSuccess) {
                    LogUtil.log { "AppsRepo" to "Failed to write encrypted config for ${protectedApp.packageName}" }
                    rootService.deleteRecursively(configFilePath) // Clean up partial file
                    return // Stop if config write fails
                }

                val writeSaltSuccess = rootService.writeBytes(bytes = salt, dst = saltFilePath)
                if (!writeSaltSuccess) {
                    LogUtil.log { "AppsRepo" to "Failed to write salt for ${protectedApp.packageName}" }
                    rootService.deleteRecursively(configFilePath) // Clean up config file
                    rootService.deleteRecursively(saltFilePath) // Clean up partial salt file
                    return // Stop if salt write fails
                }
                 LogUtil.log { "AppsRepo" to "Encrypted metadata and salt written for ${protectedApp.packageName}" }
            } catch (e: Exception) {
                LogUtil.log { "AppsRepo" to "Encryption failed for ${protectedApp.packageName}: ${e.message}" }
                e.printStackTrace()
                // Clean up any partially written files in case of an encryption error
                rootService.deleteRecursively(configFilePath)
                rootService.deleteRecursively(saltFilePath)
                return // Stop if encryption process fails
            }
        } else {
            LogUtil.log { "AppsRepo" to "Encryption not enabled for ${protectedApp.packageName}. Writing unencrypted metadata." }
            // Write unencrypted config
            rootService.writeJson(data = protectedApp, dst = configFilePath)
            // Ensure no old salt file is lingering if encryption was previously enabled and now turned off
            if(rootService.exists(saltFilePath)) {
                LogUtil.log { "AppsRepo" to "Deleting orphaned salt file for ${protectedApp.packageName} at $saltFilePath" }
                rootService.deleteRecursively(saltFilePath)
            }
        }

        // Rename the whole directory from original preserveId (e.g., /0/) to new protected preserveId (e.g., /timestamp/)
        // This moves package.json, package.json.salt (if any), and all TAR files.
        if (rootService.exists(originalAppBackupDir)) {
            if (rootService.renameTo(originalAppBackupDir, protectedAppBackupDir)) {
                LogUtil.log { "AppsRepo" to "Successfully renamed $originalAppBackupDir to $protectedAppBackupDir" }
                appsDao.update(protectedApp) // Update DB with new preserveId only if rename is successful
            } else {
                LogUtil.log { "AppsRepo" to "Failed to rename backup directory for ${protectedApp.packageName} from $originalAppBackupDir to $protectedAppBackupDir. Metadata may be in inconsistent state in $originalAppBackupDir." }
                // If rename fails, the package.json in originalAppBackupDir is now for 'protectedApp'.
                // This is an error state that needs careful handling, possibly by trying to revert package.json or flagging.
            }
        } else {
            // This state implies that the config file was written (either encrypted or plain) to a directory that
            // doesn't match app.archivesRelativeDir (the source for rename).
            // This could happen if originalAppBackupDir didn't exist and was created, then written to,
            // but app.archivesRelativeDir pointed to something else.
            // This situation should be rare if path logic is consistent.
            LogUtil.log { "AppsRepo" to "Critical error: Original backup directory $originalAppBackupDir was expected to exist for rename but was not found. The config for ${protectedApp.packageName} might be orphaned in $configFilePath."}
        }
    }

    private suspend fun protectCloudApp(cloudName: String, app: PackageEntity) = runCatching {
        cloudRepo.withClient(cloudName) { client, entity ->
            val protectedApp = app.copy(indexInfo = app.indexInfo.copy(preserveId = DateUtil.getTimestamp()))
            val remote = entity.remote
            val remoteAppsDir = pathUtil.getCloudRemoteAppsDir(remote)
            val src = "${remoteAppsDir}/${app.archivesRelativeDir}"
            val dst = "${remoteAppsDir}/${protectedApp.archivesRelativeDir}"
            val tmpDir = pathUtil.getCloudTmpDir()
            val tmpJsonPath = PathUtil.getPackageRestoreConfigDst(tmpDir)
            rootService.writeJson(data = protectedApp, dst = tmpJsonPath)
            cloudRepo.upload(client = client, src = tmpJsonPath, dstDir = src)
            rootService.deleteRecursively(tmpDir)
            client.renameTo(src, dst)
        }
    }.withLog()

    suspend fun deleteApp(cloudName: String?, app: PackageEntity) {
        if (cloudName.isNullOrEmpty().not()) {
            cloudName?.apply {
                deleteCloudApp(this, app)
            }
        } else {
            deleteLocalApp(app)
        }
    }

    private suspend fun deleteLocalApp(app: PackageEntity) {
        val appsDir = pathUtil.getLocalBackupAppsDir()
        val src = "${appsDir}/${app.archivesRelativeDir}"
        if (rootService.deleteRecursively(src)) {
            appsDao.delete(app.id)
        }
    }

    private suspend fun deleteCloudApp(cloudName: String, app: PackageEntity) = runCatching {
        cloudRepo.withClient(cloudName) { client, entity ->
            val remote = entity.remote
            val remoteAppsDir = pathUtil.getCloudRemoteAppsDir(remote)
            val src = "${remoteAppsDir}/${app.archivesRelativeDir}"
            if (client.exists(src)) {
                client.deleteRecursively(src)
                if (client.exists(src).not()) {
                    appsDao.delete(app.id)
                }
            }
        }
    }.withLog()
}
