package com.xayah.core.util.command

import com.xayah.core.util.model.ShellResult

object Appops {
    private suspend fun execute(vararg args: String): ShellResult = BaseUtil.execute("appops", *args)
    suspend fun reset(userId: Int, packageName: String): ShellResult = run {
        // appops reset --user $userId $packageName
        execute(
            "reset",
            "--user",
            "$userId",
            packageName,
        )
    }
}
