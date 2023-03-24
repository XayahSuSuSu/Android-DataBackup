package com.xayah.databackup.util.command

import com.xayah.databackup.util.joinToLineString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class Preparation {
    companion object {
        private const val QUOTE = '"'
        private const val USD = '$'
        private const val BACKSLASH = '\\'

        private suspend fun <T> runOnIO(block: suspend () -> T): T {
            return withContext(Dispatchers.IO) { block() }
        }

        suspend fun setInstallEnv(): Pair<Boolean, String> {
            var isSuccess = true
            var out = ""
            Command.execute("settings put global verifier_verify_adb_installs 0").apply {
                if (this.isSuccess.not()) {
                    isSuccess = false
                    out += this.out.joinToLineString + "\n"
                }
            }
            Command.execute("settings put global package_verifier_enable 0").apply {
                if (this.isSuccess.not()) {
                    isSuccess = false
                    out += this.out.joinToLineString + "\n"
                }
            }
            val exec = Command.execute("settings get global package_verifier_user_consent")
            if (exec.out.joinToLineString.trim() != "-1") {
                Command.execute("settings put global package_verifier_user_consent -1").apply {
                    if (this.isSuccess.not()) {
                        isSuccess = false
                        out += this.out.joinToLineString + "\n"
                    }
                }
                Command.execute("settings put global upload_apk_enable 0").apply {
                    if (this.isSuccess.not()) {
                        isSuccess = false
                        out += this.out.joinToLineString + "\n"
                    }
                }
            }
            return Pair(isSuccess, out.trim())
        }

        suspend fun getKeyboard(): Pair<Boolean, String> {
            val exec = Command.execute("settings get secure default_input_method")
            return Pair(exec.isSuccess, exec.out.joinToLineString.trim())
        }

        suspend fun setKeyboard(keyboard: String): Pair<Boolean, String> {
            var isSuccess = true
            var out = ""
            if (keyboard.isNotEmpty()) {
                Command.execute("ime enable $QUOTE$keyboard$QUOTE").apply {
                    if (this.isSuccess.not()) {
                        isSuccess = false
                        out += this.out.joinToLineString + "\n"
                    }
                }
                Command.execute("ime set $QUOTE$keyboard$QUOTE").apply {
                    if (this.isSuccess.not()) {
                        isSuccess = false
                        out += this.out.joinToLineString + "\n"
                    }
                }
                Command.execute("settings put secure default_input_method $QUOTE$keyboard$QUOTE").apply {
                    if (this.isSuccess.not()) {
                        isSuccess = false
                        out += this.out.joinToLineString + "\n"
                    }
                }
            }
            return Pair(isSuccess, out.trim())
        }

        suspend fun getAccessibilityServices(): Pair<Boolean, String> {
            val exec = Command.execute("settings get secure enabled_accessibility_services")
            return Pair(exec.isSuccess, exec.out.joinToLineString.trim())
        }

        suspend fun setAccessibilityServices(services: String): Pair<Boolean, String> {
            var isSuccess = true
            var out = ""
            if (services.isNotEmpty()) {
                Command.execute("settings put secure enabled_accessibility_services $QUOTE$services$QUOTE").apply {
                    if (this.isSuccess.not()) {
                        isSuccess = false
                        out += this.out.joinToLineString + "\n"
                    }
                }
                Command.execute("settings put secure accessibility_enabled 1").apply {
                    if (this.isSuccess.not()) {
                        isSuccess = false
                        out += this.out.joinToLineString + "\n"
                    }
                }
            }
            return Pair(isSuccess, out.trim())
        }

        suspend fun listExternalStorage(): Pair<Boolean, List<String>> {
            // mount | awk '$3 ~ /\mnt\/media_rw/ {print $3, $5}'
            val exec = Command.execute("mount | awk '${USD}3 ~ /${BACKSLASH}mnt$BACKSLASH/media_rw/ {print ${USD}3, ${USD}5}'")
            return Pair(exec.isSuccess, exec.out)
        }

        suspend fun listUsers(): Pair<Boolean, List<String>> {
            val exec = Command.execute("ls -1 $QUOTE/data/user$QUOTE")
            return Pair(exec.isSuccess, exec.out)
        }
    }
}