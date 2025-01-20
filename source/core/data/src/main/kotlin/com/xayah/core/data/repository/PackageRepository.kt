package com.xayah.core.data.repository

import android.content.Context
import android.os.Build
import com.xayah.core.data.util.srcDir
import com.xayah.core.database.dao.PackageDao
import com.xayah.core.datastore.readCompressionType
import com.xayah.core.datastore.readReloadDumpApk
import com.xayah.core.model.CompressionType
import com.xayah.core.model.DataState
import com.xayah.core.model.DataType
import com.xayah.core.model.OpType
import com.xayah.core.model.SortType
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
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.util.DateUtil
import com.xayah.core.util.LogUtil
import com.xayah.core.util.PathUtil
import com.xayah.core.util.command.BaseUtil
import com.xayah.core.util.command.Tar
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

    suspend fun getPackage(packageName: String, opType: OpType, userId: Int) = packageDao.query(packageName, opType, userId)
    fun queryPackagesFlow(opType: OpType, blocked: Boolean) = packageDao.queryPackagesFlow(opType, blocked).distinctUntilChanged()
    suspend fun queryPackages(opType: OpType, blocked: Boolean) = packageDao.queryPackages(opType, blocked)
    suspend fun queryUserIds(opType: OpType) = packageDao.queryUserIds(opType)
    suspend fun queryPackages(opType: OpType, cloud: String, backupDir: String) = packageDao.queryPackages(opType, cloud, backupDir)
    suspend fun queryActivated(opType: OpType) = packageDao.queryActivated(opType)
    suspend fun queryActivated(opType: OpType, cloud: String, backupDir: String) = packageDao.queryActivated(opType, cloud, backupDir)
    suspend fun setBlocked(id: Long, blocked: Boolean) = packageDao.setBlocked(id, blocked)
    suspend fun clearBlocked() = packageDao.clearBlocked()
    private val localBackupSaveDir get() = context.localBackupSaveDir()

    fun getArchiveDst(dstDir: String, dataType: DataType, ct: CompressionType) = "${dstDir}/${dataType.type}.${ct.suffix}"

    fun getKeyPredicateNew(key: String): (PackageEntity) -> Boolean = { p ->
        p.packageInfo.label.lowercase().contains(key.lowercase()) || p.packageName.lowercase().contains(key.lowercase())
    }

    fun getShowSystemAppsPredicate(value: Boolean): (PackageEntity) -> Boolean = { p ->
        value || p.isSystemApp.not()
    }

    fun getHasBackupsPredicate(value: Boolean, pkgUserSet: Set<String>): (PackageEntity) -> Boolean = { p ->
        value || p.pkgUserKey !in pkgUserSet
    }

    fun getHasNoBackupsPredicate(value: Boolean, pkgUserSet: Set<String>): (PackageEntity) -> Boolean = { p ->
        value || p.pkgUserKey in pkgUserSet
    }

    fun getInstalledPredicate(value: Boolean, pkgUserSet: Set<String>): (PackageEntity) -> Boolean = { p ->
        value || p.pkgUserKey !in pkgUserSet
    }

    fun getNotInstalledPredicate(value: Boolean, pkgUserSet: Set<String>): (PackageEntity) -> Boolean = { p ->
        value || p.pkgUserKey in pkgUserSet
    }

    fun getUserIdPredicateNew(userId: Int?): (PackageEntity) -> Boolean = { p ->
        runCatching { p.userId == userId }.getOrDefault(false)
    }

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

    fun getDataSrcDir(dataType: DataType, userId: Int) = dataType.srcDir(userId)

    fun getDataSrc(srcDir: String, packageName: String) = "$srcDir/$packageName"

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
                        lastUpdateTime = 0,
                    ),
                    extraInfo = PackageExtraInfo(
                        uid = -1,
                        hasKeystore = false,
                        permissions = listOf(),
                        ssaid = "",
                        lastBackupTime = 0,
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
                                                    packageEntity.packageInfo.label = applicationInfo?.loadLabel(packageManager).toString()
                                                    packageEntity.packageInfo.versionName = versionName ?: ""
                                                    packageEntity.packageInfo.versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                                        longVersionCode
                                                    } else {
                                                        versionCode.toLong()
                                                    }
                                                    packageEntity.packageInfo.flags = applicationInfo?.flags ?: 0
                                                    val iconPath = pathUtil.getPackageIconPath(packageName, false)
                                                    val iconExists = rootService.exists(iconPath)
                                                    if (iconExists.not()) {
                                                        val icon = applicationInfo?.loadIcon(packageManager)
                                                        if (icon != null) {
                                                            BaseUtil.writeIcon(icon = icon, dst = iconPath)
                                                        } else {
                                                            log { "Failed to get icon." }
                                                        }
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
                        lastBackupTime = 0L,
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
                                lastUpdateTime = 0,
                            ),
                            extraInfo = PackageExtraInfo(
                                uid = -1,
                                hasKeystore = false,
                                permissions = listOf(),
                                ssaid = "",
                                lastBackupTime = 0,
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
                                                                packageEntity.packageInfo.label = applicationInfo?.loadLabel(packageManager).toString()
                                                                packageEntity.packageInfo.versionName = versionName ?: ""
                                                                packageEntity.packageInfo.versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                                                    longVersionCode
                                                                } else {
                                                                    versionCode.toLong()
                                                                }
                                                                packageEntity.packageInfo.flags = applicationInfo?.flags ?: 0
                                                                val iconPath = pathUtil.getPackageIconPath(packageName, false)
                                                                val iconExists = rootService.exists(iconPath)
                                                                if (iconExists.not()) {
                                                                    val icon = applicationInfo?.loadIcon(packageManager)
                                                                    if (icon != null) {
                                                                        BaseUtil.writeIcon(icon = icon, dst = iconPath)
                                                                    } else {
                                                                        log { "Failed to get icon." }
                                                                    }
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
                                lastBackupTime = 0L,
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
