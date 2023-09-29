package com.xayah.databackup.util.command

import android.content.Context
import com.xayah.databackup.util.CompressionType
import com.xayah.databackup.util.DataType
import com.xayah.databackup.util.LogUtil
import com.xayah.databackup.util.SymbolUtil.QUOTE
import com.xayah.databackup.util.command.CommonUtil.executeWithLog
import com.xayah.databackup.util.command.CommonUtil.outString
import com.xayah.databackup.util.readCleanRestoring
import com.xayah.librootservice.service.RemoteRootService

fun List<String>.toSpaceString() = joinToString(separator = " ")

object CompressionUtil {
    suspend fun compress(
        logUtil: LogUtil,
        logId: Long,
        compatibleMode: Boolean,
        userId: Int,
        compressionType: CompressionType,
        archivePath: String,
        packageName: String,
        dataType: DataType,
    ): Pair<Boolean, String> {
        var isSuccess = true
        val outList = mutableListOf<String>()
        val excludeParaList = mutableListOf<String>()
        val originPath = dataType.origin(userId)
        val originPathPara = "$QUOTE$originPath$QUOTE $QUOTE$packageName$QUOTE"

        when (dataType) {
            DataType.PACKAGE_USER, DataType.PACKAGE_USER_DE -> {
                // Exclude cache
                val folders = listOf(".ota", "cache", "lib", "code_cache", "no_backup")
                for (item in folders) {
                    excludeParaList.add("--exclude=$QUOTE$packageName/$item$QUOTE")
                }
            }

            DataType.PACKAGE_DATA, DataType.PACKAGE_OBB, DataType.PACKAGE_MEDIA -> {
                // Exclude cache
                val folders = listOf("cache")
                for (item in folders) {
                    excludeParaList.add("--exclude=$QUOTE$packageName/$item$QUOTE")
                }
                // Exclude Backup_*
                excludeParaList.add("--exclude=${QUOTE}Backup_$QUOTE*")
            }

            else -> {
                return Pair(false, outList.toLineString().trim())
            }
        }
        val cmd = if (compatibleMode)
            "- -C $originPathPara ${if (compressionType == CompressionType.TAR) "" else "| ${compressionType.compressPara}"} > $archivePath"
        else
            "$archivePath -C $originPathPara ${if (compressionType == CompressionType.TAR) "" else "-I $QUOTE${compressionType.compressPara}$QUOTE"}"

        // Compress data dir.
        logUtil.executeWithLog(logId, "tar --totals ${excludeParaList.toSpaceString()} -cpf $cmd").also { result ->
            if (result.isSuccess.not()) isSuccess = false
            outList.add(result.outString())
        }

        return Pair(isSuccess, outList.toLineString().trim())
    }

    suspend fun compress(
        logUtil: LogUtil,
        logId: Long,
        compatibleMode: Boolean,
        compressionType: CompressionType,
        archivePath: String,
        originPath: String,
    ): Pair<Boolean, String> {
        var isSuccess = true
        val outList = mutableListOf<String>()

        val cmd = if (compatibleMode)
            "- ./* ${if (compressionType == CompressionType.TAR) "" else "| ${compressionType.compressPara}"} > $archivePath"
        else
            "$archivePath ./* ${if (compressionType == CompressionType.TAR) "" else "-I $QUOTE${compressionType.compressPara}$QUOTE"}"

        // Compress config dir.
        logUtil.executeWithLog(logId, "cd $originPath").also { result ->
            if (result.isSuccess.not()) isSuccess = false
            outList.add(result.outString())
        }
        logUtil.executeWithLog(logId, "tar --totals -cpf $cmd").also { result ->
            if (result.isSuccess.not()) isSuccess = false
            outList.add(result.outString())
        }
        logUtil.executeWithLog(logId, "cd /").also { result ->
            if (result.isSuccess.not()) isSuccess = false
            outList.add(result.outString())
        }

        return Pair(isSuccess, outList.toLineString().trim())
    }

    suspend fun decompress(
        logUtil: LogUtil,
        logId: Long,
        context: Context,
        userId: Int,
        compressionType: CompressionType,
        archivePath: String,
        packageName: String,
        dataType: DataType,
    ): Pair<Boolean, String> {
        var isSuccess = true
        val outList = mutableListOf<String>()
        val excludeParaList = mutableListOf<String>()
        val cleanRestoringPara = if (context.readCleanRestoring()) "--recursive-unlink" else ""
        val originPath = dataType.origin(userId)

        when (dataType) {
            DataType.MEDIA_MEDIA -> {}

            DataType.PACKAGE_USER, DataType.PACKAGE_USER_DE, DataType.PACKAGE_DATA, DataType.PACKAGE_OBB, DataType.PACKAGE_MEDIA -> {
                // Exclude cache
                val folders = listOf(".ota", "cache", "lib", "code_cache", "no_backup")
                for (item in folders) {
                    excludeParaList.add("--exclude=$QUOTE$packageName/$item$QUOTE")
                }
                if (dataType == DataType.PACKAGE_DATA || dataType == DataType.PACKAGE_OBB || dataType == DataType.PACKAGE_MEDIA) {
                    // Exclude Backup_*
                    excludeParaList.add("--exclude=${QUOTE}Backup_$QUOTE*")
                }
            }

            else -> {
                return Pair(false, outList.toLineString().trim())
            }
        }
        val cmd = "$archivePath -C $originPath ${compressionType.decompressPara}"


        // Decompress the archive.
        logUtil.executeWithLog(logId, "tar --totals ${excludeParaList.toSpaceString()} $cleanRestoringPara -xmpf $cmd").also { result ->
            if (result.isSuccess.not()) isSuccess = false
            outList.add(result.outString())
        }

        return Pair(isSuccess, outList.toLineString().trim())
    }

    suspend fun test(
        logUtil: LogUtil,
        logId: Long,
        compressionType: CompressionType,
        archivePath: String,
        remoteRootService: RemoteRootService,
    ): Pair<Boolean, String> {
        var isSuccess = true
        val outList = mutableListOf<String>()

        val cmd = "$QUOTE$archivePath$QUOTE ${compressionType.decompressPara}"
        // Test the archive.
        logUtil.executeWithLog(logId, "tar -t -f $cmd > /dev/null 2>&1").also { result ->
            if (result.isSuccess.not()) {
                isSuccess = false
                outList.add("$archivePath is broken, trying to delete it.")
                // Delete the archive if test failed.
                remoteRootService.deleteRecursively(archivePath)
            }
        }

        return Pair(isSuccess, outList.toLineString().trim())
    }
}
