package com.xayah.databackup.util.command

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.topjohnwu.superuser.Shell
import com.xayah.databackup.DataBackupApplication
import com.xayah.databackup.data.LogCmdType
import com.xayah.databackup.util.ConstantUtil
import com.xayah.databackup.util.LogUtil
import com.xayah.librootservice.util.withIOContext

object CommonUtil {
    fun Context.copyToClipboard(content: String) {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.setPrimaryClip(ClipData.newPlainText(ConstantUtil.ClipDataLabel, content))
    }

    suspend fun execute(cmd: String): Shell.Result = withIOContext {
        Shell.cmd(cmd).exec().also { result ->
            if (result.code == 127) {
                // If the code is 127, the shell may have been dead.
                DataBackupApplication.Companion.EnvInitializer.initShell(
                    Shell.getShell(),
                    DataBackupApplication.application
                )
            }
        }
    }

    /**
     * Execution functions encapsulated by Log.
     */
    suspend fun LogUtil.execute(logId: Long, cmd: String): Shell.Result = withIOContext {
        logCmd(logId, LogCmdType.SHELL_IN, cmd)
        Shell.cmd(cmd).exec().also { result ->
            for (line in result.out) logCmd(logId, LogCmdType.SHELL_OUT, line)
            if (result.code == 127) {
                // If the code is 127, the shell may have been dead.
                DataBackupApplication.Companion.EnvInitializer.initShell(
                    Shell.getShell(),
                    DataBackupApplication.application
                )
                logCmd(logId, LogCmdType.SHELL_OUT, "The shell may have been dead.")
            }
            logCmd(logId, LogCmdType.SHELL_CODE, result.code.toString())
        }
    }

    /**
     * Execution functions encapsulated by Log with given shell.
     */
    suspend fun LogUtil.execute(logId: Long, cmd: String, shell: Shell): Shell.Result = withIOContext {
        logCmd(logId, LogCmdType.SHELL_IN, cmd)
        val out = mutableListOf<String>()
        shell.newJob().to(out).add(cmd).exec().also { result ->
            for (line in out) logCmd(logId, LogCmdType.SHELL_OUT, line)
            logCmd(logId, LogCmdType.SHELL_CODE, result.code.toString())
        }
    }

    fun Shell.Result.outString(): String {
        return out.joinToString(separator = "\n")
    }
}
