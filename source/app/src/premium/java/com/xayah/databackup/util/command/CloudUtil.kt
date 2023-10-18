package com.xayah.databackup.util.command

import com.google.gson.reflect.TypeToken
import com.xayah.databackup.DataBackupApplication
import com.xayah.databackup.data.AccountConfigSizeBytes
import com.xayah.databackup.util.GsonUtil
import com.xayah.databackup.util.LogUtil
import com.xayah.databackup.util.SymbolUtil
import com.xayah.databackup.util.command.CommonUtil.execute
import com.xayah.librootservice.util.withIOContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AccountSizeInfo(
    val count: Int = 0,
    val bytes: Long = 0,
    val sizeless: Int = 0,
)

object CloudUtil {
    private const val LogTag = "CloudUtil"

    object Config {
        suspend fun dump(): Pair<Boolean, String> {
            var isSuccess = true
            val outList = mutableListOf<String>()

            execute("rclone config dump").also { result ->
                if (result.isSuccess.not()) isSuccess = false
                outList.add(result.outString)
            }

            return Pair(isSuccess, outList.toLineString().trim())
        }


        suspend fun create(logUtil: LogUtil, name: String, type: String, args: String): Pair<Boolean, String> {
            val logId = logUtil.log(LogTag, "Create rclone configuration.")
            var isSuccess = true
            val outList = mutableListOf<String>()

            logUtil.execute(
                logId,
                "rclone config create ${SymbolUtil.QUOTE}${name}${SymbolUtil.QUOTE} ${SymbolUtil.QUOTE}${type}${SymbolUtil.QUOTE} $args"
            )
                .also { result ->
                    if (result.isSuccess.not()) isSuccess = false
                    outList.add(result.outString)
                }

            return Pair(isSuccess, outList.toLineString().trim())
        }

        suspend fun delete(logUtil: LogUtil, name: String): Pair<Boolean, String> {
            val logId = logUtil.log(LogTag, "Delete rclone configuration.")
            var isSuccess = true
            val outList = mutableListOf<String>()

            logUtil.execute(
                logId,
                "rclone config delete ${SymbolUtil.QUOTE}${name}${SymbolUtil.QUOTE}"
            )
                .also { result ->
                    if (result.isSuccess.not()) isSuccess = false
                    outList.add(result.outString)
                }

            return Pair(isSuccess, outList.toLineString().trim())
        }
    }

    suspend fun size(logUtil: LogUtil, gsonUtil: GsonUtil, name: String): Pair<Boolean, AccountSizeInfo> = withIOContext {
        val logId = logUtil.log(LogTag, "$name: Count size.")
        var isSuccess = true
        val outList = mutableListOf<String>()

        val shell = DataBackupApplication.getBuilder(context = DataBackupApplication.application).build()
        logUtil.execute(logId, "rclone size --json ${name}:", shell).also { result ->
            if (result.isSuccess.not()) isSuccess = false
            outList.add(result.outString)
        }
        withContext(Dispatchers.IO) {
            shell.close()
        }

        val type = object : TypeToken<AccountSizeInfo>() {}.type
        val info = if (isSuccess) gsonUtil.fromJson(outList.toLineString().trim(), type) else AccountSizeInfo(bytes = AccountConfigSizeBytes.FetchFailed.value)

        Pair(isSuccess, info)
    }

    suspend fun mount(logUtil: LogUtil, remote: String, destination: String, vararg args: String): Pair<Boolean, String> = withIOContext {
        val logId = logUtil.log(LogTag, "$remote: Mount.")
        var isSuccess = true
        val outList = mutableListOf<String>()

        val shell = DataBackupApplication.getBuilder(context = DataBackupApplication.application).build()
        logUtil.execute(
            logId,
            "rclone mount $remote $destination --daemon --allow-non-empty --allow-other --vfs-cache-mode off ${args.toList().toSpaceString()}",
            shell
        ).also { result ->
            if (result.isSuccess.not()) isSuccess = false
            outList.add(result.outString)
        }
        withContext(Dispatchers.IO) {
            shell.close()
        }

        Pair(isSuccess, outList.toLineString().trim())
    }

