package com.xayah.databackup.util.command

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.graphics.drawable.toBitmap
import com.xayah.databackup.util.PathUtil
import com.xayah.databackup.util.binArchivePath
import com.xayah.databackup.util.binPath
import com.xayah.databackup.util.filesPath
import com.xayah.databackup.util.iconPath
import com.xayah.librootservice.util.ExceptionUtil.tryOn
import com.xayah.librootservice.util.withIOContext
import net.lingala.zip4j.ZipFile
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object EnvUtil {
    private suspend fun releaseAssets(context: Context, path: String, name: String) {
        withIOContext {
            tryOn {
                val assets = File(context.filesPath(), name)
                if (!assets.exists()) {
                    val outStream = FileOutputStream(assets)
                    val inputStream = context.resources.assets.open(path)
                    inputStream.copyTo(outStream)
                    assets.setExecutable(true)
                    assets.setReadable(true)
                    assets.setWritable(true)
                    outStream.flush()
                    inputStream.close()
                    outStream.close()
                }
            }
        }
    }

    /**
     * Unzip and return file headers.
     */
    private fun unzip(path: String, out: String): List<String> {
        var headers = listOf<String>()
        tryOn {
            val zip = ZipFile(path)
            zip.extractAll(out)
            headers = zip.fileHeaders.map { it.fileName }
        }
        return headers
    }

    @SuppressLint("SetWorldWritable", "SetWorldReadable")
    private fun File.setPermissions(): Boolean {
        if (setExecutable(true, false).not()) return false
        if (setWritable(true, false).not()) return false
        if (setReadable(true, false).not()) return false
        return true
    }

    private fun PackageManager.getPackageInfoCompat(packageName: String, flags: Int = 0): PackageInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            getPackageInfo(packageName, flags)
        }

    fun Context.getCurrentAppVersionName(): String {
        return packageManager.getPackageInfoCompat(packageName).versionName
    }

    suspend fun releaseBin(context: Context): Boolean {
        return withIOContext {
            val bin = File(context.binPath())
            val binArchive = File(context.binArchivePath())

            // Remove old bin files
            bin.deleteRecursively()
            binArchive.deleteRecursively()

            // Release binaries
            releaseAssets(context, "bin.zip", "bin.zip")
            unzip(context.binArchivePath(), context.binPath())

            // All binaries need full permissions
            bin.listFiles()?.forEach { file ->
                if (file.setPermissions().not()) return@withIOContext false
            }

            // Remove binary archive
            binArchive.deleteRecursively()

            return@withIOContext true
        }
    }

    suspend fun saveIcon(context: Context, packageName: String, appIcon: Drawable) {
        withIOContext {
            tryOn {
                val byteArrayOutputStream = ByteArrayOutputStream()
                appIcon.toBitmap().compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
                val byteArray = byteArrayOutputStream.toByteArray()
                byteArrayOutputStream.flush()
                byteArrayOutputStream.close()
                File(PathUtil.getIconPath(context, packageName)).writeBytes(byteArray)
            }
        }
    }

    /**
     * Create internal icon directory for caching.
     */
    suspend fun createIconDirectory(context: Context) {
        withIOContext {
            tryOn {
                val icon = File(context.iconPath())
                if (icon.exists().not()) icon.mkdirs()
            }
        }
    }
}
