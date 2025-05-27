package com.xayah.core.rootservice.impl

import android.annotation.TargetApi
import android.app.ActivityManagerHidden
import android.app.ActivityThread
import android.app.AppOpsManager
import android.app.AppOpsManagerHidden
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.IPackageManager
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PackageInfoFlags
import android.content.pm.PackageManagerHidden
import android.content.pm.PermissionInfo
import android.content.pm.UserInfo
import android.os.Build
import android.os.Parcel
import android.os.ParcelFileDescriptor
import android.os.ServiceManager
import android.os.StatFs
import android.os.UserHandle
import android.os.UserHandleHidden
import android.os.UserManagerHidden
import android.view.SurfaceControlHidden
import androidx.core.content.pm.PermissionInfoCompat
import com.android.server.display.DisplayControl
import com.topjohnwu.superuser.ShellUtils
import com.xayah.core.datastore.ConstantUtil.DEFAULT_IDLE_TIMEOUT
import com.xayah.core.hiddenapi.castTo
import com.xayah.core.model.database.PackagePermission
import com.xayah.core.rootservice.IRemoteRootService
import com.xayah.core.rootservice.parcelables.PathParcelable
import com.xayah.core.rootservice.parcelables.StatFsParcelable
import com.xayah.core.rootservice.parcelables.StorageStatsParcelable
import com.xayah.core.rootservice.util.ExceptionUtil.tryOn
import com.xayah.core.rootservice.util.ExceptionUtil.tryWithBoolean
import com.xayah.core.rootservice.util.SsaidUtil
import com.xayah.core.util.FileUtil
import com.xayah.core.util.HashUtil
import com.xayah.core.util.PathUtil
import com.xayah.core.util.command.BaseUtil.setAllPermissions
import com.xayah.libnative.NativeLib
import java.io.File
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.pathString

internal class RemoteRootServiceImpl(private val context: Context) : IRemoteRootService.Stub() {
    private val lock = Any()
    private var systemContext: Context
    private var packageManager: PackageManager
    private var packageManagerHidden: PackageManagerHidden
    private var packageManagerService: IPackageManager
    private var userManager: UserManagerHidden
    private var activityManager: ActivityManagerHidden
    private var appOpsManager: AppOpsManagerHidden

    private fun getSystemContext(): Context = ActivityThread.systemMain().systemContext

    @TargetApi(Build.VERSION_CODES.O)
    private fun getStorageStatsManager(): StorageStatsManager = systemContext.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager

    private fun getUserManager(): UserManagerHidden = UserManagerHidden.get(systemContext).castTo()

    private fun getActivityManager(): ActivityManagerHidden = systemContext.getSystemService(Context.ACTIVITY_SERVICE).castTo()

    private fun getAppOpsManager(): AppOpsManagerHidden = systemContext.getSystemService(Context.APP_OPS_SERVICE).castTo()

    init {
        systemContext = getSystemContext()
        packageManager = systemContext.packageManager
        packageManagerHidden = packageManager.castTo()
        packageManagerService = IPackageManager.Stub.asInterface(ServiceManager.getService("package"))
        userManager = getUserManager()
        activityManager = getActivityManager()
        appOpsManager = getAppOpsManager()
    }

    override fun readStatFs(path: String): StatFsParcelable = synchronized(lock) {
        val statFs = StatFs(path)
        StatFsParcelable(statFs.availableBytes, statFs.totalBytes)
    }

    override fun mkdirs(path: String): Boolean = synchronized(lock) {
        tryOn(
            block = {
                val file = File(path)
                if (file.exists().not()) file.mkdirs() else true
            },
            onException = { false }
        )
    }

    override fun copyRecursively(path: String, targetPath: String, overwrite: Boolean): Boolean = synchronized(lock) {
        tryOn(block = { File(path).copyRecursively(target = File(targetPath), overwrite = overwrite) }, onException = { false })
    }

    override fun copyTo(path: String, targetPath: String, overwrite: Boolean): Boolean = synchronized(lock) {
        tryWithBoolean {
            File(path).copyTo(target = File(targetPath), overwrite = overwrite)
        }
    }

    override fun renameTo(src: String, dst: String): Boolean = synchronized(lock) {
        runCatching { File(src).renameTo(File(dst)) }.getOrElse { false }
    }

