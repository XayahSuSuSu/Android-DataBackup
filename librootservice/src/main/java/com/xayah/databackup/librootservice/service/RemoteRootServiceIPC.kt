package com.xayah.databackup.librootservice.service

import android.annotation.SuppressLint
import android.app.ActivityThreadHidden
import android.app.usage.StorageStats
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManagerHidden
import android.content.pm.UserInfo
import android.os.*
import com.xayah.databackup.libhiddenapi.HiddenApiBypassUtil
import com.xayah.databackup.librootservice.IRemoteRootService
import com.xayah.databackup.librootservice.parcelables.StatFsParcelable
import java.io.*
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.io.path.pathString

@SuppressLint("NewApi", "PrivateApi")
@Suppress("UNCHECKED_CAST")
class RemoteRootServiceIPC : IRemoteRootService.Stub() {
    private lateinit var systemContext: Context
    private lateinit var serviceManager: IBinder
    private lateinit var userManager: UserManager
    private lateinit var storageStatsManager: StorageStatsManager
    private lateinit var actionLogFile: File
    private val packageInfoQueue: Queue<PackageInfo> = LinkedList()
    private val queuePollMaxSize = 50

    private lateinit var memoryFile: MemoryFile

    /**
     * 获取systemContext
     */
    private fun getSystemContext(): Context {
        val activityThread = ActivityThreadHidden.systemMain()
        return ActivityThreadHidden.getSystemContext(activityThread)
    }

    private fun getServiceManager(): IBinder {
        return ServiceManagerHidden.getService("package")
    }

    private fun getUserManager(): UserManager {
        return UserManagerHidden.get(systemContext)
    }

    private fun getStorageStatsManager(): StorageStatsManager {
        return systemContext.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
    }

    init {
        initializeService()
    }

    override fun checkConnection(): Boolean {
        return true
    }

    override fun exists(path: String): Boolean {
        return try {
            File(path).exists()
        } catch (e: Exception) {
            false
        }
    }

