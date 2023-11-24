package com.xayah.core.util.command

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import com.topjohnwu.superuser.Shell
import com.xayah.core.common.util.trim
import com.xayah.core.util.BinArchiveName
import com.xayah.core.util.LogUtil.TAG_SHELL_CODE
import com.xayah.core.util.LogUtil.TAG_SHELL_IN
import com.xayah.core.util.LogUtil.TAG_SHELL_OUT
import com.xayah.core.util.LogUtil.log
import com.xayah.core.util.SymbolUtil.QUOTE
import com.xayah.core.util.SymbolUtil.USD
import com.xayah.core.util.binArchivePath
import com.xayah.core.util.binDir
import com.xayah.core.util.extensionDir
import com.xayah.core.util.filesDir
import com.xayah.core.util.model.ShellResult
import com.xayah.core.util.withIOContext
import net.lingala.zip4j.ZipFile
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

private class EnvInitializer : Shell.Initializer() {
    companion object {
        fun initShell(shell: Shell, context: Context) {
            shell.newJob()
                .add("nsenter -t 1 -m su") // Switch to global namespace
                .add("export PATH=${context.binDir()}:${USD}PATH")
                .add("export PATH=${context.extensionDir()}:${USD}PATH")
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

    private fun getNewShell() = getShellBuilder().build()

    suspend fun execute(vararg args: String, shell: Shell? = null, log: Boolean = true): ShellResult = withIOContext {
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

        if (log) {
            log { TAG_SHELL_IN to shellResult.inputString }
            log { TAG_SHELL_OUT to shellResult.outString }
            log { TAG_SHELL_CODE to shellResult.code.toString() }
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

    @SuppressLint("SetWorldWritable", "SetWorldReadable")
    private fun File.setAllPermissions(): Boolean = run {
        setExecutable(true, false).also {
            if (it.not()) return@run false
        }
        setWritable(true, false).also {
            if (it.not()) return@run false
        }
        setReadable(true, false).also {
            if (it.not()) return@run false
        }
    }

    /**
     * Unzip and return file headers.
     */
    private suspend fun unzip(src: String, dst: String): List<String> = withIOContext {
        runCatching {
            val zip = ZipFile(src)
            zip.extractAll(dst)
            zip.fileHeaders.map { it.fileName }
        }.getOrElse { listOf() }
    }

    private suspend fun releaseAssets(context: Context, src: String, child: String) {
        withIOContext {
            runCatching {
                val assets = File(context.filesDir(), child)
                if (!assets.exists()) {
                    val outStream = FileOutputStream(assets)
                    val inputStream = context.resources.assets.open(src)
                    inputStream.copyTo(outStream)
                    assets.setExecutable(true)
                    assets.setReadable(true)
                    assets.setWritable(true)
                    outStream.flush()
                    inputStream.close()
                    outStream.close()
                }
            }
        }
    }

    suspend fun releaseBase(context: Context): Boolean = withIOContext {
        val bin = File(context.binDir())
        val binArchive = File(context.binArchivePath())

        // Remove old bin files
        bin.deleteRecursively()
        binArchive.deleteRecursively()

        // Release binaries
        releaseAssets(context = context, src = BinArchiveName, child = BinArchiveName)
        unzip(src = context.binArchivePath(), dst = context.binDir())

        // All binaries need full permissions
        bin.listFiles()?.forEach { file ->
            if (file.setAllPermissions().not()) return@withIOContext false
        }

        // Remove binary archive
        binArchive.deleteRecursively()

        return@withIOContext true
    }

    suspend fun readLink(pid: String) = run {
        // readlink /proc/$pid/ns/mnt
        execute(
            "readlink",
            "/proc/$pid/ns/mnt",
            log = false,
        ).outString
    }

    suspend fun readVariable(variable: String) = run {
        // echo "$variable"
        execute(
            "echo",
            "${QUOTE}$USD$variable${QUOTE}",
            log = false,
        ).outString
    }
}
