package com.xayah.databackup.util.command

import com.google.gson.reflect.TypeToken
import com.xayah.databackup.ui.activity.main.page.cloud.AccountConfigSizeBytes
import com.xayah.databackup.util.GsonUtil
import com.xayah.databackup.util.LogUtil
import com.xayah.databackup.util.SymbolUtil
import com.xayah.databackup.util.command.CommonUtil.executeWithLog
import com.xayah.databackup.util.command.CommonUtil.outString

data class AccountSizeInfo(
    val count: Int = 0,
    val bytes: Long = 0,
    val sizeless: Int = 0,
)

object CloudUtil {
    private const val LogTag = "CloudUtil"

    object Config {
        suspend fun dump(logUtil: LogUtil): Pair<Boolean, String> {
            val logId = logUtil.log(LogTag, "Dump rclone configurations.")
            var isSuccess = true
            val outList = mutableListOf<String>()

            logUtil.executeWithLog(logId, "rclone config dump").also { result ->
                if (result.isSuccess.not()) isSuccess = false
                outList.add(result.outString())
            }

            return Pair(isSuccess, outList.toLineString().trim())
        }


        suspend fun create(logUtil: LogUtil, name: String, type: String, args: String): Pair<Boolean, String> {
            val logId = logUtil.log(LogTag, "Create rclone configuration.")
            var isSuccess = true
            val outList = mutableListOf<String>()

            logUtil.executeWithLog(
                logId,
                "rclone config create ${SymbolUtil.QUOTE}${name}${SymbolUtil.QUOTE} ${SymbolUtil.QUOTE}${type}${SymbolUtil.QUOTE} $args"
            )
                .also { result ->
                    if (result.isSuccess.not()) isSuccess = false
                    outList.add(result.outString())
                }

            return Pair(isSuccess, outList.toLineString().trim())
        }

        suspend fun delete(logUtil: LogUtil, name: String): Pair<Boolean, String> {
            val logId = logUtil.log(LogTag, "Delete rclone configuration.")
            var isSuccess = true
            val outList = mutableListOf<String>()

            logUtil.executeWithLog(
                logId,
                "rclone config delete ${SymbolUtil.QUOTE}${name}${SymbolUtil.QUOTE}"
            )
                .also { result ->
                    if (result.isSuccess.not()) isSuccess = false
                    outList.add(result.outString())
                }

            return Pair(isSuccess, outList.toLineString().trim())
        }
    }

    suspend fun size(logUtil: LogUtil, gsonUtil: GsonUtil, name: String): Pair<Boolean, AccountSizeInfo> {
        val logId = logUtil.log(LogTag, "$name: Count size.")
        var isSuccess = true
        val outList = mutableListOf<String>()

        logUtil.executeWithLog(logId, "rclone size --json ${name}:", true).also { result ->
            if (result.isSuccess.not()) isSuccess = false
            outList.add(result.outString())
        }

        val type = object : TypeToken<AccountSizeInfo>() {}.type
        val info = if (isSuccess) gsonUtil.fromJson(outList.toLineString().trim(), type) else AccountSizeInfo(bytes = AccountConfigSizeBytes.FetchFailed.value)

        return Pair(isSuccess, info)
    }
}
