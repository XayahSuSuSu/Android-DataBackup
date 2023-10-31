package com.xayah.databackup.util.command

import android.os.Build
import com.xayah.databackup.data.CompressionType
import com.xayah.databackup.librootservice.RootService
import com.xayah.databackup.util.Path
import com.xayah.databackup.util.joinToLineString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class Installation {
    companion object {
        private const val QUOTE = '"'
        val tmpDir = "${Path.getAppInternalFilesPath()}/tmp/data_backup"

        private suspend fun <T> runOnIO(block: suspend () -> T): T {
            return withContext(Dispatchers.IO) { block() }
        }

        private suspend fun pmInstall(userId: String, apkPath: String): Pair<Boolean, String> {
            val exec = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                Command.execute("pm install --user $QUOTE$userId$QUOTE -r -t $QUOTE$apkPath$QUOTE")
            } else {
                Command.execute("pm install -i com.android.vending --user $QUOTE$userId$QUOTE -r -t $QUOTE$apkPath$QUOTE")
            }
            return Pair(exec.isSuccess, exec.out.joinToLineString.trim())
        }

        private suspend fun pmInstallCreate(userId: String): Pair<Boolean, String> {
            val exec = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                Command.execute("pm install-create --user $QUOTE$userId$QUOTE -t | grep -E -o '[0-9]+'")
            } else {
                Command.execute("pm install-create -i com.android.vending --user $QUOTE$userId$QUOTE -t | grep -E -o '[0-9]+'")
            }
            return Pair(exec.isSuccess, exec.out.joinToLineString.trim())
        }

        suspend fun decompressAPKArchive(compressionType: CompressionType, packageName: String, apkPath: String, onSuccess: (Boolean) -> Unit, onLog: (String) -> Unit): MutableList<String> {
            val apkDir = "$tmpDir/$packageName"
            if (RootService.getInstance().deleteRecursively(apkDir).not()) onSuccess(false)
            if (RootService.getInstance().mkdirs(apkDir).not()) {
                onSuccess(false)
                onLog("Failed to mkdirs: $apkDir.\n")
            }

            // Decompress apk archive
            val input = when (compressionType) {
                CompressionType.TAR -> {
                    ""
                }

                CompressionType.ZSTD, CompressionType.LZ4 -> {
                    "-I ${QUOTE}zstd$QUOTE"
                }
            }
            Command.execute("tar --totals $input -xmpf $QUOTE$apkPath$QUOTE -C $QUOTE$apkDir$QUOTE").apply {
                if (Compression.isCompressionSucceed(this).not()) onSuccess(false)
                onLog(this.out.joinToLineString + "\n")
            }
            return RootService.getInstance().listFilesPath(apkDir)
        }

        suspend fun installAPK(compressionType: CompressionType, packageName: String, apkPath: String, userId: String): Pair<Boolean, String> {
            var isSuccess = true
            var out = ""
            runOnIO {
                val apksPath = decompressAPKArchive(
                    compressionType = compressionType,
                    packageName = packageName,
                    apkPath = apkPath,
                    onSuccess = {
                        isSuccess = it
                    },
                    onLog = {
                        out += it
                    })
                when (apksPath.size) {
                    0 -> {
                        isSuccess = false
                        out += "$tmpDir is empty.\n"
                        return@runOnIO
                    }

                    1 -> {
                        pmInstall(userId, apksPath[0]).apply {
                            if (this.first.not()) isSuccess = false
                            out += this.second + "\n"
                        }
                    }
                    else -> {
                        var session: String
                        pmInstallCreate(userId).apply {
                            if (this.first.not()) {
                                isSuccess = false
                                out += "Failed to get install session.\n"
                                return@runOnIO
                            }
                            session = this.second
                            out += "Install session: $session\n"
                        }

                        for (i in apksPath) {
                            Command.execute("pm install-write $QUOTE$session$QUOTE $QUOTE${Path.getFileNameByPath(i)}$QUOTE $QUOTE${i}$QUOTE").apply {
                                if (this.isSuccess.not()) {
                                    isSuccess = false
                                    out += this.out.joinToLineString + "\n"
                                    return@runOnIO
                                }
                            }
                        }
                        Command.execute("pm install-commit $QUOTE$session$QUOTE").apply {
                            if (this.isSuccess.not()) isSuccess = false
                            out += this.out.joinToLineString + "\n"
                        }
                    }
                }
                if (RootService.getInstance().deleteRecursively(tmpDir).not()) isSuccess = false
            }
            return Pair(isSuccess, out.trim())
        }
    }
}