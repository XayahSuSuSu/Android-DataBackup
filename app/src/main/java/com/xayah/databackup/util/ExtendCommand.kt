package com.xayah.databackup.util

import com.xayah.databackup.data.RcloneConfig
import com.xayah.databackup.data.RcloneMount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class ExtendCommand {
    companion object {
        private const val TAG = "ExtendCommand"

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
            Command.execute("ls -l ${Path.getFilesDir()}/extend").out.apply {
                var count = 0
                try {
                    val fileList = this.subList(1, this.size)
                    for (i in fileList) if (i.contains("-rwxrwxrwx")) count++
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return count == 2
            }
        }

        /**
         * 检查Rclone版本
         */
        suspend fun checkRcloneVersion(): String {
            Command.execute("rclone --version").out.apply {
                return this[0].replace("rclone", "").trim()
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
         * 检查Fusermount版本
         */
        suspend fun rcloneConfigCreate(
            name: String,
            url: String,
            user: String,
            pass: String
        ): Boolean {
            Command.execute("rclone config create \"${name}\" webdav url=\"${url}\" vendor=other user=\"${user}\" pass=\"${pass}\"")
                .apply {
                    return this.isSuccess
                }
        }

        /**
         * 解析Rclone配置文件
         */
        suspend fun rcloneConfigParse(): MutableList<RcloneConfig> {
            val rcloneConfigList = mutableListOf<RcloneConfig>()
            val exec = Command.execute("rclone config show").apply {
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
                        "url" -> rcloneConfig?.url = element[1]
                        "vendor" -> rcloneConfig?.vendor = element[1]
                        "user" -> rcloneConfig?.user = element[1]
                        "pass" -> rcloneConfig?.pass = element[1]
                    }
                }
            }
            return rcloneConfigList
        }

        /**
         * Rclone配置移除
         */
        suspend fun rcloneConfigDelete(name: String): Boolean {
            Command.execute("rclone config delete \"${name}\"").apply {
                return this.isSuccess
            }
        }

        /**
         * 构建挂载列表
         */
        suspend fun getRcloneMountMap(): HashMap<String, RcloneMount> {
            var rcloneMountMap = hashMapOf<String, RcloneMount>()
            runOnIO {
                // 读取应用列表配置文件
                Command.cat(Path.getRcloneMountListPath()).apply {
                    if (this.first) {
                        rcloneMountMap = JSON.fromMountHashMapJson(this.second)
                    }
                }
            }
            return rcloneMountMap
        }

        /**
         * Rclone挂载
         */
        suspend fun rcloneMount(name: String, dest: String): Boolean {
            Command.execute("rclone mount \"${name}:\" \"${dest}\" --allow-non-empty --allow-other --allow-root --daemon --vfs-cache-mode off")
                .apply {
                    return this.isSuccess
                }
        }

        /**
         * Rclone挂载检验
         */
        suspend fun rcloneMountCheck(dest: String): Boolean {
            Command.execute("df -h").apply {
                return this.out.joinToLineString.contains(dest)
            }
        }

        /**
         * Rclone取消挂载
         */
        suspend fun rcloneUnmount(dest: String): Boolean {
            Command.execute("fusermount -u $dest").apply {
                var isSuccess = this.isSuccess
                if (isSuccess.not()) {
                    // 取消挂载失败, 尝试杀死进程
                    Command.execute("pgrep 'rclone' | xargs kill -9").apply {
                        isSuccess = this.isSuccess
                    }
                }
                return isSuccess
            }
        }
    }
}