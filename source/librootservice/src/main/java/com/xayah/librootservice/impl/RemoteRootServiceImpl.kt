package com.xayah.librootservice.impl

import android.app.ActivityThreadHidden
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
import android.os.UserManager
import android.os.UserManagerHidden
import com.topjohnwu.superuser.ShellUtils
import com.xayah.libhiddenapi.HiddenApiBypassUtil
import com.xayah.librootservice.IRemoteRootService
import com.xayah.librootservice.parcelables.PathParcelable
import com.xayah.librootservice.parcelables.StatFsParcelable
import com.xayah.librootservice.util.ExceptionUtil.tryOn
import com.xayah.librootservice.util.ExceptionUtil.tryWithBoolean
import java.io.File
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.atomic.AtomicLong
import kotlin.io.path.name
import kotlin.io.path.pathString

internal class RemoteRootServiceImpl : IRemoteRootService.Stub() {
    companion object {
        const val ParcelTmpFilePath = "/data/local/tmp"
        const val ParcelTmpFileName = "data_backup_tmp"
    }

    private val lock = Any()
    private var systemContext: Context
    private var storageStatsManager: StorageStatsManager
    private var userManager: UserManager

    private fun getSystemContext(): Context = ActivityThreadHidden.getSystemContext(ActivityThreadHidden.systemMain())

    private fun getStorageStatsManager(): StorageStatsManager = systemContext.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager

    private fun getUserManager(): UserManager = UserManagerHidden.get(systemContext)

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

        HiddenApiBypassUtil.addHiddenApiExemptions("")
        systemContext = getSystemContext()
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

    override fun exists(path: String): Boolean = synchronized(lock) {
        tryOn(block = { File(path).exists() }, onException = { false })
    }

    override fun createNewFile(path: String): Boolean = synchronized(lock) {
        tryOn(block = { File(path).createNewFile() }, onException = { false })
    }

    override fun deleteRecursively(path: String): Boolean = synchronized(lock) {
        tryOn(block = { File(path).deleteRecursively() }, onException = { false })
    }

    override fun listFilePaths(path: String): List<String> = synchronized(lock) {
        tryOn(
            block = {
                File(path).listFiles()!!.map { it.path }
            },
            onException = {
                listOf()
            }
        )
    }

    override fun readText(path: String): ParcelFileDescriptor = synchronized(lock) {
        val parcel = Parcel.obtain()
        parcel.setDataPosition(0)

        val text = tryOn(
            block = {
                File(path).readText()
            },
            onException = {
                ""
            }
        )
        parcel.writeString(text)

        val tmp = File(ParcelTmpFilePath, ParcelTmpFileName)
        tmp.createNewFile()
        tmp.writeBytes(parcel.marshall())
        val pfd = ParcelFileDescriptor.open(tmp, ParcelFileDescriptor.MODE_READ_WRITE)
        tmp.deleteRecursively()

        parcel.recycle()
        pfd
    }

    override fun calculateSize(path: String): Long  = synchronized(lock) {
        val size = AtomicLong(0)
        tryOn {
            Files.walkFileTree(Paths.get(path), object : SimpleFileVisitor<Path>() {
                override fun preVisitDirectory(dir: Path?, attrs: BasicFileAttributes?): FileVisitResult {
                    return FileVisitResult.CONTINUE
                }

                override fun visitFile(file: Path?, attrs: BasicFileAttributes?): FileVisitResult {
                    if (file != null && attrs != null) {
                        size.addAndGet(attrs.size())
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
        }
        size.get()
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

    /**
     * AIDL limits transaction to 1M which means it may throw [android.os.TransactionTooLargeException]
     * when the package list is too large. So we just make it parcelable and write into tmp file to avoid that.
     */
    override fun getInstalledPackagesAsUser(flags: Int, userId: Int): ParcelFileDescriptor = synchronized(lock) {
        val parcel = Parcel.obtain()
        parcel.setDataPosition(0)

        val packages = PackageManagerHidden.getInstalledPackagesAsUser(systemContext.packageManager, flags, userId)
        parcel.writeTypedList(packages)

        val tmp = File(ParcelTmpFilePath, ParcelTmpFileName)
        tmp.createNewFile()
        tmp.writeBytes(parcel.marshall())
        val pfd = ParcelFileDescriptor.open(tmp, ParcelFileDescriptor.MODE_READ_WRITE)
        tmp.deleteRecursively()

        parcel.recycle()
        pfd
    }


    override fun getPackageSourceDir(packageName: String, userId: Int): List<String> = synchronized(lock) {
        tryOn(
            block = {
                val sourceDirList = mutableListOf<String>()
                val packageInfo = PackageManagerHidden.getPackageInfoAsUser(systemContext.packageManager, packageName, 0, userId)
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
            PackageManagerHidden.getPackageInfoAsUser(systemContext.packageManager, packageName, 0, userId)
        }
    }

    override fun getPackageUid(packageName: String, userId: Int): Int = synchronized(lock) {
        tryOn(
            block = {
                PackageManagerHidden.getPackageInfoAsUser(systemContext.packageManager, packageName, 0, userId).applicationInfo.uid
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
                UserManagerHidden.getUsers(userManager = userManager)
            },
            onException = {
                listOf()
            }
        )
    }

    override fun walkFileTree(path: String): ParcelFileDescriptor = synchronized(lock) {
        val parcel = Parcel.obtain()
        parcel.setDataPosition(0)

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
                                val pathList = file.pathString.split("/")
                                val pathString = file.pathString
                                val nameWithoutExtension = file.name.split(".").first()
                                val extension = file.name.replace("${nameWithoutExtension}.", "")
                                pathParcelableList.add(PathParcelable(pathList, pathString, nameWithoutExtension, extension))
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

        val tmp = File(ParcelTmpFilePath, ParcelTmpFileName)
        tmp.createNewFile()
        tmp.writeBytes(parcel.marshall())
        val pfd = ParcelFileDescriptor.open(tmp, ParcelFileDescriptor.MODE_READ_WRITE)
        tmp.deleteRecursively()

        parcel.recycle()
        pfd
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
}
