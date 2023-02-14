package com.xayah.databackup.util

import android.content.Context
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.topjohnwu.superuser.Shell
import com.xayah.databackup.App
import com.xayah.databackup.data.RcloneConfig
import com.xayah.databackup.data.RcloneMount
import com.xayah.databackup.view.setLoading
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ExtendCommand {

    companion object {
        init {
            SafeFile.create(Path.getInternalLogPath()) {
                it.apply {
                    deleteRecursive()
                    mkdirs()
                }
            }
        }

        private const val TAG = "ExtendCommand"
        val logPath =
            "${Path.getInternalLogPath()}/rclone_log_${GlobalObject.getInstance().timeStampOnStart}"

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
            context: Context,
            type: String,
            name: String,
            args: String,
        ): Boolean {
            BottomSheetDialog(context).apply {
                setLoading()
                // 新建配置
                Command.execute("rclone config create \"${name}\" \"${type}\" $args --log-file $logPath")
                return if (rcloneTest(name)) {
                    // 测试通过
                    Toast.makeText(App.globalContext, GlobalString.success, Toast.LENGTH_SHORT)
                        .show()
                    dismiss()
                    true
                } else {
                    Toast.makeText(App.globalContext, "无法连接至服务器!", Toast.LENGTH_SHORT).show()
                    rcloneConfigDelete(name, false)
                    dismiss()
                    false
                }
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
        suspend fun rcloneConfigDelete(name: String, notify: Boolean = true): Boolean {
            Command.execute("rclone config delete \"${name}\" --log-file $logPath")
                .apply {
                    if (notify) notifyForCommand(this.isSuccess)
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
                SafeFile.create(Path.getRcloneMountListPath()) {
                    rcloneMountMap = GsonUtil.getInstance().fromRcloneMountMapJson(it.readText())
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
            val tmpList = mutableListOf<String>()
            val tmpShell = Shell.Builder.create()
                .setFlags(Shell.FLAG_MOUNT_MASTER or Shell.FLAG_REDIRECT_STDERR)
                .setTimeout(8)
                .setInitializers(App.EnvInitializer::class.java)
                .build()
            // Kill Rclone守护进程
            tmpShell.newJob().to(tmpList).add("kill -9 \$(ps -A | grep rclone | awk '{print \$2}')")
                .exec()
            tmpShell.newJob().to(tmpList).add("mount").exec().apply {
                for (i in this.out) {
                    if (i.contains("fuse.rclone")) {
                        val path = i.split(" ")[2]
                        Command.execute("umount -f \"${path}\"")
                    }
                }
            }
            tmpShell.waitAndClose()
            val map = getRcloneMountMap()
            for (i in map.values) {
                i.mounted = false
            }
            GsonUtil.saveRcloneMountMapToFile(map)
        }

        /**
         * 检查本地扩展模块版本
         */
        suspend fun checkExtendLocalVersion(): String {
            var version = ""
            withContext(Dispatchers.IO) {
                val extendVersionPath = "${Path.getAppInternalFilesPath()}/extend/version"
                SafeFile.create(extendVersionPath) {
                    version = it.readText()
                }
            }
            return version
        }

        /**
         * Rclone非递归列出文件列表以测试服务器连接
         */
        suspend fun rcloneTest(name: String): Boolean {
            return Command.execute("rclone ls \"${name}:\" --max-depth 1 > /dev/null 2>&1").isSuccess
        }
    }
}