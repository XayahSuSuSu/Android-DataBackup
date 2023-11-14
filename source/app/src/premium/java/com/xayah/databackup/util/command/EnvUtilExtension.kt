package com.xayah.databackup.util.command

import android.content.Context
import com.xayah.databackup.util.command.EnvUtil.setPermissions
import com.xayah.databackup.util.extensionArchivePath
import com.xayah.databackup.util.extensionPath
import com.xayah.core.rootservice.util.withIOContext
import java.io.File

private suspend fun releaseExtension(context: Context): Boolean {
    return withIOContext {
        val extension = File(context.extensionPath())
        val extensionArchive = File(context.extensionArchivePath())

        // Remove old extension files
        extension.deleteRecursively()
        extensionArchive.deleteRecursively()

        // Release binaries
        EnvUtil.releaseAssets(context, "extension.zip", "extension.zip")
        EnvUtil.unzip(context.extensionArchivePath(), context.extensionPath())

        // All binaries need full permissions
        extension.listFiles()?.forEach { file ->
            if (file.setPermissions().not()) return@withIOContext false
            // Rename fusermount to fusermount3 for rclone
            if (file.name == "fusermount") file.renameTo(File(file.parent, "fusermount3"))
        }

        // Remove binary archive
        extensionArchive.deleteRecursively()

        return@withIOContext true
    }
}

suspend fun releaseBin(context: Context): Boolean = withIOContext {
    var result = true
    if (EnvUtil.releaseBase(context).not()) result = false
    if (releaseExtension(context).not()) result = false
    result
}
