package com.xayah.core.util.command

import com.xayah.core.common.util.toSpaceString
import com.xayah.core.common.util.trim
import com.xayah.core.util.SymbolUtil
import com.xayah.core.util.model.ShellResult

object Tar {
    private suspend fun execute(vararg args: String): ShellResult = BaseUtil.execute("tar", *args)

    suspend fun compressInCur(cur: String, src: String, dst: String, extra: String): ShellResult {
        // Move to $cur path.
        BaseUtil.execute("cd", cur)

        // Compress
        val result = if (extra.isEmpty()) {
            // tar --totals -cpf - $src > "$dst"
            execute(
                "--totals",
                "-cpf",
                "- $src",
                ">",
                "${SymbolUtil.QUOTE}$dst${SymbolUtil.QUOTE}",
            )
        } else {
            // tar --totals -cpf - $src | $extra > "$dst"
            execute(
                "--totals",
                "-cpf",
                "- $src",
                "|",
                extra,
                ">",
                "${SymbolUtil.QUOTE}$dst${SymbolUtil.QUOTE}",
            )
        }

        // Move back
        BaseUtil.execute("cd", "/")

        return result
    }

    suspend fun compress(exclusionList: List<String>, h: String, srcDir: String, src: String, dst: String, extra: String): ShellResult =
        run {
            val exclusion = exclusionList.trim().map { "--exclude=$it" }.toSpaceString()
            if (extra.isEmpty()) {
                // tar --totals "$exclusion" $h -cpf - -C "$srcDir" -- "$src" > "$dst"
                execute(
                    "--totals",
                    exclusion,
                    h,
                    "-cpf",
                    "-",
                    "-C",
                    "${SymbolUtil.QUOTE}$srcDir${SymbolUtil.QUOTE}",
                    "--",
                    "${SymbolUtil.QUOTE}$src${SymbolUtil.QUOTE}",
                    ">",
                    "${SymbolUtil.QUOTE}$dst${SymbolUtil.QUOTE}",
                )
            } else {
                // tar --totals "$exclusion" $h -cpf - -C "$srcDir" -- "$src" | $extra > "$dst"
                execute(
                    "--totals",
                    exclusion,
                    h,
                    "-cpf",
                    "-",
                    "-C",
                    "${SymbolUtil.QUOTE}$srcDir${SymbolUtil.QUOTE}",
                    "--",
                    "${SymbolUtil.QUOTE}$src${SymbolUtil.QUOTE}",
                    "|",
                    extra,
                    ">",
                    "${SymbolUtil.QUOTE}$dst${SymbolUtil.QUOTE}",
                )
            }
        }

    suspend fun test(src: String, extra: String): ShellResult = if (extra.isEmpty()) {
        // tar -tf "$src" > /dev/null 2>&1
        execute(
            "-tf",
            "${SymbolUtil.QUOTE}$src${SymbolUtil.QUOTE}",
            ">",
            "/dev/null",
            "2>&1",
        )
    } else {
        // zstd -d -c "$src" | tar -tf - > /dev/null 2>&1
        BaseUtil.execute(
            "zstd",
            "-d",
            "-c",
            "${SymbolUtil.QUOTE}$src${SymbolUtil.QUOTE}",
            "|",
            "tar",
            "-tf",
            "-",
            ">",
            "/dev/null",
            "2>&1",
        )
    }

    suspend fun decompress(src: String, dst: String, extra: String): ShellResult = run {
        if (extra.isEmpty()) {
            // tar --totals -xmpf "$src" -C "$dst"
            execute(
                "--totals",
                "-xmpf",
                "${SymbolUtil.QUOTE}$src${SymbolUtil.QUOTE}",
                "-C",
                "${SymbolUtil.QUOTE}$dst${SymbolUtil.QUOTE}",
            )
        } else {
            // zstd -d -c "$src" | tar --totals -xmpf - -C "$dst"
            BaseUtil.execute(
                "zstd",
                "-d",
                "-c",
                "${SymbolUtil.QUOTE}$src${SymbolUtil.QUOTE}",
                "|",
                "tar",
                "--totals",
                "-xmpf",
                "-",
                "-C",
                "${SymbolUtil.QUOTE}$dst${SymbolUtil.QUOTE}",
            )
        }
    }

    suspend fun decompress(exclusionList: List<String>, clear: String, m: Boolean, src: String, dst: String, extra: String): ShellResult = run {
        val exclusion = exclusionList.trim().map { "--exclude=$it" }.toSpaceString()
        if (extra.isEmpty()) {
            // tar --totals "$exclusion" $clear -xmpf "$src" -C "$dst"
            execute(
                "--totals",
                exclusion,
                clear,
                if (m) "-xmpf" else "-xpf",
                "${SymbolUtil.QUOTE}$src${SymbolUtil.QUOTE}",
                "-C",
                "${SymbolUtil.QUOTE}$dst${SymbolUtil.QUOTE}",
            )
        } else {
            // zstd -d -c "$src" | tar --totals "$exclusion" $clear -xmpf - -C "$dst"
            BaseUtil.execute(
                "zstd",
                "-d",
                "-c",
                "${SymbolUtil.QUOTE}$src${SymbolUtil.QUOTE}",
                "|",
                "tar",
                "--totals",
                exclusion,
                clear,
                "-xmpf",
                "-",
                "-C",
                "${SymbolUtil.QUOTE}$dst${SymbolUtil.QUOTE}",
            )
        }
    }

    fun buildCompressInCurStreamCmd(cur: String, src: String, extra: String): String {
        val quotedCur = "${SymbolUtil.QUOTE}$cur${SymbolUtil.QUOTE}"
        val tarCmd = "cd $quotedCur && tar --totals -cpf - $src"
        return if (extra.isEmpty()) tarCmd else "$tarCmd | $extra"
    }

    fun buildCompressStreamCmd(
        exclusionList: List<String>,
        h: String,
        srcDir: String,
        src: String,
        extra: String,
    ): String {
        val exclusion = exclusionList.trim().map { "--exclude=$it" }.toSpaceString()
        val parts = listOf(
            "tar", "--totals", "--warning=no-file-changed", "--warning=no-file-removed",
            exclusion, h, "-cpf", "-",
            "-C", "${SymbolUtil.QUOTE}$srcDir${SymbolUtil.QUOTE}",
            "--", "${SymbolUtil.QUOTE}$src${SymbolUtil.QUOTE}"
        ).filter { it.isNotBlank() }.joinToString(" ")
        // GNU tar 退出码 1 表示打包期间有文件被后台修改（非致命，例如 QQ/微信推送进程），退出码 2 才是致命错误
        val safeTar = "($parts; rc=\$?; [ \$rc -eq 0 ] || [ \$rc -eq 1 ])"
        return if (extra.isEmpty()) safeTar else "$safeTar | $extra"
    }
}
