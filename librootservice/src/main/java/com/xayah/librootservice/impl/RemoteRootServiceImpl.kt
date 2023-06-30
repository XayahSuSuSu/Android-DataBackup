package com.xayah.librootservice.impl

import android.os.StatFs
import com.xayah.librootservice.IRemoteRootService
import com.xayah.librootservice.parcelables.StatFsParcelable
import com.xayah.librootservice.util.ExceptionUtil
import java.io.File

internal class RemoteRootServiceImpl : IRemoteRootService.Stub() {
    private val lock = Any()

    override fun readStatFs(path: String): StatFsParcelable {
        synchronized(lock) {
            val statFs = StatFs(path)
            return StatFsParcelable(statFs.availableBytes, statFs.totalBytes)
        }
    }

    override fun mkdirs(path: String): Boolean {
        synchronized(lock) {
            return ExceptionUtil.tryOn {
                val file = File(path)
                if (file.exists().not()) file.mkdirs()
            }
        }
    }

    override fun copyTo(path: String, targetPath: String, overwrite: Boolean): Boolean {
        synchronized(lock) {
            return ExceptionUtil.tryOn {
                File(path).copyTo(File(targetPath), overwrite)
            }
        }
    }
}
