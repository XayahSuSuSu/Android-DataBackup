package com.xayah.databackup.util.command

import com.xayah.databackup.util.DataType
import com.xayah.databackup.util.LogUtil
import com.xayah.databackup.util.SymbolUtil.QUOTE
import com.xayah.databackup.util.SymbolUtil.USD
import com.xayah.databackup.util.command.CommonUtil.executeWithLog
import com.xayah.databackup.util.command.CommonUtil.outString

class SELinuxUtil(private val logId: Long, private val logUtil: LogUtil) {
    suspend fun getContext(path: String): Pair<Boolean, String> {
        var isSuccess = true
        var out = ""

        logUtil.executeWithLog(logId, "ls -Zd $QUOTE$path$QUOTE | awk 'NF>1{print ${USD}1}'").also { result ->
            if (result.isSuccess.not()) {
                isSuccess = false
                out += result.outString() + "\n"
            }
        }
        return Pair(isSuccess, out.trim())
    }

    suspend fun restoreContext(path: String, pathContext: String, packageName: String, uid: Int, dataType: DataType): Pair<Boolean, String> {
        var isSuccess = true
        var out = ""

        if (uid == -1) {
            isSuccess = false
            out += "Failed to get uid of $packageName."
        } else {
            logUtil.executeWithLog(logId, "chown -hR $QUOTE$uid:$uid$QUOTE $QUOTE$path/$QUOTE").also { result ->
                if (result.isSuccess.not()) {
                    isSuccess = false
                    out += result.outString() + "\n"
                }
            }
            if (dataType == DataType.PACKAGE_USER || dataType == DataType.PACKAGE_USER_DE || dataType == DataType.PACKAGE_MEDIA) {
                logUtil.executeWithLog(logId, "restorecon -RFD $QUOTE$path/$QUOTE").also { result ->
                    if (result.isSuccess.not()) {
                        isSuccess = false
                        out += result.outString() + "\n"
                    }
                }
            }
            if (pathContext.isNotEmpty()) {
                logUtil.executeWithLog(logId, "chcon -hR $QUOTE$pathContext$QUOTE $QUOTE$path/$QUOTE").also { result ->
                    if (result.isSuccess.not()) {
                        isSuccess = false
                        out += result.outString() + "\n"
                    }
                }
            } else {
                logUtil.executeWithLog(
                    logId,
                    "ls -Zd $QUOTE$path/../$QUOTE | awk 'NF>1{print ${USD}1}' | sed -e ${QUOTE}s/system_data_file/app_data_file/g$QUOTE"
                ).also { result ->
                    if (result.isSuccess.not()) {
                        isSuccess = false
                        out += result.outString() + "\n"
                    } else {
                        logUtil.executeWithLog(logId, "chcon -hR $QUOTE${result.outString()}$QUOTE $QUOTE$path/$QUOTE").also { innerResult ->
                            if (innerResult.isSuccess.not()) {
                                isSuccess = false
                                out += innerResult.outString() + "\n"
                            }
                        }
                    }
                }
            }
        }
        return Pair(isSuccess, out.trim())
    }
}
