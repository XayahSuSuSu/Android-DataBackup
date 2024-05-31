package com.xayah.core.util.command

object PackageUtil {
    suspend fun hasKeystore(su: String, uid: Int): Boolean =
        // su $uid -c keystore_cli_v2 list
        BaseUtil.execute(
            su,
            uid.toString(),
            "-c",
            "keystore_cli_v2",
            "list",
            log = false
        ).out.size > 1
}
