package com.xayah.databackup.util

class Bashrc {
    companion object {
        fun getStorageSpace(path: String): Pair<Boolean, String> {
            val exec = Command.execute("get_storage_space $path")
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        fun getAPKPath(packageName: String, userId: String): Pair<Boolean, String> {
            val exec = Command.execute("get_apk_path $packageName $userId")
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        fun cd(path: String): Pair<Boolean, String> {
            val exec = Command.execute("cd_to_path $path")
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        fun compressAPK(
            compressionType: String, outPut: String, onAddLine: (line: String?) -> Unit
        ): Pair<Boolean, String> {
            val cmd = "compress_apk $compressionType $outPut"
            val exec = Command.execute(cmd) {
                onAddLine(it)
            }
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
            val exec = Command.execute(cmd) {
                onAddLine(it)
            }
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        fun setInstallEnv(): Pair<Boolean, String> {
            val exec = Command.execute("set_install_env")
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        fun installAPK(
            inPath: String, packageName: String, userId: String, onAddLine: (line: String?) -> Unit
        ): Pair<Int, String> {
            val cmd = "install_apk $inPath $packageName $userId"
            val exec = Command.execute(cmd) {
                onAddLine(it)
            }
            return Pair(exec.code, exec.out.joinToString(separator = "\n"))
        }

        fun setOwnerAndSELinux(
            dataType: String, packageName: String, path: String, userId: String
        ): Pair<Boolean, String> {
            val exec = Command.execute("set_owner_and_SELinux $dataType $packageName $path $userId")
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
            val exec = Command.execute(cmd) {
                onAddLine(it)
            }
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        fun getAppVersion(packageName: String): Pair<Boolean, String> {
            val exec = Command.execute("get_app_version $packageName")
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        fun getAppVersionCode(userId: String, packageName: String): Pair<Boolean, String> {
            val exec = Command.execute("get_app_version_code $userId $packageName")
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        fun writeToFile(content: String, path: String): Pair<Boolean, String> {
            val prefix = path.split("/").toMutableList().apply {
                removeLast()
            }
            val newPath = prefix.joinToString(separator = "/")
            Command.mkdir(newPath)
            val exec = Command.execute("write_to_file \'$content\' \"$path\"", false)
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        fun testArchive(
            compressionType: String,
            inputPath: String,
        ): Pair<Boolean, String> {
            val cmd = "test_archive $compressionType $inputPath"
            val exec = Command.execute(cmd) {}
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        fun listUsers(): Pair<Boolean, MutableList<String>> {
            val exec = Command.execute("list_users")
            return Pair(exec.isSuccess, exec.out)
        }

        fun listPackages(userId: String): Pair<Boolean, MutableList<String>> {
            val exec = Command.execute("list_packages $userId")
            return Pair(exec.isSuccess, exec.out)
        }

        fun countSize(path: String, type: Int): Pair<Boolean, String> {
            val exec = Command.execute("count_size $path $type")
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        fun checkOTG(): Pair<Int, String> {
            val exec = Command.execute("check_otg")
            return Pair(exec.code, exec.out.joinToString(separator = "\n"))
        }
    }
}