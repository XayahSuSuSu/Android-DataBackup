package com.xayah.core.util

import android.annotation.SuppressLint
import android.content.Context
import com.xayah.core.datastore.readBackupSavePath
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.nio.file.Paths
import javax.inject.Inject
import kotlin.io.path.pathString

const val IconRelativeDir = "icon"
const val BinRelativeDir = "bin"
const val ExtensionRelativeDir = "extension"
const val ArchivesRelativeDir = "archives"
const val PackagesRelativeDir = "packages"
const val ConfigsRelativeDir = "configs"
const val ConfigsPackageRestoreName = "PackageRestoreConfigs.pb"

fun Context.filesDir(): String = filesDir.path
fun Context.binDir(): String = "${filesDir()}/$BinRelativeDir"
fun Context.extensionDir(): String = "${filesDir()}/$ExtensionRelativeDir"
fun Context.iconDir(): String = "${filesDir()}/$IconRelativeDir"
fun Context.localBackupSaveDir(): String = runBlocking { readBackupSavePath().first() }

class PathUtil @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        fun getParentPath(path: String): String = Paths.get(path).parent.pathString

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
    }

    fun getPackageIconPath(packageName: String): String = "${context.iconDir()}/${getPackageIconRelativePath(packageName)}"
    fun getConfigsDir(parent: String = context.localBackupSaveDir()): String = "${parent}/${getConfigsRelativeDir()}"
    fun getArchivesDir(parent: String = context.localBackupSaveDir()): String = "${parent}/${getArchivesRelativeDir()}"
    fun getArchivesPackagesDir(parent: String = context.localBackupSaveDir()): String = "${parent}/${getArchivesPackagesRelativeDir()}"
}
