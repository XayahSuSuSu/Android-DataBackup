package com.xayah.core.model

import com.xayah.core.model.database.CloudEntity

const val DefaultPreserveId = 0L

data class BlacklistAppItem(
    var packageName: String,
    var userId: Int,
)

data class ConfigurationsBlacklist(
    var apps: List<BlacklistAppItem>
)

data class Configurations(
    val blacklist: ConfigurationsBlacklist,
    var cloud: List<CloudEntity>
)
