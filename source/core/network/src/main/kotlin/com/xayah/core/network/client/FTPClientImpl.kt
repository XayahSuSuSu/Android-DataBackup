package com.xayah.core.network.client

import android.content.Context
import com.xayah.core.common.util.toPathString
import com.xayah.core.model.database.CloudEntity
import com.xayah.core.model.database.FTPExtra
import com.xayah.core.network.R
import com.xayah.core.network.util.getExtraEntity
import com.xayah.core.util.GsonUtil
import com.xayah.core.util.LogUtil
import com.xayah.core.util.toPathList
import com.xayah.core.util.withMainContext
import com.xayah.libpickyou.parcelables.DirChildrenParcelable
import com.xayah.libpickyou.parcelables.FileParcelable
import com.xayah.libpickyou.ui.PickYouLauncher
import com.xayah.libpickyou.ui.model.PickerType
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Paths
import javax.security.auth.login.LoginException
import kotlin.io.path.Path
import kotlin.io.path.pathString

class FTPClientImpl(private val entity: CloudEntity, private val extra: FTPExtra) : CloudClient {
    private var client: FTPClient? = null

    private fun log(msg: () -> String): String = run {
        LogUtil.log { "FTPClientImpl" to msg() }
        msg()
    }

    private fun withClient(block: (client: FTPClient) -> Unit) = run {
        if (client == null) throw NullPointerException("Client is null.")
        block(client!!)
    }

    override fun connect() {
        client = FTPClient().apply {
            autodetectUTF8 = true
            connect(entity.host, extra.port)
            if (login(entity.user, entity.pass).not()) throw LoginException("Failed to login, user: ${entity.user}, pass: ${entity.pass}.")
            enterLocalPassiveMode()
            val fileType = FTP.BINARY_FILE_TYPE
            if (setFileType(fileType).not()) throw LoginException("Failed to set file type: $fileType.")
        }
    }

    override fun disconnect() {
        withClient { client ->
            if (client.isConnected) {
                if (client.logout().not()) throw LoginException("Failed to logout.")
                client.disconnect()
            }
        }
        client = null
    }

    override fun mkdir(dst: String) = withClient { client ->
        log { "mkdir: $dst" }
        if (client.makeDirectory(dst).not()) throw IOException("Failed to mkdir: $dst.")
    }

    override fun mkdirRecursively(dst: String) {
        val dirs = dst.split("/")
        withClient { client ->
            for (i in dirs) {
                if (client.changeWorkingDirectory(i).not()) {
                    mkdir(i)
                    client.changeWorkingDirectory(i)
                }
            }
            client.changeWorkingDirectory("/")
        }
    }

    override fun upload(src: String, dst: String) = withClient { client ->
        val name = Paths.get(src).fileName
        val dstPath = "$dst/$name"
        log { "upload: $src to $dstPath" }
        val srcFile = File(src)
        val srcInputStream = FileInputStream(srcFile)
        client.storeFile(dstPath, srcInputStream)
        srcInputStream.close()
    }

    override fun download(src: String, dst: String) = withClient { client ->
        val name = Paths.get(src).fileName
        val dstPath = "$dst/$name"
        log { "download: $src to $dstPath" }
        val dstFile = File(dstPath)
        val srcInputStream: InputStream = client.retrieveFileStream(src)
        val dstOutPutStream: OutputStream = dstFile.outputStream()
        srcInputStream.copyTo(dstOutPutStream)
        srcInputStream.close()
        dstOutPutStream.close()
        client.completePendingCommand()
    }

    override fun deleteFile(src: String) = withClient { client ->
        log { "deleteFile: $src" }
        if (client.deleteFile(src).not()) throw IOException("Failed to delete file: $src.")
    }

    override fun removeDirectory(src: String) = withClient { client ->
        log { "removeDirectory: $src" }
        if (client.removeDirectory(src).not()) throw IOException("Failed to remove dir: $src.")
    }

