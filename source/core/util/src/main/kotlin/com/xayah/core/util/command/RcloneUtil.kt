package com.xayah.core.util.command

import com.xayah.core.util.CloudTmpTestFileName
import com.xayah.core.util.DateUtil
import com.xayah.core.util.SymbolUtil.QUOTE
import com.xayah.core.util.model.ShellResult

private fun List<String>.dropDebugMsg() = this.filter { (it.split(" ").getOrNull(2) ?: "") != "DEBUG" }

object Rclone {
    private val shell = BaseUtil.getNewShell()
    private suspend fun execute(vararg args: String): ShellResult = BaseUtil.execute("rclone", *args, shell = shell)
    private suspend fun executeWithExtension(vararg args: String, retries: Boolean = true, logToFile: Boolean = true): ShellResult = run {
        val shellResult = ShellResult(code = -1, input = listOf(), out = listOf())

        execute(
            *args,
            if (retries) argRetries else "",
            if (logToFile) argLogFile else "",
        ).also { result ->
            shellResult.code = result.code
            shellResult.input = result.input
            shellResult.out = result.out.dropDebugMsg()
        }

        shellResult
    }

    private val timestamp: Long = DateUtil.getTimestamp()
    private const val LOG_FILE_Prefix = "rclone_"

    private const val argRetries = "--retries 1 --low-level-retries 3"
    private lateinit var argLogFile: String

    fun initialize(logDir: String) = run {
        this.argLogFile = "-vv 2>&1 | tee $logDir/$LOG_FILE_Prefix$timestamp"
    }

    object Config {
        suspend fun create(name: String, type: String, vararg args: String): ShellResult = run {
            // rclone config create "${name}" "${type}" "$args"
            execute(
                "config",
                "create",
                "${QUOTE}$name${QUOTE}",
                "${QUOTE}$type${QUOTE}",
                *args,
            )
        }

        suspend fun dump(): ShellResult = run {
            // rclone config dump
            execute(
                "config",
                "dump",
            )
        }

        suspend fun delete(name: String): ShellResult = run {
            // rclone config delete "$name"
            execute(
                "config",
                "delete",
                "${QUOTE}$name${QUOTE}",
            )
        }
    }

    suspend fun mount(src: String, dst: String, vararg args: String): ShellResult = run {
        // rclone mount "$src" "$dst" "$args"
        executeWithExtension(
            "mount",
            "${QUOTE}$src${QUOTE}",
            "${QUOTE}$dst${QUOTE}",
            *args,
            retries = false,
        )
    }

    suspend fun mkdir(dst: String): ShellResult = run {
        // rclone mkdir "$dst" *args
        executeWithExtension(
            "mkdir",
            "$QUOTE$dst$QUOTE",
        )
    }

    /**
     * Try creating a tmp dir to test remote IO.
     */
    suspend fun testIO(remote: String): ShellResult = run {
        val dstDir = "$remote/$CloudTmpTestFileName"
        mkdir(dst = dstDir).also { result ->
            if (result.isSuccess) {
                rmdirs(src = dstDir)
            }
        }
    }

    suspend fun copy(src: String, dst: String): ShellResult = run {
        // rclone copy $src $dst
        executeWithExtension(
            "copy",
            "$QUOTE$src$QUOTE",
            "$QUOTE$dst$QUOTE",
        )
    }

    suspend fun size(src: String): ShellResult = run {
        // rclone size $src
        executeWithExtension(
            "size",
            "$QUOTE$src$QUOTE",
            "--json",
        )
    }

    suspend fun purge(src: String): ShellResult = run {
        // rclone purge $src
        executeWithExtension(
            "purge",
            "$QUOTE$src$QUOTE",
        )
    }

    suspend fun rmdirs(src: String): ShellResult = run {
        // rclone rmdirs $src
        executeWithExtension(
            "rmdirs",
            "$QUOTE$src$QUOTE",
        )
    }
}
