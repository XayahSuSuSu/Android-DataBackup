package com.xayah.databackup.entity.backup

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.xayah.databackup.service.restore.ApkInstallProcess.Companion.GOOGLE_PLAY_PACKAGE_NAME

/**
 * Records the app's installation source at backup time.
 */
@JsonClass(generateAdapter = true)
data class ApkInstallSource(
    val initiatingPackageName: String? = null,
    val installingPackageName: String? = null,
    val originatingPackageName: String? = null,
) {
    val isPlayStore: Boolean get() = installingPackageName == GOOGLE_PLAY_PACKAGE_NAME

    fun serialize(): String = Moshi.Builder().build().adapter<ApkInstallSource>().toJson(this)

    companion object {
        const val FILE_NAME = "install-source.json"

        fun deserialize(content: String): ApkInstallSource = requireNotNull(Moshi.Builder().build().adapter<ApkInstallSource>().fromJson(content))
    }
}
