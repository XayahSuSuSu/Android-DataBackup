package com.xayah.databackup.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import androidx.appcompat.content.res.AppCompatResources
import com.topjohnwu.superuser.Shell
import com.xayah.databackup.App
import com.xayah.databackup.R
import com.xayah.databackup.data.AppEntity
import net.lingala.zip4j.ZipFile
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

        private fun cp(src: String, dst: String): Boolean {
            return Shell.cmd("cp $src $dst").exec().isSuccess
        }

        fun unzipByZip4j(filePath: String, outPath: String) {
            ZipFile(filePath).extractAll(outPath)
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

        fun getAppList(context: Context, path: String): MutableList<AppEntity> {
            val appList: MutableList<AppEntity> = mutableListOf()
            val packages = Shell.cmd("ls $path").exec().out
            for (i in packages) {
                val info = Shell.cmd("cat ${path}/${i}/info").exec().out
                try {
                    val appName = info[0].split("=")
                    val packageName = info[1].split("=")
                    val appEntity = AppEntity(0, appName[1], packageName[1]).apply {
                        icon = AppCompatResources.getDrawable(
                            context, R.drawable.ic_round_android
                        )
                        backupPath = "${path}/${i}"
                    }
                    appList.add(appEntity)
                } catch (e: IndexOutOfBoundsException) {
                    e.printStackTrace()
                }
            }
            return appList
        }

        fun extractAssets(mContext: Context, assetsPath: String, outName: String) {
            try {
                val assets = File(Path.getFilesDir(mContext), outName)
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
        ): Boolean {
            Bashrc.compress(compressionType, dataType, packageName, outPut).apply {
                if (!this.first) {
                    App.log.add(App.globalContext.getString(R.string.compress_failed))
                    return false
                }
            }
            return true
        }

        fun compressAPK(
            compressionType: String,
            packageName: String,
            outPut: String
        ): Boolean {
            val apkPathPair = Bashrc.getAPKPath(packageName).apply {
                if (!this.first) {
                    App.log.add(
                        "${packageName}: ${
                            App.globalContext.getString(R.string.path_not_exist)
                        }"
                    )
                    return false
                }
            }
            Bashrc.cd(apkPathPair.second).apply {
                if (!this.first) {
                    App.log.add(
                        "${apkPathPair.second}: ${
                            App.globalContext.getString(R.string.path_not_exist)
                        }"
                    )
                    return false
                }
            }
            Bashrc.compressAPK(compressionType, outPut).apply {
                if (!this.first) {
                    App.log.add(App.globalContext.getString(R.string.compress_apk_failed))
                    return false
                }
            }
            Bashrc.cd("~").apply {
                if (!this.first) {
                    App.log.add("~: ${App.globalContext.getString(R.string.path_not_exist)}")
                    return false
                }
            }
            return true
        }

        private fun decompress(
            compressionType: String,
            dataType: String,
            inputPath: String,
            packageName: String
        ): Boolean {
            Bashrc.decompress(compressionType, dataType, inputPath, packageName).apply {
                if (!this.first) {
                    App.log.add(App.globalContext.getString(R.string.decompress_failed))
                    return false
                }
            }
            return true
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
        ): Boolean {
            // 禁止APK验证
            Bashrc.setInstallEnv()

            Bashrc.installAPK(inPath, packageName).apply {
                if (!this.first) {
                    App.log.add(
                        App.globalContext.getString(R.string.install_apk_failed_or_skip)
                    )
                    return false
                }
            }
            return true
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
        ): Boolean {
            var result = true
            val fileList = Shell.cmd("ls $inPath | grep -v apk.* | grep .tar").exec().out
            for (i in fileList) {
                val item = i.split(".")
                val dataType = item[0]
                var path = ""
                val compressionType = getCompressionTypeByName(i)
                if (compressionType.isNotEmpty()) {
                    decompress(compressionType, dataType, "${inPath}/${i}", packageName).apply {
                        if (!this)
                            result = false
                    }
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
            return result
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

        fun generateAppInfo(appName: String, packageName: String, outPut: String): Boolean {
            var content = "\""
            content += "appName=${appName}" + "\\n"
            content += "packageName=${packageName}" + "\\n"
            content += "version=${getAppVersion(packageName)}" + "\\n"
            content += "\""
            Bashrc.writeToFile(content, "${outPut}/info").apply {
                if (!this.first) {
                    App.log.add(App.globalContext.getString(R.string.generate_app_info_failed))
                    return false
                }
            }
            return true
        }

        fun compressMedia(
            compressionType: String,
            inputPath: String,
            outPut: String
        ): Boolean {
            Bashrc.compressMedia(compressionType, inputPath, outPut).apply {
                if (!this.first) {
                    App.log.add(App.globalContext.getString(R.string.compress_failed))
                    return false
                }
            }
            return true
        }

        fun decompressMedia(inputPath: String, name: String): Boolean {
            Bashrc.decompress(getCompressionTypeByName(name), "media", "${inputPath}/$name", "")
                .apply {
                    if (!this.first) {
                        App.log.add(App.globalContext.getString(R.string.decompress_failed))
                        return false
                    }
                }
            return true
        }

        fun testArchive(
            compressionType: String,
            inputPath: String,
        ): Boolean {
            Bashrc.testArchive(compressionType, inputPath).apply {
                if (!this.first) {
                    App.log.add(App.globalContext.getString(R.string.broken))
                    return false
                }
            }
            return true
        }

        fun testArchiveForEach(inPath: String): Boolean {
            var result = true
            val fileList = Shell.cmd("ls $inPath | grep .tar").exec().out
            for (i in fileList) {
                val compressionType = getCompressionTypeByName(i)
                if (compressionType.isNotEmpty()) {
                    testArchive(compressionType, "${inPath}/${i}").apply {
                        if (!this)
                            result = false
                    }
                }
            }
            return result
        }

        fun backupItself(
            packageName: String,
            outPut: String
        ): Boolean {
            mkdir(outPut)
            val apkPathPair = Bashrc.getAPKPath(packageName).apply {
                if (!this.first) {
                    App.log.add(
                        "${packageName}: ${
                            App.globalContext.getString(R.string.path_not_exist)
                        }"
                    )
                    return false
                }
            }
            cp("${apkPathPair.second}/base.apk", "${outPut}/DataBackup.apk").apply {
                if (!this) {
                    App.log.add(
                        "${packageName}: ${
                            App.globalContext.getString(R.string.path_not_exist)
                        }"
                    )
                    return false
                }
            }
            return true
        }
    }
}