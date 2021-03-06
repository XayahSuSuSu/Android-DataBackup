package com.xayah.databackup.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import com.topjohnwu.superuser.CallbackList
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ShellUtils
import com.xayah.databackup.App
import com.xayah.databackup.data.*
import net.lingala.zip4j.ZipFile
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class Command {
    companion object {
        fun cat(path: String): Pair<Boolean, String> {
            val exec = execute("cat $path")
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        fun ls(path: String): Boolean {
            execute("ls -i $path").apply {
                return this.isSuccess
            }
        }

        fun countFile(path: String): Int {
            execute("ls -i $path").apply {
                if (this.isSuccess)
                    return this.out.size
                else
                    return 0
            }
        }

        fun rm(path: String): Boolean {
            execute("rm -rf $path").apply {
                return this.isSuccess
            }
        }

        fun mkdir(path: String): Boolean {
            if (execute("ls -i $path").isSuccess)
                return true
            execute("mkdir -p $path").apply {
                return this.isSuccess
            }
        }

        fun unzip(filePath: String, outPath: String) {
            if (mkdir(outPath)) execute("unzip $filePath -d $outPath")
        }

        private fun cp(src: String, dst: String): Boolean {
            return execute("cp $src $dst").isSuccess
        }

        fun unzipByZip4j(filePath: String, outPath: String) {
            try {
                ZipFile(filePath).extractAll(outPath)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun getAppInfoBackupList(context: Context): MutableList<AppInfoBackup> {
            val packageManager = context.packageManager
            val userId = context.readBackupUser()
            // ???????????????????????????getInstalledPackages()???????????????????????????????????????
            val packages = packageManager.getInstalledPackages(0)
            // ???????????????????????????????????????
            val listPackages = Bashrc.listPackages(userId).second
            for ((index, j) in listPackages.withIndex()) listPackages[index] =
                j.replace("package:", "")
            // ????????????
            val cachedAppInfoBackupList = mutableListOf<AppInfoBackup>()
            val appInfoBackupList = mutableListOf<AppInfoBackup>()
            cat(Path.getAppInfoBackupListPath()).apply {
                if (this.first) {
                    try {
                        val jsonArray = JSON.stringToJsonArray(this.second)
                        for (i in jsonArray) {
                            cachedAppInfoBackupList.add(
                                JSON.jsonElementToEntity(
                                    i, AppInfoBackup::class.java
                                ) as AppInfoBackup
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            for (i in packages) {
                try {
                    if (i.packageName == "com.xayah.databackup" || listPackages.indexOf(i.packageName) == -1)
                    // ??????????????????????????????
                        continue
                    if (!context.readIsSupportSystemApp()) if ((i.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0) continue
                    // ??????????????????
                    var appInfo = AppInfoBackup(
                        null,
                        "",
                        "",
                        "",
                        "",
                        AppInfoBase("", "", "", 0, app = true, data = true),
                    )
                    for (j in cachedAppInfoBackupList) {
                        if (i.packageName == j.infoBase.packageName) {
                            appInfo = j
                            break
                        }
                    }
                    val appIcon = i.applicationInfo.loadIcon(packageManager)
                    val appName = i.applicationInfo.loadLabel(packageManager).toString()
                    val versionName = i.versionName
                    val versionCode = i.longVersionCode
                    val packageName = i.packageName
                    appInfo.appIcon = appIcon
                    appInfo.apply {
                        infoBase.appName = appName
                        infoBase.packageName = packageName
                        infoBase.versionName = versionName
                        infoBase.versionCode = versionCode
                    }
                    appInfoBackupList.add(appInfo)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return appInfoBackupList
        }

        fun getCachedAppInfoBackupList(
            context: Context, isFiltered: Boolean = false // ??????????????????????????????????????????item
        ): MutableList<AppInfoBackup> {
            val packageManager = context.packageManager
            val userId = context.readBackupUser()
            // ???????????????????????????getInstalledPackages()???????????????????????????????????????
            val packages = packageManager.getInstalledPackages(0)
            // ???????????????????????????????????????
            val listPackages = Bashrc.listPackages(userId).second
            for ((index, j) in listPackages.withIndex()) listPackages[index] =
                j.replace("package:", "")
            // ????????????
            val cachedAppInfoBackupList = mutableListOf<AppInfoBackup>()
            val appInfoBackupList = mutableListOf<AppInfoBackup>()
            cat(Path.getAppInfoBackupListPath()).apply {
                if (this.first) {
                    try {
                        val jsonArray = JSON.stringToJsonArray(this.second)
                        for (i in jsonArray) {
                            cachedAppInfoBackupList.add(
                                JSON.jsonElementToEntity(
                                    i, AppInfoBackup::class.java
                                ) as AppInfoBackup
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            for (i in cachedAppInfoBackupList) {
                for (j in packages) {
                    if (i.infoBase.packageName == j.packageName) {
                        if (isFiltered) if (!i.infoBase.app && !i.infoBase.data) continue
                        appInfoBackupList.add(i)
                        break
                    }
                }
            }
            return appInfoBackupList
        }

        fun getCachedAppInfoRestoreList(isFiltered: Boolean = false): MutableList<AppInfoRestore> {
            val cachedAppInfoRestoreList = mutableListOf<AppInfoRestore>()
            cat(Path.getAppInfoRestoreListPath()).apply {
                if (this.first) {
                    try {
                        val jsonArray = JSON.stringToJsonArray(this.second)
                        for (i in jsonArray) {
                            val item = JSON.jsonElementToEntity(
                                i, AppInfoRestore::class.java
                            ) as AppInfoRestore
                            if (isFiltered) if (!item.infoBase.app && !item.infoBase.data) continue
                            cachedAppInfoRestoreList.add(item)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            return cachedAppInfoRestoreList
        }

        fun getCachedAppInfoRestoreActualList(): MutableList<AppInfoRestore> {
            val cachedAppInfoRestoreActualList = mutableListOf<AppInfoRestore>()
            val cachedAppInfoRestoreList = mutableListOf<AppInfoRestore>()
            cat(Path.getAppInfoRestoreListPath()).apply {
                if (this.first) {
                    try {
                        val jsonArray = JSON.stringToJsonArray(this.second)
                        for (i in jsonArray) {
                            val item = JSON.jsonElementToEntity(
                                i, AppInfoRestore::class.java
                            ) as AppInfoRestore
                            cachedAppInfoRestoreList.add(item)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            execute("ls ${Path.getBackupDataSavePath()}").apply {
                if (isSuccess) {
                    for (i in out) {
                        val tmp = cachedAppInfoRestoreList.find { it.infoBase.packageName == i }
                        val tmpIndex = cachedAppInfoRestoreList.indexOf(tmp)
                        if (tmpIndex == -1) cachedAppInfoRestoreActualList.add(
                            AppInfoRestore(
                                null, AppInfoBase(
                                    GlobalString.appRetrieved, i, "", 0, app = false, data = false
                                )
                            )
                        )
                        else cachedAppInfoRestoreActualList.add(cachedAppInfoRestoreList[tmpIndex])
                    }
                }
            }
            return cachedAppInfoRestoreActualList
        }

        fun retrieve(mAppInfoRestoreActualList: MutableList<AppInfoRestore>) {
            val mAppInfoBackupList = getCachedAppInfoBackupList(App.globalContext, false)
            for (i in mAppInfoBackupList) {
                val tmp =
                    mAppInfoRestoreActualList.find { it.infoBase.packageName == i.infoBase.packageName }
                val tmpIndex = mAppInfoRestoreActualList.indexOf(tmp)
                if (tmpIndex == -1) {
                    i.apply {
                        appSize = ""
                        userSize = ""
                        dataSize = ""
                        obbSize = ""
                    }
                }
            }
            JSON.writeJSONToFile(
                JSON.entityArrayToJsonArray(mAppInfoBackupList as MutableList<Any>),
                Path.getAppInfoBackupListPath()
            )
            JSON.writeJSONToFile(
                JSON.entityArrayToJsonArray(mAppInfoRestoreActualList as MutableList<Any>),
                Path.getAppInfoRestoreListPath()
            )
        }

        fun addOrUpdateList(item: Any, dst: MutableList<Any>, callback: (item: Any) -> Boolean) {
            val tmp = dst.find { callback(it) }
            val tmpIndex = dst.indexOf(tmp)
            if (tmpIndex == -1) dst.add(item)
            else dst[tmpIndex] = item
        }

        fun getCachedMediaInfoBackupList(isFiltered: Boolean = false): MutableList<MediaInfo> {
            // ????????????
            val cachedMediaInfoList = mutableListOf<MediaInfo>()
            cat(Path.getMediaInfoBackupListPath()).apply {
                if (this.first) {
                    try {
                        val jsonArray = JSON.stringToJsonArray(this.second)
                        for (i in jsonArray) {
                            val item =
                                JSON.jsonElementToEntity(i, MediaInfo::class.java) as MediaInfo
                            cachedMediaInfoList.add(item)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            if (cachedMediaInfoList.isEmpty()) {
                cachedMediaInfoList.add(
                    MediaInfo(
                        "Pictures", "/storage/emulated/0/Pictures", false, ""
                    )
                )
                cachedMediaInfoList.add(
                    MediaInfo(
                        "Download", "/storage/emulated/0/Download", false, ""
                    )
                )
                cachedMediaInfoList.add(MediaInfo("Music", "/storage/emulated/0/Music", false, ""))
                cachedMediaInfoList.add(MediaInfo("DCIM", "/storage/emulated/0/DCIM", false, ""))
            }
            return if (!isFiltered) cachedMediaInfoList else cachedMediaInfoList.filter { it.data }
                .toMutableList()
        }

        fun getCachedMediaInfoRestoreList(isFiltered: Boolean = false): MutableList<MediaInfo> {
            val cachedMediaInfoRestoreList = mutableListOf<MediaInfo>()
            cat(Path.getMediaInfoRestoreListPath()).apply {
                if (this.first) {
                    try {
                        val jsonArray = JSON.stringToJsonArray(this.second)
                        for (i in jsonArray) {
                            val item = JSON.jsonElementToEntity(
                                i, MediaInfo::class.java
                            ) as MediaInfo
                            if (isFiltered) if (!item.data) continue
                            cachedMediaInfoRestoreList.add(item)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            return cachedMediaInfoRestoreList
        }

        fun getCachedAppInfoBackupListNum(): AppInfoBaseNum {
            val appInfoBaseNum = AppInfoBaseNum(0, 0)
            for (i in App.globalAppInfoBackupList) {
                if (i.infoBase.app) appInfoBaseNum.appNum++
                if (i.infoBase.data) appInfoBaseNum.dataNum++
            }
            return appInfoBaseNum
        }

        fun getCachedAppInfoRestoreListNum(): AppInfoBaseNum {
            val appInfoBaseNum = AppInfoBaseNum(0, 0)
            for (i in App.globalAppInfoRestoreList) {
                if (i.infoBase.app) appInfoBaseNum.appNum++
                if (i.infoBase.data) appInfoBaseNum.dataNum++
            }
            return appInfoBaseNum
        }

        fun getCachedBackupInfoList(): MutableList<BackupInfo> {
            val cachedBackupInfoList = mutableListOf<BackupInfo>()
            cat(Path.getBackupInfoListPath()).apply {
                if (this.first) {
                    try {
                        val jsonArray = JSON.stringToJsonArray(this.second)
                        for (i in jsonArray) {
                            val item = JSON.jsonElementToEntity(
                                i, BackupInfo::class.java
                            ) as BackupInfo
                            cachedBackupInfoList.add(item)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            return cachedBackupInfoList
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
            outPut: String,
            dataPath: String,
            dataSize: String? = null,
            onAddLine: (line: String?) -> Unit = {}
        ): Boolean {
            if (dataType == "media") {
                countSize(dataPath, 1).apply {
                    if (this == dataSize) {
                        return true
                    }
                }
            } else {
                countSize(
                    "${dataPath}/${packageName}", 1
                ).apply {
                    if (this == dataSize) {
                        return true
                    }
                }
            }
            Bashrc.compress(
                compressionType, dataType, packageName, outPut, dataPath
            ) { onAddLine(it) }.apply {
                if (!this.first) {
                    return false
                }
            }
            return true
        }

        fun compressAPK(
            compressionType: String,
            packageName: String,
            outPut: String,
            userId: String,
            apkSize: String? = null,
            onAddLine: (line: String?) -> Unit = {}
        ): Boolean {
            val apkPathPair = Bashrc.getAPKPath(packageName, userId).apply {
                if (!this.first) {
                    return false
                }
            }
            countSize(
                apkPathPair.second, 1
            ).apply {
                if (this == apkSize) {
                    return true
                }
            }
            Bashrc.cd(apkPathPair.second).apply {
                if (!this.first) {
                    return false
                }
            }
            Bashrc.compressAPK(compressionType, outPut) {
                onAddLine(it)
            }.apply {
                if (!this.first) {
                    return false
                }
            }
            Bashrc.cd("~").apply {
                if (!this.first) {
                    return false
                }
            }
            return true
        }

        fun decompress(
            compressionType: String,
            dataType: String,
            inputPath: String,
            packageName: String,
            dataPath: String,
            onAddLine: (line: String?) -> Unit = {}
        ): Boolean {
            Bashrc.decompress(compressionType, dataType, inputPath, packageName, dataPath) {
                onAddLine(it)
            }.apply {
                if (!this.first) {
                    return false
                }
            }
            return true
        }

        fun installAPK(
            inPath: String,
            packageName: String,
            userId: String,
            versionCode: String,
            onAddLine: (line: String?) -> Unit = {}
        ): Boolean {
            val appVersionCode = getAppVersionCode(userId, packageName)
            if (versionCode < appVersionCode) {
                return true
            }

            // ??????APK??????
            Bashrc.setInstallEnv()

            Bashrc.installAPK(inPath, packageName, userId) {
                onAddLine(it)
            }.apply {
                return when (this.first) {
                    0 -> true
                    else -> {
                        false
                    }
                }
            }
        }

        fun setOwnerAndSELinux(
            dataType: String, packageName: String, path: String, userId: String
        ) {
            Bashrc.setOwnerAndSELinux(dataType, packageName, path, userId).apply {
                if (!this.first) {
                    return
                }
            }
        }

        private fun getAppVersionCode(userId: String, packageName: String): String {
            Bashrc.getAppVersionCode(userId, packageName).apply {
                if (!this.first) {
                    return ""
                }
                return this.second
            }
        }

        private fun testArchive(
            compressionType: String,
            inputPath: String,
        ): Boolean {
            Bashrc.testArchive(compressionType, inputPath).apply {
                if (!this.first) {
                    return false
                }
            }
            return true
        }

        fun backupItself(
            packageName: String, outPut: String, userId: String
        ): Boolean {
            mkdir(outPut)
            val apkPath = Bashrc.getAPKPath(packageName, userId)
            val apkPathPair = apkPath.apply {
                if (!this.first) {
                    return false
                }
            }
            val apkSize = countSize("${outPut}/DataBackup.apk", 1)
            countSize(apkPathPair.second, 1).apply {
                if (this == apkSize) {
                    return true
                }
            }
            cp("${apkPathPair.second}/base.apk", "${outPut}/DataBackup.apk").apply {
                if (!this) {
                    return false
                }
            }
            return true
        }

        fun countSize(path: String, type: Int = 0): String {
            Bashrc.countSize(path, type).apply {
                return if (!this.first) "0"
                else if (this.second.isEmpty()) "0"
                else this.second
            }
        }

        fun checkRoot(): Boolean {
            return execute("ls /").isSuccess
        }

        fun checkBin(context: Context): Boolean {
            execute("ls -l ${Path.getFilesDir(context)}/bin").out.apply {
                val fileList = this.subList(1, this.size)
                var count = 0
                for (i in fileList) if (i.contains("-rwxrwxrwx")) count++
                return count == 4
            }
        }

        fun checkBashrc(): Boolean {
            return execute("check_bashrc").isSuccess
        }

        fun getVersion(): String {
            var version = ""
            version = App.globalContext.packageManager.getPackageInfo(
                App.globalContext.packageName, 0
            ).versionName
            return version
        }

        fun getDate(): String {
            var date = ""
            try {
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).apply {
                    date = format(Date())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return date
        }

        fun getCompressionTypeByPath(path: String): String {
            ShellUtils.fastCmd("ls $path").apply {
                return try {
                    when (this.split("/").last().split(".").last()) {
                        "tar" -> "tar"
                        "lz4" -> "lz4"
                        "zst" -> "zstd"
                        else -> ""
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    ""
                }
            }
        }

        fun execute(
            cmd: String,
            isAddToLog: Boolean = true,
            callback: ((line: String) -> Unit)? = null
        ): Shell.Result {
            if (isAddToLog)
                App.logcat.add("Shell_In: $cmd")
            val shell = Shell.cmd(cmd)
            callback?.apply {
                val callbackList: CallbackList<String?> = object : CallbackList<String?>() {
                    override fun onAddElement(line: String?) {
                        line?.apply {
                            if (isAddToLog)
                                App.logcat.add("Shell_Out: $line")
                            callback(line)
                        }
                    }
                }
                shell.to(callbackList)
            }
            val result = shell.exec()
            if (isAddToLog)
                result.apply {
                    for (i in this.out) App.logcat.add("Shell_Out: $i")
                }
            return result
        }
    }
}