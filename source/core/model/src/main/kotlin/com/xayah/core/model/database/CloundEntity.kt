package com.xayah.core.model.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.xayah.core.model.CloudType
import com.xayah.core.model.SFTPAuthMode
import com.xayah.core.model.SmbAuthMode
import com.xayah.core.model.SmbVersion

data class FTPExtra(
    val port: Int,
)

data class SMBExtra(
    val share: String,
    val port: Int,
    val domain: String,
    val version: List<SmbVersion>,
    val mode: SmbAuthMode = SmbAuthMode.PASSWORD,
)

data class SFTPExtra(
    val port: Int,
    val privateKey: String,
    val mode: SFTPAuthMode = SFTPAuthMode.PASSWORD,
)

data class WebDAVExtra(
    val insecure: Boolean,
)

@Entity
data class CloudEntity(
    @PrimaryKey var name: String,
    val type: CloudType,
    val host: String,
    val user: String,
    val pass: String,
    val remote: String,
    val extra: String,
    val activated: Boolean,
)
