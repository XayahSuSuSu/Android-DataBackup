package com.xayah.core.common.util

/**
 * For modules, use reflection to get [com.xayah.databackup.BuildConfig].
 */
private fun fromBuildConfig(key: String): Any? =
    runCatching { Class.forName("com.xayah.databackup.BuildConfig").getField(key).get(null) }.getOrNull()

object BuildConfigUtil {
    val DEBUG = runCatching { fromBuildConfig("DEBUG") as Boolean }.getOrDefault(false)
    val ENABLE_VERBOSE = runCatching { fromBuildConfig("ENABLE_VERBOSE") as Boolean }.getOrDefault(false)
    val VERSION_NAME = runCatching { fromBuildConfig("VERSION_NAME") as String }.getOrDefault("")
    val VERSION_CODE = runCatching { fromBuildConfig("VERSION_CODE") as Int }.getOrDefault(0).toLong()
    val FLAVOR_feature = runCatching { fromBuildConfig("FLAVOR_feature") as String }.getOrDefault("")
    val FLAVOR_abi = runCatching { fromBuildConfig("FLAVOR_abi") as String }.getOrDefault("")
    val SUPPORTED_LOCALES = runCatching { fromBuildConfig("SUPPORTED_LOCALES") as Array<String> }.getOrDefault(arrayOf())
}
