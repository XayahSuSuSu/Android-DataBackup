package com.xayah.databackup.util.command

import com.xayah.databackup.util.SymbolUtil
import com.xayah.databackup.util.SymbolUtil.QUOTE
import com.xayah.databackup.util.command.CommonUtil.outString

object PreparationUtil {
    suspend fun listExternalStorage(): List<String> {
        // mount | awk '$3 ~ /\mnt\/media_rw/ {print $3, $5}'
        val exec =
            CommonUtil.execute("mount | awk '${SymbolUtil.USD}3 ~ /${SymbolUtil.BACKSLASH}mnt${SymbolUtil.BACKSLASH}/media_rw/ {print ${SymbolUtil.USD}3, ${SymbolUtil.USD}5}'; mount > /dev/null 2>&1")
        return exec.out
    }

    suspend fun tree(path: String): String {
        val exec = CommonUtil.execute("tree -N $path")
        return exec.outString()
    }

    suspend fun getKeyboard(): Pair<Boolean, String> {
        val exec = CommonUtil.execute("settings get secure default_input_method")
        return Pair(exec.isSuccess, exec.outString().trim())
    }

    suspend fun setKeyboard(keyboard: String): Pair<Boolean, String> {
        var isSuccess = true
        var out = ""
        if (keyboard.isNotEmpty()) {
            CommonUtil.execute("ime enable $QUOTE$keyboard$QUOTE").apply {
                if (this.isSuccess.not()) {
                    isSuccess = false
                    out += this.outString() + "\n"
                }
            }
            CommonUtil.execute("ime set $QUOTE$keyboard$QUOTE").apply {
                if (this.isSuccess.not()) {
                    isSuccess = false
                    out += this.outString() + "\n"
                }
            }
            CommonUtil.execute("settings put secure default_input_method $QUOTE$keyboard$QUOTE").apply {
                if (this.isSuccess.not()) {
                    isSuccess = false
                    out += this.outString() + "\n"
                }
            }
        } else {
            isSuccess = false
        }
        return Pair(isSuccess, out.trim())
    }

    suspend fun getAccessibilityServices(): Pair<Boolean, String> {
        val exec = CommonUtil.execute("settings get secure enabled_accessibility_services")
        return Pair(exec.isSuccess, exec.outString().trim())
    }

    suspend fun setAccessibilityServices(services: String): Pair<Boolean, String> {
        var isSuccess = true
        var out = ""
        if (services.isNotEmpty()) {
            CommonUtil.execute("settings put secure enabled_accessibility_services $QUOTE$services$QUOTE").apply {
                if (this.isSuccess.not()) {
                    isSuccess = false
                    out += this.outString() + "\n"
                }
            }
            CommonUtil.execute("settings put secure accessibility_enabled 1").apply {
                if (this.isSuccess.not()) {
                    isSuccess = false
                    out += this.outString() + "\n"
                }
            }
        } else {
            isSuccess = false
        }
        return Pair(isSuccess, out.trim())
    }
}
