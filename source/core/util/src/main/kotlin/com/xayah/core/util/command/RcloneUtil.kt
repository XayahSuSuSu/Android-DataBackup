package com.xayah.core.util.command

import com.xayah.core.util.SymbolUtil.QUOTE
import com.xayah.core.util.model.ShellResult

object Rclone {
    private val shell = BaseUtil.getNewShell()
    private suspend fun execute(vararg args: String): ShellResult = BaseUtil.execute("rclone", *args, shell = shell)
    private const val argRetries = "--retries 1 --low-level-retries 3"

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
        execute(
            "mount",
            "${QUOTE}$src${QUOTE}",
            "${QUOTE}$dst${QUOTE}",
            *args,
        )
    }

    suspend fun mkdir(dst: String, dryRun: Boolean = false): ShellResult = run {
        // rclone mkdir "$dst" *args
        execute(
            "mkdir",
            "$QUOTE$dst$QUOTE",
            if (dryRun) "--dry-run" else "",
            argRetries,
        )
    }

    suspend fun copy(src: String, dst: String): ShellResult = run {
        // rclone copy $src $dst
        execute(
            "copy",
            "$QUOTE$src$QUOTE",
            "$QUOTE$dst$QUOTE",
            argRetries,
        )
    }

    suspend fun size(src: String): ShellResult = run {
        // rclone size $src
        execute(
            "size",
            "$QUOTE$src$QUOTE",
            "--json",
            argRetries
        )
    }

    suspend fun purge(src: String): ShellResult = run {
        // rclone purge $src
        execute(
            "purge",
            "$QUOTE$src$QUOTE",
            argRetries,
        )
    }

    suspend fun rmdirs(src: String): ShellResult = run {
        // rclone rmdirs $src
        execute(
            "rmdirs",
            "$QUOTE$src$QUOTE",
            argRetries,
        )
    }
}