    override fun exists(path: String): Boolean = synchronized(lock) {
        tryOn(block = { File(path).exists() }, onException = { false })
    }

    override fun createNewFile(path: String): Boolean = synchronized(lock) {
        tryOn(block = { File(path).createNewFile() }, onException = { false })
    }

    override fun deleteRecursively(path: String): Boolean = synchronized(lock) {
        tryOn(block = { File(path).deleteRecursively() }, onException = { false })
    }

    override fun listFilePaths(path: String, listFiles: Boolean, listDirs: Boolean): List<String> = synchronized(lock) {
        FileUtil.listFilePaths(path = path, listFiles = listFiles, listDirs = listDirs)
    }

    private fun writeToParcel(block: (Parcel) -> Unit): ParcelFileDescriptor {
        val parcel = Parcel.obtain()
        parcel.setDataPosition(0)
        block(parcel)
        val tmpFile = File.createTempFile("databackup-parcel-", ".tmp", context.cacheDir)
        tmpFile.delete()
        tmpFile.createNewFile()
        tmpFile.writeBytes(parcel.marshall())
        val pfd = ParcelFileDescriptor.open(tmpFile, ParcelFileDescriptor.MODE_READ_WRITE)
        tmpFile.delete()
        parcel.recycle()
        return pfd
    }

    override fun readText(path: String): ParcelFileDescriptor = synchronized(lock) {
        writeToParcel { parcel ->
            parcel.writeString(FileUtil.readText(path))
        }
    }

    override fun readBytes(path: String): ParcelFileDescriptor =
        synchronized(lock) {
            writeToParcel { parcel ->
                val bytes = tryOn(
                    block = {
                        File(path).readBytes()
                    },
                    onException = {
                        ByteArray(0)
                    }
                )
                parcel.writeInt(bytes.size)
                parcel.writeByteArray(bytes)
            }
        }

    override fun calculateSize(path: String): Long = synchronized(lock) {
        NativeLib.calculateSize(path)
    }

