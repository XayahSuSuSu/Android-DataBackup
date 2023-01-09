package com.xayah.databackup.util

import com.xayah.databackup.data.RcloneConfig


class ExtendCommand {
    companion object {
        private const val TAG = "ExtendCommand"

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
    }
}