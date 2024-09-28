package com.xayah.core.util

import android.annotation.SuppressLint
import android.content.Context
import com.xayah.core.datastore.readBackupSavePath
import com.xayah.core.util.command.SELinux
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

const val LogRelativeDir = "log"
const val IconRelativeDir = "icon"
const val BinRelativeDir = "bin"
const val TmpRelativeDir = "tmp"
const val ApksRelativeDir = "apks"
const val AppsRelativeDir = "apps"
const val FilesRelativeDir = "files"
const val ConfigsRelativeDir = "configs"
const val ConfigsPackageRestoreName = "package_restore_config.json"
const val ConfigsMediaRestoreName = "media_restore_config.json"
const val ConfigsConfigurationsName = "configurations.json"
const val BinArchiveName = "bin.zip"
const val CloudTmpRelativeDir = "DataBackupTmpDir"

fun Context.filesDir(): String = filesDir.path
fun Context.logDir(): String = "${filesDir()}/$LogRelativeDir"
fun Context.binDir(): String = "${filesDir()}/$BinRelativeDir"
fun Context.binArchivePath(): String = "${filesDir()}/$BinArchiveName"
fun Context.iconDir(): String = "${filesDir()}/$IconRelativeDir"
fun Context.tmpApksDir(): String = "${filesDir()}/$TmpRelativeDir/$ApksRelativeDir"
fun Context.localBackupSaveDir(): String = runBlocking { readBackupSavePath().first() }
fun Context.cloudTmpAbsoluteDir(): String = "${filesDir()}/$CloudTmpRelativeDir"

class PathUtil @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        /**
         * Returns the parent path, or empty string if this path does not have a parent.
         */
        fun getParentPath(path: String): String {
            if (path.contains('/').not() || path == "/") return ""
            val child = path.substring(path.lastIndexOf('/'))
            return path.replace(child, "")
        }

        /**
         * Returns the name of the file or directory denoted by this path, or empty string if this path has zero elements.
         */
        fun getFileName(path: String): String {
            if (path.isEmpty()) return ""
            return path.substring(path.lastIndexOf('/') + 1)
        }

        // Paths for processing.
        @SuppressLint("SdCardPath")
        fun getPackageUserDir(userId: Int): String = "/data/user/${userId}"
        fun getPackageUserDeDir(userId: Int): String = "/data/user_de/${userId}"
        fun getDataMediaDir(): String = "/data/media"
        fun getPackageDataDir(userId: Int): String = "${getDataMediaDir()}/${userId}/Android/data"
        fun getPackageObbDir(userId: Int): String = "${getDataMediaDir()}/${userId}/Android/obb"
        fun getPackageMediaDir(userId: Int): String = "${getDataMediaDir()}/${userId}/Android/media"

        fun getPackageIconRelativePath(packageName: String): String = "${packageName}.png"
        fun getPackageAdaptiveIconRelativePath(packageName: String): String = "adaptive@${getPackageIconRelativePath(packageName)}"
        fun getConfigsRelativeDir(): String = ConfigsRelativeDir

        fun getAppsRelativeDir(): String = AppsRelativeDir
        fun getFilesRelativeDir(): String = FilesRelativeDir


        fun getPackageRestoreConfigDst(dstDir: String): String = "${dstDir}/$ConfigsPackageRestoreName"
        fun getMediaRestoreConfigDst(dstDir: String): String = "${dstDir}/$ConfigsMediaRestoreName"

        suspend fun setFilesDirSELinux(context: Context) = SELinux.getContext(path = context.filesDir()).also { result ->
            val pathContext = if (result.isSuccess) result.outString else ""
            SELinux.chcon(context = pathContext, path = context.filesDir())
            val uidGid = context.applicationInfo.uid.toUInt()
            SELinux.chown(uid = uidGid, gid = uidGid, path = context.filesDir())
        }

        fun getSsaidPath(userId: Int) = "/data/system/users/$userId/settings_ssaid.xml"

        fun getPackageIconPath(context: Context, packageName: String, adaptive: Boolean): String = "${context.iconDir()}/${if (adaptive) getPackageAdaptiveIconRelativePath(packageName) else getPackageIconRelativePath(packageName)}"
    }

    fun getCloudTmpDir(): String = context.cloudTmpAbsoluteDir()
    fun getPackageIconPath(packageName: String, adaptive: Boolean): String = getPackageIconPath(context, packageName, adaptive)
    private fun getConfigsDir(parent: String): String = "${parent}/${getConfigsRelativeDir()}"
    fun getLocalBackupConfigsDir(): String = getConfigsDir(parent = context.localBackupSaveDir())
    fun getCloudTmpConfigsDir(): String = getConfigsDir(parent = context.cloudTmpAbsoluteDir())
    fun getCloudRemoteConfigsDir(remote: String): String = getConfigsDir(parent = remote)

    private fun getAppsDir(parent: String): String = "${parent}/${getAppsRelativeDir()}"
    private fun getFilesDir(parent: String): String = "${parent}/${getFilesRelativeDir()}"
    fun getLocalBackupAppsDir(): String = getAppsDir(parent = context.localBackupSaveDir())
    fun getCloudTmpAppsDir(): String = getAppsDir(parent = context.cloudTmpAbsoluteDir())
    fun getCloudRemoteAppsDir(remote: String): String = getAppsDir(parent = remote)
    fun getLocalBackupFilesDir(): String = getFilesDir(parent = context.localBackupSaveDir())
    fun getCloudTmpFilesDir(): String = getFilesDir(parent = context.cloudTmpAbsoluteDir())
    fun getCloudRemoteFilesDir(remote: String): String = getFilesDir(parent = remote)


    fun getTmpApkPath(packageName: String): String = "${context.tmpApksDir()}/$packageName"
}
