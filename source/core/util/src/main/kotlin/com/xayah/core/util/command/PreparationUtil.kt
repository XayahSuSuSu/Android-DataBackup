package com.xayah.core.util.command

import com.xayah.core.util.SymbolUtil.BACKSLASH
import com.xayah.core.util.SymbolUtil.QUOTE
import com.xayah.core.util.SymbolUtil.USD
import com.xayah.core.util.command.BaseUtil.execute
import com.xayah.core.util.model.ShellResult

object PreparationUtil {
    suspend fun listExternalStorage(): ShellResult = run {
        // mount | awk '$3 ~ /\mnt\/media_rw/ {print $3}'
        execute(
            "mount",
            "|",
            "awk",
            "'${USD}3 ~ /${BACKSLASH}mnt${BACKSLASH}/media_rw/ {print ${USD}3}'",
        )
    }

    suspend fun getExternalStorageType(path: String): ShellResult = run {
        // mount | awk '$3 == "/mnt/media_rw/6EBF-FE14" {print $5}'
        execute(
            "mount",
            "|",
            "awk",
            "'${USD}3 == ${QUOTE}${path}${QUOTE} {print ${USD}5}'",
        )
    }
}
