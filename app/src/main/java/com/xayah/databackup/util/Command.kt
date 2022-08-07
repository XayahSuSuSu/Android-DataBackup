package com.xayah.databackup.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import com.topjohnwu.superuser.CallbackList
import com.topjohnwu.superuser.Shell
import com.xayah.databackup.App
import com.xayah.databackup.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class Command {
    companion object {
        private suspend fun <T> runOnIO(block: suspend () -> T): T {
            return withContext(Dispatchers.IO) { block() }
        }

        private suspend fun cat(path: String): Pair<Boolean, String> {
            val exec = execute("cat $path")
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        suspend fun ls(path: String): Boolean {
            execute("ls -i $path").apply {
                return this.isSuccess
            }
        }

        suspend fun countFile(path: String): Int {
            execute("ls -i $path").apply {
                return if (this.isSuccess)
                    this.out.size
                else
                    0
            }
        }

        suspend fun rm(path: String): Boolean {
            execute("rm -rf $path").apply {
                return this.isSuccess
            }
        }

        suspend fun mkdir(path: String): Boolean {
            if (execute("ls -i $path").isSuccess)
                return true
            execute("mkdir -p $path").apply {
                return this.isSuccess
            }
        }

        suspend fun unzip(filePath: String, outPath: String) {
            if (mkdir(outPath)) execute("unzip $filePath -d $outPath")
        }

        private suspend fun cp(src: String, dst: String): Boolean {
            return execute("cp $src $dst").isSuccess
        }

        fun unzipByZip4j(filePath: String, outPath: String) {
            try {
                ZipFile(filePath).extractAll(outPath)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        suspend fun getAppInfoBackupList(context: Context): MutableList<AppInfoBackup> {
            val packageManager = context.packageManager
            // 备份列表
            val cachedAppInfoBackupList = mutableListOf<AppInfoBackup>()
            val appInfoBackupList = mutableListOf<AppInfoBackup>()
            runOnIO {
                val userId = context.readBackupUser()
                // 出于某些原因，只有getInstalledPackages()才能正确获取所有的应用信息
                val packages = packageManager.getInstalledPackages(0)
                // 获取指定用户的所有应用信息
                val listPackages = Bashrc.listPackages(userId).second
                for ((index, j) in listPackages.withIndex()) listPackages[index] =
                    j.replace("package:", "")
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
                        // 自身或非指定用户应用
                            continue
                        if (!context.readIsSupportSystemApp()) if ((i.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0) continue
                        // 寻找缓存数据
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
            }
            return appInfoBackupList
        }

        private suspend fun getCachedAppInfoBackupList(
            context: Context, isFiltered: Boolean = false // 是否过滤应用和数据均未勾选的item
        ): MutableList<AppInfoBackup> {
            val packageManager = context.packageManager
            // 备份列表
            val cachedAppInfoBackupList = mutableListOf<AppInfoBackup>()
            val appInfoBackupList = mutableListOf<AppInfoBackup>()
            runOnIO {
                val userId = context.readBackupUser()
                // 出于某些原因，只有getInstalledPackages()才能正确获取所有的应用信息
                val packages = packageManager.getInstalledPackages(0)
                // 获取指定用户的所有应用信息
                val listPackages = Bashrc.listPackages(userId).second
                for ((index, j) in listPackages.withIndex()) listPackages[index] =
                    j.replace("package:", "")
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
            }
            return appInfoBackupList
        }

        suspend fun getCachedAppInfoRestoreList(isFiltered: Boolean = false): MutableList<AppInfoRestore> {
            val cachedAppInfoRestoreList = mutableListOf<AppInfoRestore>()
            runOnIO {
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
            }
            return cachedAppInfoRestoreList
        }

        suspend fun getCachedAppInfoRestoreActualList(): MutableList<AppInfoRestore> {
            // 根据包名文件夹获取应用实际列表
            val cachedAppInfoRestoreActualList = mutableListOf<AppInfoRestore>()
            val cachedAppInfoRestoreList = mutableListOf<AppInfoRestore>()
            runOnIO {
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
                                        GlobalString.appRetrieved,
                                        i,
                                        "",
                                        0,
                                        app = true,
                                        data = true
                                    )
                                )
                            )
                            else cachedAppInfoRestoreActualList.add(cachedAppInfoRestoreList[tmpIndex])
                        }
                    }
                }
            }
            return cachedAppInfoRestoreActualList
        }

        suspend fun retrieve(mAppInfoRestoreActualList: MutableList<AppInfoRestore>) {
            runOnIO {
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
        }

        fun addOrUpdateList(
            item: Any,
            dst: MutableList<Any>,
            callback: (item: Any) -> Boolean
        ) {
            val tmp = dst.find { callback(it) }
            val tmpIndex = dst.indexOf(tmp)
            if (tmpIndex == -1) dst.add(item)
            else dst[tmpIndex] = item
        }

        suspend fun getCachedMediaInfoBackupList(isFiltered: Boolean = false): MutableList<MediaInfo> {
            // 媒体备份列表
            var cachedMediaInfoList = mutableListOf<MediaInfo>()
            runOnIO {
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
                    cachedMediaInfoList.add(
                        MediaInfo(
                            "Music",
                            "/storage/emulated/0/Music",
                            false,
                            ""
                        )
                    )
                    cachedMediaInfoList.add(
                        MediaInfo(
                            "DCIM",
                            "/storage/emulated/0/DCIM",
                            false,
                            ""
                        )
                    )
                }
                if (isFiltered) cachedMediaInfoList =
                    cachedMediaInfoList.filter { it.data }.toMutableList()
            }
            return cachedMediaInfoList
        }

        suspend fun getCachedMediaInfoRestoreList(isFiltered: Boolean = false): MutableList<MediaInfo> {
            val cachedMediaInfoRestoreList = mutableListOf<MediaInfo>()
            runOnIO {
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
            }
            return cachedMediaInfoRestoreList
        }

        suspend fun getCachedBackupInfoList(): MutableList<BackupInfo> {
            val cachedBackupInfoList = mutableListOf<BackupInfo>()
            runOnIO {
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
            }
            return cachedBackupInfoList
        }

        fun extractAssets(mContext: Context, assetsPath: String, outName: String) {
            try {
                val assets = File(Path.getFilesDir(), outName)
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

        suspend fun compress(
            compressionType: String,
            dataType: String,
            packageName: String,
            outPut: String,
            dataPath: String,
            dataSize: String? = null,
            onAddLine: (line: String?) -> Unit = {}
        ): Boolean {
            var ret = true
            runOnIO {
                if (dataType == "media") {
                    countSize(dataPath, 1).apply {
                        if (this == dataSize) {
                            ret = true
                        }
                    }
                } else {
                    countSize(
                        "${dataPath}/${packageName}", 1
                    ).apply {
                        if (this == dataSize) {
                            ret = true
                        }
                    }
                }
                Bashrc.compress(
                    compressionType, dataType, packageName, outPut, dataPath
                ) { onAddLine(it) }.apply {
                    if (!this.first) {
                        ret = false
                    }
                }
            }
            return ret
        }

        suspend fun compressAPK(
            compressionType: String,
            packageName: String,
            outPut: String,
            userId: String,
            apkSize: String? = null,
            onAddLine: (line: String?) -> Unit = {}
        ): Boolean {
            var ret = true
            runOnIO {
                val apkPathPair = Bashrc.getAPKPath(packageName, userId).apply { ret = this.first }
                countSize(
                    apkPathPair.second, 1
                ).apply { ret = this == apkSize }
                Bashrc.cd(apkPathPair.second).apply { ret = this.first }
                Bashrc.compressAPK(compressionType, outPut) {
                    onAddLine(it)
                }.apply { ret = this.first }
                Bashrc.cd("~").apply { ret = this.first }
            }
            return ret
        }

        suspend fun decompress(
            compressionType: String,
            dataType: String,
            inputPath: String,
            packageName: String,
            dataPath: String,
            onAddLine: (line: String?) -> Unit = {}
        ): Boolean {
            var ret = true
            runOnIO {
                Bashrc.decompress(
                    compressionType,
                    dataType,
                    inputPath,
                    packageName,
                    dataPath
                ) { onAddLine(it) }.apply { ret = this.first }
            }
            return ret
        }

        suspend fun installAPK(
            inPath: String,
            packageName: String,
            userId: String,
            versionCode: String,
            onAddLine: (line: String?) -> Unit = {}
        ): Boolean {
            var ret = true
            runOnIO {
                val appVersionCode = getAppVersionCode(userId, packageName)
                ret = versionCode < appVersionCode
                // 禁止APK验证
                Bashrc.setInstallEnv()
                // 安装APK
                Bashrc.installAPK(inPath, packageName, userId) {
                    onAddLine(it)
                }.apply {
                    ret = when (this.first) {
                        0 -> true
                        else -> {
                            false
                        }
                    }
                }
            }
            return ret
        }

        suspend fun setOwnerAndSELinux(
            dataType: String,
            packageName: String,
            path: String,
            userId: String,
        ) {
            Bashrc.setOwnerAndSELinux(
                dataType,
                packageName,
                path,
                userId,
                App.globalContext.readAutoFixMultiUserContext()
            )
                .apply {
                    if (!this.first) {
                        return
                    }
                }
        }

        private suspend fun getAppVersionCode(userId: String, packageName: String): String {
            Bashrc.getAppVersionCode(userId, packageName).apply {
                if (!this.first) {
                    return ""
                }
                return this.second
            }
        }

        private suspend fun testArchive(
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

        suspend fun backupItself(
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

        suspend fun countSize(path: String, type: Int = 0): String {
            Bashrc.countSize(path, type).apply {
                return if (!this.first) "0"
                else if (this.second.isEmpty()) "0"
                else this.second
            }
        }

        suspend fun checkRoot(): Boolean {
            return execute("ls /").isSuccess
        }

        suspend fun checkBin(): Boolean {
            execute("ls -l ${Path.getFilesDir()}/bin").out.apply {
                val fileList = this.subList(1, this.size)
                var count = 0
                for (i in fileList) if (i.contains("-rwxrwxrwx")) count++
                return count == 4
            }
        }

        suspend fun checkBashrc(): Boolean {
            return execute("check_bashrc").isSuccess
        }

        fun getVersion(): String {
            return App.globalContext.packageManager.getPackageInfo(
                App.globalContext.packageName, 0
            ).versionName
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

        suspend fun getCompressionTypeByPath(path: String): String {
            execute("ls $path").out.joinToLineString.apply {
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

        suspend fun execute(
            cmd: String,
            isAddToLog: Boolean = true,
            callback: ((line: String) -> Unit)? = null
        ): Shell.Result {
            val result = runOnIO {
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
                shell.exec().apply {
                    if (isAddToLog)
                        this.apply {
                            for (i in this.out) App.logcat.add("Shell_Out: $i")
                        }
                }
            }
            return result
        }

        suspend fun checkLsZd(): Boolean {
            return execute("ls -Zd").isSuccess
        }

        suspend fun listBackupUsers(): MutableList<String> {
            return execute("ls ${Path.getBackupUserPath()}").out
        }
    }
}