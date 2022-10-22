package com.xayah.databackup.util

import com.xayah.databackup.App
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

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

        suspend fun moveLogToOut(): Pair<Boolean, String> {
            // 将内置日志移到备份目录
            val exec = runOnIO {
                val path = Path.getShellLogPath()
                Command.mkdir(path)
                Command.execute("mv ${App.logcat.logPath} $path", true)
            }
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        suspend fun writeToFile(content: String, path: String): Pair<Boolean, String> {
            val exec = runOnIO {
                var name = ""
                val prefix = path.split("/").toMutableList().apply {
                    name = last()
                    removeLast()
                }
                if (name != "") {
                    val newPath = prefix.joinToString(separator = "/")
                    Command.mkdir(newPath)
                    kotlin.runCatching {
                        File("${Path.getFilesDir()}/$name").apply {
                            createNewFile()
                            writeText(content)
                        }
                    }

                }
                Command.execute("mv ${Path.getFilesDir()}/$name $path", true)
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

        suspend fun findPackage(
            userId: String,
            packageName: String
        ): Pair<Boolean, MutableList<String>> {
            val exec = runOnIO { Command.execute("find_package $userId $packageName") }
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

        suspend fun getKeyboard(): Pair<Boolean, String> {
            val exec = runOnIO { Command.execute("get_keyboard") }
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        suspend fun setKeyboard(keyboard: String): Pair<Boolean, String> {
            val exec = runOnIO { Command.execute("set_keyboard $keyboard") }
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        suspend fun getAccessibilityServices(): Pair<Boolean, String> {
            val exec = runOnIO { Command.execute("get_accessibility_services") }
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        suspend fun setAccessibilityServices(services: String): Pair<Boolean, String> {
            val exec = runOnIO { Command.execute("set_accessibility_services $services") }
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }
    }
}