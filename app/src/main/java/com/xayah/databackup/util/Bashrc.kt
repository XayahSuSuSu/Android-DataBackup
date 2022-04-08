package com.xayah.databackup.util

import android.os.Environment
import com.topjohnwu.superuser.CallbackList
import com.topjohnwu.superuser.Shell
import com.xayah.databackup.App
import com.xayah.databackup.R

class Bashrc {
    companion object {
        fun getStorageSpace(): Pair<Boolean, String> {
            val exec =
                Shell.cmd("get_storage_space ${Environment.getExternalStorageDirectory().path}")
                    .exec()
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        fun getAPKPath(packageName: String): Pair<Boolean, String> {
            App.log.add(App.globalContext.getString(R.string.get_apk_path))
            val exec = Shell.cmd("get_apk_path $packageName").exec()
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        fun cd(path: String): Pair<Boolean, String> {
            val exec = Shell.cmd("cd_to_path $path").exec()
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        fun compressAPK(compressionType: String, outPut: String): Pair<Boolean, String> {
            App.log.add(App.globalContext.getString(R.string.compress_apk))
            val cmd = "compress_apk $compressionType $outPut"
            val callbackList: CallbackList<String?> = object : CallbackList<String?>() {
                override fun onAddElement(line: String?) {
                    if (line != null) {
                        App.log.add(line)
                    }
                }
            }
            val exec = Shell.cmd(cmd).to(callbackList).exec()
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        fun compress(
            compressionType: String,
            dataType: String,
            packageName: String,
            outPut: String,
        ): Pair<Boolean, String> {
            App.log.add("${App.globalContext.getString(R.string.compress)}$dataType")
            val cmd = "compress $compressionType $dataType $packageName $outPut"
            val callbackList: CallbackList<String?> = object : CallbackList<String?>() {
                override fun onAddElement(line: String?) {
                    if (line != null) {
                        App.log.add(line)
                    }
                }
            }
            val exec = Shell.cmd(cmd).to(callbackList).exec()
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        fun setInstallEnv(): Pair<Boolean, String> {
            val exec = Shell.cmd("set_install_env").exec()
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        fun installAPK(inPath: String, packageName: String): Pair<Boolean, String> {
            App.log.add("${App.globalContext.getString(R.string.install)}${packageName}")
            val cmd = "install_apk $inPath $packageName"
            val callbackList: CallbackList<String?> = object : CallbackList<String?>() {
                override fun onAddElement(line: String?) {
                    if (line != null) {
                        App.log.add(line)
                    }
                }
            }
            val exec = Shell.cmd(cmd).to(callbackList).exec()
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        fun setOwnerAndSELinux(
            dataType: String, packageName: String, path: String
        ): Pair<Boolean, String> {
            App.log.add(App.globalContext.getString(R.string.set_SELinux))
            val exec = Shell.cmd("set_owner_and_SELinux $dataType $packageName $path").exec()
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        fun decompress(
            compressionType: String, dataType: String, inputPath: String, packageName: String
        ): Pair<Boolean, String> {
            App.log.add("${App.globalContext.getString(R.string.decompress)}$dataType")
            val cmd = "decompress $compressionType $dataType $inputPath $packageName"
            val callbackList: CallbackList<String?> = object : CallbackList<String?>() {
                override fun onAddElement(line: String?) {
                    if (line != null) {
                        App.log.add(line)
                    }
                }
            }
            val exec = Shell.cmd(cmd).to(callbackList).exec()
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        fun getAppVersion(packageName: String): Pair<Boolean, String> {
            val exec = Shell.cmd("get_app_version $packageName").exec()
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        fun writeToFile(content: String, path: String): Pair<Boolean, String> {
            val exec = Shell.cmd("write_to_file $content $path").exec()
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        fun compressMedia(
            compressionType: String,
            inputPath: String,
            outPut: String
        ): Pair<Boolean, String> {
            App.log.add("${App.globalContext.getString(R.string.compress)}${inputPath}")
            val cmd = "compress_media $compressionType $inputPath $outPut"
            val callbackList: CallbackList<String?> = object : CallbackList<String?>() {
                override fun onAddElement(line: String?) {
                    if (line != null) {
                        App.log.add(line)
                    }
                }
            }
            val exec = Shell.cmd(cmd).to(callbackList).exec()
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }
    }
}