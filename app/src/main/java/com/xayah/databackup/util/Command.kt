package com.xayah.databackup.util

import android.content.Context
import android.os.Build
import android.os.Environment
import com.topjohnwu.superuser.CallbackList
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ShellUtils
import com.xayah.databackup.R
import com.xayah.databackup.model.AppInfo
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class Command {
    companion object {
        fun ls(path: String): Boolean {
            return Shell.cmd("ls -i $path").exec().isSuccess
        }

        private fun rm(path: String): Boolean {
            return Shell.cmd("rm -rf $path").exec().isSuccess
        }

        private fun mkdir(path: String): Boolean {
            return Shell.cmd("mkdir -p $path").exec().isSuccess
        }

        fun unzip(filePath: String, outPath: String) {
            if (mkdir(outPath)) Shell.cmd("unzip $filePath -d $outPath").exec()
        }

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

        private fun compress(
            compressionType: String,
            dataType: String,
            packageName: String,
            outPut: String,
            onCallback: (line: String) -> Unit = {}
        ) {
            val dataPath: String
            var cmd: String
            when (dataType) {
                "user" -> {
                    dataPath = "/data/data"
                    cmd =
                        "tar --exclude=\"${packageName}/.ota\" --exclude=\"${packageName}/cache\" --exclude=\"${packageName}/lib\" -cpf - -C \"${dataPath}\" \"${packageName}\" | pv -f"
                }
                else -> {
                    dataPath = "/data/media/0/Android/${dataType}"
                    cmd =
                        "tar --exclude=\"Backup_\"* --exclude=\"${packageName}/cache\" -cPpf - \"${dataPath}/${packageName}\" | pv -f"
                }
            }
            when (compressionType) {
                "tar" -> {
                    cmd += " > \"${outPut}/${dataType}.tar\""
                }
                "zstd" -> {
                    cmd += " | zstd -r -T0 --ultra -6 -q --priority=rt > \"${outPut}/${dataType}.tar.zst\""
                }
                "lz4" -> {
                    cmd += " | zstd -r -T0 --ultra -1 -q --priority=rt --format=lz4 > \"${outPut}/${dataType}.tar.lz4\""
                }
            }
            if (ls("${dataPath}/${packageName}")) {
                val callbackList: CallbackList<String?> = object : CallbackList<String?>() {
                    override fun onAddElement(line: String?) {
                        if (line != null) {
                            onCallback(line)
                        }
                    }
                }
                Shell.cmd(cmd).to(callbackList).exec()
            }
        }

        private fun compressAPK(
            compressionType: String,
            packageName: String,
            outPut: String,
            onCallback: (line: String) -> Unit = {}
        ) {
            ShellUtils.fastCmd("apk_path=\"\$(pm path \"${packageName}\" | cut -f2 -d ':')\"")
            val apkPath = ShellUtils.fastCmd("echo \${apk_path%/*}")
            ShellUtils.fastCmd("cd $apkPath")
            val apks = Shell.cmd("ls").exec().out
            if (apks.size == 1) {
                // 暂时仅支持非Split Apk
                var cmd = ""
                when (compressionType) {
                    "tar" -> {
                        cmd = "tar -cf \"${outPut}/apk.tar\" *.apk"
                    }
                    "zstd" -> {
                        cmd =
                            "tar -cf - *apk | zstd -r -T0 --ultra -6 -q --priority=rt >\"${outPut}/apk.tar.zst\""
                    }
                    "lz4" -> {
                        cmd =
                            "tar -cf - *.apk | zstd -r -T0 --ultra -1 -q --priority=rt --format=lz4 >\"${outPut}/apk.tar.lz4\""
                    }
                }
                val callbackList: CallbackList<String?> = object : CallbackList<String?>() {
                    override fun onAddElement(line: String?) {
                        if (line != null) {
                            onCallback(line)
                        }
                    }
                }
                Shell.cmd(cmd).to(callbackList).exec()
            }
            ShellUtils.fastCmd("cd ~")
        }

        private fun decompress(
            compressionType: String,
            dataType: String,
            inputPath: String,
            onCallback: (line: String) -> Unit = {}
        ) {
            val dataPath: String
            var cmd = "pv -f \"${inputPath}\" | tar --recursive-unlink"
            when (dataType) {
                "user" -> {
                    dataPath = "/data/data"
                    when (compressionType) {
                        "tar" -> {
                            cmd += " -xmpf - -C \"${dataPath}\""
                        }
                        "zstd", "lz4" -> {
                            cmd += " -I zstd -xmpf - -C \"${dataPath}\""
                        }
                    }
                }
                else -> {
                    when (compressionType) {
                        "tar" -> {
                            cmd += " -xmPpf -"
                        }
                        "zstd", "lz4" -> {
                            cmd += " -I zstd -xmPpf -"
                        }
                    }
                }
            }
            val callbackList: CallbackList<String?> = object : CallbackList<String?>() {
                override fun onAddElement(line: String?) {
                    if (line != null) {
                        onCallback(line)
                    }
                }
            }
            Shell.cmd(cmd).to(callbackList).exec()
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

        private fun installAPK(
            inPath: String,
            packageName: String,
            onCallback: (line: String) -> Unit = {}
        ) {
            // 禁止APK验证
            ShellUtils.fastCmd("settings put global verifier_verify_adb_installs 0")
            ShellUtils.fastCmd("settings put global package_verifier_enable 0")
            val packageVerifierUserConsent =
                ShellUtils.fastCmd("settings get global package_verifier_user_consent")
            if (packageVerifierUserConsent.isNotEmpty() && packageVerifierUserConsent != "-1") {
                ShellUtils.fastCmd("settings put global package_verifier_user_consent -1")
                ShellUtils.fastCmd("settings put global upload_apk_enable 0")
            }
            val tmpDir = "/data/local/tmp/data_backup"
            rm(tmpDir)
            mkdir(tmpDir)
            if (ShellUtils.fastCmd("pm path \"${packageName}\"").isEmpty()) {
                val fileList = Shell.cmd("ls ${inPath}/apk.*").exec().out
                for (i in fileList) {
                    var cmd = ""
                    when (getCompressionTypeByName(i)) {
                        "tar" -> {
                            cmd = "pv -f \"${i}\" | tar -xmpf - -C \"${tmpDir}\""
                        }
                        "zstd", "lz4" -> {
                            cmd = "pv -f \"${i}\" | tar -I zstd -xmpf - -C \"${tmpDir}\""
                        }
                    }
                    if (cmd.isNotEmpty()) {
                        val callbackList: CallbackList<String?> =
                            object : CallbackList<String?>() {
                                override fun onAddElement(line: String?) {
                                    if (line != null) {
                                        onCallback(line)
                                    }
                                }
                            }
                        Shell.cmd(cmd).to(callbackList).exec()
                        val apkList = Shell.cmd("ls ${tmpDir}/*.apk").exec().out
                        if (apkList.size == 1) {
                            // 暂时仅支持非Split Apk
                            for (j in apkList) {
                                ShellUtils.fastCmd("pm install -i com.android.vending --user 0 -r ${tmpDir}/*.apk")
                            }
                        }
                    }
                }
            }
            rm(tmpDir)
        }

        private fun setOwnerAndSELinux(packageName: String, path: String) {
            var owner = ShellUtils.fastCmd("cat \"/config/sdcardfs/${packageName}/appid\"")
            if (owner.isEmpty()) owner =
                ShellUtils.fastCmd("dumpsys package \"${packageName}\" | awk '/userId=/{print \$1}' | cut -f2 -d '=' | head -1")
            owner = ShellUtils.fastCmd("echo \"${owner}\" | egrep -o '[0-9]+'")
            Shell.cmd("chown -hR \"${owner}:${owner}\" \"${path}/\"").exec()
            Shell.cmd("restorecon -RF \"${path}/\"").exec()
        }

        fun backup(
            compressionType: String,
            packageName: String,
            outPut: String,
            onCallback: (line: String) -> Unit = {}
        ) {
            compressAPK(compressionType, packageName, outPut, onCallback)
            compress(compressionType, "user", packageName, outPut, onCallback)
            compress(compressionType, "data", packageName, outPut, onCallback)
            compress(compressionType, "obb", packageName, outPut, onCallback)
        }

        fun restore(packageName: String, inPath: String, onCallback: (line: String) -> Unit = {}) {
            installAPK(inPath, packageName)
            val fileList = Shell.cmd("ls $inPath").exec().out
            for (i in fileList) {
                val item = i.split(".")
                val dataType = item[0]
                var path = ""
                val compressionType = getCompressionTypeByName(i)
                if (compressionType.isNotEmpty()) {
                    decompress(compressionType, dataType, "${inPath}/${i}", onCallback)
                    when (dataType) {
                        "user" -> {
                            path = "/data/data"
                        }
                        "data", "obb" -> {
                            path = "/data/media/0/Android/${dataType}"
                        }
                    }
                    if (path.isNotEmpty())
                        setOwnerAndSELinux(packageName, "${path}/${packageName}")
                }
            }
        }
    }
}