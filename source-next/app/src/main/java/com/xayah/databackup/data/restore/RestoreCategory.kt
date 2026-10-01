package com.xayah.databackup.data.restore

import com.xayah.databackup.data.rustic.RusticSourceCategory
import com.xayah.databackup.database.entity.Option
import kotlinx.serialization.Serializable

@Serializable
enum class RestoreCategory { Apps, Networks, Contacts, CallLogs, Messages }

val AppRestoreParts = setOf(
    RusticSourceCategory.Apk,
    RusticSourceCategory.InternalData,
    RusticSourceCategory.ExternalData,
    RusticSourceCategory.AdditionalData,
)

internal fun Set<RusticSourceCategory>.toAppOptions() = Option(
    apk = RusticSourceCategory.Apk in this,
    internalData = RusticSourceCategory.InternalData in this,
    externalData = RusticSourceCategory.ExternalData in this,
    additionalData = RusticSourceCategory.AdditionalData in this,
)
