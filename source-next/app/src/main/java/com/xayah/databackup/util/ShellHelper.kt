package com.xayah.databackup.util

import android.content.Context
import com.topjohnwu.superuser.Shell
import com.xayah.databackup.App
import com.xayah.databackup.util.SymbolHelper.USD
import kotlinx.coroutines.flow.first

object ShellHelper {
    private const val TAG = "ShellHelper"

    private class EnvInitializer : Shell.Initializer() {
        private fun initShell(shell: Shell) {
            shell.newJob()
                .add("nsenter --mount=/proc/1/ns/mnt sh") // Switch to global namespace
                .add("set -o pipefail") // Ensure that the exit code of each command is correct.
                .exec()
        }

        override fun onInit(context: Context, shell: Shell): Boolean {
            initShell(shell)
            return true
        }
    }

    private suspend fun getShellBuilder(context: Context) = Shell.Builder.create()
        .setFlags(Shell.FLAG_MOUNT_MASTER)
        .setInitializers(EnvInitializer::class.java)
        .setCommands(context.readString(CustomSuFile).first())
        .setTimeout(30)

    suspend fun initMainShell(context: Context) = run {
        Shell.enableVerboseLogging = true
        Shell.setDefaultBuilder(getShellBuilder(context))
    }

    private suspend fun getNewShell(context: Context): Shell? = runCatching { getShellBuilder(context).build() }.getOrNull()

    private suspend fun kill(context: Context, vararg keys: String) {
        val shell = getNewShell(context)
        if (shell != null) {
            // ps -A | grep -w $key1 | grep -w $key2 | ... | awk 'NF>1{print $2}' | xargs kill
            val keysArg = keys.map { "| grep -w $it" }.toTypedArray()
            val args = mutableListOf<String>()
            args.add("ps")
            args.add("-A")
            args.addAll(keysArg)
            args.add("|")
            args.add("awk")
            args.add("'NF>1{print ${USD}2}'")
            args.add("|")
            args.add("xargs")
            args.add("kill")
            shell.newJob().to(null, null).add(args.joinToString(separator = " ")).exec()
            shell.close()
        } else {
            LogHelper.e(TAG, "kill", "Failed to get a new shell!")
        }
    }

    suspend fun killRootService() {
        kill(App.application, "${App.application.packageName}:root")
    }

    suspend fun rm(path: String) {
        val shell = getNewShell(App.application)
        if (shell != null) {
            shell.newJob().to(null, null).add("rm $path").exec()
            shell.close()
        } else {
            LogHelper.e(TAG, "rm", "Failed to get a new shell!")
        }
    }
}
