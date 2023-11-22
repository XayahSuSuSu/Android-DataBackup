package com.xayah.core.util.command

import com.xayah.core.common.util.toSpaceString
import com.xayah.core.common.util.trim
import com.xayah.core.util.SymbolUtil
import com.xayah.core.util.model.ShellResult

object Tar {
    private suspend fun execute(vararg args: String): ShellResult = BaseUtil.execute("tar", *args)

    suspend fun compressInCur(usePipe: Boolean, cur: String, src: String, dst: String, extra: String): ShellResult = run {
        // Move to $cur path.
        BaseUtil.execute("cd", cur)

        // Compress
        val result = if (usePipe) {
            if (extra.isEmpty()) {
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
                    "| $extra",
                    ">",
                    "${SymbolUtil.QUOTE}$dst${SymbolUtil.QUOTE}",
                )
            }
        } else {
            if (extra.isEmpty()) {
                // tar --totals -cpf "$dst" $src
                execute(
                    "--totals",
                    "-cpf",
                    "${SymbolUtil.QUOTE}$dst${SymbolUtil.QUOTE}",
                    src,
                )
            } else {
                // tar --totals -cpf "$dst" $src -I "$extra"
                execute(
                    "--totals",
                    "-cpf",
                    "${SymbolUtil.QUOTE}$dst${SymbolUtil.QUOTE}",
                    src,
                    "-I",
                    "${SymbolUtil.QUOTE}$extra${SymbolUtil.QUOTE}",
                )
            }
        }

        // Move back
        BaseUtil.execute("cd", "/")

        if (result.code == 1) result.code = 0
        result
    }

    suspend fun compress(usePipe: Boolean, exclusionList: List<String>, h: String, srcDir: String, src: String, dst: String, extra: String): ShellResult =
        run {
            val exclusion = exclusionList.trim().map { "--exclude=$it" }.toSpaceString()
            val result = if (usePipe) {
                if (extra.isEmpty()) {
                    // tar --totals "$exclusion" $h -cpf - -C "$srcDir" "$src" > "$dst"
                    execute(
                        "--totals",
                        exclusion,
                        h,
                        "-cpf",
                        "-",
                        "-C",
                        "${SymbolUtil.QUOTE}$srcDir${SymbolUtil.QUOTE}",
                        "${SymbolUtil.QUOTE}$src${SymbolUtil.QUOTE}",
                        ">",
                        "${SymbolUtil.QUOTE}$dst${SymbolUtil.QUOTE}",
                    )
                } else {
                    // tar --totals "$exclusion" $h -cpf - -C "$srcDir" "$src" | $extra > "$dst"
                    execute(
                        "--totals",
                        exclusion,
                        h,
                        "-cpf",
                        "-",
                        "-C",
                        "${SymbolUtil.QUOTE}$srcDir${SymbolUtil.QUOTE}",
                        "${SymbolUtil.QUOTE}$src${SymbolUtil.QUOTE}",
                        "| $extra",
                        ">",
                        "${SymbolUtil.QUOTE}$dst${SymbolUtil.QUOTE}",
                    )
                }
            } else {
                if (extra.isEmpty()) {
                    // tar --totals "$exclusion" $h -cpf "$dst" -C "$srcDir" "$src"
                    execute(
                        "--totals",
                        exclusion,
                        h,
                        "-cpf",
                        "${SymbolUtil.QUOTE}$dst${SymbolUtil.QUOTE}",
                        "-C",
                        "${SymbolUtil.QUOTE}$srcDir${SymbolUtil.QUOTE}",
                        "${SymbolUtil.QUOTE}$src${SymbolUtil.QUOTE}",
                    )
                } else {
                    // tar --totals "$exclusion" $h -cpf "$dst" -C "$srcDir" "$src" -I "$extra"
                    execute(
                        "--totals",
                        exclusion,
                        h,
                        "-cpf",
                        "${SymbolUtil.QUOTE}$dst${SymbolUtil.QUOTE}",
                        "-C",
                        "${SymbolUtil.QUOTE}$srcDir${SymbolUtil.QUOTE}",
                        "${SymbolUtil.QUOTE}$src${SymbolUtil.QUOTE}",
                        "-I",
                        "${SymbolUtil.QUOTE}$extra${SymbolUtil.QUOTE}",
                    )
                }
            }

            if (result.code == 1) result.code = 0
            result
        }

    suspend fun test(src: String, extra: String): ShellResult = if (extra.isEmpty()) {
        // tar -t -f "$src" > /dev/null 2>&1
        execute(
            "-t",
            "-f",
            "${SymbolUtil.QUOTE}$src${SymbolUtil.QUOTE}",
            ">",
            "/dev/null",
            "2>&1",
        )
    } else {
        // tar -t -f "$src" -I "$extra" > /dev/null 2>&1
        execute(
            "-t",
            "-f",
            "${SymbolUtil.QUOTE}$src${SymbolUtil.QUOTE}",
            "-I",
            "${SymbolUtil.QUOTE}$extra${SymbolUtil.QUOTE}",
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
            // tar --totals -I "$extra" -xmpf "$src" -C "$dst"
            execute(
                "--totals",
                "-I",
                "${SymbolUtil.QUOTE}$extra${SymbolUtil.QUOTE}",
                "-xmpf",
                "${SymbolUtil.QUOTE}$src${SymbolUtil.QUOTE}",
                "-C",
                "${SymbolUtil.QUOTE}$dst${SymbolUtil.QUOTE}",
            )
        }
    }

    suspend fun decompress(src: String, dst: String, extra: String, target: String): ShellResult = run {
        if (extra.isEmpty()) {
            // tar --totals -xpf "$src" -C "$dst" --wildcards --no-anchored "$target"
            execute(
                "--totals",
                "-xmpf",
                "${SymbolUtil.QUOTE}$src${SymbolUtil.QUOTE}",
                "-C",
                "${SymbolUtil.QUOTE}$dst${SymbolUtil.QUOTE}",
                "--wildcards",
                "--no-anchored",
                "${SymbolUtil.QUOTE}$target${SymbolUtil.QUOTE}",
            )
        } else {
            // tar --totals -I "$extra" -xpf "$src" -C "$dst" --wildcards --no-anchored "$target"
            execute(
                "--totals",
                "-I",
                "${SymbolUtil.QUOTE}$extra${SymbolUtil.QUOTE}",
                "-xmpf",
                "${SymbolUtil.QUOTE}$src${SymbolUtil.QUOTE}",
                "-C",
                "${SymbolUtil.QUOTE}$dst${SymbolUtil.QUOTE}",
                "--wildcards",
                "--no-anchored",
                "${SymbolUtil.QUOTE}$target${SymbolUtil.QUOTE}",
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
            // tar --totals "$exclusion" $clear -I "$extra" -xmpf "$src" -C "$dst"
            execute(
                "--totals",
                exclusion,
                clear,
                "-I",
                "${SymbolUtil.QUOTE}$extra${SymbolUtil.QUOTE}",
                if (m) "-xmpf" else "-xpf",
                "${SymbolUtil.QUOTE}$src${SymbolUtil.QUOTE}",
                "-C",
                "${SymbolUtil.QUOTE}$dst${SymbolUtil.QUOTE}",
            )
        }
    }
}
