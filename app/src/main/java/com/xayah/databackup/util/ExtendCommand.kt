package com.xayah.databackup.util

import android.widget.Toast
import com.topjohnwu.superuser.io.SuFile
import com.xayah.databackup.App
import com.xayah.databackup.data.RcloneConfig
import com.xayah.databackup.data.RcloneMount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ExtendCommand {
    init {
        SuFile(Path.getInternalLogPath()).apply {
            deleteRecursively()
            mkdirs()
        }
    }

    companion object {
        private const val TAG = "ExtendCommand"
        val logPath = "${Path.getInternalLogPath()}/rclone_log_${App.getTimeStamp()}"

        /**
         * 切换至IO协程运行
         */
        private suspend fun <T> runOnIO(block: suspend () -> T): T {
            return withContext(Dispatchers.IO) { block() }
        }

        /**
         * 检查扩展文件
         */
        suspend fun checkExtend(): Boolean {
            Command.execute("ls -l \"${Path.getAppInternalFilesPath()}/extend\"").out.apply {
                var count = 0
                try {
                    val fileList = this.subList(1, this.size)
                    for (i in fileList) if (i.contains("-rwxrwxrwx")) count++
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return count == 3
            }
        }

        /**
         * 检查Rclone版本
         */
        suspend fun checkRcloneVersion(): String {
            Command.execute("rclone --version").out.apply {
                return try {
                    this[0].replace("rclone", "").trim()
                } catch (e: Exception) {
                    ""
                }
            }
        }

        /**
         * 检查Fusermount版本
         */
        suspend fun checkFusermountVersion(): String {
            Command.execute("fusermount --version").out.apply {
                return this.joinToLineString.replace("fusermount3 version:", "").trim()
            }
        }

        /**
         * 根据命令执行成功与否弹出相应Toast
         */
        private fun notifyForCommand(isSuccess: Boolean) {
            if (isSuccess) {
                Toast.makeText(App.globalContext, GlobalString.success, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(App.globalContext, GlobalString.failed, Toast.LENGTH_SHORT).show()
            }
        }

        /**
         * 检查Fusermount版本
         */
        suspend fun rcloneConfigCreate(
            type: String,
            name: String,
            args: String,
        ): Boolean {
            Command.execute("rclone config create \"${name}\" \"${type}\" $args --log-file $logPath")
                .apply {
                    notifyForCommand(this.isSuccess)
                    return this.isSuccess
                }
        }

        /**
         * 解析Rclone配置文件
         */
        suspend fun rcloneConfigParse(): MutableList<RcloneConfig> {
            val rcloneConfigList = mutableListOf<RcloneConfig>()
            try {
                val exec =
                    Command.execute("rclone config show")
                        .apply {
                            this.out.add("[]")
                        }
                var rcloneConfig: RcloneConfig? = null
                for (i in exec.out) {
                    if (i.isEmpty()) continue
                    if (i.first() == '[' && i.last() == ']') {
                        // 配置起始符
                        if (rcloneConfig != null) {
                            rcloneConfigList.add(rcloneConfig)
                        }
                        rcloneConfig = RcloneConfig(
                            name = i.replace("[", "").replace("]", "")
                        )
                    } else {
                        val element = i.split(" = ")
                        when (element[0]) {
                            "type" -> rcloneConfig?.type = element[1]
                            "user" -> rcloneConfig?.user = element[1]
                            "pass" -> rcloneConfig?.pass = element[1]
                            // WebDav
                            "url" -> rcloneConfig?.url = element[1]
                            "vendor" -> rcloneConfig?.vendor = element[1]
                            // FTP
                            "host" -> rcloneConfig?.host = element[1]
                            "port" -> rcloneConfig?.port = element[1]
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return rcloneConfigList
        }

        /**
         * Rclone配置移除
         */
        suspend fun rcloneConfigDelete(name: String): Boolean {
            Command.execute("rclone config delete \"${name}\" --log-file $logPath")
                .apply {
                    notifyForCommand(this.isSuccess)
                    return this.isSuccess
                }
        }

        /**
         * 构建挂载列表
         */
        suspend fun getRcloneMountMap(): HashMap<String, RcloneMount> {
            var rcloneMountMap = hashMapOf<String, RcloneMount>()
            runOnIO {
                // 读取配置文件
                try {
                    SuFile(Path.getRcloneMountListPath()).apply {
                        rcloneMountMap = GsonUtil.getInstance().fromRcloneMountMapJson(readText())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return rcloneMountMap
        }

        /**
         * Rclone挂载
         */
        suspend fun rcloneMount(name: String, dest: String): Boolean {
            Command.execute("rclone mount \"${name}:\" \"${dest}\" --allow-non-empty --allow-other --daemon --vfs-cache-mode off --log-file $logPath")
                .apply {
                    notifyForCommand(this.isSuccess)
                    return this.isSuccess
                }
        }

        /**
         * Rclone取消挂载
         */
        suspend fun rcloneUnmount(name: String, notify: Boolean = true): Boolean {
            var isSuccess = true

            // Kill Rclone守护进程
            Command.execute("kill -9 \$(ps -A | grep rclone | awk '{print \$2}')")
            Command.execute("mount").apply {
                for (i in this.out) {
                    if (i.contains("${name}:") && i.contains("rclone")) {
                        val path = i.split(" ")[2]
                        Command.execute("umount -f \"${path}\"").apply {
                            if (this.isSuccess.not()
                                && this.out.joinToLineString.contains("Invalid argument").not()
                            ) isSuccess = false
                        }
                    }
                }
            }
            if (notify)
                notifyForCommand(isSuccess)
            return isSuccess
        }

        /**
         * Rclone取消挂载所有配置
         */
        suspend fun rcloneUnmountAll() {
            // Kill Rclone守护进程
            Command.execute("kill -9 \$(ps -A | grep rclone | awk '{print \$2}')")
        }

        /**
         * 检查本地扩展模块版本
         */
        suspend fun checkExtendLocalVersion(): String {
            var version = ""
            withContext(Dispatchers.IO) {
                val extendVersionPath = "${Path.getAppInternalFilesPath()}/extend/version"
                version = SuFile(extendVersionPath).readText()
            }
            return version
        }
    }
}