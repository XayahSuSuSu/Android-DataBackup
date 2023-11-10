package com.xayah.core.util.command

import com.xayah.core.util.SymbolUtil.BACKSLASH
import com.xayah.core.util.SymbolUtil.QUOTE
import com.xayah.core.util.SymbolUtil.USD
import com.xayah.core.util.command.BaseUtil.execute
import com.xayah.core.util.model.ShellResult

object PreparationUtil {
    suspend fun listExternalStorage(): ShellResult = run {
        // mount | awk '$3 ~ /\mnt\/media_rw/ {print $3}'
        execute(
            "mount",
            "|",
            "awk",
            "'${USD}3 ~ /${BACKSLASH}mnt${BACKSLASH}/media_rw/ {print ${USD}3}'",
        )
    }

    suspend fun getExternalStorageType(path: String): ShellResult = run {
        // mount | awk '$3 == "/mnt/media_rw/6EBF-FE14" {print $5}'
        execute(
            "mount",
            "|",
            "awk",
            "'${USD}3 == ${QUOTE}${path}${QUOTE} {print ${USD}5}'",
        )
    }

    suspend fun getInputMethods(): ShellResult = run {
        // settings get secure default_input_method
        execute(
            "settings",
            "get",
            "secure",
            "default_input_method",
        )
    }

    suspend fun setInputMethods(inputMethods: String): ShellResult = run {
        var isSuccess: Boolean
        val out = mutableListOf<String>()

        // ime enable "$inputMethods"
        execute(
            "ime",
            "enable",
            "${QUOTE}${inputMethods}${QUOTE}",
        ).also { result ->
            isSuccess = result.isSuccess
            out.addAll(result.out)
        }

        // ime set "$inputMethods"
        execute(
            "ime",
            "set",
            "${QUOTE}${inputMethods}${QUOTE}",
        ).also { result ->
            isSuccess = isSuccess and result.isSuccess
            out.addAll(result.out)
        }

        // settings put secure default_input_method "$inputMethods"
        execute(
            "settings",
            "put",
            "secure",
            "default_input_method",
            "${QUOTE}${inputMethods}${QUOTE}",
        ).also { result ->
            isSuccess = isSuccess and result.isSuccess
            out.addAll(result.out)
        }

        ShellResult(code = if (isSuccess) 0 else -1, input = listOf(), out = out)
    }

    suspend fun getAccessibilityServices(): ShellResult = run {
        // settings get secure enabled_accessibility_services
        execute(
            "settings",
            "get",
            "secure",
            "enabled_accessibility_services",
        )
    }

    suspend fun setAccessibilityServices(accessibilityServices: String): ShellResult = run {
        var isSuccess: Boolean
        val out = mutableListOf<String>()

        // settings put secure enabled_accessibility_services "$accessibilityServices"
        execute(
            "settings",
            "put",
            "secure",
            "enabled_accessibility_services",
            "${QUOTE}${accessibilityServices}${QUOTE}",
        ).also { result ->
            isSuccess = result.isSuccess
            out.addAll(result.out)
        }

        // settings put secure accessibility_enabled 1
        execute(
            "settings",
            "put",
            "secure",
            "accessibility_enabled",
            "1",
        ).also { result ->
            isSuccess = isSuccess and result.isSuccess
            out.addAll(result.out)
        }

        ShellResult(code = if (isSuccess) 0 else -1, input = listOf(), out = out)
    }
}
