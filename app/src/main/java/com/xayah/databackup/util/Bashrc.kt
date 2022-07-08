package com.xayah.databackup.util

import com.topjohnwu.superuser.CallbackList
import com.topjohnwu.superuser.Shell

class Bashrc {
    companion object {
        fun getStorageSpace(path: String): Pair<Boolean, String> {
            val exec = Shell.cmd("get_storage_space $path").exec()
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        fun getAPKPath(packageName: String, userId: String): Pair<Boolean, String> {
            val exec = Shell.cmd("get_apk_path $packageName $userId").exec()
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        fun cd(path: String): Pair<Boolean, String> {
            val exec = Shell.cmd("cd_to_path $path").exec()
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        fun compressAPK(
            compressionType: String,
            outPut: String,
            onAddLine: (line: String?) -> Unit
        ): Pair<Boolean, String> {
            val cmd = "compress_apk $compressionType $outPut"
            val callbackList: CallbackList<String?> = object : CallbackList<String?>() {
                override fun onAddElement(line: String?) {
                    if (line != null) {
                        onAddLine(line)
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
            dataPath: String,
            onAddLine: (line: String?) -> Unit
        ): Pair<Boolean, String> {
            val cmd = "compress $compressionType $dataType $packageName $outPut $dataPath"
            val callbackList: CallbackList<String?> = object : CallbackList<String?>() {
                override fun onAddElement(line: String?) {
                    if (line != null) {
                        onAddLine(line)
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

        fun installAPK(
            inPath: String,
            packageName: String,
            userId: String,
            onAddLine: (line: String?) -> Unit
        ): Pair<Int, String> {
            val cmd = "install_apk $inPath $packageName $userId"
            val callbackList: CallbackList<String?> = object : CallbackList<String?>() {
                override fun onAddElement(line: String?) {
                    if (line != null) {
                        onAddLine(line)
                    }
                }
            }
            val exec = Shell.cmd(cmd).to(callbackList).exec()
            return Pair(exec.code, exec.out.joinToString(separator = "\n"))
        }

        fun setOwnerAndSELinux(
            dataType: String, packageName: String, path: String, userId: String
        ): Pair<Boolean, String> {
            val exec =
                Shell.cmd("set_owner_and_SELinux $dataType $packageName $path $userId").exec()
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        fun decompress(
            compressionType: String,
            dataType: String,
            inputPath: String,
            packageName: String,
            dataPath: String,
            onAddLine: (line: String?) -> Unit
        ): Pair<Boolean, String> {
            val cmd = "decompress $compressionType $dataType $inputPath $packageName $dataPath"
            val callbackList: CallbackList<String?> = object : CallbackList<String?>() {
                override fun onAddElement(line: String?) {
                    if (line != null) {
                        onAddLine(line)
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

        fun getAppVersionCode(userId: String, packageName: String): Pair<Boolean, String> {
            val exec = Shell.cmd("get_app_version_code $userId $packageName").exec()
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        fun writeToFile(content: String, path: String): Pair<Boolean, String> {
            val prefix = path.split("/").toMutableList().apply {
                removeLast()
            }
            val newPath = prefix.joinToString(separator = "/")
            Command.mkdir(newPath)
            val exec = Shell.cmd("write_to_file \'$content\' $path").exec()
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        fun testArchive(
            compressionType: String,
            inputPath: String,
        ): Pair<Boolean, String> {
            val cmd = "test_archive $compressionType $inputPath"
            val callbackList: CallbackList<String?> = object : CallbackList<String?>() {
                override fun onAddElement(line: String?) {
                    if (line != null) { }
                }
            }
            val exec = Shell.cmd(cmd).to(callbackList).exec()
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        fun listUsers(): Pair<Boolean, MutableList<String>> {
            val exec = Shell.cmd("list_users").exec()
            return Pair(exec.isSuccess, exec.out)
        }

        fun listPackages(userId: String): Pair<Boolean, MutableList<String>> {
            val exec = Shell.cmd("list_packages $userId").exec()
            return Pair(exec.isSuccess, exec.out)
        }

        fun countSize(path: String, type: Int): Pair<Boolean, String> {
            val exec = Shell.cmd("count_size $path $type").exec()
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        fun checkOTG(): Pair<Int, String> {
            val exec = Shell.cmd("check_otg").exec()
            return Pair(exec.code, exec.out.joinToString(separator = "\n"))
        }
    }
}