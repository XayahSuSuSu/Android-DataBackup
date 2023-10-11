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

data class ShellResult(
    var code: Int,
    var out: List<String>,
) {
    val isSuccess: Boolean
        get() = code == 0

    val outString: String
        get() = out.joinToString(separator = "\n")

}

object CommonUtil {
    fun Context.copyToClipboard(content: String) {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.setPrimaryClip(ClipData.newPlainText(ConstantUtil.ClipDataLabel, content))
    }

    suspend fun execute(cmd: String): ShellResult = withIOContext {
        val shellResult = ShellResult(code = -1, out = listOf())

        Shell.cmd(cmd).exec().also { result ->
            shellResult.code = result.code
            shellResult.out = result.out

            if (shellResult.code == 127) {
                // If the code is 127, the shell may have been dead.
                DataBackupApplication.Companion.EnvInitializer.initShell(
                    Shell.getShell(),
                    DataBackupApplication.application
                )
            }
        }
        shellResult
    }

    /**
     * Execution functions encapsulated by Log.
     */
    suspend fun LogUtil.execute(logId: Long, cmd: String): ShellResult = withIOContext {
        val shellResult = ShellResult(code = -1, out = listOf())

        logCmd(logId, LogCmdType.SHELL_IN, cmd)
        Shell.cmd(cmd).exec().also { result ->
            shellResult.code = result.code
            shellResult.out = result.out

            for (line in shellResult.out) logCmd(logId, LogCmdType.SHELL_OUT, line)
            if (shellResult.code == 127) {
                // If the code is 127, the shell may have been dead.
                DataBackupApplication.Companion.EnvInitializer.initShell(
                    Shell.getShell(),
                    DataBackupApplication.application
                )
                logCmd(logId, LogCmdType.SHELL_OUT, "The shell may have been dead.")
            }
            logCmd(logId, LogCmdType.SHELL_CODE, shellResult.code.toString())
        }
        shellResult
    }

    /**
     * Execution functions encapsulated by Log with given shell.
     */
    suspend fun LogUtil.execute(logId: Long, cmd: String, shell: Shell): ShellResult = withIOContext {
        val shellResult = ShellResult(code = -1, out = listOf())

        logCmd(logId, LogCmdType.SHELL_IN, cmd)
        val outList = mutableListOf<String>()

        shell.newJob().to(outList, outList).add(cmd).exec().also { result ->
            shellResult.code = result.code
            logCmd(logId, LogCmdType.SHELL_CODE, result.code.toString())
        }
        shellResult.out = outList
        shellResult.out.forEach { line ->
            logCmd(logId, LogCmdType.SHELL_OUT, line)
        }

        shellResult
    }
}
