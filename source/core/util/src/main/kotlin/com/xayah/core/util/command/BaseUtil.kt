package com.xayah.core.util.command

import com.topjohnwu.superuser.Shell
import com.xayah.core.util.model.ShellResult
import com.xayah.core.util.trim
import com.xayah.core.util.withIOContext

object BaseUtil {
    suspend fun execute(vararg args: String): ShellResult = withIOContext {
        val shellResult = ShellResult(code = -1, input = args.toList().trim(), out = listOf())

        Shell.cmd(shellResult.inputString).exec().also { result ->
            shellResult.code = result.code
            shellResult.out = result.out
        }
        shellResult
    }
}
