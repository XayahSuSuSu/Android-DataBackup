package com.xayah.core.util

/**
 * For modules, use reflection to get [com.xayah.databackup.BuildConfig].
 */
private fun fromBuildConfig(key: String): Any? =
    runCatching { Class.forName("com.xayah.databackup.BuildConfig").getField(key).get(null) }.getOrNull()

object BuildConfigUtil {
    val ENABLE_VERBOSE = runCatching { fromBuildConfig("ENABLE_VERBOSE") as Boolean }.getOrDefault(false)
    val VERSION_NAME = runCatching { fromBuildConfig("VERSION_NAME") as String }.getOrDefault("")
    val FLAVOR_feature = runCatching { fromBuildConfig("FLAVOR_feature") as String }.getOrDefault("")
    val FLAVOR_abi = runCatching { fromBuildConfig("FLAVOR_abi") as String }.getOrDefault("")
}
