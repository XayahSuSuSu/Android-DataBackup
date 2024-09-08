package com.xayah.core.model

import com.google.gson.annotations.SerializedName
import com.xayah.core.model.database.CloudEntity

const val DefaultPreserveId = 0L

data class BlacklistAppItem(
    var packageName: String,
    var userId: Int,
)

data class BlacklistFileItem(
    var name: String,
    var path: String,
)

data class FileItem(
    var name: String,
    var path: String,
)

data class ConfigurationsBlacklist(
    var apps: List<BlacklistAppItem>,
    var files: List<BlacklistFileItem>,
)

data class Configurations(
    val blacklist: ConfigurationsBlacklist,
    var cloud: List<CloudEntity>,
    var file: List<FileItem>
)

data class ContributorItem(
    val name: String,
    var avatar: String,
    var desc: String,
    var link: String,
)

data class TranslatorItem(
    val email: String,
    @SerializedName("full_name") var fullName: String,
    @SerializedName("change_count") var changeCount: String,
)

/**
 * Action value(Int):
 * 0: Replace
 * 1: Remove
 */
data class TranslatorRevisionItem(
    val name: String,
    var avatar: String,
    var link: String,
    var actions: Map<String, Int>,
)

data class UserInfo(
    var id: Int,
    var name: String,
)
