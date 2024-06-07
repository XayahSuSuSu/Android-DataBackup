package com.xayah.core.util.command

import com.xayah.core.common.util.leSdk33
import com.xayah.core.common.util.ltSdk30
import com.xayah.core.util.SymbolUtil.QUOTE
import com.xayah.core.util.model.ShellResult

object Pm {
    private suspend fun execute(vararg args: String): ShellResult = BaseUtil.execute("pm", *args)
    suspend fun install(userId: Int, src: String): ShellResult = if (ltSdk30) {
        // pm install --user "$userId" -r -t "$src"
        execute(
            "install",
            "--user",
            "$QUOTE$userId$QUOTE",
            "-r",
            "-t",
            "$QUOTE$src$QUOTE",
        )
    } else if (leSdk33) {
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
    } else {
        // pm install --bypass-low-target-sdk-block -i com.android.vending --user "$userId" -r -t "$src"
        execute(
            "install",
            "--bypass-low-target-sdk-block",
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
        suspend fun create(userId: Int): ShellResult = if (ltSdk30) {
            // pm install-create --user "$userId" -t | grep -E -o '[0-9]+'
            execute(
                "install-create",
                "--user",
                "$QUOTE$userId$QUOTE",
                "-t",
                "|",
                "grep -E -o '[0-9]+'",
            )
        } else if (leSdk33) {
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
        } else {
            // pm install-create --bypass-low-target-sdk-block -i com.android.vending --user "$userId" -t | grep -E -o '[0-9]+'
            execute(
                "install-create",
                "--bypass-low-target-sdk-block",
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
