package com.xayah.databackup.util

import com.topjohnwu.superuser.Shell
import com.xayah.databackup.App
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class Bashrc {
    companion object {
        /**
         * 切换至IO协程运行
         */
        private suspend fun <T> runOnIO(block: suspend () -> T): T {
            return withContext(Dispatchers.IO) { block() }
        }

        /**
         * 读取存储空间
         */
        suspend fun getStorageSpace(path: String): Pair<Boolean, String> {
            val tmpList = mutableListOf<String>()
            val tmpShell = Shell.Builder.create()
                .setFlags(Shell.FLAG_MOUNT_MASTER or Shell.FLAG_REDIRECT_STDERR)
                .setTimeout(8)
                .setInitializers(App.EnvInitializer::class.java)
                .build()
            val exec = runOnIO {
                tmpShell.newJob().to(tmpList).add("get_storage_space \"${path}\"").exec()
            }
            tmpShell.waitAndClose()
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        /**
         * 获取APK路径
         */
        suspend fun getAPKPath(
            packageName: String,
            userId: String,
        ): Pair<Boolean, String> {
            val exec = runOnIO { Command.execute("get_apk_path \"${packageName}\" \"${userId}\"") }
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        /**
         * 路径跳转命令
         */
        suspend fun cd(path: String): Pair<Boolean, String> {
            val exec = runOnIO { Command.execute("cd_to_path \"${path}\"") }
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        /**
         * 压缩APK
         */
        suspend fun compressAPK(
            compressionType: String,
            apkPath: String,
            outPut: String,
            onAddLine: (line: String?) -> Unit
        ): Pair<Boolean, String> {
            val exec = runOnIO {
                val cmd = "compress_apk \"${compressionType}\" \"${apkPath}\" \"${outPut}\""
                Command.execute(cmd) {
                    onAddLine(it)
                }
            }
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
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
            onAddLine: (line: String?) -> Unit
        ): Pair<Boolean, String> {
            val exec = runOnIO {
                val cmd =
                    "compress \"${compressionType}\" \"${dataType}\" \"${packageName}\" \"${outPut}\" \"${dataPath}\""
                Command.execute(cmd) {
                    onAddLine(it)
                }
            }
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        /**
         * 设置安装环境
         */
        suspend fun setInstallEnv(): Pair<Boolean, String> {
            val exec = runOnIO { Command.execute("set_install_env") }
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        /**
         * 安装APK
         */
        suspend fun installAPK(
            inPath: String, packageName: String, userId: String, onAddLine: (line: String?) -> Unit
        ): Pair<Int, String> {
            val exec = runOnIO {
                val cmd = "install_apk \"${inPath}\" \"${packageName}\" \"${userId}\""
                Command.execute(cmd) {
                    onAddLine(it)
                }
            }
            return Pair(exec.code, exec.out.joinToString(separator = "\n"))
        }

        /**
         * 读取SELinux上下文
         */
        suspend fun getSELinuxContext(
            path: String,
        ): String {
            val exec = runOnIO { Command.execute("get_SELinux_context \"${path}\"") }
            return if (exec.isSuccess) exec.out.joinToString(separator = "\n") else ""
        }

        /**
         * 设置所有者和SELinux上下文
         */
        suspend fun setOwnerAndSELinux(
            dataType: String,
            packageName: String,
            path: String,
            userId: String,
            supportFixContext: Boolean,
            context: String,
        ): Pair<Boolean, String> {
            val exec =
                runOnIO { Command.execute("set_owner_and_SELinux \"${dataType}\" \"${packageName}\" \"${path}\" \"${userId}\" \"${supportFixContext}\" \"${context}\"") }
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
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
            onAddLine: (line: String?) -> Unit
        ): Pair<Boolean, String> {
            var out = ""
            val exec = runOnIO {
                val cmd =
                    "decompress \"${compressionType}\" \"${dataType}\" \"${inputPath}\" \"${packageName}\" \"${dataPath}\""
                Command.execute(cmd) {
                    onAddLine(it)
                    out += "${it}\n"
                }
            }
            return Pair(exec.isSuccess, out)
        }

        /**
         * 读取应用版本
         */
        suspend fun getAppVersion(packageName: String): Pair<Boolean, String> {
            val exec = runOnIO { Command.execute("get_app_version \"${packageName}\"") }
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        /**
         * 读取应用版本代码
         */
        suspend fun getAppVersionCode(userId: String, packageName: String): Pair<Boolean, String> {
            val exec =
                runOnIO { Command.execute("get_app_version_code \"${userId}\" \"${packageName}\"") }
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        /**
         * 测试压缩包
         */
        suspend fun testArchive(
            compressionType: String,
            inputPath: String,
        ): Pair<Boolean, String> {
            val exec = runOnIO {
                val cmd = "test_archive \"${compressionType}\" \"${inputPath}\""
                Command.execute(cmd) {}
            }
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        /**
         * 列出当前所有用户
         */
        suspend fun listUsers(): Pair<Boolean, MutableList<String>> {
            val exec = runOnIO { Command.execute("list_users") }
            return Pair(exec.isSuccess, exec.out)
        }

        /**
         * 列出`userId`用户的所有应用包名
         */
        suspend fun listPackages(userId: String): Pair<Boolean, MutableList<String>> {
            val exec = runOnIO { Command.execute("list_packages \"${userId}\"", false) }
            return Pair(exec.isSuccess, exec.out)
        }

        /**
         * 查询是否安装该应用
         */
        suspend fun findPackage(
            userId: String,
            packageName: String
        ): Pair<Boolean, MutableList<String>> {
            val exec = runOnIO { Command.execute("find_package \"${userId}\" \"${packageName}\"") }
            return Pair(exec.isSuccess, exec.out)
        }

        /**
         * 计算`path`占用大小
         */
        suspend fun countSize(path: String, type: Int): Pair<Boolean, String> {
            val exec = runOnIO { Command.execute("count_size \"${path}\" \"${type}\"") }
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        /**
         * 检查OTG
         */
        suspend fun listExternalStorage(): Pair<Boolean, List<String>> {
            val exec = runOnIO { Command.execute("list_external_storage") }
            return Pair(exec.isSuccess, exec.out)
        }

        /**
         * 读取输入法信息
         */
        suspend fun getKeyboard(): Pair<Boolean, String> {
            val exec = runOnIO { Command.execute("get_keyboard") }
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        /**
         * 设置输入法
         */
        suspend fun setKeyboard(keyboard: String): Pair<Boolean, String> {
            val exec = runOnIO { Command.execute("set_keyboard \"${keyboard}\"") }
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        /**
         * 读取无障碍信息
         */
        suspend fun getAccessibilityServices(): Pair<Boolean, String> {
            val exec = runOnIO { Command.execute("get_accessibility_services") }
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        /**
         * 设置无障碍
         */
        suspend fun setAccessibilityServices(services: String): Pair<Boolean, String> {
            val exec = runOnIO { Command.execute("set_accessibility_services \"${services}\"") }
            return Pair(exec.isSuccess, exec.out.joinToString(separator = "\n"))
        }

        /**
         * 检查Bashrc环境
         */
        suspend fun checkBashrc(): Boolean {
            val exec = runOnIO { Command.execute("check_bashrc") }
            return exec.isSuccess
        }
    }
}