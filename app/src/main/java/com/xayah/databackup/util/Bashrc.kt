package com.xayah.databackup.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class Bashrc {
    companion object {
        private suspend fun <T> runOnIO(block: suspend () -> T): T {
            return withContext(Dispatchers.IO) { block() }
        }

        suspend fun getStorageSpace(path: String): Pair<Boolean, String> {
            val exec = runOnIO { Command.execute("get_storage_space $path") }
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        suspend fun getAPKPath(packageName: String, userId: String): Pair<Boolean, String> {
            val exec = runOnIO { Command.execute("get_apk_path $packageName $userId") }
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        suspend fun cd(path: String): Pair<Boolean, String> {
            val exec = runOnIO { Command.execute("cd_to_path $path") }
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        suspend fun compressAPK(
            compressionType: String, outPut: String, onAddLine: (line: String?) -> Unit
        ): Pair<Boolean, String> {
            val exec = runOnIO {
                val cmd = "compress_apk $compressionType $outPut"
                Command.execute(cmd) {
                    onAddLine(it)
                }
            }
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        suspend fun compress(
            compressionType: String,
            dataType: String,
            packageName: String,
            outPut: String,
            dataPath: String,
            onAddLine: (line: String?) -> Unit
        ): Pair<Boolean, String> {
            val exec = runOnIO {
                val cmd = "compress $compressionType $dataType $packageName $outPut $dataPath"
                Command.execute(cmd) {
                    onAddLine(it)
                }
            }
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        suspend fun setInstallEnv(): Pair<Boolean, String> {
            val exec = runOnIO { Command.execute("set_install_env") }
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        suspend fun installAPK(
            inPath: String, packageName: String, userId: String, onAddLine: (line: String?) -> Unit
        ): Pair<Int, String> {
            val exec = runOnIO {
                val cmd = "install_apk $inPath $packageName $userId"
                Command.execute(cmd) {
                    onAddLine(it)
                }
            }
            return Pair(exec.code, exec.out.joinToString(separator = "\n"))
        }

        suspend fun setOwnerAndSELinux(
            dataType: String,
            packageName: String,
            path: String,
            userId: String,
            supportFixContext: Boolean
        ): Pair<Boolean, String> {
            val exec =
                runOnIO { Command.execute("set_owner_and_SELinux $dataType $packageName $path $userId $supportFixContext") }
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        suspend fun decompress(
            compressionType: String,
            dataType: String,
            inputPath: String,
            packageName: String,
            dataPath: String,
            onAddLine: (line: String?) -> Unit
        ): Pair<Boolean, String> {
            val exec = runOnIO {
                val cmd = "decompress $compressionType $dataType $inputPath $packageName $dataPath"
                Command.execute(cmd) {
                    onAddLine(it)
                }
            }
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        suspend fun getAppVersion(packageName: String): Pair<Boolean, String> {
            val exec = runOnIO { Command.execute("get_app_version $packageName") }
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        suspend fun getAppVersionCode(userId: String, packageName: String): Pair<Boolean, String> {
            val exec = runOnIO { Command.execute("get_app_version_code $userId $packageName") }
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        suspend fun writeToFile(content: String, path: String): Pair<Boolean, String> {
            val exec = runOnIO {
                val prefix = path.split("/").toMutableList().apply {
                    removeLast()
                }
                val newPath = prefix.joinToString(separator = "/")
                Command.mkdir(newPath)
                Command.execute("write_to_file \'$content\' \"$path\"", false)
            }
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        suspend fun testArchive(
            compressionType: String,
            inputPath: String,
        ): Pair<Boolean, String> {
            val exec = runOnIO {
                val cmd = "test_archive $compressionType $inputPath"
                Command.execute(cmd) {}
            }
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        suspend fun listUsers(): Pair<Boolean, MutableList<String>> {
            val exec = runOnIO { Command.execute("list_users") }
            return Pair(exec.isSuccess, exec.out)
        }

        suspend fun listPackages(userId: String): Pair<Boolean, MutableList<String>> {
            val exec = runOnIO { Command.execute("list_packages $userId") }
            return Pair(exec.isSuccess, exec.out)
        }

        suspend fun countSize(path: String, type: Int): Pair<Boolean, String> {
            val exec = runOnIO { Command.execute("count_size $path $type") }
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        suspend fun checkOTG(): Pair<Int, String> {
            val exec = runOnIO { Command.execute("check_otg") }
            return Pair(exec.code, exec.out.joinToString(separator = "\n"))
        }
    }
}