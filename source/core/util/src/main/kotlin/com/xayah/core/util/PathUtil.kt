package com.xayah.core.util

import android.annotation.SuppressLint
import android.content.Context
import com.xayah.core.datastore.readBackupSavePath
import com.xayah.core.datastore.readRestoreSavePath
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.nio.file.Paths
import javax.inject.Inject
import kotlin.io.path.pathString

const val IconRelativeDir = "icon"
const val BinRelativeDir = "bin"
const val TmpRelativeDir = "tmp"
const val ApksRelativeDir = "apks"
const val ExtensionRelativeDir = "extension"
const val ArchivesRelativeDir = "archives"
const val PackagesRelativeDir = "packages"
const val MediumRelativeDir = "medium"
const val ConfigsRelativeDir = "configs"
const val ConfigsPackageRestoreName = "package_restore_config.pb"
const val ConfigsMediaRestoreName = "media_restore_config.pb"

fun Context.filesDir(): String = filesDir.path
fun Context.binDir(): String = "${filesDir()}/$BinRelativeDir"
fun Context.extensionDir(): String = "${filesDir()}/$ExtensionRelativeDir"
fun Context.iconDir(): String = "${filesDir()}/$IconRelativeDir"
fun Context.tmpDir(): String = "${filesDir()}/$TmpRelativeDir"
fun Context.tmpApksDir(): String = "${filesDir()}/$TmpRelativeDir/$ApksRelativeDir"
fun Context.localBackupSaveDir(): String = runBlocking { readBackupSavePath().first() }
fun Context.localRestoreSaveDir(): String = runBlocking { readRestoreSavePath().first() }

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
    }

    fun getPackageIconPath(packageName: String): String = "${context.iconDir()}/${getPackageIconRelativePath(packageName)}"
    fun getConfigsDir(parent: String): String = "${parent}/${getConfigsRelativeDir()}"
    fun getLocalBackupConfigsDir(): String = getConfigsDir(parent = context.localBackupSaveDir())
    fun getArchivesDir(parent: String): String = "${parent}/${getArchivesRelativeDir()}"
    fun getLocalBackupArchivesDir(): String = getArchivesDir(parent = context.localBackupSaveDir())
    fun getArchivesPackagesDir(parent: String): String = "${parent}/${getArchivesPackagesRelativeDir()}"
    fun getLocalBackupArchivesPackagesDir(): String = getArchivesPackagesDir(parent = context.localBackupSaveDir())
    fun getLocalRestoreArchivesPackagesDir(): String = getArchivesPackagesDir(parent = context.localRestoreSaveDir())
    fun getArchivesMediumDir(parent: String): String = "${parent}/${getArchivesMediumRelativeDir()}"
    fun getLocalBackupArchivesMediumDir(): String = getArchivesMediumDir(parent = context.localBackupSaveDir())
    fun getLocalRestoreArchivesMediumDir(): String = getArchivesMediumDir(parent = context.localRestoreSaveDir())

    fun getTmpApkPath(packageName: String): String = "${context.tmpApksDir()}/$packageName"
}