    suspend fun unmount(logUtil: LogUtil, name: String, destination: String): Pair<Boolean, String> = withIOContext {
        val logId = logUtil.log(LogTag, "$name: Unmount.")
        var isSuccess = true
        val outList = mutableListOf<String>()

        val shell = DataBackupApplication.getBuilder(context = DataBackupApplication.application).build()
        logUtil.execute(logId, "umount -f $destination", shell).also { result ->
            if (result.isSuccess.not()) isSuccess = false
            outList.add(result.outString)
        }
        withContext(Dispatchers.IO) {
            shell.close()
        }

        Pair(isSuccess, outList.toLineString().trim())
    }

    suspend fun unmountAll(logUtil: LogUtil): Pair<Boolean, String> = withIOContext {
        val logId = logUtil.log(LogTag, "Unmount all.")
        var isSuccess = true
        val outList = mutableListOf<String>()

        val shell = DataBackupApplication.getBuilder(context = DataBackupApplication.application).build()
        logUtil.execute(logId, "mount | grep rclone | awk '{print ${SymbolUtil.USD}3}'", shell).also { result ->
            if (result.isSuccess) {
                result.out.forEach {
                    logUtil.execute(
                        logId,
                        "if test ! -z ${SymbolUtil.QUOTE}${SymbolUtil.USD}(mount | grep $it)${SymbolUtil.QUOTE}; then umount -f $it; fi",
                        shell
                    ).also { result ->
                        if (result.isSuccess.not()) isSuccess = false
                        outList.add(result.outString)
                    }
                }
            }
        }
        withContext(Dispatchers.IO) {
            shell.close()
        }

        Pair(isSuccess, outList.toLineString().trim())
    }

    suspend fun mkdir(logUtil: LogUtil, remote: String): Pair<Boolean, String> = withIOContext {
        val logId = logUtil.log(LogTag, "$remote: Create.")
        var isSuccess = true
        val outList = mutableListOf<String>()

        val shell = DataBackupApplication.getBuilder(context = DataBackupApplication.application).build()
        logUtil.execute(logId, "rclone mkdir $remote", shell).also { result ->
            if (result.isSuccess.not()) isSuccess = false
            outList.add(result.outString)
        }
        withContext(Dispatchers.IO) {
            shell.close()
        }

        Pair(isSuccess, outList.toLineString().trim())
    }

    suspend fun copy(logUtil: LogUtil, src: String, dst: String): Pair<Boolean, String> = withIOContext {
        val logId = logUtil.log(LogTag, "Rclone: CopyTo $src $dst.")
        var isSuccess = true
        val outList = mutableListOf<String>()

        val shell = DataBackupApplication.getBuilder(context = DataBackupApplication.application).build()
        logUtil.execute(logId, "rclone copy -P --stats-one-line $src $dst", shell).also { result ->
            if (result.isSuccess.not()) isSuccess = false
            outList.add(result.outString)
        }
        withContext(Dispatchers.IO) {
            shell.close()
        }

        Pair(isSuccess, outList.toLineString().trim())
    }
}

object Rclone {
    private suspend fun execute(vararg args: String): ShellResult = CommonUtil.execute("rclone", *args)

    suspend fun mount(src: String, dst: String): ShellResult = run {
        // rclone mount "$src" "$dst" --daemon --allow-non-empty --allow-other --vfs-cache-mode off --allow-other
        execute(
            "mount",
            "${SymbolUtil.QUOTE}$src${SymbolUtil.QUOTE}",
            "${SymbolUtil.QUOTE}$dst${SymbolUtil.QUOTE}",
            "--daemon",
            "--allow-non-empty",
            "--allow-other",
            "--vfs-cache-mode off",
            "--allow-other",
        )
    }

    suspend fun unmount(dst: String): ShellResult = run {
        // umount -f "$dst"
        CommonUtil.execute(
            "umount",
            "-f",
            "${SymbolUtil.QUOTE}$dst${SymbolUtil.QUOTE}",
        )
    }

    suspend fun mkdir(dst: String, dryRun: Boolean = false): ShellResult = run {
        // umount -f "$dst"
        execute(
            "mkdir",
            "${SymbolUtil.QUOTE}$dst${SymbolUtil.QUOTE}",
            if (dryRun) "--dry-run" else ""
        )
    }
}
