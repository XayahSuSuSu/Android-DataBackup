package com.xayah.core.util.command

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import com.topjohnwu.superuser.Shell
import com.xayah.core.common.util.trim
import com.xayah.core.util.SymbolUtil
import com.xayah.core.util.SymbolUtil.USD
import com.xayah.core.util.binDir
import com.xayah.core.util.extensionDir
import com.xayah.core.util.filesDir
import com.xayah.core.util.model.ShellResult
import com.xayah.core.util.withIOContext
import java.io.ByteArrayOutputStream
import java.io.File

private class EnvInitializer : Shell.Initializer() {
    companion object {
        fun initShell(shell: Shell, context: Context) {
            shell.newJob()
                .add("nsenter -t 1 -m su") // Switch to global namespace
                .add("export PATH=${context.binDir()}:${SymbolUtil.USD}PATH")
                .add("export PATH=${context.extensionDir()}:${SymbolUtil.USD}PATH")
                .add("export HOME=${context.filesDir()}")
                .add("set -o pipefail") // Ensure that the exit code of each command is correct.
                .exec()
        }
    }

    override fun onInit(context: Context, shell: Shell): Boolean {
        initShell(shell, context)
        return true
    }
}

object BaseUtil {
    fun getShellBuilder() = Shell.Builder.create()
        .setFlags(Shell.FLAG_MOUNT_MASTER or Shell.FLAG_REDIRECT_STDERR)
        .setInitializers(EnvInitializer::class.java)
        .setTimeout(3)

    fun getShellBuilder(context: Context) = getShellBuilder().setContext(context)


    fun getNewShell() = getShellBuilder().build()

    suspend fun execute(vararg args: String, shell: Shell? = null): ShellResult = withIOContext {
        val shellResult = ShellResult(code = -1, input = args.toList().trim(), out = listOf())

        if (shell == null) {
            Shell.cmd(shellResult.inputString).exec().also { result ->
                shellResult.code = result.code
                shellResult.out = result.out
            }
        } else {
            val outList = mutableListOf<String>()
            shell.newJob().to(outList, outList).add(shellResult.inputString).exec().also { result ->
                shellResult.code = result.code
                shellResult.out = outList
            }
        }

        shellResult
    }

    suspend fun kill(vararg keys: String) {
        // ps -A | grep -w $key1 | grep -w $key2 | ... | awk 'NF>1{print $2}' | xargs kill -9
        val keysArg = keys.map { "| grep -w $it" }.toTypedArray()
        execute(
            "ps -A",
            *keysArg,
            "| awk 'NF>1{print ${USD}2}'",
            "| xargs kill -9",
            shell = getNewShell()
        )
    }

    suspend fun mkdirs(dst: String) = withIOContext {
        runCatching {
            val file = File(dst)
            if (file.exists().not()) file.mkdirs() else true
        }
    }

    suspend fun writeIcon(icon: Drawable, dst: String) = withIOContext {
        runCatching {
            val byteArrayOutputStream = ByteArrayOutputStream()
            icon.toBitmap().compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            byteArrayOutputStream.flush()
            byteArrayOutputStream.close()
            File(dst).writeBytes(byteArray)
        }
    }

    suspend fun readIcon(context: Context, src: String): Drawable? = withIOContext {
        runCatching {
            val bytes = File(src).readBytes()
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size).toDrawable(context.resources)
        }.getOrNull()
    }
}
