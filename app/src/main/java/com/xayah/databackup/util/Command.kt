package com.xayah.databackup.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import com.topjohnwu.superuser.Shell
import com.xayah.databackup.App
import com.xayah.databackup.R
import com.xayah.databackup.data.AppEntity
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class Command {
    companion object {
        fun ls(path: String): Boolean {
            return Shell.cmd("ls -i $path").exec().isSuccess
        }

        private fun mkdir(path: String): Boolean {
            return Shell.cmd("mkdir -p $path").exec().isSuccess
        }

        fun unzip(filePath: String, outPath: String) {
            if (mkdir(outPath)) Shell.cmd("unzip $filePath -d $outPath").exec()
        }

        fun getAppList(context: Context, room: Room?): MutableList<AppEntity> {
            val appList: MutableList<AppEntity> = mutableListOf()
            room?.let {
                val packageManager = context.packageManager
                val packages = packageManager.getInstalledPackages(0)
                for (i in packages) {
                    try {
                        if (i.packageName == "com.xayah.databackup")
                            continue
                        if ((i.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0) {
                            val appIcon = i.applicationInfo.loadIcon(packageManager)
                            val appName = i.applicationInfo.loadLabel(packageManager).toString()
                            val packageName = i.packageName
                            var appEntity = room.findByPackage(packageName)
                            if (appEntity == null) {
                                appEntity =
                                    AppEntity(0, appName, packageName, getAppVersion(packageName))
                            } else {
                                appEntity.appName = appName
                            }
                            room.insertOrUpdate(appEntity)
                            appEntity.icon = appIcon
                            appList.add(appEntity)
                        }
                    } catch (e: IllegalStateException) {
                        e.printStackTrace()
                        break
                    }
                }
            }
            return appList
        }

        fun extractAssets(mContext: Context, assetsPath: String, outName: String) {
            try {
                val assets = File(Path.getExternalFilesDir(mContext), outName)
                if (!assets.exists()) {
                    val outStream = FileOutputStream(assets)
                    val inputStream = mContext.resources.assets.open(assetsPath)
                    inputStream.copyTo(outStream)
                    assets.setExecutable(true)
                    assets.setReadable(true)
                    assets.setWritable(true)
                    outStream.flush()
                    inputStream.close()
                    outStream.close()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        fun getABI(): String {
            val mABIs = Build.SUPPORTED_ABIS
            if (mABIs.isNotEmpty()) {
                return mABIs[0]
            }
            return ""
        }

        fun compress(
            compressionType: String,
            dataType: String,
            packageName: String,
            outPut: String
        ) {
            Bashrc.compress(compressionType, dataType, packageName, outPut).apply {
                if (!this.first) {
                    App.log.add(App.globalContext.getString(R.string.compress_failed))
                    return
                }
            }
        }

        fun compressAPK(
            compressionType: String,
            packageName: String,
            outPut: String
        ) {
            val apkPathPair = Bashrc.getAPKPath(packageName).apply {
                if (!this.first) {
                    App.log.add(
                        "${packageName}: ${
                            App.globalContext.getString(R.string.compress_apk_failed)
                        }"
                    )
                    return
                }
            }
            Bashrc.cd(apkPathPair.second).apply {
                if (!this.first) {
                    App.log.add(
                        "${apkPathPair.second}: ${
                            App.globalContext.getString(R.string.path_not_exist)
                        }"
                    )
                    return
                }
            }
            Bashrc.compressAPK(compressionType, outPut).apply {
                if (!this.first) {
                    App.log.add(App.globalContext.getString(R.string.compress_apk_failed))
                    return
                }
            }
            Bashrc.cd("~").apply {
                if (!this.first) {
                    App.log.add("~: ${App.globalContext.getString(R.string.path_not_exist)}")
                    return
                }
            }
        }

        private fun decompress(
            compressionType: String,
            dataType: String,
            inputPath: String,
            packageName: String
        ) {
            Bashrc.decompress(compressionType, dataType, inputPath, packageName).apply {
                if (!this.first) {
                    App.log.add(App.globalContext.getString(R.string.decompress_failed))
                    return
                }
            }
        }

        private fun getCompressionTypeByName(name: String): String {
            val item = name.split(".")
            when (item.size) {
                2 -> {
                    when (item[1]) {
                        "tar" -> {
                            return "tar"
                        }
                    }
                }
                3 -> {
                    when ("${item[1]}.${item[2]}") {
                        "tar.zst" -> {
                            return "zstd"
                        }
                        "tar.lz4" -> {
                            return "lz4"
                        }
                    }
                }
            }
            return ""
        }

        fun installAPK(
            inPath: String,
            packageName: String
        ) {
            // 禁止APK验证
            Bashrc.setInstallEnv()

            Bashrc.installAPK(inPath, packageName).apply {
                if (!this.first) {
                    App.log.add(
                        App.globalContext.getString(R.string.install_apk_failed_or_skip)
                    )
                    return
                }
            }
        }

        private fun setOwnerAndSELinux(dataType: String, packageName: String, path: String) {
            Bashrc.setOwnerAndSELinux(dataType, packageName, path).apply {
                if (!this.first) {
                    App.log.add(App.globalContext.getString(R.string.set_SELinux_failed))
                    return
                }
            }
        }

        fun restoreData(
            packageName: String,
            inPath: String
        ) {
            val fileList = Shell.cmd("ls $inPath | grep -v apk.* | grep .tar").exec().out
            for (i in fileList) {
                val item = i.split(".")
                val dataType = item[0]
                var path = ""
                val compressionType = getCompressionTypeByName(i)
                if (compressionType.isNotEmpty()) {
                    decompress(compressionType, dataType, "${inPath}/${i}", packageName)
                    when (dataType) {
                        "user" -> {
                            path = "/data/data"
                        }
                        "data", "obb" -> {
                            path = "/data/media/0/Android/${dataType}"
                        }
                    }
                    if (path.isNotEmpty())
                        setOwnerAndSELinux(dataType, packageName, "${path}/${packageName}")
                }
            }
        }

        private fun getAppVersion(packageName: String): String {
            Bashrc.getAppVersion(packageName).apply {
                if (!this.first) {
                    App.log.add(App.globalContext.getString(R.string.get_app_version_failed))
                    return ""
                }
                return this.second
            }
        }

        fun generateAppInfo(appName: String, packageName: String, outPut: String) {
            var content = "\""
            content += "appName=${appName}" + "\\n"
            content += "packageName=${packageName}" + "\\n"
            content += "version=${getAppVersion(packageName)}" + "\\n"
            content += "\""
            Bashrc.writeToFile(content, "${outPut}/info").apply {
                if (!this.first) {
                    App.log.add(App.globalContext.getString(R.string.generate_app_info_failed))
                }
            }
        }
    }
}