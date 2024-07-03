package com.xayah.core.util.command

import com.xayah.core.common.util.toSpaceString
import com.xayah.core.common.util.trim
import com.xayah.core.util.SymbolUtil
import com.xayah.core.util.model.ShellResult

object Tar {
    private suspend fun execute(vararg args: String): ShellResult = BaseUtil.execute("tar", *args)

    suspend fun compressInCur(usePipe: Boolean, cur: String, src: String, dst: String, extra: String): ShellResult {
        // Move to $cur path.
        BaseUtil.execute("cd", cur)

        // Compress
        val result = if (extra.isEmpty()) {
            // tar -cpf - $src > "$dst"
            execute(
                "-cpf",
                "- $src",
                ">",
                "${SymbolUtil.QUOTE}$dst${SymbolUtil.QUOTE}",
            )
        } else {
            // tar -cpf - $src | $extra > "$dst"
            execute(
                "-cpf",
                "- $src",
                "| $extra",
                ">",
                "${SymbolUtil.QUOTE}$dst${SymbolUtil.QUOTE}",
            )
        }

        // Move back
        BaseUtil.execute("cd", "/")

        return result
    }

    suspend fun compress(usePipe: Boolean, exclusionList: List<String>, h: String, srcDir: String, src: String, dst: String, extra: String): ShellResult =
        run {
            val exclusion = exclusionList.trim().map { "--exclude=$it" }.toSpaceString()
            if (extra.isEmpty()) {
                // tar "$exclusion" $h -cpf - -C "$srcDir" -- "$src" > "$dst"
                execute(
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
                // tar "$exclusion" $h -cpf - -C "$srcDir" -- "$src" | $extra > "$dst"
                execute(
                    exclusion,
                    h,
                    "-cpf",
                    "-",
                    "-C",
                    "${SymbolUtil.QUOTE}$srcDir${SymbolUtil.QUOTE}",
                    "--",
                    "${SymbolUtil.QUOTE}$src${SymbolUtil.QUOTE}",
                    "| $extra",
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
            // tar -xmpf "$src" -C "$dst"
            execute(
                "-xmpf",
                "${SymbolUtil.QUOTE}$src${SymbolUtil.QUOTE}",
                "-C",
                "${SymbolUtil.QUOTE}$dst${SymbolUtil.QUOTE}",
            )
        } else {
            // zstd -d -c "$src" | tar -xmpf - -C "$dst"
            BaseUtil.execute(
                "zstd",
                "-d",
                "-c",
                "${SymbolUtil.QUOTE}$src${SymbolUtil.QUOTE}",
                "|",
                "tar",
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
            // tar "$exclusion" -xmpf "$src" -C "$dst"
            execute(
                exclusion,
                if (m) "-xmpf" else "-xpf",
                "${SymbolUtil.QUOTE}$src${SymbolUtil.QUOTE}",
                "-C",
                "${SymbolUtil.QUOTE}$dst${SymbolUtil.QUOTE}",
            )
        } else {
            // zstd -d -c "$src" | tar "$exclusion" -xmpf - -C "$dst"
            BaseUtil.execute(
                "zstd",
                "-d",
                "-c",
                "${SymbolUtil.QUOTE}$src${SymbolUtil.QUOTE}",
                "|",
                "tar",
                exclusion,
                if (m) "-xmpf" else "-xpf",
                "-",
                "-C",
                "${SymbolUtil.QUOTE}$dst${SymbolUtil.QUOTE}",
            )
        }
    }
}
