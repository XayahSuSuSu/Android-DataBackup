package com.xayah.databackup.util

import android.content.Context
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.flow.first

object ShellHelper {
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
}
