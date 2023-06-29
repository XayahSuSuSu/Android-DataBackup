package com.xayah.databackup.util.command

import com.xayah.databackup.util.SymbolUtil

object PreparationUtil {
    suspend fun listExternalStorage(): List<String> {
        // mount | awk '$3 ~ /\mnt\/media_rw/ {print $3, $5}'
        val exec =
            CommonUtil.execute("mount | awk '${SymbolUtil.USD}3 ~ /${SymbolUtil.BACKSLASH}mnt${SymbolUtil.BACKSLASH}/media_rw/ {print ${SymbolUtil.USD}3, ${SymbolUtil.USD}5}'; mount > /dev/null 2>&1")
        return exec.out
    }
}