    override fun clearEmptyDirectoriesRecursively(path: String) = synchronized(lock) {
        tryOn {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                clearEmptyDirectoriesRecursivelyApi26(path)
            } else {
                clearEmptyDirectoriesRecursivelyApi24(path)
            }
        }
    }

    private fun clearEmptyDirectoriesRecursivelyApi24(path: String) {
        val dir = File(path)
        if (dir.isDirectory) {
            dir.listFiles()?.also { items ->
                if (items.isEmpty()) {
                    dir.delete()
                } else {
                    items.forEach {
                        clearEmptyDirectoriesRecursivelyApi24(it.absolutePath)
                    }
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun clearEmptyDirectoriesRecursivelyApi26(path: String) {
        Files.walkFileTree(Paths.get(path), object : SimpleFileVisitor<Path>() {
            override fun preVisitDirectory(dir: Path?, attrs: BasicFileAttributes?): FileVisitResult {
                if (dir != null && attrs != null) {
                    if (Files.isDirectory(dir) && Files.list(dir).count() == 0L) {
                        // Empty dir
                        Files.delete(dir)
                    }
                }
                return FileVisitResult.CONTINUE
            }

            override fun visitFile(file: Path?, attrs: BasicFileAttributes?): FileVisitResult {
                return FileVisitResult.CONTINUE
            }

            override fun visitFileFailed(file: Path?, exc: IOException?): FileVisitResult {
                return FileVisitResult.CONTINUE
            }

            override fun postVisitDirectory(dir: Path?, exc: IOException?): FileVisitResult {
                return FileVisitResult.CONTINUE
            }
        })
    }

    override fun setAllPermissions(src: String): Unit = synchronized(lock) { File(src).setAllPermissions() }

    override fun getUidGid(path: String): IntArray = synchronized(lock) {
        NativeLib.getUidGid(path)
    }

    /**
     * AIDL limits transaction to 1M which means it may throw [android.os.TransactionTooLargeException]
     * when the package list is too large. So we just make it parcelable and write into tmp file to avoid that.
     */
    override fun getInstalledPackagesAsUser(flags: Int, userId: Int): ParcelFileDescriptor = synchronized(lock) {
        writeToParcel { parcel ->
            val packages = packageManagerHidden.getInstalledPackagesAsUser(flags, userId)
            parcel.writeTypedList(packages)
        }
    }

    override fun getPackageInfoAsUser(packageName: String, flags: Int, userId: Int): PackageInfo =
        synchronized(lock) { packageManagerHidden.getPackageInfoAsUser(packageName, flags, userId) }

    override fun grantRuntimePermission(packageName: String, permName: String, user: UserHandle) {
        synchronized(lock) {
            packageManagerHidden.grantRuntimePermission(packageName, permName, user)
        }
    }

    override fun revokeRuntimePermission(packageName: String, permName: String, user: UserHandle) {
        synchronized(lock) {
            packageManagerHidden.revokeRuntimePermission(packageName, permName, user)
        }
    }

    override fun getPermissionFlags(packageName: String, permName: String, user: UserHandle) =
        synchronized(lock) { packageManagerHidden.getPermissionFlags(permName, packageName, user) }

    override fun updatePermissionFlags(packageName: String, permName: String, user: UserHandle, flagMask: Int, flagValues: Int) {
        synchronized(lock) {
            packageManagerHidden.updatePermissionFlags(permName, packageName, flagMask, flagValues, user)
        }
    }

    override fun getPackageSourceDir(packageName: String, userId: Int): List<String> = synchronized(lock) {
        runCatching {
            val sourceDirList = mutableListOf<String>()
            val packageInfo = packageManagerHidden.getPackageInfoAsUser(packageName, 0, userId)
            sourceDirList.add(packageInfo.applicationInfo!!.sourceDir)
            val splitSourceDirs = packageInfo.applicationInfo!!.splitSourceDirs
            if (!splitSourceDirs.isNullOrEmpty()) for (i in splitSourceDirs) sourceDirList.add(i)
            sourceDirList
        }.getOrElse { listOf() }
    }

    override fun queryInstalled(packageName: String, userId: Int): Boolean = synchronized(lock) {
        tryWithBoolean {
            packageManagerHidden.getPackageInfoAsUser(packageName, 0, userId)
        }
    }

    override fun getPackageUid(packageName: String, userId: Int): Int = synchronized(lock) {
        runCatching {
            packageManagerHidden.getPackageInfoAsUser(packageName, 0, userId).applicationInfo!!.uid
        }.getOrElse { -1 }
    }

    override fun getUserHandle(userId: Int): UserHandle = synchronized(lock) {
        UserHandleHidden.of(userId)
    }

    private fun queryStatsForPackageApi24(packageInfo: PackageInfo, user: UserHandle): StorageStatsParcelable {
        val userHandle: UserHandleHidden = user.castTo()
        // https://cs.android.com/android/platform/superproject/+/android-8.0.0_r51:frameworks/base/services/core/java/com/android/server/pm/Installer.java;l=225
        // https://cs.android.com/android/platform/superproject/+/android-8.0.0_r51:frameworks/native/cmds/installd/InstalldNativeService.cpp;l=1340
        // https://cs.android.com/android/platform/superproject/+/android-8.0.0_r51:frameworks/base/services/usage/java/com/android/server/usage/StorageStatsService.java;l=439
        // https://cs.android.com/android/platform/superproject/+/android-8.0.0_r51:frameworks/base/core/java/android/app/usage/StorageStats.java;l=31
        val userDir = "${PathUtil.getPackageUserDir(userHandle.identifier)}/${packageInfo.packageName}"
        val userCacheDir = "$userDir/cache"
        val userCodeCacheDir = "$userDir/code_cache"
        val dataDir = "${PathUtil.getPackageDataDir(userHandle.identifier)}/${packageInfo.packageName}"
        val dataCacheDir = "$dataDir/cache"
        val dataCodeCacheDir = "$dataDir/code_cache"
        val obbDir = "${PathUtil.getPackageObbDir(userHandle.identifier)}/${packageInfo.packageName}"
        val obbCacheDir = "$obbDir/cache"
        val obbCodeCacheDir = "$obbDir/code_cache"
        val mediaDir = "${PathUtil.getPackageMediaDir(userHandle.identifier)}/${packageInfo.packageName}"
        val mediaCacheDir = "$mediaDir/cache"
        val mediaCodeCacheDir = "$mediaDir/code_cache"

        val appDirSize = runCatching { FileUtil.calculateSize(PathUtil.getParentPath(packageInfo.applicationInfo!!.sourceDir)) }.getOrElse { 0 }
        val cacheDirSize = FileUtil.calculateSize(userCacheDir) + FileUtil.calculateSize(userCodeCacheDir) +
                FileUtil.calculateSize(dataCacheDir) + FileUtil.calculateSize(dataCodeCacheDir) +
                FileUtil.calculateSize(obbCacheDir) + FileUtil.calculateSize(obbCodeCacheDir) +
                FileUtil.calculateSize(mediaCacheDir) + FileUtil.calculateSize(mediaCodeCacheDir)
        val dataDirSize = FileUtil.calculateSize(userDir) + FileUtil.calculateSize(dataDir) + FileUtil.calculateSize(obbDir) + FileUtil.calculateSize(mediaDir) - cacheDirSize
        return StorageStatsParcelable(
            appDirSize,
            cacheDirSize,
            dataDirSize,
            0,
        )
    }

    @TargetApi(Build.VERSION_CODES.O)
    fun queryStatsForPackageApi26(packageInfo: PackageInfo, user: UserHandle): StorageStatsParcelable {
        val stats = runCatching { getStorageStatsManager().queryStatsForPackage(packageInfo.applicationInfo!!.storageUuid, packageInfo.packageName, user) }.getOrNull()

        return StorageStatsParcelable(
            stats?.appBytes ?: 0,
            stats?.cacheBytes ?: 0,
            stats?.dataBytes ?: 0,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) stats?.externalCacheBytes ?: 0 else 0,
        )
    }

    override fun queryStatsForPackage(packageInfo: PackageInfo, user: UserHandle): StorageStatsParcelable? = synchronized(lock) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                queryStatsForPackageApi26(packageInfo, user)
            } else {
                queryStatsForPackageApi24(packageInfo, user)
            }
        }.getOrNull()
    }

    override fun getUsers(): List<UserInfo> = synchronized(lock) {
        tryOn(
            block = {
                userManager.users
            },
            onException = {
                listOf()
            }
        )
    }

    override fun walkFileTree(path: String): ParcelFileDescriptor = synchronized(lock) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            walkFileTreeApi26(path)
        } else {
            walkFileTreeApi24(path)
        }
    }

    private fun walkFileTreeApi24(path: String): ParcelFileDescriptor {
        fun walkFileTreeRecursively(path: String): List<PathParcelable> {
            val pathParcelableList = mutableListOf<PathParcelable>()
            val file = File(path)
            if (file.isFile) {
                pathParcelableList.add(PathParcelable(file.absolutePath))
            } else if (file.isDirectory) {
                for (item in file.listFiles()!!) {
                    pathParcelableList.addAll(walkFileTreeRecursively(item.absolutePath))
                }
            }
            return pathParcelableList
        }
        return writeToParcel { parcel ->
            parcel.writeTypedList(
                tryOn(
                    block = { walkFileTreeRecursively(path) },
                    onException = {
                        listOf()
                    },
                )
            )
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun walkFileTreeApi26(path: String): ParcelFileDescriptor = writeToParcel { parcel ->
        parcel.writeTypedList(
            tryOn(
                block = {
                    val pathParcelableList = mutableListOf<PathParcelable>()
                    Files.walkFileTree(Paths.get(path), object : SimpleFileVisitor<Path>() {
                        override fun preVisitDirectory(dir: Path?, attrs: BasicFileAttributes?): FileVisitResult {
                            return FileVisitResult.CONTINUE
                        }

                        override fun visitFile(file: Path?, attrs: BasicFileAttributes?): FileVisitResult {
                            if (file != null && attrs != null) {
                                pathParcelableList.add(PathParcelable(file.pathString))
                            }
                            return FileVisitResult.CONTINUE
                        }

                        override fun visitFileFailed(file: Path?, exc: IOException?): FileVisitResult {
                            return FileVisitResult.CONTINUE
                        }

                        override fun postVisitDirectory(dir: Path?, exc: IOException?): FileVisitResult {
                            return FileVisitResult.CONTINUE
                        }
                    })
                    pathParcelableList
                },
                onException = {
                    listOf()
                }
            )
        )
    }

    override fun getPackageArchiveInfo(path: String): PackageInfo? = synchronized(lock) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            systemContext.packageManager.getPackageArchiveInfo(path, PackageInfoFlags.of(PackageManager.GET_ACTIVITIES.toLong()))?.apply {
                applicationInfo?.sourceDir = path
                applicationInfo?.publicSourceDir = path
            }
        } else {
            systemContext.packageManager.getPackageArchiveInfo(path, PackageManager.GET_ACTIVITIES)?.apply {
                applicationInfo?.sourceDir = path
                applicationInfo?.publicSourceDir = path
            }
        }
    }

    override fun getPackageSsaidAsUser(packageName: String, uid: Int, userId: Int): String? = synchronized(lock) { SsaidUtil(userId).getSsaid(packageName, uid) }
    override fun setPackageSsaidAsUser(packageName: String, uid: Int, userId: Int, ssaid: String) {
        synchronized(lock) { SsaidUtil(userId).setSsaid(packageName, uid, ssaid) }
    }

    override fun setDisplayPowerMode(mode: Int) {
        synchronized(lock) {
            val physicalDisplayIds: LongArray = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                DisplayControl.getPhysicalDisplayIds()
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                SurfaceControlHidden.getPhysicalDisplayIds()
            } else {
                LongArray(1) { 0L }
            }
            physicalDisplayIds.forEach { id ->
                val token = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    DisplayControl.getPhysicalDisplayToken(id)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    SurfaceControlHidden.getPhysicalDisplayToken(id)
                } else {
                    SurfaceControlHidden.getBuiltInDisplay(id.toInt())
                }
                SurfaceControlHidden.setDisplayPowerMode(token, mode)
            }
        }
    }

    override fun getScreenOffTimeout(): Int = synchronized(lock) {
        ShellUtils.fastCmd("settings get system screen_off_timeout").toIntOrNull() ?: DEFAULT_IDLE_TIMEOUT
    }

    override fun setScreenOffTimeout(timeout: Int): Unit = synchronized(lock) {
        ShellUtils.fastCmd("settings put system screen_off_timeout $timeout")
    }

    override fun forceStopPackageAsUser(packageName: String, userId: Int) = synchronized(lock) {
        activityManager.forceStopPackageAsUser(packageName, userId)
    }

    override fun setApplicationEnabledSetting(packageName: String, newState: Int, flags: Int, userId: Int, callingPackage: String?) = synchronized(lock) {
        packageManagerService.setApplicationEnabledSetting(packageName, newState, flags, userId, callingPackage)
    }

    override fun getApplicationEnabledSetting(packageName: String, userId: Int): Int = synchronized(lock) {
        packageManagerService.getApplicationEnabledSetting(packageName, userId)
    }

    override fun getPermissions(packageInfo: PackageInfo): List<PackagePermission> = synchronized(lock) {
        val permissions = mutableListOf<PackagePermission>()
        val uid = packageInfo.applicationInfo?.uid ?: -1
        val packageName = packageInfo.packageName
        val requestedPermissions = packageInfo.requestedPermissions?.toList() ?: listOf()
        val requestedPermissionsFlags = packageInfo.requestedPermissionsFlags?.toList() ?: listOf()
        val ops = runCatching {
            appOpsManager.getOpsForPackage(uid, packageName, null).getOrNull(0)?.ops?.associate {
                it.op to it.mode
            }
        }.getOrNull()
        requestedPermissions.forEachIndexed { i, name ->
            runCatching {
                val permissionInfo = packageManager.getPermissionInfo(name, 0)
                val protection = PermissionInfoCompat.getProtection(permissionInfo)
                val protectionFlags = PermissionInfoCompat.getProtectionFlags(permissionInfo)
                val isGranted = (requestedPermissionsFlags[i] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
                val op = AppOpsManagerHidden.permissionToOpCode(name)
                val mode = ops?.get(op) ?: AppOpsManager.MODE_IGNORED
                if ((op != AppOpsManagerHidden.OP_NONE)
                    || (protection == PermissionInfo.PROTECTION_DANGEROUS || (protectionFlags and PermissionInfo.PROTECTION_FLAG_DEVELOPMENT) != 0)
                ) {
                    permissions.add(PackagePermission(name, isGranted, op, mode))
                }
            }
        }
        permissions
    }

    override fun setOpsMode(code: Int, uid: Int, packageName: String?, mode: Int) = synchronized(lock) {
        appOpsManager.setMode(code, uid, packageName, mode)
    }

    override fun calculateMD5(src: String): String = synchronized(lock) { HashUtil.calculateMD5(src) }
}
