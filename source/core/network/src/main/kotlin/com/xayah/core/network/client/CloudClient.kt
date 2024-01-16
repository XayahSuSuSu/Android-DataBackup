package com.xayah.core.network.client

import android.content.Context
import com.xayah.core.model.CloudType
import com.xayah.core.model.database.CloudEntity
import com.xayah.core.model.database.FTPExtra
import com.xayah.core.model.database.SMBExtra
import com.xayah.core.network.util.getExtraEntity
import com.xayah.libpickyou.parcelables.DirChildrenParcelable

interface CloudClient {
    fun connect()
    fun disconnect()
    fun mkdir(dst: String)
    fun mkdirRecursively(dst: String)
    fun upload(src: String, dst: String)
    fun download(src: String, dst: String)
    fun deleteFile(src: String)
    fun removeDirectory(src: String)
    fun deleteRecursively(src: String)
    fun listFiles(src: String): DirChildrenParcelable
    fun size(src: String): Long
    suspend fun testConnection()
    suspend fun setRemote(context: Context, onSet: suspend (remote: String, extra: String) -> Unit)
}

fun CloudEntity.getCloud() = when (this.type) {
    CloudType.FTP -> {
        val extra = getExtraEntity<FTPExtra>()!!
        FTPClientImpl(this, extra)
    }

    CloudType.WEBDAV -> {
        WebDAVClientImpl(this)
    }

    CloudType.SMB -> {
        val extra = getExtraEntity<SMBExtra>()!!
        SMBClientImpl(this, extra)
    }
}
