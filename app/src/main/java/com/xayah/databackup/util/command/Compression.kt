package com.xayah.databackup.util.command

import com.xayah.databackup.data.CompressionType
import com.xayah.databackup.data.DataType
import com.xayah.databackup.librootservice.RootService
import com.xayah.databackup.util.Path
import com.xayah.databackup.util.joinToLineString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class Compression {
    companion object {
        private const val ZSTD_PARA = "zstd -r -T0 --ultra -1 -q --priority=rt"
        private const val LZ4_PARA = "zstd -r -T0 --ultra -1 -q --priority=rt --format=lz4"
        private const val QUOTE = '"'
        private const val MEDIA_PATH_FILE_NAME = "com.xayah.databackup.PATH"

        private suspend fun <T> runOnIO(block: suspend () -> T): T {
            return withContext(Dispatchers.IO) { block() }
        }

        private fun getSuffixAndType(compressionType: CompressionType): Pair<String, String> {
            return when (compressionType) {
                CompressionType.TAR -> Pair(".tar", "")
                CompressionType.ZSTD -> Pair(".tar.zst", ZSTD_PARA)
                CompressionType.LZ4 -> Pair(".tar.lz4", LZ4_PARA)
            }
        }

        private fun getMediaPathFilePath(dataPath: String): String {
            return "$dataPath/$MEDIA_PATH_FILE_NAME"
        }

        suspend fun compressAPK(
            compressionType: CompressionType,
            apkPath: String,
            outPut: String,
            compatibleMode: Boolean
        ): Pair<Boolean, String> {
            var isSuccess = true
            var out = ""
            runOnIO {
                // Make output dir
                if (RootService.getInstance().mkdirs(outPut).not()) isSuccess = false
                // Cd to apk path
                Command.execute("cd $apkPath").apply {
                    if (this.isSuccess.not()) isSuccess = false
                    out += this.out.joinToLineString + "\n"
                }
                // Compress
                val cmd: String
                val (suffix, type) = getSuffixAndType(compressionType)
                val target = "$QUOTE${outPut}/apk$suffix$QUOTE"
                cmd = if (compatibleMode)
                    "- ./*.apk ${if (compressionType == CompressionType.TAR) "" else "| $type"} > $target"
                else
                    "$target ./*.apk ${if (compressionType == CompressionType.TAR) "" else "-I $QUOTE$type$QUOTE"}"
                val exec = Command.execute("tar --totals -cpf $cmd")
                if (exec.isSuccess.not()) isSuccess = false
                out += exec.out.joinToLineString + "\n"
                // Cd back
                Command.execute("cd /").apply {
                    if (this.isSuccess.not()) isSuccess = false
                    out += this.out.joinToLineString + "\n"
                }
            }
            return Pair(isSuccess, out.trim())
        }

        suspend fun compressArchive(
            compressionType: CompressionType,
            dataType: DataType,
            packageName: String,
            outPut: String,
            dataPath: String,
            compatibleMode: Boolean,
        ): Pair<Boolean, String> {
            var isSuccess = true
            var out = ""
            runOnIO {
                // Make output dir
                if (RootService.getInstance().mkdirs(outPut).not()) isSuccess = false
                // Compress
                val cmd: String
                val (suffix, type) = getSuffixAndType(compressionType)
                val exclude: String
                val origin: String
                val target: String

                // Check if data path exists
                if (dataType != DataType.MEDIA) {
                    if (RootService.getInstance().exists("$dataPath/$packageName").not()) {
                        isSuccess = false
                        out = "No such path: $dataPath/$packageName"
                        return@runOnIO
                    }
                } else {
                    if (RootService.getInstance().exists(dataPath).not()) {
                        isSuccess = false
                        out = "No such path: $dataPath"
                        return@runOnIO
                    }
                }

                when (dataType) {
                    DataType.USER, DataType.USER_DE -> {
                        exclude =
                            "--totals --exclude=$QUOTE$packageName/.ota$QUOTE --exclude=$QUOTE$packageName/cache$QUOTE --exclude=$QUOTE$packageName/lib$QUOTE --exclude=$QUOTE$packageName/code_cache$QUOTE --exclude=$QUOTE$packageName/no_backup$QUOTE"
                        origin = "$QUOTE$dataPath$QUOTE $QUOTE$packageName$QUOTE"
                        target = "$QUOTE${outPut}/${dataType.type}$suffix$QUOTE"
                    }
                    DataType.DATA, DataType.OBB -> {
                        exclude = "--totals --exclude=${QUOTE}Backup_$QUOTE* --exclude=$QUOTE$packageName/cache$QUOTE"
                        origin = "$QUOTE$dataPath$QUOTE $QUOTE$packageName$QUOTE"
                        target = "$QUOTE${outPut}/${dataType.type}$suffix$QUOTE"
                    }
                    DataType.MEDIA -> {
                        val fileName = Path.getFileNameByPath(dataPath)
                        exclude = "--totals --exclude=${QUOTE}Backup_$QUOTE* --exclude=$QUOTE${fileName}/cache$QUOTE"
                        origin = "$QUOTE${Path.getParentPath(dataPath)}$QUOTE $QUOTE${Path.getFileNameByPath(dataPath)}$QUOTE"
                        target = "$QUOTE${outPut}/$fileName$suffix$QUOTE"
                        if (RootService.getInstance().writeText(getMediaPathFilePath(dataPath), dataPath).not()) {
                            isSuccess = false
                            out = "Failed to write data path to: ${getMediaPathFilePath(dataPath)}"
                            return@runOnIO
                        }
                    }
                    else -> {
                        isSuccess = false
                        out = "Wrong data type: ${dataType.type}."
                        return@runOnIO
                    }
                }
                cmd = if (compatibleMode)
                    "- -C $origin ${if (compressionType == CompressionType.TAR) "" else "| $type"} > $target"
                else
                    "$target -C $origin ${if (compressionType == CompressionType.TAR) "" else "-I $QUOTE$type$QUOTE"}"
                val exec = Command.execute("tar --totals $exclude -cpf $cmd")
                if (dataType == DataType.MEDIA) {
                    RootService.getInstance().deleteRecursively(getMediaPathFilePath(dataPath))
                }
                if (exec.isSuccess.not()) isSuccess = false
                out = exec.out.joinToLineString
            }
            return Pair(isSuccess, out.trim())
        }

        suspend fun decompressArchive(
            compressionType: CompressionType,
            dataType: DataType,
            inputPath: String,
            packageName: String,
            dataPath: String,
        ): Pair<Boolean, String> {
            var isSuccess = true
            var out = ""
            runOnIO {
                val cmd: String
                var tmpDir = ""
                var mediaPath = ""
                val parameter: String
                val target: String

                when (dataType) {
                    DataType.MEDIA -> {
                        parameter = "-xpf"
                        tmpDir = "${Path.getAppInternalFilesPath()}/tmp/data_backup"
                        if (RootService.getInstance().deleteRecursively(tmpDir).not()) isSuccess = false
                        if (RootService.getInstance().mkdirs(tmpDir).not()) isSuccess = false

                        // Get the media path
                        Command.execute("tar -xpf $QUOTE$inputPath$QUOTE -C $QUOTE$tmpDir$QUOTE --wildcards --no-anchored $QUOTE$MEDIA_PATH_FILE_NAME$QUOTE ${if (compressionType == CompressionType.TAR) "" else "-I ${QUOTE}zstd$QUOTE"}")
                            .apply {
                                if (this.isSuccess.not()) {
                                    isSuccess = false
                                    out = "Failed to extract $MEDIA_PATH_FILE_NAME from this archive: $inputPath."
                                    return@runOnIO
                                }
                            }
                        mediaPath = RootService.getInstance().readText("$tmpDir/$packageName/$MEDIA_PATH_FILE_NAME")
                        if (mediaPath.isNotEmpty()) {
                            target = "$QUOTE${Path.getParentPath(mediaPath)}$QUOTE"
                        } else {
                            isSuccess = false
                            out = "Failed to get media path from $MEDIA_PATH_FILE_NAME."
                            return@runOnIO
                        }
                    }
                    else -> {
                        parameter = "-xmpf"
                        target = "$QUOTE${dataPath}$QUOTE"
                    }
                }
                cmd = "$target ${if (compressionType == CompressionType.TAR) "" else "-I ${QUOTE}zstd$QUOTE"}"
                val exec = Command.execute("tar --totals --recursive-unlink $parameter $QUOTE$inputPath$QUOTE -C $cmd")
                if (dataType == DataType.MEDIA) {
                    RootService.getInstance().deleteRecursively(getMediaPathFilePath(mediaPath))
                    RootService.getInstance().deleteRecursively(tmpDir)
                }
                if (exec.isSuccess.not()) isSuccess = false
                out = exec.out.joinToLineString
            }
            return Pair(isSuccess, out.trim())
        }

        suspend fun testArchive(compressionType: CompressionType, inputPath: String): Pair<Boolean, String> {
            val exec = when (compressionType) {
                CompressionType.TAR -> {
                    Command.execute("tar -t -f $QUOTE$inputPath$QUOTE > /dev/null 2>&1")
                }
                CompressionType.ZSTD, CompressionType.LZ4 -> {
                    Command.execute("tar -t -f $QUOTE$inputPath$QUOTE -I ${QUOTE}zstd$QUOTE > /dev/null 2>&1")
                }
            }
            return Pair(exec.isSuccess, exec.out.joinToLineString.trim())
        }
    }
}
