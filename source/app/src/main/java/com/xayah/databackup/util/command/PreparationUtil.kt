package com.xayah.databackup.util.command

import android.content.Context
import com.xayah.core.util.SymbolUtil
import com.xayah.core.util.SymbolUtil.QUOTE

object PreparationUtil {
    suspend fun listExternalStorage(): List<String> {
        // mount | awk '$3 ~ /\mnt\/media_rw/ {print $3}'
        val exec =
            CommonUtil.execute("mount | awk '${SymbolUtil.USD}3 ~ /${SymbolUtil.BACKSLASH}mnt${SymbolUtil.BACKSLASH}/media_rw/ {print ${SymbolUtil.USD}3}'")
        return exec.out
    }

    suspend fun getExternalStorageType(path: String): String {
        // mount | awk '$3 == "/mnt/media_rw/6EBF-FE14" {print $5}'
        val exec = CommonUtil.execute("mount | awk '${SymbolUtil.USD}3 == $QUOTE${path}$QUOTE {print ${SymbolUtil.USD}5}'")
        return exec.out.firstOrNull() ?: ""
    }

    suspend fun tree(path: String): String {
        val exec = CommonUtil.execute("tree -N $path")
        return exec.outString
    }

    suspend fun tree(path: String, exclude: List<String>): String {
        val para = exclude.joinToString(separator = "|")
        val exec = CommonUtil.execute("tree -N $path -I '$para'")
        return exec.outString
    }

    suspend fun getKeyboard(): Pair<Boolean, String> {
        val exec = CommonUtil.execute("settings get secure default_input_method")
        return Pair(exec.isSuccess, exec.outString.trim())
    }

    suspend fun setKeyboard(keyboard: String): Pair<Boolean, String> {
        var isSuccess = true
        var out = ""
        if (keyboard.isNotEmpty()) {
            CommonUtil.execute("ime enable $QUOTE$keyboard$QUOTE").apply {
                if (this.isSuccess.not()) {
                    isSuccess = false
                    out += this.outString + "\n"
                }
            }
            CommonUtil.execute("ime set $QUOTE$keyboard$QUOTE").apply {
                if (this.isSuccess.not()) {
                    isSuccess = false
                    out += this.outString + "\n"
                }
            }
            CommonUtil.execute("settings put secure default_input_method $QUOTE$keyboard$QUOTE").apply {
                if (this.isSuccess.not()) {
                    isSuccess = false
                    out += this.outString + "\n"
                }
            }
        } else {
            isSuccess = false
        }
        return Pair(isSuccess, out.trim())
    }

    suspend fun getAccessibilityServices(): Pair<Boolean, String> {
        val exec = CommonUtil.execute("settings get secure enabled_accessibility_services")
        return Pair(exec.isSuccess, exec.outString.trim())
    }

    suspend fun setAccessibilityServices(services: String): Pair<Boolean, String> {
        var isSuccess = true
        var out = ""
        if (services.isNotEmpty()) {
            CommonUtil.execute("settings put secure enabled_accessibility_services $QUOTE$services$QUOTE").apply {
                if (this.isSuccess.not()) {
                    isSuccess = false
                    out += this.outString + "\n"
                }
            }
            CommonUtil.execute("settings put secure accessibility_enabled 1").apply {
                if (this.isSuccess.not()) {
                    isSuccess = false
                    out += this.outString + "\n"
                }
            }
        } else {
            isSuccess = false
        }
        return Pair(isSuccess, out.trim())
    }

    suspend fun setInstallEnv(): Pair<Boolean, String> {
        var isSuccess = true
        var out = ""
        CommonUtil.execute("settings put global verifier_verify_adb_installs 0").apply {
            if (this.isSuccess.not()) {
                isSuccess = false
                out += this.outString + "\n"
            }
        }
        CommonUtil.execute("settings put global package_verifier_enable 0").apply {
            if (this.isSuccess.not()) {
                isSuccess = false
                out += this.outString + "\n"
            }
        }
        CommonUtil.execute("settings get global package_verifier_user_consent").apply {
            if (this.outString.trim() != "-1") {
                CommonUtil.execute("settings put global package_verifier_user_consent -1").apply {
                    if (this.isSuccess.not()) {
                        isSuccess = false
                        out += this.outString + "\n"
                    }
                }
                CommonUtil.execute("settings put global upload_apk_enable 0").apply {
                    if (this.isSuccess.not()) {
                        isSuccess = false
                        out += this.outString + "\n"
                    }
                }
            }
        }

        return Pair(isSuccess, out.trim())
    }

    /**
     * [copyRecursively] actually [deleteRecursively] then [copyTo] target path.
     *
     *
     * This implement [copyRecursively] and preserve the specified attributes (default: mode,ownership,timestamps),
     * if possible additional attributes: context, links, xattr, all) via shell command.
     */
    suspend fun copyRecursivelyAndPreserve(path: String, targetPath: String): Pair<Boolean, String> {
        val exec = CommonUtil.execute("cp -rp $path $targetPath")
        return Pair(exec.isSuccess, exec.outString.trim())
    }

    suspend fun killDaemon(context: Context) {
        // kill -9 $(ps -A | grep com.xayah.databackup.premium:root:daemon | awk 'NF>1{print $2}')
        CommonUtil.execute("kill -9 ${SymbolUtil.USD}(ps -A | grep ${context.packageName}:root:daemon | awk 'NF>1{print ${SymbolUtil.USD}2}')")
    }
}
