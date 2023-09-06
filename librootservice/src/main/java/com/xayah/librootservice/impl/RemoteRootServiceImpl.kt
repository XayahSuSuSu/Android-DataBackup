package com.xayah.librootservice.impl

import android.app.ActivityThreadHidden
import android.content.Context
import android.content.pm.PackageManagerHidden
import android.os.Parcel
import android.os.ParcelFileDescriptor
import android.os.StatFs
import com.xayah.libhiddenapi.HiddenApiBypassUtil
import com.xayah.librootservice.IRemoteRootService
import com.xayah.librootservice.parcelables.StatFsParcelable
import com.xayah.librootservice.util.ExceptionUtil.tryOn
import com.xayah.librootservice.util.ExceptionUtil.tryWithBoolean
import java.io.File

internal class RemoteRootServiceImpl : IRemoteRootService.Stub() {
    companion object {
        const val ParcelTmpFilePath = "/data/local/tmp"
        const val ParcelTmpFileName = "data_backup_tmp"
    }

    private val lock = Any()
    private var systemContext: Context

    private fun getSystemContext(): Context {
        val activityThread = ActivityThreadHidden.systemMain()
        return ActivityThreadHidden.getSystemContext(activityThread)
    }

    init {
        HiddenApiBypassUtil.addHiddenApiExemptions("")
        systemContext = getSystemContext()
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
}
