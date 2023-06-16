package com.xayah.librootservice.impl

import android.os.StatFs
import com.xayah.librootservice.IRemoteRootService
import com.xayah.librootservice.parcelables.StatFsParcelable

internal class RemoteRootServiceImpl : IRemoteRootService.Stub() {
    private val lock = Any()

    override fun readStatFs(path: String): StatFsParcelable {
        synchronized(lock) {
            val statFs = StatFs(path)
            return StatFsParcelable(statFs.availableBytes, statFs.totalBytes)
        }
    }
}
