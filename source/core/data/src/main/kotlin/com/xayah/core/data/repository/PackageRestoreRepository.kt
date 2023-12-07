package com.xayah.core.data.repository

import android.content.Context
import androidx.annotation.StringRes
import com.xayah.core.data.R
import com.xayah.core.database.dao.PackageRestoreEntireDao
import com.xayah.core.database.model.OperationMask
import com.xayah.core.database.model.PackageRestoreEntire
import com.xayah.core.datastore.readRcloneMainAccountRemote
import com.xayah.core.datastore.readRestoreFilterFlagIndex
import com.xayah.core.datastore.readRestoreInstallationTypeIndex
import com.xayah.core.datastore.readRestoreSavePath
import com.xayah.core.datastore.readRestoreSortType
import com.xayah.core.datastore.readRestoreSortTypeIndex
import com.xayah.core.datastore.readRestoreUserId
import com.xayah.core.datastore.saveRestoreFilterFlagIndex
import com.xayah.core.datastore.saveRestoreInstallationTypeIndex
import com.xayah.core.datastore.saveRestoreSortType
import com.xayah.core.datastore.saveRestoreSortTypeIndex
import com.xayah.core.model.CompressionType
import com.xayah.core.model.SortType
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.rootservice.util.withIOContext
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.model.TopBarState
import com.xayah.core.ui.util.fromString
import com.xayah.core.ui.util.fromStringArgs
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.util.CloudTmpAbsoluteDir
import com.xayah.core.util.IconRelativeDir
import com.xayah.core.util.LogUtil
import com.xayah.core.util.PathUtil
import com.xayah.core.util.SymbolUtil.DOT
import com.xayah.core.util.command.Rclone
import com.xayah.core.util.command.SELinux
import com.xayah.core.util.command.Tar
import com.xayah.core.util.filesDir
import com.xayah.core.util.localRestoreSaveDir
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.text.Collator
import javax.inject.Inject

class PackageRestoreRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val rootService: RemoteRootService,
    private val packageRestoreDao: PackageRestoreEntireDao,
    private val cloudRepository: CloudRepository,
    private val pathUtil: PathUtil,
) {
    private fun log(msg: () -> String) = LogUtil.log { "PackageRestoreRepository" to msg() }

    fun getString(@StringRes resId: Int) = context.getString(resId)

    fun observePackages(timestamp: Long) = packageRestoreDao.queryPackagesFlow(timestamp).distinctUntilChanged()
    val packages = packageRestoreDao.observeActivePackages().distinctUntilChanged()
    val packagesApkOnly = packages.map { packages -> packages.filter { it.operationCode == OperationMask.Apk } }.distinctUntilChanged()
    val packagesDataOnly = packages.map { packages -> packages.filter { it.operationCode == OperationMask.Data } }.distinctUntilChanged()
    val packagesBoth = packages.map { packages -> packages.filter { it.operationCode == OperationMask.Both } }.distinctUntilChanged()
    val selectedPackages = packageRestoreDao.observeSelectedPackages().distinctUntilChanged()
    val selectedAPKsCount = packageRestoreDao.countSelectedAPKs().distinctUntilChanged()
    val selectedDataCount = packageRestoreDao.countSelectedData().distinctUntilChanged()

    val restoreFilterFlagIndex = context.readRestoreFilterFlagIndex().distinctUntilChanged()
    val restoreSortTypeIndex = context.readRestoreSortTypeIndex().distinctUntilChanged()
    val restoreSortType = context.readRestoreSortType().distinctUntilChanged()
    val restoreInstallationTypeIndex = context.readRestoreInstallationTypeIndex().distinctUntilChanged()

    val restoreSavePath = context.readRestoreSavePath().distinctUntilChanged()
    private val configsDir = restoreSavePath.map { pathUtil.getConfigsDir(it) }.distinctUntilChanged()

    val remoteDir = context.readRcloneMainAccountRemote().distinctUntilChanged()

    suspend fun writePackagesProtoBuf(dst: String, onStoredList: suspend (MutableList<PackageRestoreEntire>) -> List<PackageRestoreEntire>) {
        val packageRestoreList: MutableList<PackageRestoreEntire> = mutableListOf()
        runCatching {
            val storedList = rootService.readProtoBuf<List<PackageRestoreEntire>>(src = dst)
            packageRestoreList.addAll(storedList!!)
        }
        rootService.writeProtoBuf(data = onStoredList(packageRestoreList), dst = dst)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeTimestamps() = restoreSavePath.flatMapLatest { packageRestoreDao.observeTimestamps(it).distinctUntilChanged() }.distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeRemoteTimestamps() = remoteDir.flatMapLatest { packageRestoreDao.observeTimestamps(it).distinctUntilChanged() }.distinctUntilChanged()

    suspend fun saveRestoreSortType(value: SortType) = context.saveRestoreSortType(value = value)
    suspend fun saveRestoreSortTypeIndex(value: Int) = context.saveRestoreSortTypeIndex(value = value)
    suspend fun saveRestoreFilterFlagIndex(value: Int) = context.saveRestoreFilterFlagIndex(value = value)
    suspend fun saveRestoreInstallationTypeIndex(value: Int) = context.saveRestoreInstallationTypeIndex(value = value)

    suspend fun updatePackage(entity: PackageRestoreEntire) = packageRestoreDao.update(entity)
    suspend fun updateActive(active: Boolean) = packageRestoreDao.updateActive(active = active)
    suspend fun updateActive(active: Boolean, timestamp: Long, savePath: String) =
        packageRestoreDao.updateActive(active = active, timestamp = timestamp, savePath = savePath)

    suspend fun andOpCodeByMask(mask: Int, packageNames: List<String>) = packageRestoreDao.andOpCodeByMask(mask, packageNames)
    suspend fun orOpCodeByMask(mask: Int, packageNames: List<String>) = packageRestoreDao.orOpCodeByMask(mask, packageNames)
    suspend fun delete(items: List<PackageRestoreEntire>) = withIOContext {
        items.forEach { item ->
            val path = "${pathUtil.getLocalRestoreArchivesPackagesDir()}/${item.packageName}/${item.timestamp}"
            log { "Trying to delete: $path" }
            rootService.deleteRecursively(path = path)
        }
        rootService.clearEmptyDirectoriesRecursively(path = context.localRestoreSaveDir())

        val configsDst = PathUtil.getPackageRestoreConfigDst(dstDir = pathUtil.getLocalRestoreConfigsDir())
        writePackagesProtoBuf(configsDst) { storedList ->
            storedList.apply {
                items.forEach { item ->
                    removeIf { it.packageName == item.packageName && it.timestamp == item.timestamp && it.savePath == item.savePath }
                }
            }.toList()
        }

        packageRestoreDao.delete(items)
    }

    suspend fun deleteRemote(items: List<PackageRestoreEntire>) = withIOContext {
        val remoteDir = context.readRcloneMainAccountRemote().first()
        val remotePackagesDir = pathUtil.getArchivesPackagesDir(parent = remoteDir)
        items.forEach { item ->
            val path = "${remotePackagesDir}/${item.packageName}/${item.timestamp}"
            log { "Trying to delete: $path" }
            Rclone.purge(src = path)
            rootService.deleteRecursively(path = path)
        }

        Rclone.rmdirs(src = pathUtil.getArchivesDir(remoteDir))
        Rclone.rmdirs(src = pathUtil.getConfigsDir(remoteDir))

        val remoteConfigsDstDir = pathUtil.getConfigsDir(remoteDir)
        val configsSrc = PathUtil.getPackageRestoreConfigDst(dstDir = remoteConfigsDstDir)
        val tmpDstDir = CloudTmpAbsoluteDir
        val tmpConfigsDstDir = pathUtil.getConfigsDir(tmpDstDir)
        val configsDst = PathUtil.getPackageRestoreConfigDst(dstDir = tmpConfigsDstDir)
        runCatching {
            cloudRepository.download(src = configsSrc, dstDir = tmpConfigsDstDir, onDownloaded = {
                writePackagesProtoBuf(configsDst) { storedList ->
                    storedList.apply {
                        items.forEach { item ->
                            removeIf { it.packageName == item.packageName && it.timestamp == item.timestamp && it.savePath == item.savePath }
                        }
                    }.toList()
                }

                cloudRepository.upload(src = configsDst, dstDir = remoteConfigsDstDir).also { result ->
                    if (result.isSuccess) {
                        packageRestoreDao.delete(items)
                    } else {
                        log { "Failed to upload $configsDst to $remoteConfigsDstDir" }
                    }
                }
            })
        }
    }

    fun getFlagPredicate(index: Int): (PackageRestoreEntire) -> Boolean = { packageRestore ->
        when (index) {
            1 -> packageRestore.isSystemApp.not()
            2 -> packageRestore.isSystemApp
            else -> true
        }
    }

    fun getInstallationPredicate(index: Int): (PackageRestoreEntire) -> Boolean = { packageRestore ->
        when (index) {
            1 -> packageRestore.installed
            2 -> packageRestore.installed.not()
            else -> true
        }
    }

    fun getKeyPredicate(key: String): (PackageRestoreEntire) -> Boolean = { packageRestore ->
        packageRestore.label.lowercase().contains(key.lowercase()) || packageRestore.packageName.lowercase().contains(key.lowercase())
    }

    fun getSortComparator(sortIndex: Int, sortType: SortType): Comparator<in PackageRestoreEntire> = when (sortIndex) {
        1 -> sortByDataSize(sortType)
        else -> sortByAlphabet(sortType)
    }

    private fun sortByAlphabet(type: SortType): Comparator<PackageRestoreEntire> = Comparator { entity1, entity2 ->
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

    private fun sortByDataSize(type: SortType): Comparator<PackageRestoreEntire> = when (type) {
        SortType.ASCENDING -> {
            compareBy { entity -> entity.sizeBytes }
        }

        SortType.DESCENDING -> {
            compareByDescending { entity -> entity.sizeBytes }
        }
    }

    suspend fun loadLocalConfig() {
        val packageRestoreList: MutableList<PackageRestoreEntire> = mutableListOf()
        runCatching {
            val configPath = PathUtil.getPackageRestoreConfigDst(dstDir = configsDir.first())
            if (rootService.exists(configPath)) {
                val storedList = rootService.readProtoBuf<List<PackageRestoreEntire>>(src = configPath)
                packageRestoreList.addAll(storedList!!)
            }
        }
        packageRestoreList.forEach { packageInfo ->
            val packageRestore =
                packageRestoreDao.queryPackage(packageName = packageInfo.packageName, timestamp = packageInfo.timestamp, savePath = packageInfo.savePath)
            val id = packageRestore?.id ?: 0
            val active = packageRestore?.active ?: false
            val operationCode = packageRestore?.operationCode ?: OperationMask.None
            val savePath = context.localRestoreSaveDir()
            packageRestoreDao.upsert(packageInfo.copy(id = id, active = active, operationCode = operationCode, savePath = savePath))
        }
    }

    suspend fun loadRemoteConfig() {
        val packageRestoreList: MutableList<PackageRestoreEntire> = mutableListOf()
        val remoteDir = context.readRcloneMainAccountRemote().first()
        val remoteConfigsDstDir = pathUtil.getConfigsDir(remoteDir)
        val configsSrc = PathUtil.getPackageRestoreConfigDst(dstDir = remoteConfigsDstDir)
        val tmpDstDir = CloudTmpAbsoluteDir
        val tmpConfigsDstDir = pathUtil.getConfigsDir(tmpDstDir)
        val configsDst = PathUtil.getPackageRestoreConfigDst(dstDir = tmpConfigsDstDir)
        runCatching {
            cloudRepository.download(src = configsSrc, dstDir = tmpConfigsDstDir, onDownloaded = {
                val storedList = rootService.readProtoBuf<List<PackageRestoreEntire>>(src = configsDst)
                packageRestoreList.addAll(storedList!!)
            })
        }
        packageRestoreList.forEach { packageInfo ->
            val packageRestore =
                packageRestoreDao.queryPackage(packageName = packageInfo.packageName, timestamp = packageInfo.timestamp, savePath = packageInfo.savePath)
            val id = packageRestore?.id ?: 0
            val active = packageRestore?.active ?: false
            val operationCode = packageRestore?.operationCode ?: OperationMask.None
            val savePath = remoteDir
            packageRestoreDao.upsert(packageInfo.copy(id = id, active = active, operationCode = operationCode, savePath = savePath))
        }
    }

    suspend fun loadLocalIcon() {
        val archivePath = "${configsDir.first()}/$IconRelativeDir.${CompressionType.TAR.suffix}"
        Tar.decompress(src = archivePath, dst = context.filesDir(), extra = CompressionType.TAR.decompressPara)
        SELinux.getContext(path = context.filesDir()).also { result ->
            val pathContext = if (result.isSuccess) result.outString else ""
            SELinux.chcon(context = pathContext, path = context.filesDir())
            SELinux.chown(uid = context.applicationInfo.uid, path = context.filesDir())
        }
    }

    suspend fun loadRemoteIcon() {
        val remoteDir = context.readRcloneMainAccountRemote().first()
        val remoteConfigsDstDir = pathUtil.getConfigsDir(remoteDir)
        val iconSrc = "${remoteConfigsDstDir}/$IconRelativeDir.${CompressionType.TAR.suffix}"
        val tmpDstDir = CloudTmpAbsoluteDir
        val tmpConfigsDstDir = pathUtil.getConfigsDir(tmpDstDir)
        val iconDst = "${tmpConfigsDstDir}/$IconRelativeDir.${CompressionType.TAR.suffix}"

        cloudRepository.download(src = iconSrc, dstDir = tmpConfigsDstDir, onDownloaded = {
            Tar.decompress(src = iconDst, dst = context.filesDir(), extra = CompressionType.TAR.decompressPara)
            SELinux.getContext(path = context.filesDir()).also { result ->
                val pathContext = if (result.isSuccess) result.outString else ""
                SELinux.chcon(context = pathContext, path = context.filesDir())
                SELinux.chown(uid = context.applicationInfo.uid, path = context.filesDir())
            }
        })
    }

    /**
     * Update sizeBytes, installed state.
     */
    suspend fun update(topBarState: MutableStateFlow<TopBarState>) {
        val packages = packageRestoreDao.queryAll()
        val packagesCount = (packages.size - 1).coerceAtLeast(1)
        // Get 1/10 of total count.
        val epoch: Int = ((packagesCount + 1) / 10).coerceAtLeast(1)

        packages.forEachIndexed { index, entity ->
            val timestampPath = "${pathUtil.getLocalRestoreArchivesPackagesDir()}/${entity.packageName}/${entity.timestamp}"
            val sizeBytes = rootService.calculateSize(timestampPath)
            val installed = rootService.queryInstalled(entity.packageName, context.readRestoreUserId().first())
            entity.sizeBytes = sizeBytes
            entity.installed = installed
            if (entity.isExists.not()) {
                entity.backupOpCode = OperationMask.None
                entity.operationCode = OperationMask.None
            }

            if (index % epoch == 0)
                topBarState.emit(
                    TopBarState(
                        progress = index.toFloat() / packagesCount,
                        title = StringResourceToken.fromStringId(R.string.updating)
                    )
                )
        }
        packageRestoreDao.upsert(packages)
        topBarState.emit(
            TopBarState(
                progress = 1f, title = StringResourceToken.fromStringArgs(
                    StringResourceToken.fromStringId(R.string.local),
                    StringResourceToken.fromString(DOT.toString()),
                    StringResourceToken.fromStringId(R.string.restore_list),
                )
            )
        )
    }

    /**
     * Update sizeBytes, installed state.
     */
    suspend fun updateRemote(topBarState: MutableStateFlow<TopBarState>, endTitle: StringResourceToken) {
        val packages = packageRestoreDao.queryAll()
        val packagesCount = (packages.size - 1).coerceAtLeast(1)
        // Get 1/10 of total count.
        val epoch: Int = ((packagesCount + 1) / 10).coerceAtLeast(1)
        packages.forEachIndexed { index, entity ->
            val installed = rootService.queryInstalled(entity.packageName, context.readRestoreUserId().first())
            entity.installed = installed
            if (entity.isExists.not()) {
                entity.backupOpCode = OperationMask.None
                entity.operationCode = OperationMask.None
            }

            if (index % epoch == 0)
                topBarState.emit(
                    TopBarState(
                        progress = index.toFloat() / packagesCount,
                        title = StringResourceToken.fromStringId(R.string.updating)
                    )
                )
        }
        packageRestoreDao.upsert(packages)
        topBarState.emit(TopBarState(progress = 1f, title = endTitle))
    }
}
