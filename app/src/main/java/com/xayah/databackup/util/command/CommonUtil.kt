package com.xayah.databackup.util.command

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import com.topjohnwu.superuser.Shell
import com.xayah.databackup.DataBackupApplication
import com.xayah.databackup.util.ConstantUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object CommonUtil {
    /**
     * Switch to IO coroutine
     */
    private suspend fun <T> runOnIO(block: suspend () -> T): T {
        return withContext(Dispatchers.IO) { block() }
    }

    fun Context.copyToClipboard(content: String) {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.setPrimaryClip(ClipData.newPlainText(ConstantUtil.ClipDataLabel, content))
    }

    /**
     * Execution functions encapsulated by Log
     */
    suspend fun execute(cmd: String, logcat: Boolean = true): Shell.Result {
        val tag = object {}.javaClass.enclosingMethod?.name

        val result = runOnIO {
            if (logcat) Log.d(tag, "SHELL_IN: $cmd")
            Shell.cmd(cmd).exec().apply {
                if (logcat) for (i in this.out) Log.d(tag, "SHELL_OUT: $i")
            }
        }

        if (result.code == 127) {
            // If the code is 127, the shell may have been dead.
            DataBackupApplication.Companion.EnvInitializer.initShell(
                Shell.getShell(),
                DataBackupApplication.application
            )

            if (logcat) Log.d(tag, "The shell may have been dead.")
        }

        return result
    }

    fun Shell.Result.outString(): String {
        return out.joinToString(separator = "\n")
    }
}
