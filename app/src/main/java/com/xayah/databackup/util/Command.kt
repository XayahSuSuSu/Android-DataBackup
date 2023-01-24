package com.xayah.databackup.util

import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Process
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
        const val TAG = "Command"

        private val storageStatsManager =
            App.globalContext.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager

        /**
         * 切换至IO协程运行
         */
        private suspend fun <T> runOnIO(block: suspend () -> T): T {
            return withContext(Dispatchers.IO) { block() }
        }

        /**
         * `cat`命令, 用于文件读取
         */
        suspend fun cat(path: String): Pair<Boolean, String> {
            val exec = execute("cat \"${path}\"", false)
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        /**
         * `ls -i`命令
         */
        suspend fun ls(path: String): Boolean {
            execute("ls -i \"${path}\"").apply {
                return this.isSuccess
            }
        }

        /**
         * 利用`ls`计数
         */
        suspend fun countFile(path: String): Int {
            execute("ls -i \"${path}\"").apply {
                return if (this.isSuccess)
                    this.out.size
                else
                    0
            }
        }

        /**
         * `rm -rf`命令, 用于删除文件, 可递归
         */
        suspend fun rm(path: String): Boolean {
            execute("rm -rf \"${path}\"").apply {
                return this.isSuccess
            }
        }

        /**
         * `mkdir`命令, 用于文件夹创建, 可递归
         */
        suspend fun mkdir(path: String): Boolean {
            if (execute("ls -i \"${path}\"").isSuccess)
                return true
            execute("mkdir -p \"${path}\"").apply {
                return this.isSuccess
            }
        }

        /**
         * Deprecated, `unzip`命令, 用于解压zip, 不兼容部分机型
         */
        @Deprecated("unzip", ReplaceWith(""))
        suspend fun unzip(filePath: String, outPath: String) {
            if (mkdir(outPath)) execute("unzip \"${filePath}\" -d \"${outPath}\"")
        }

        /**
         * `cp`命令, 用于复制
         */
        suspend fun cp(src: String, dst: String): Boolean {
            return execute("cp \"${src}\" \"${dst}\"").isSuccess
        }

        /**
         * 使用`net.lingala.zip4j`库解压zip
         */
        fun unzipByZip4j(filePath: String, outPath: String) {
            try {
                ZipFile(filePath).extractAll(outPath)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        /**
         * 构建应用列表
         */
        suspend fun getAppInfoList(): MutableList<AppInfo> {
            val appInfoList = mutableListOf<AppInfo>()

            runOnIO {
                // 读取应用列表配置文件
                cat(Path.getAppInfoListPath()).apply {
                    if (this.first) {
                        try {
                            val jsonArray = JSON.stringToJsonArray(this.second)
                            for (i in jsonArray) {
                                val item =
                                    JSON.jsonElementToEntity(i, AppInfo::class.java) as AppInfo
                                appInfoList.add(item)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                // 根据备份目录实际文件调整列表
                var hasApp = false
                var hasData = false
                execute("find \"${Path.getBackupDataSavePath()}\" -name \"*\" -type f").apply {
                    // 根据实际文件和配置调整RestoreList
                    for ((index, i) in appInfoList.withIndex()) {
                        val tmpList = mutableListOf<AppInfoItem>()
                        for (j in i.restoreList) {
                            if (this.out.toString().contains(j.date)) {
                                tmpList.add(j)
                            }
                        }
                        appInfoList[index].restoreList = tmpList
                    }
                    if (isSuccess) {
                        this.out.add("///") // 添加尾部元素, 保证原尾部元素参与
                        var restoreList = mutableListOf<AppInfoItem>()
                        for ((index, i) in this.out.withIndex()) {
                            try {
                                if (index < this.out.size - 1) {
                                    val info =
                                        i.replace(Path.getBackupDataSavePath(), "").split("/")
                                    val infoNext =
                                        this.out[index + 1].replace(
                                            Path.getBackupDataSavePath(),
                                            ""
                                        )
                                            .split("/")
                                    val packageName = info[1]
                                    val packageNameNext = infoNext[1]
                                    val date = info[2]
                                    val dateNext = infoNext[2]
                                    val fileName = info[3]
                                    if (info.size == 4) {
                                        if (fileName.contains("apk.tar"))
                                            hasApp = true
                                        else if (fileName.contains("data.tar"))
                                            hasData = true
                                        else if (fileName.contains("obb.tar"))
                                            hasData = true
                                        else if (fileName.contains("user.tar"))
                                            hasData = true
                                        else if (fileName.contains("user_de.tar"))
                                            hasData = true

                                        if (date != dateNext || packageName != packageNameNext) {
                                            // 与下一路径不同日期
                                            val restoreListIndex =
                                                restoreList.indexOfFirst { date == it.date }
                                            val restore = if (restoreListIndex == -1) AppInfoItem(
                                                app = false,
                                                data = false,
                                                hasApp = true,
                                                hasData = true,
                                                versionName = "",
                                                versionCode = 0,
                                                appSize = "",
                                                userSize = "",
                                                userDeSize = "",
                                                dataSize = "",
                                                obbSize = "",
                                                date = date
                                            ) else restoreList[restoreListIndex]

                                            restore.apply {
                                                this.hasApp = this.hasApp && hasApp
                                                this.hasData = this.hasData && hasData
                                                this.app = this.app && hasApp
                                                this.data = this.data && hasData
                                            }

                                            if (restoreListIndex == -1) restoreList.add(restore)

                                            hasApp = false
                                            hasData = false
                                        }
                                        if (packageName != packageNameNext) {
                                            // 与下一路径不同包名
                                            // 寻找已保存的数据
                                            val appInfoIndex =
                                                appInfoList.indexOfFirst { packageName == it.packageName }
                                            val appInfo = if (appInfoIndex == -1)
                                                AppInfo(
                                                    appName = "",
                                                    packageName = "",
                                                    isSystemApp = false,
                                                    firstInstallTime = 0,
                                                    backup = AppInfoItem(
                                                        app = false,
                                                        data = false,
                                                        hasApp = true,
                                                        hasData = true,
                                                        versionName = "",
                                                        versionCode = 0,
                                                        appSize = "",
                                                        userSize = "",
                                                        userDeSize = "",
                                                        dataSize = "",
                                                        obbSize = "",
                                                        date = ""
                                                    ),
                                                    _restoreIndex = -1,
                                                    restoreList = mutableListOf(),
                                                    appIconString = "",
                                                    storageStats = StorageStats()
                                                ) else appInfoList[appInfoIndex]

                                            appInfo.apply {
                                                if (appInfoIndex == -1) this.appName =
                                                    GlobalString.appRetrieved
                                                this.packageName = packageName
                                                this.isOnThisDevice = false
                                                this.restoreList = restoreList
                                            }

                                            if (appInfoIndex == -1) appInfoList.add(appInfo)

                                            hasApp = false
                                            hasData = false
                                            restoreList = mutableListOf()
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
                // 根据本机应用调整列表
                val packageManager = App.globalContext.packageManager
                val userId = App.globalContext.readBackupUser()
                // 通过PackageManager获取所有应用信息
                val packages = packageManager.getInstalledPackages(0)
                // 获取指定用户的所有应用信息
                val listPackages = Bashrc.listPackages(userId).second
                for ((index, j) in listPackages.withIndex()) listPackages[index] =
                    j.replace("package:", "")
                for (i in packages) {
                    try {
                        // 自身或非指定用户应用
                        if (i.packageName == App.globalContext.packageName || listPackages.indexOf(i.packageName) == -1) continue
                        val isSystemApp =
                            (i.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                        // 寻找已保存的数据
                        val appInfoIndex =
                            appInfoList.indexOfFirst { i.packageName == it.packageName }
                        val appInfo = if (appInfoIndex == -1)
                            AppInfo(
                                appName = "",
                                packageName = "",
                                isSystemApp = isSystemApp,
                                firstInstallTime = 0,
                                backup = AppInfoItem(
                                    app = false,
                                    data = false,
                                    hasApp = true,
                                    hasData = true,
                                    versionName = "",
                                    versionCode = 0,
                                    appSize = "",
                                    userSize = "",
                                    userDeSize = "",
                                    dataSize = "",
                                    obbSize = "",
                                    date = ""
                                ),
                                _restoreIndex = -1,
                                restoreList = mutableListOf(),
                                appIconString = "",
                                storageStats = StorageStats()
                            ) else appInfoList[appInfoIndex]

                        val appIcon = i.applicationInfo.loadIcon(packageManager)
                        val appName = i.applicationInfo.loadLabel(packageManager).toString()
                        val versionName = i.versionName
                        val versionCode = i.longVersionCode
                        val packageName = i.packageName
                        val firstInstallTime = i.firstInstallTime
                        appInfo.apply {
                            this.appName = appName
                            this.packageName = packageName
                            this.firstInstallTime = firstInstallTime
                            this.appIcon = appIcon
                            this.backup.versionName = versionName
                            this.backup.versionCode = versionCode
                            this.isOnThisDevice = true
                        }
                        try {
                            storageStatsManager.queryStatsForPackage(
                                i.applicationInfo.storageUuid,
                                i.packageName,
                                Process.myUserHandle()
                            ).apply {
                                val storageStats = StorageStats(appBytes, cacheBytes, dataBytes)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    storageStats.externalCacheBytes = externalCacheBytes
                                }
                                appInfo.storageStats = storageStats
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        if (appInfoIndex == -1) appInfoList.add(appInfo)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            return appInfoList
        }

        /**
         * 构建媒体列表
         */
        suspend fun getMediaInfoList(): MutableList<MediaInfo> {
            val mediaInfoList = mutableListOf<MediaInfo>()
            runOnIO {
                // 读取媒体列表配置文件
                cat(Path.getMediaInfoListPath()).apply {
                    if (this.first) {
                        try {
                            val jsonArray = JSON.stringToJsonArray(this.second)
                            for (i in jsonArray) {
                                val item =
                                    JSON.jsonElementToEntity(i, MediaInfo::class.java) as MediaInfo
                                mediaInfoList.add(item)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                // 如果为空, 添加默认路径
                if (mediaInfoList.isEmpty()) {
                    val nameList = listOf("Pictures", "Download", "Music", "DCIM")
                    val pathList = listOf(
                        "/storage/emulated/0/Pictures",
                        "/storage/emulated/0/Download",
                        "/storage/emulated/0/Music",
                        "/storage/emulated/0/DCIM"
                    )
                    for ((index, _) in nameList.withIndex()) {
                        mediaInfoList.add(
                            MediaInfo(
                                name = nameList[index],
                                path = pathList[index],
                                backup = MediaInfoItem(
                                    data = false,
                                    size = "",
                                    date = ""
                                ),
                                _restoreIndex = -1,
                                restoreList = mutableListOf()
                            )
                        )
                    }
                }

                // 根据备份目录实际文件调整列表
                var hasData = false
                execute("find \"${Path.getBackupMediaSavePath()}\" -name \"*\" -type f").apply {
                    // 根据实际文件和配置调整RestoreList
                    for ((index, i) in mediaInfoList.withIndex()) {
                        val tmpList = mutableListOf<MediaInfoItem>()
                        for (j in i.restoreList) {
                            if (this.out.toString().contains(j.date)) {
                                tmpList.add(j)
                            }
                        }
                        mediaInfoList[index].restoreList = tmpList
                    }
                    if (isSuccess) {
                        this.out.add("///") // 添加尾部元素, 保证原尾部元素参与
                        var restoreList = mutableListOf<MediaInfoItem>()
                        for ((index, i) in this.out.withIndex()) {
                            if (index < this.out.size - 1) {
                                val info = i.replace(Path.getBackupMediaSavePath(), "").split("/")
                                val infoNext =
                                    this.out[index + 1].replace(Path.getBackupMediaSavePath(), "")
                                        .split("/")
                                val name = info[1]
                                val nameNext = infoNext[1]
                                val date = info[2]
                                val dateNext = infoNext[2]
                                val fileName = info[3]
                                if (info.size == 4) {
                                    if (fileName.contains("${name}.tar"))
                                        hasData = true

                                    if (date != dateNext || name != nameNext) {
                                        // 与下一路径不同日期
                                        val restoreListIndex =
                                            restoreList.indexOfFirst { date == it.date }
                                        val restore = if (restoreListIndex == -1) MediaInfoItem(
                                            data = false,
                                            size = "",
                                            date = date
                                        ) else restoreList[restoreListIndex]

                                        restore.apply {
                                            this.data = this.data && hasData
                                        }

                                        if (restoreListIndex == -1) restoreList.add(restore)

                                        hasData = false
                                    }
                                    if (name != nameNext) {
                                        // 与下一路径不同包名
                                        // 寻找已保存的数据
                                        val mediaInfoIndex =
                                            mediaInfoList.indexOfFirst { name == it.name }
                                        val mediaInfo = if (mediaInfoIndex == -1)
                                            MediaInfo(
                                                name = "",
                                                path = "",
                                                backup = MediaInfoItem(
                                                    data = false,
                                                    size = "",
                                                    date = ""
                                                ),
                                                _restoreIndex = -1,
                                                restoreList = mutableListOf(),
                                            ) else mediaInfoList[mediaInfoIndex]

                                        mediaInfo.apply {
                                            this.name = name
                                            this.restoreList = restoreList
                                        }

                                        if (mediaInfoIndex == -1) mediaInfoList.add(mediaInfo)

                                        hasData = false
                                        restoreList = mutableListOf()
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return mediaInfoList
        }

        /**
         * 读取备份信息列表
         */
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

        /**
         * 释放资源文件
         */
        fun releaseAssets(mContext: Context, assetsPath: String, outName: String) {
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

        /**
         * 获取ABI
         */
        fun getABI(): String {
            val mABIs = Build.SUPPORTED_ABIS
            if (mABIs.isNotEmpty()) {
                return mABIs[0]
            }
            return ""
        }

        /**
         * 压缩
         */
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
            var update = true
            val filePath = if (dataType == "media") {
                "${outPut}/${packageName}.${getSuffixByCompressionType(compressionType)}"
            } else {
                "${outPut}/${dataType}.${getSuffixByCompressionType(compressionType)}"
            }

            runOnIO {
                if (App.globalContext.readBackupStrategy() == BackupStrategy.Cover) {
                    // 当备份策略为覆盖时, 计算目录大小并判断是否更新
                    if (dataType == "media") {
                        countSize(dataPath, 1).apply {
                            if (this == dataSize) {
                                update = false
                            }
                        }
                    } else {
                        countSize(
                            "${dataPath}/${packageName}", 1
                        ).apply {
                            if (this == dataSize) {
                                update = false
                            }
                        }
                    }
                    // 检测是否实际存在压缩包, 若不存在则仍然更新
                    ls(filePath).apply {
                        if (!this) update = true
                    }
                }
                if (update) {
                    onAddLine(ProcessCompressing)
                    Bashrc.compress(
                        compressionType, dataType, packageName, outPut, dataPath
                    ) { onAddLine(it) }.apply {
                        if (!this.first) {
                            ret = false
                        }
                    }
                } else {
                    onAddLine(ProcessSkip)
                }
                // 检测是否生成压缩包
                ls(filePath).apply {
                    if (!this) ret = false
                    else {
                        if (App.globalContext.readIsBackupTest()) {
                            // 校验
                            onAddLine(ProcessTesting)
                            testArchive(compressionType, filePath)
                        }
                    }
                }
            }
            onAddLine(ProcessFinished)
            return ret
        }

        /**
         * 压缩APK
         */
        suspend fun compressAPK(
            compressionType: String,
            packageName: String,
            outPut: String,
            userId: String,
            apkSize: String? = null,
            onAddLine: (line: String?) -> Unit = {}
        ): Boolean {
            var ret = true
            var update = true
            val filePath = "${outPut}/apk.${getSuffixByCompressionType(compressionType)}"
            runOnIO {
                val apkPathPair = Bashrc.getAPKPath(packageName, userId).apply { ret = this.first }
                if (App.globalContext.readBackupStrategy() == BackupStrategy.Cover) {
                    // 当备份策略为覆盖时, 计算目录大小并判断是否更新
                    countSize(
                        apkPathPair.second, 1
                    ).apply {
                        if (this == apkSize) {
                            update = false
                        }
                    }
                    // 检测是否实际存在压缩包, 若不存在则仍然更新
                    ls(filePath).apply {
                        // 后续若直接令state = this会导致state非正常更新
                        if (!this) update = true
                    }
                }
                if (update) {
                    onAddLine(ProcessCompressing)
                    Bashrc.cd(apkPathPair.second).apply { ret = this.first }
                    Bashrc.compressAPK(compressionType, outPut) {
                        onAddLine(it)
                    }.apply { ret = this.first }
                    Bashrc.cd("/").apply { ret = this.first }
                } else {
                    onAddLine(ProcessSkip)
                }
                // 检测是否生成压缩包
                ls(filePath).apply {
                    // 后续若直接令state = this会导致state非正常更新
                    if (!this) ret = false
                    else {
                        if (App.globalContext.readIsBackupTest()) {
                            // 校验
                            onAddLine(ProcessTesting)
                            testArchive(compressionType, filePath)
                        }
                    }
                }
            }
            onAddLine(ProcessFinished)
            return ret
        }

        /**
         * 解压
         */
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
                onAddLine(ProcessDecompressing)
                Bashrc.decompress(
                    compressionType,
                    dataType,
                    inputPath,
                    packageName,
                    dataPath
                ) { onAddLine(it) }.apply { ret = this.first }
            }
            onAddLine(ProcessFinished)
            return ret
        }

        /**
         * 安装APK
         */
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
                onAddLine(ProcessInstallingApk)
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
            onAddLine(ProcessFinished)
            return ret
        }

        /**
         * 配置Owner以及SELinux相关
         */
        suspend fun setOwnerAndSELinux(
            dataType: String,
            packageName: String,
            path: String,
            userId: String,
            onAddLine: (line: String?) -> Unit = {}
        ) {
            onAddLine(ProcessSettingSELinux)
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
            onAddLine(ProcessFinished)
        }

        /**
         * 获取应用版本代码
         */
        private suspend fun getAppVersionCode(userId: String, packageName: String): String {
            Bashrc.getAppVersionCode(userId, packageName).apply {
                if (!this.first) {
                    return ""
                }
                return this.second
            }
        }

        /**
         * 测试压缩包
         */
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

        /**
         * 备份自身
         */
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

        /**
         * 计算大小(占用)
         */
        suspend fun countSize(path: String, type: Int = 0): String {
            Bashrc.countSize(path, type).apply {
                return if (!this.first) "0"
                else if (this.second.isEmpty()) "0"
                else this.second
            }
        }

        /**
         * 检查ROOT
         */
        suspend fun checkRoot(): Boolean {
            return execute("ls /").isSuccess && Shell.rootAccess()
        }

        /**
         * 检查二进制文件
         */
        suspend fun checkBin(): Boolean {
            execute("ls -l \"${Path.getFilesDir()}/bin\"").out.apply {
                var count = 0
                try {
                    val fileList = this.subList(1, this.size)
                    for (i in fileList) if (i.contains("-rwxrwxrwx")) count++
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return count == 4
            }
        }

        /**
         * 检查Bash环境
         */
        suspend fun checkBashrc(): Boolean {
            return execute("check_bashrc").isSuccess
        }

        /**
         * 获取本应用版本名称
         */
        fun getVersion(): String {
            return App.globalContext.packageManager.getPackageInfo(
                App.globalContext.packageName, 0
            ).versionName
        }

        /**
         * 获取日期, `timeStamp`为空时获取当前日期, 否则为时间戳转日期
         */
        fun getDate(timeStamp: String = ""): String {
            var date = ""
            try {
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).apply {
                    date = if (timeStamp == "") {
                        format(Date())
                    } else {
                        format(Date(timeStamp.toLong()))
                    }
                }
            } catch (e: Exception) {
                date = timeStamp
                e.printStackTrace()
            }
            return date
        }

        /**
         * 通过路径解析压缩方式
         */
        suspend fun getCompressionTypeByPath(path: String): String {
            execute("ls \"$path\"").out.joinToLineString.apply {
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

        /**
         * 通过压缩方式得到后缀
         */
        fun getSuffixByCompressionType(type: String): String {
            return when (type) {
                "tar" -> "tar"
                "lz4" -> "tar.lz4"
                "zstd" -> "tar.zst"
                else -> ""
            }
        }

        /**
         * 经由Log封装的执行函数
         */
        suspend fun execute(
            cmd: String,
            isAddToLog: Boolean = true,
            callback: ((line: String) -> Unit)? = null
        ): Shell.Result {
            val result = runOnIO {
                if (isAddToLog)
                    App.logcat.addLine("SHELL_IN: $cmd")
                val shell = Shell.cmd(cmd)
                callback?.apply {
                    val callbackList: CallbackList<String?> = object : CallbackList<String?>() {
                        override fun onAddElement(line: String?) {
                            line?.apply {
                                if (isAddToLog)
                                    App.logcat.addLine("SHELL_OUT: $line")
                                callback(line)
                            }
                        }
                    }
                    shell.to(callbackList)
                }
                shell.exec().apply {
                    if (isAddToLog)
                        this.apply {
                            for (i in this.out) App.logcat.addLine("SHELL_OUT: $i")
                        }
                }
            }
            return result
        }

        /**
         * 检查`ls -Zd`命令是否可用
         */
        suspend fun checkLsZd(): Boolean {
            return execute("ls -Zd").isSuccess
        }

        /**
         * 列出备份用户
         */
        suspend fun listBackupUsers(): MutableList<String> {
            val exec = execute("ls \"${Path.getBackupUserPath()}\"")
            val users = mutableListOf<String>()
            for (i in exec.out) {
                try {
                    i.toInt()
                    users.add(i)
                } catch (e: NumberFormatException) {
                    e.printStackTrace()
                }
            }
            return users
        }
    }
}