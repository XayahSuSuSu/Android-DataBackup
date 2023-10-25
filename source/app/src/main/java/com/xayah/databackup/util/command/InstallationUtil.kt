package com.xayah.databackup.util.command

import android.os.Build
import com.xayah.core.model.ShellResult
import com.xayah.databackup.util.SymbolUtil.QUOTE

object Pm {
    suspend fun execute(vararg args: String): ShellResult = CommonUtil.execute("pm", *args)
    suspend fun install(userId: Int, src: String): ShellResult = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
        // pm install --user "$userId" -r -t "$src"
        execute(
            "install",
            "--user",
            "$QUOTE$userId$QUOTE",
            "-r",
            "-t",
            "$QUOTE$src$QUOTE",
        )
    } else {
        // pm install -i com.android.vending --user "$userId" -r -t "$src"
        execute(
            "install",
            "-i",
            "com.android.vending",
            "--user",
            "$QUOTE$userId$QUOTE",
            "-r",
            "-t",
            "$QUOTE$src$QUOTE",
        )
    }

    object Install {
        suspend fun create(userId: Int): ShellResult = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            // pm install-create --user "$userId" -t | grep -E -o '[0-9]+'
            execute(
                "install-create",
                "--user",
                "$QUOTE$userId$QUOTE",
                "-t",
                "|",
                "grep -E -o '[0-9]+'",
            )
        } else {
            // pm install-create -i com.android.vending --user "$userId" -t | grep -E -o '[0-9]+'
            execute(
                "install-create",
                "-i",
                "com.android.vending",
                "--user",
                "$QUOTE$userId$QUOTE",
                "-t",
                "|",
                "grep -E -o '[0-9]+'",
            )
        }

        suspend fun write(session: String, srcName: String, src: String): ShellResult = run {
            // pm install-write "$session" "$srcDir" "$src"
            execute(
                "install-write",
                "$QUOTE$session$QUOTE",
                "$QUOTE$srcName$QUOTE",
                "$QUOTE$src$QUOTE",
            )
        }

        suspend fun commit(session: String): ShellResult = run {
            // pm install-commit "$session"
            execute(
                "install-commit",
                "$QUOTE$session$QUOTE",
            )
        }
    }
}
