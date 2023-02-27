package com.xayah.librootservice.service

import android.annotation.SuppressLint
import android.app.usage.StorageStats
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.UserInfo
import android.os.IBinder
import android.os.UserHandle
import android.os.UserManager
import com.xayah.librootservice.IRemoteRootService
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.io.*
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.io.path.pathString

@SuppressLint("NewApi", "PrivateApi")
class RemoteRootServiceIPC : IRemoteRootService.Stub() {
    private lateinit var systemContext: Context
    private lateinit var serviceManager: IBinder
    private lateinit var userManager: UserManager
    private lateinit var storageStatsManager: StorageStatsManager
    private lateinit var actionLogFile: File
    private val packageInfoQueue: Queue<PackageInfo> = LinkedList()
    private val queuePollMaxSize = 50

    /**
     * 获取systemContext
     */
    private fun getSystemContext(): Context {
        val activityThread =
            HiddenApiBypass.invoke(Class.forName("android.app.ActivityThread"), null, "systemMain")
        return HiddenApiBypass.invoke(
            Class.forName("android.app.ActivityThread"),
            activityThread,
            "getSystemContext"
        ) as Context
    }

    private fun getServiceManager(): IBinder {
        return HiddenApiBypass.invoke(
            Class.forName("android.os.ServiceManager"),
            null,
            "getService",
            "package"
        ) as IBinder
    }

    private fun getUserManager(): UserManager {
        return HiddenApiBypass.invoke(
            Class.forName("android.os.UserManager"),
            null,
            "get",
            systemContext
        ) as UserManager
    }

    private fun getStorageStatsManager(): StorageStatsManager {
        return systemContext.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
    }

    init {
        initializeService()
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

    override fun mkdirs(path: String): Boolean {
        return try {
            val file = File(path)
            if (file.exists().not()) file.mkdirs() else true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 计算文件/文件夹大小
     */
    override fun countSize(path: String): Long {
        val size = AtomicLong(0)
        try {
            Files.walkFileTree(Paths.get(path), object : SimpleFileVisitor<Path>() {
                override fun visitFile(file: Path?, attrs: BasicFileAttributes?): FileVisitResult {
                    if (attrs != null) {
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

    override fun initializeService() {
        HiddenApiBypass.addHiddenApiExemptions("")
        systemContext = getSystemContext()
        serviceManager = getServiceManager()
        userManager = getUserManager()
        storageStatsManager = getStorageStatsManager()
    }

    override fun getUserHandle(userId: Int): UserHandle {
        return HiddenApiBypass.invoke(
            Class.forName("android.os.UserHandle"),
            null,
            "of",
            userId
        ) as UserHandle
    }

    @SuppressLint("NewApi")
    @Suppress("UNCHECKED_CAST")
    override fun getUsers(
        excludePartial: Boolean,
        excludeDying: Boolean,
        excludePreCreated: Boolean
    ): MutableList<UserInfo> {
        var users = mutableListOf<UserInfo>()
        try {
            users = (HiddenApiBypass.invoke(
                Class.forName("android.os.UserManager"),
                userManager,
                "getUsers",
                true,
                false,
                true
            ) as List<UserInfo>).toMutableList()
        } catch (_: Exception) {
        }
        return users
    }

    @Suppress("UNCHECKED_CAST")
    override fun offerInstalledPackagesAsUser(flags: Int, userId: Int): Boolean {
        return try {
            packageInfoQueue.clear()
            (HiddenApiBypass.invoke(
                Class.forName("android.content.pm.PackageManager"),
                systemContext.packageManager,
                "getInstalledPackagesAsUser",
                flags,
                userId
            ) as List<PackageInfo>).forEach {
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

    override fun queryStatsForPackage(packageInfo: PackageInfo, user: UserHandle): StorageStats {
        return storageStatsManager.queryStatsForPackage(
            packageInfo.applicationInfo.storageUuid,
            packageInfo.packageName,
            user
        )
    }
}