    override fun createNewFile(path: String): Boolean {
        return try {
            File(path).createNewFile()
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun deleteRecursively(path: String): Boolean {
        return try {
            File(path).deleteRecursively()
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun listFilesPath(path: String): MutableList<String> {
        return try {
            val list = mutableListOf<String>()
            for (i in File(path).listFiles()!!) {
                list.add(i.path)
            }
            list
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    override fun mkdirs(path: String): Boolean {
        return try {
            val file = File(path)
            if (file.exists().not()) file.mkdirs() else true
        } catch (e: Exception) {
            false
        }
    }

    override fun copyTo(path: String, targetPath: String, overwrite: Boolean): Boolean {
        return try {
            mkdirs(Paths.get(path).parent.pathString)
            val file = File(path)
            file.copyTo(File(targetPath), overwrite)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun countFiles(path: String): Int {
        return try {
            File(path).listFiles()!!.size
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Count size of files/directories
     */
    override fun countSize(path: String, regex: String): Long {
        val size = AtomicLong(0)
        try {
            Files.walkFileTree(Paths.get(path), object : SimpleFileVisitor<Path>() {
                override fun visitFile(file: Path?, attrs: BasicFileAttributes?): FileVisitResult {
                    if (file != null && attrs != null) {
                        if (regex.isEmpty() || Regex(regex).matches(file.fileName.pathString))
                            size.addAndGet(attrs.size())
                    }
                    return FileVisitResult.CONTINUE
                }

                override fun preVisitDirectory(dir: Path?, attrs: BasicFileAttributes?): FileVisitResult {
                    return FileVisitResult.CONTINUE
                }

                override fun visitFileFailed(file: Path?, exc: IOException?): FileVisitResult {
                    return FileVisitResult.CONTINUE
                }

                override fun postVisitDirectory(dir: Path?, exc: IOException?): FileVisitResult {
                    return FileVisitResult.CONTINUE
                }
            })
        } catch (_: Exception) {
        }
        return size.get()
    }

    override fun readText(path: String): String {
        return try {
            val file = File(path)
            file.readText()
        } catch (e: Exception) {
            ""
        }
    }

    override fun readBytes(path: String): ByteArray {
        return try {
            val file = File(path)
            file.readBytes()
        } catch (e: Exception) {
            ByteArray(0)
        }
    }

    override fun readByDescriptor(path: String): ParcelFileDescriptor {
        synchronized(this) {
            val bytes = File(path).readBytes()
            memoryFile = MemoryFile("memoryFileDataBackupRead", bytes.size).apply {
                writeBytes(bytes, 0, 0, bytes.size)
            }
            return ParcelFileDescriptor.dup(MemoryFileHidden.getFileDescriptor(memoryFile))
        }
    }

    override fun closeMemoryFile(): Boolean {
        return try {
            memoryFile.close()
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun writeText(path: String, text: String): Boolean {
        return try {
            val file = File(path)
            file.writeText(text)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun writeBytes(path: String, bytes: ByteArray): Boolean {
        return try {
            val fileOutputStream = FileOutputStream(path)
            fileOutputStream.write(bytes)
            fileOutputStream.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun writeByDescriptor(path: String, descriptor: ParcelFileDescriptor): Boolean {
        return try {
            val fileInputStream = FileInputStream(descriptor.fileDescriptor)
            val fileOutputStream = FileOutputStream(path)
            fileOutputStream.write(fileInputStream.readBytes())
            fileInputStream.close()
            fileOutputStream.close()
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun initActionLogFile(path: String): Boolean {
        return try {
            mkdirs(Paths.get(path).parent.pathString)
            actionLogFile = File(path)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun appendActionLog(text: String): Boolean {
        return try {
            actionLogFile.appendText(text)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun readStatFs(path: String): StatFsParcelable {
        val statFs = StatFs(path)
        return StatFsParcelable(statFs.availableBytes, statFs.totalBytes)
    }

    override fun initializeService() {
        HiddenApiBypassUtil.addHiddenApiExemptions("")
        systemContext = getSystemContext()
        serviceManager = getServiceManager()
        userManager = getUserManager()
        storageStatsManager = getStorageStatsManager()
    }

    override fun getUserHandle(userId: Int): UserHandle {
        return UserHandleHidden.of(userId)
    }

    override fun getUsers(): MutableList<UserInfo> {
        var users = mutableListOf<UserInfo>()
        try {
            users = UserManagerHidden.getUsers(userManager = userManager).toMutableList()
        } catch (_: Exception) {
        }
        return users
    }

    override fun offerInstalledPackagesAsUser(flags: Int, userId: Int): Boolean {
        return try {
            packageInfoQueue.clear()
            PackageManagerHidden.getInstalledPackagesAsUser(systemContext.packageManager, flags, userId).forEach {
                packageInfoQueue.offer(it)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun pollInstalledPackages(): MutableList<PackageInfo> {
        val packages = mutableListOf<PackageInfo>()
        for (i in 0 until queuePollMaxSize) {
            val packageInfo = packageInfoQueue.poll()
            if (packageInfo != null) {
                packages.add(packageInfo)
            } else {
                break
            }
        }
        return packages
    }

    /**
     * Normally there aren't too many suspended packages, so queue solution is not necessary.
     */
    override fun getSuspendedPackages(): MutableList<PackageInfo> {
        val packages = mutableListOf<PackageInfo>()
        try {
            for (userId in UserManagerHidden.getUsers(userManager = userManager).toMutableList()) {
                PackageManagerHidden.getInstalledPackagesAsUser(systemContext.packageManager, 0, userId.id).forEach {
                    if (PackageManagerHidden.isPackageSuspended(systemContext.packageManager, it.packageName))
                        packages.add(it)
                }
            }
        } catch (_: Exception) {
        }
        return packages
    }

    override fun queryInstalled(packageName: String, userId: Int): Boolean {
        return try {
            PackageManagerHidden.getPackageInfoAsUser(systemContext.packageManager, packageName, 0, userId)
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun queryStatsForPackage(packageInfo: PackageInfo, user: UserHandle): StorageStats {
        return storageStatsManager.queryStatsForPackage(
            packageInfo.applicationInfo.storageUuid,
            packageInfo.packageName,
            user
        )
    }

    override fun grantRuntimePermission(packageName: String, permName: String, userId: Int): Boolean {
        return try {
            PackageManagerHidden.grantRuntimePermission(systemContext.packageManager, packageName, permName, getUserHandle(userId))
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun displayPackageFilePath(packageName: String, userId: Int): List<String> {
        return try {
            val list = mutableListOf<String>()
            val packageInfo = PackageManagerHidden.getPackageInfoAsUser(systemContext.packageManager, packageName, 0, userId)
            list.add(packageInfo.applicationInfo.sourceDir)
            val splitSourceDirs = packageInfo.applicationInfo.splitSourceDirs
            if (splitSourceDirs != null && splitSourceDirs.isNotEmpty())
                for (i in splitSourceDirs)
                    list.add(i)
            list
        } catch (_: Exception) {
            listOf()
        }
    }

    override fun setPackagesSuspended(packageNames: Array<String>, suspended: Boolean): Boolean {
        return try {
            PackageManagerHidden.setPackagesSuspended(systemContext.packageManager, packageNames, suspended, null, null, null)
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun getPackageUid(packageName: String, userId: Int): Int {
        return try {
            val packageInfo = PackageManagerHidden.getPackageInfoAsUser(systemContext.packageManager, packageName, 0, userId)
            packageInfo.applicationInfo.uid
        } catch (_: Exception) {
            -1
        }
    }

    override fun getPackageLongVersionCode(packageName: String, userId: Int): Long {
        return try {
            val packageInfo = PackageManagerHidden.getPackageInfoAsUser(systemContext.packageManager, packageName, 0, userId)
            packageInfo.longVersionCode
        } catch (_: Exception) {
            -1L
        }
    }
}
