package com.xayah.databackup.util.command

import com.xayah.databackup.util.SymbolUtil.QUOTE
import com.xayah.databackup.util.SymbolUtil.USD
import com.xayah.databackup.util.command.CommonUtil.execute

object SELinux {
    suspend fun getContext(path: String): ShellResult = run {
        // ls -Zd "$path" | awk 'NF>1{print $1}'
        execute(
            "ls",
            "-Zd",
            "$QUOTE$path$QUOTE",
            "|",
            "awk 'NF>1{print ${USD}1}'"
        )
    }

    suspend fun chown(uid: Int, path: String): ShellResult = run {
        // chown -hR "$uid:$uid" "$path/"
        execute(
            "chown",
            "-hR",
            "$QUOTE$uid:$uid$QUOTE",
            "$QUOTE$path/$QUOTE",
        )
    }

    suspend fun chcon(context: String, path: String): ShellResult = run {
        // chcon -hR "$context" "$path/"
        execute(
            "chcon",
            "-hR",
            "$QUOTE$context$QUOTE",
            "$QUOTE$path/$QUOTE",
        )
    }
}