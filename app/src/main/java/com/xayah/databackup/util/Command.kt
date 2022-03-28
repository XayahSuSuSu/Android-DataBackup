package com.xayah.databackup.util

import android.content.Context
import android.os.Environment
import com.topjohnwu.superuser.Shell
import com.xayah.databackup.R
import com.xayah.databackup.model.AppInfo

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

        fun getAppList(context: Context): MutableList<AppInfo> {
            val appList: MutableList<AppInfo> = mutableListOf()

            val packageManager = context.packageManager
            val packages = packageManager.getInstalledPackages(0)
            for (i in packages) {
                val appIcon = i.applicationInfo.loadIcon(packageManager)
                val appName = i.applicationInfo.loadLabel(packageManager).toString()
                val packageName = i.packageName
                val appInfo = AppInfo(appIcon, appName, packageName)
                appList.add(appInfo)
            }
            return appList
        }
    }
}