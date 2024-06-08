package com.xayah.core.rootservice.impl

import android.app.ActivityThread
import android.app.usage.StorageStats
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PackageInfoFlags
import android.content.pm.PackageManagerHidden
import android.content.pm.UserInfo
import android.os.Build
import android.os.Parcel
import android.os.ParcelFileDescriptor
import android.os.StatFs
import android.os.UserHandle
import android.os.UserHandleHidden
import android.os.UserManagerHidden
import android.view.SurfaceControlHidden
import com.android.server.display.DisplayControl
import com.topjohnwu.superuser.ShellUtils
import com.xayah.core.hiddenapi.castTo
import com.xayah.core.rootservice.IRemoteRootService
import com.xayah.core.rootservice.parcelables.PathParcelable
import com.xayah.core.rootservice.parcelables.StatFsParcelable
import com.xayah.core.rootservice.util.ExceptionUtil.tryOn
import com.xayah.core.rootservice.util.ExceptionUtil.tryWithBoolean
import com.xayah.core.rootservice.util.SsaidUtil
import com.xayah.core.util.FileUtil
import com.xayah.core.util.HashUtil
import com.xayah.core.util.command.BaseUtil.setAllPermissions
import java.io.File
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.pathString

internal class RemoteRootServiceImpl : IRemoteRootService.Stub() {
    companion object {
        const val ParcelTmpFilePath = "/data/local/tmp"
        const val ParcelTmpFileName = "data_backup_tmp"
    }

    private val lock = Any()
    private var systemContext: Context
    private var packageManager: PackageManager
    private var packageManagerHidden: PackageManagerHidden
    private var storageStatsManager: StorageStatsManager
    private var userManager: UserManagerHidden

    private fun getSystemContext(): Context = ActivityThread.systemMain().systemContext

    private fun getStorageStatsManager(): StorageStatsManager = systemContext.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager

    private fun getUserManager(): UserManagerHidden = UserManagerHidden.get(systemContext).castTo()

    init {
        /**
         * If [ParcelTmpFilePath] has incorrect SELinux context, the transaction will get failed:
         * Fatal Exception: android.os.DeadObjectException: Transaction failed on small parcel; remote process probably died, but this could also be caused by running out of binder buffe
         * Correct SELinux context should be: u:object_r:shell_data_file:s0
         *
         * If [ParcelTmpFilePath] doesn't exist, the transaction will failed:
         * pfd must not be null
         */
        ShellUtils.fastCmd(
            """
            mkdir "$ParcelTmpFilePath/"
            """.trimIndent()
        )
        ShellUtils.fastCmd(
            """
            chcon -hR "u:object_r:shell_data_file:s0" "$ParcelTmpFilePath/"
            """.trimIndent()
        )

        systemContext = getSystemContext()
        packageManager = systemContext.packageManager
        packageManagerHidden = packageManager.castTo()
        storageStatsManager = getStorageStatsManager()
        userManager = getUserManager()
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

    private fun writeToParcel(onWrite: (Parcel) -> Unit) = run {
        val parcel = Parcel.obtain()
        parcel.setDataPosition(0)

        onWrite(parcel)

        val tmp = File(ParcelTmpFilePath, ParcelTmpFileName)
        tmp.createNewFile()
        tmp.writeBytes(parcel.marshall())
        val pfd = ParcelFileDescriptor.open(tmp, ParcelFileDescriptor.MODE_READ_WRITE)
        tmp.deleteRecursively()

        parcel.recycle()
        pfd
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
        FileUtil.calculateSize(path = path)
    }

    override fun clearEmptyDirectoriesRecursively(path: String) = synchronized(lock) {
        tryOn {
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
    }

    override fun setAllPermissions(src: String): Unit = synchronized(lock) { File(src).setAllPermissions() }

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
        tryOn(
            block = {
                val sourceDirList = mutableListOf<String>()
                val packageInfo = packageManagerHidden.getPackageInfoAsUser(packageName, 0, userId)
                sourceDirList.add(packageInfo.applicationInfo.sourceDir)
                val splitSourceDirs = packageInfo.applicationInfo.splitSourceDirs
                if (!splitSourceDirs.isNullOrEmpty()) for (i in splitSourceDirs) sourceDirList.add(i)
                sourceDirList
            },
            onException = { listOf() }
        )
    }

    override fun queryInstalled(packageName: String, userId: Int): Boolean = synchronized(lock) {
        tryWithBoolean {
            packageManagerHidden.getPackageInfoAsUser(packageName, 0, userId)
        }
    }

    override fun getPackageUid(packageName: String, userId: Int): Int = synchronized(lock) {
        tryOn(
            block = {
                packageManagerHidden.getPackageInfoAsUser(packageName, 0, userId).applicationInfo.uid
            },
            onException = {
                -1
            }
        )
    }

    override fun getUserHandle(userId: Int): UserHandle = synchronized(lock) {
        UserHandleHidden.of(userId)
    }

    override fun queryStatsForPackage(packageInfo: PackageInfo, user: UserHandle): StorageStats? = synchronized(lock) {
        tryOn(
            block = {
                storageStatsManager.queryStatsForPackage(packageInfo.applicationInfo.storageUuid, packageInfo.packageName, user)
            },
            onException = {
                null
            }
        )
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
        writeToParcel { parcel ->
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
        ShellUtils.fastCmd("settings get system screen_off_timeout").toIntOrNull() ?: 30000
    }

    override fun setScreenOffTimeout(timeout: Int): Unit = synchronized(lock) {
        ShellUtils.fastCmd("settings put system screen_off_timeout $timeout")
    }

    override fun calculateMD5(src: String): String = synchronized(lock) { HashUtil.calculateMD5(src) }
}
