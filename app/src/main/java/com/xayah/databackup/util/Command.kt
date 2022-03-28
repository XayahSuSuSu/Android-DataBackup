package com.xayah.databackup.util

import android.content.Context
import android.os.Environment
import com.topjohnwu.superuser.Shell
import com.xayah.databackup.R

class Command {
    companion object {
        fun getStorageSpace(mContext: Context): String {
            val exec = Shell.cmd(
                "echo \"\$(df -h ${
                    Environment.getExternalStorageDirectory().path
                } | sed -n 's|% /.*|%|p' | awk '{print \$(NF-3),\$(NF-2),\$(NF)}' | sed 's/G//g' | awk 'END{print \"\"\$2\" GB/\"\$1\" GB \"\$3}')\""
            ).exec()
            if (exec.isSuccess) {
                return exec.out.joinToString()
            }
            return mContext.getString(R.string.error)
        }
    }
}