    /**
     * Actually this is not a recursive function,
     * just keep this name to make it easier to understand.
     */
    override fun deleteRecursively(src: String) = withClient { client ->
        val srcPath = Path(src)
        var srcFile = client.mlistFile(src)
        if (srcFile == null) {
            srcFile = client.listFiles(runCatching { srcPath.parent.pathString }.getOrElse { "." })
                .firstOrNull { it.name == srcPath.fileName.pathString }
        }
        if (srcFile != null) {
            if (srcFile.isDirectory.not()) {
                deleteFile(src)
            } else {
                val dirs = mutableListOf(src)
                val paths = mutableListOf(src)

                // Delete files and append all empty dirs.
                while (paths.isNotEmpty()) {
                    val dir = paths.first()
                    val files = client.listFiles(dir)
                    for (file in files) {
                        val path = "${dir}/${file.name}"
                        if (file.isDirectory.not()) {
                            deleteFile(path)
                        } else {
                            paths.add(path)
                            dirs.add(path)
                        }
                    }
                    paths.removeFirst()
                }

                // Remove reversed empty dirs.
                for (path in dirs.reversed()) removeDirectory(path)
            }
        } else {
            throw IOException("$src not found.")
        }
    }

    override fun listFiles(src: String): DirChildrenParcelable {
        val files = mutableListOf<FileParcelable>()
        val directories = mutableListOf<FileParcelable>()
        withClient { client ->
            val clientFiles = client.listFiles(src)
            for (file in clientFiles) {
                val creationTime = file.timestamp.timeInMillis
                val fileParcelable = FileParcelable(file.name, creationTime)
                if (file.isSymbolicLink) {
                    fileParcelable.link = file.link
                }
                if (file.isDirectory) directories.add(fileParcelable)
                else files.add(fileParcelable)
            }
        }
        files.sortBy { it.name }
        directories.sortBy { it.name }
        return DirChildrenParcelable(files = files, directories = directories)
    }

    override fun size(src: String): Long {
        var size = 0L
        withClient { client ->
            val srcPath = Path(src)
            var srcFile = client.mlistFile(src)
            if (srcFile == null) {
                srcFile = client.listFiles(runCatching { srcPath.parent.pathString }.getOrElse { "." })
                    .firstOrNull { it.name == srcPath.fileName.pathString }
            }
            if (srcFile != null) {
                if (srcFile.isDirectory.not()) {
                    size += client.getSize(src).toLong()
                } else {
                    val files = listFiles(src)
                    for (i in files.files) {
                        size += client.getSize("${src}/${i.name}").toLong()
                    }
                    for (i in files.directories) {
                        size("${src}/${i.name}")
                    }
                }

            } else {
                throw IOException("$src not found.")
            }
        }
        log { "size: $size, $src" }
        return size
    }

    override suspend fun testConnection() {
        connect()
        disconnect()
    }

    private fun handleOriginalPath(path: String): String = run {
        val pathSplit = path.toPathList().toMutableList()
        // Remove “$Cloud:”
        pathSplit.removeFirst()
        pathSplit.toPathString()
    }

    override suspend fun setRemote(context: Context, onSet: suspend (remote: String, extra: String) -> Unit) {
        val extra = entity.getExtraEntity<FTPExtra>()!!
        connect()
        PickYouLauncher.apply {
            val prefix = "${context.getString(R.string.cloud)}:"
            sTraverseBackend = { listFiles(it.pathString.replaceFirst(prefix, "")) }
            sMkdirsBackend = { parent, child ->
                runCatching { mkdirRecursively(handleOriginalPath("$parent/$child")) }.isSuccess
            }
            sTitle = context.getString(R.string.select_target_directory)
            sPickerType = PickerType.DIRECTORY
            sLimitation = 1
            sRootPathList = listOf(prefix)
            sDefaultPathList = listOf(prefix)

        }
        withMainContext {
            val pathList = PickYouLauncher.awaitPickerOnce(context)
            pathList.firstOrNull()?.also { pathString ->
                onSet(handleOriginalPath(pathString), GsonUtil().toJson(extra))
            }
        }
        disconnect()
    }
}
