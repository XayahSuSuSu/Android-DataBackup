package com.xayah.core.util

import android.annotation.SuppressLint
import android.content.Context
import com.xayah.core.datastore.readBackupSavePath
import com.xayah.core.datastore.readRestoreSavePath
import com.xayah.core.util.command.SELinux
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.nio.file.Paths
import javax.inject.Inject
import kotlin.io.path.pathString

const val LogRelativeDir = "log"
const val IconRelativeDir = "icon"
const val BinRelativeDir = "bin"
const val TmpRelativeDir = "tmp"
const val ApksRelativeDir = "apks"
const val MountsRelativeDir = "mounts"
const val ArchivesRelativeDir = "archives"
const val PackagesRelativeDir = "packages"
const val MediumRelativeDir = "medium"
const val ConfigsRelativeDir = "configs"
const val ConfigsPackageRestoreName = "package_restore_config.pb"
const val ConfigsMediaRestoreName = "media_restore_config.pb"
const val BinArchiveName = "bin.zip"
const val CloudTmpTestFileName = "DataBackupCloudTmpTest"
const val CloudTmpRelativeDir = "DataBackupTmpDir"

fun Context.filesDir(): String = filesDir.path
fun Context.logDir(): String = "${filesDir()}/$LogRelativeDir"
fun Context.binDir(): String = "${filesDir()}/$BinRelativeDir"
fun Context.binArchivePath(): String = "${filesDir()}/$BinArchiveName"
fun Context.iconDir(): String = "${filesDir()}/$IconRelativeDir"
fun Context.tmpDir(): String = "${filesDir()}/$TmpRelativeDir"
fun Context.tmpApksDir(): String = "${filesDir()}/$TmpRelativeDir/$ApksRelativeDir"
fun Context.tmpMountsDir(): String = "${filesDir()}/$TmpRelativeDir/$MountsRelativeDir"
fun Context.localBackupSaveDir(): String = runBlocking { readBackupSavePath().first() }
fun Context.localRestoreSaveDir(): String = runBlocking { readRestoreSavePath().first() }
fun Context.cloudTmpAbsoluteDir(): String = "${filesDir()}/$CloudTmpRelativeDir"

class PathUtil @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        fun getParentPath(path: String): String = Paths.get(path).parent.pathString
        fun getFileName(path: String): String = Paths.get(path).fileName.pathString

        // Paths for processing.
        @SuppressLint("SdCardPath")
        fun getPackageUserDir(userId: Int): String = "/data/user/${userId}"
        fun getPackageUserDeDir(userId: Int): String = "/data/user_de/${userId}"
        fun getPackageDataDir(userId: Int): String = "/data/media/${userId}/Android/data"
        fun getPackageObbDir(userId: Int): String = "/data/media/${userId}/Android/obb"
        fun getPackageMediaDir(userId: Int): String = "/data/media/${userId}/Android/media"

        fun getPackageIconRelativePath(packageName: String): String = "${packageName}.png"
        fun getConfigsRelativeDir(): String = ConfigsRelativeDir
        fun getArchivesRelativeDir(): String = ArchivesRelativeDir
        fun getArchivesPackagesRelativeDir(): String = "$ArchivesRelativeDir/$PackagesRelativeDir"
        fun getArchivesMediumRelativeDir(): String = "$ArchivesRelativeDir/$MediumRelativeDir"

        fun getMediaRestoreConfigDst(dstDir: String): String = "${dstDir}/$ConfigsMediaRestoreName"
        fun getPackageRestoreConfigDst(dstDir: String): String = "${dstDir}/$ConfigsPackageRestoreName"

        suspend fun setFilesDirSELinux(context: Context) = SELinux.getContext(path = context.filesDir()).also { result ->
            val pathContext = if (result.isSuccess) result.outString else ""
            SELinux.chcon(context = pathContext, path = context.filesDir())
            SELinux.chown(uid = context.applicationInfo.uid, path = context.filesDir())
        }
    }

    fun getPackageIconPath(packageName: String): String = "${context.iconDir()}/${getPackageIconRelativePath(packageName)}"
    fun getConfigsDir(parent: String): String = "${parent}/${getConfigsRelativeDir()}"
    fun getLocalBackupConfigsDir(): String = getConfigsDir(parent = context.localBackupSaveDir())
    fun getLocalRestoreConfigsDir(): String = getConfigsDir(parent = context.localRestoreSaveDir())
    fun getCloudTmpConfigsDir(): String = getConfigsDir(parent = context.cloudTmpAbsoluteDir())
    fun getArchivesDir(parent: String): String = "${parent}/${getArchivesRelativeDir()}"
    fun getLocalBackupArchivesDir(): String = getArchivesDir(parent = context.localBackupSaveDir())
    fun getArchivesPackagesDir(parent: String): String = "${parent}/${getArchivesPackagesRelativeDir()}"
    fun getLocalBackupArchivesPackagesDir(): String = getArchivesPackagesDir(parent = context.localBackupSaveDir())
    fun getLocalRestoreArchivesPackagesDir(): String = getArchivesPackagesDir(parent = context.localRestoreSaveDir())
    fun getCloudTmpArchivesPackagesDir(): String = getArchivesPackagesDir(parent = context.cloudTmpAbsoluteDir())
    fun getArchivesMediumDir(parent: String): String = "${parent}/${getArchivesMediumRelativeDir()}"
    fun getLocalBackupArchivesMediumDir(): String = getArchivesMediumDir(parent = context.localBackupSaveDir())
    fun getLocalRestoreArchivesMediumDir(): String = getArchivesMediumDir(parent = context.localRestoreSaveDir())

    fun getTmpApkPath(packageName: String): String = "${context.tmpApksDir()}/$packageName"
    fun getTmpMountPath(name: String): String = "${context.tmpMountsDir()}/$name"
}
