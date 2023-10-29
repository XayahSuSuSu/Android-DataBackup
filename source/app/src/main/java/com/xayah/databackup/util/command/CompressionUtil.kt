package com.xayah.databackup.util.command

import com.xayah.core.model.ShellResult
import com.xayah.core.util.toSpaceString
import com.xayah.core.util.trim
import com.xayah.core.util.SymbolUtil.QUOTE

object Tar {
    suspend fun execute(vararg args: String): ShellResult = CommonUtil.execute("tar", *args)

    suspend fun compressInCur(usePipe: Boolean, cur: String, src: String, dst: String, extra: String): ShellResult {
        // Move to $cur path.
        CommonUtil.execute("cd", cur)

        // Compress
        val result = if (usePipe) {
            if (extra.isEmpty()) {
                // tar --totals -cpf - $src > "$dst"
                execute(
                    "--totals",
                    "-cpf",
                    "- $src",
                    ">",
                    "$QUOTE$dst$QUOTE",
                )
            } else {
                // tar --totals -cpf - $src | $extra > "$dst"
                execute(
                    "--totals",
                    "-cpf",
                    "- $src",
                    "| $extra",
                    ">",
                    "$QUOTE$dst$QUOTE",
                )
            }
        } else {
            if (extra.isEmpty()) {
                // tar --totals -cpf "$dst" $src
                execute(
                    "--totals",
                    "-cpf",
                    "$QUOTE$dst$QUOTE",
                    src,
                )
            } else {
                // tar --totals -cpf "$dst" $src -I "$extra"
                execute(
                    "--totals",
                    "-cpf",
                    "$QUOTE$dst$QUOTE",
                    src,
                    "-I",
                    "$QUOTE$extra$QUOTE",
                )
            }
        }

        // Move back
        CommonUtil.execute("cd", "/")

        return result
    }

    suspend fun compress(usePipe: Boolean, exclusionList: List<String>, h: String, srcDir: String, src: String, dst: String, extra: String): ShellResult =
        run {
            val exclusion = exclusionList.trim().map { "--exclude=$it" }.toSpaceString()
            if (usePipe) {
                if (extra.isEmpty()) {
                    // tar --totals "$exclusion" $h -cpf - -C "$srcDir" "$src" > "$dst"
                    execute(
                        "--totals",
                        exclusion,
                        h,
                        "-cpf",
                        "-",
                        "-C",
                        "$QUOTE$srcDir$QUOTE",
                        "$QUOTE$src$QUOTE",
                        ">",
                        "$QUOTE$dst$QUOTE",
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
                        "$QUOTE$srcDir$QUOTE",
                        "$QUOTE$src$QUOTE",
                        "| $extra",
                        ">",
                        "$QUOTE$dst$QUOTE",
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
                    "$QUOTE$dst$QUOTE",
                    "-C",
                    "$QUOTE$srcDir$QUOTE",
                    "$QUOTE$src$QUOTE",
                )
            } else {
                // tar --totals "$exclusion" $h -cpf "$dst" -C "$srcDir" "$src" -I "$extra"
                execute(
                    "--totals",
                    exclusion,
                    h,
                    "-cpf",
                    "$QUOTE$dst$QUOTE",
                    "-C",
                    "$QUOTE$srcDir$QUOTE",
                    "$QUOTE$src$QUOTE",
                    "-I",
                    "$QUOTE$extra$QUOTE",
                )
            }
        }
    }

    suspend fun test(src: String, extra: String): ShellResult = if (extra.isEmpty()) {
        // tar -t -f "$src" > /dev/null 2>&1
        execute(
            "-t",
            "-f",
            "$QUOTE$src$QUOTE",
            ">",
            "/dev/null",
            "2>&1",
        )
    } else {
        // tar -t -f "$src" -I "$extra" > /dev/null 2>&1
        execute(
            "-t",
            "-f",
            "$QUOTE$src$QUOTE",
            "-I",
            "$QUOTE$extra$QUOTE",
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
                "$QUOTE$src$QUOTE",
                "-C",
                "$QUOTE$dst$QUOTE",
            )
        } else {
            // tar --totals -I "$extra" -xmpf "$src" -C "$dst"
            execute(
                "--totals",
                "-I",
                "$QUOTE$extra$QUOTE",
                "-xmpf",
                "$QUOTE$src$QUOTE",
                "-C",
                "$QUOTE$dst$QUOTE",
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
                "$QUOTE$src$QUOTE",
                "-C",
                "$QUOTE$dst$QUOTE",
            )
        } else {
            // tar --totals "$exclusion" $clear -I "$extra" -xmpf "$src" -C "$dst"
            execute(
                "--totals",
                exclusion,
                clear,
                "-I",
                "$QUOTE$extra$QUOTE",
                if (m) "-xmpf" else "-xpf",
                "$QUOTE$src$QUOTE",
                "-C",
                "$QUOTE$dst$QUOTE",
            )
        }
    }
}
