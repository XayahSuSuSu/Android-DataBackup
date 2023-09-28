package com.xayah.databackup.util.command

import android.os.Build
import com.xayah.databackup.util.CompressionType
import com.xayah.databackup.util.LogUtil
import com.xayah.databackup.util.PathUtil
import com.xayah.databackup.util.SymbolUtil.QUOTE
import com.xayah.databackup.util.command.CommonUtil.executeWithLog
import com.xayah.databackup.util.command.CommonUtil.outString

class InstallationUtil(private val logId: Long, private val logUtil: LogUtil) {
    suspend fun decompress(archivePath: String, tmpApkPath: String, compressionType: CompressionType): Pair<Boolean, String> {
        var isSuccess = true
        var out = ""
        logUtil.executeWithLog(logId, "tar --totals ${compressionType.decompressPara} -xmpf $QUOTE$archivePath$QUOTE -C $QUOTE$tmpApkPath$QUOTE")
            .also { result ->
                if (result.isSuccess.not()) {
                    isSuccess = false
                    out += result.outString() + "\n"
                }
            }
        return Pair(isSuccess, out.trim())
    }

    suspend fun pmInstall(userId: Int, apkPath: String): Pair<Boolean, String> {
        var isSuccess = true
        var out = ""

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            logUtil.executeWithLog(logId, "pm install --user $QUOTE$userId$QUOTE -r -t $QUOTE$apkPath$QUOTE").also { result ->
                if (result.isSuccess.not()) {
                    isSuccess = false
                    out += result.outString() + "\n"
                }
            }
        } else {
            logUtil.executeWithLog(logId, "pm install -i com.android.vending --user $QUOTE$userId$QUOTE -r -t $QUOTE$apkPath$QUOTE").also { result ->
                if (result.isSuccess.not()) {
                    isSuccess = false
                    out += result.outString() + "\n"
                }
            }
        }
        return Pair(isSuccess, out.trim())
    }

    suspend fun pmInstallCreate(userId: Int): Pair<Boolean, String> {
        var isSuccess = true
        var session = ""

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            logUtil.executeWithLog(logId, "pm install-create --user $QUOTE$userId$QUOTE -t | grep -E -o '[0-9]+'").also { result ->
                if (result.isSuccess.not()) isSuccess = false
                session = result.outString() + "\n"

            }
        } else {
            logUtil.executeWithLog(logId, "pm install-create -i com.android.vending --user $QUOTE$userId$QUOTE -t | grep -E -o '[0-9]+'").also { result ->
                if (result.isSuccess.not()) isSuccess = false
                session = result.outString() + "\n"
            }
        }
        return Pair(isSuccess, session.trim())
    }

    suspend fun pmInstallWrite(session: String, apkPath: String): Pair<Boolean, String> {
        var isSuccess = true
        var out = ""

        logUtil.executeWithLog(logId, "pm install-write $QUOTE$session$QUOTE $QUOTE${PathUtil.getFileName(apkPath)}$QUOTE $QUOTE${apkPath}$QUOTE")
            .also { result ->
                if (result.isSuccess.not()) {
                    isSuccess = false
                    out += result.outString() + "\n"
                }
            }
        return Pair(isSuccess, out.trim())
    }

    suspend fun pmInstallCommit(session: String): Pair<Boolean, String> {
        var isSuccess = true
        var out = ""

        logUtil.executeWithLog(logId, "pm install-commit $QUOTE$session$QUOTE").also { result ->
            if (result.isSuccess.not()) {
                isSuccess = false
                out += result.outString() + "\n"
            }
        }
        return Pair(isSuccess, out.trim())
    }
}
