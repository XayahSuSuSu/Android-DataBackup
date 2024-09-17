package com.xayah.core.network.client

import android.content.Context
import com.xayah.core.common.util.toPathString
import com.xayah.core.model.SFTPAuthMode
import com.xayah.core.model.database.CloudEntity
import com.xayah.core.model.database.SFTPExtra
import com.xayah.core.network.R
import com.xayah.core.network.io.CountingOutputStreamImpl
import com.xayah.core.network.util.getExtraEntity
import com.xayah.core.rootservice.parcelables.PathParcelable
import com.xayah.core.util.GsonUtil
import com.xayah.core.util.LogUtil
import com.xayah.core.util.PathUtil
import com.xayah.core.util.toPathList
import com.xayah.core.util.withMainContext
import com.xayah.libpickyou.parcelables.DirChildrenParcelable
import com.xayah.libpickyou.parcelables.FileParcelable
import com.xayah.libpickyou.ui.PickYouLauncher
import com.xayah.libpickyou.ui.model.PickerType
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.FileMode
import net.schmizz.sshj.sftp.OpenMode
import net.schmizz.sshj.sftp.RemoteFile
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.userauth.password.PasswordFinder
import net.schmizz.sshj.userauth.password.Resource
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class SFTPClientImpl(private val entity: CloudEntity, private val extra: SFTPExtra) : CloudClient {
    private var sshClient: SSHClient? = null
    private var sftpClient: SFTPClient? = null

    private fun log(msg: () -> String): String = run {
        LogUtil.log { "SFTPClientImpl" to msg() }
        msg()
    }

    private fun <T> withSSHClient(block: (client: SSHClient) -> T) = run {
        if (sshClient == null) throw NullPointerException("SSHClient is null.")
        block(sshClient!!)
    }

    private fun <T> withSFTPClient(block: (client: SFTPClient) -> T) = run {
        if (sftpClient == null) throw NullPointerException("SFTPClient is null.")
        block(sftpClient!!)
    }

    override fun connect() {
        sshClient = SSHClient().apply {
            addHostKeyVerifier(PromiscuousVerifier())
            connect(entity.host, extra.port)

            when (extra.mode) {
                SFTPAuthMode.PASSWORD -> {
                    authPassword(entity.user, entity.pass)
                }

                SFTPAuthMode.PUBLIC_KEY -> {
                    val passwordFinder = object : PasswordFinder {
                        var retryCount = 0

                        override fun reqPassword(resource: Resource<*>?): CharArray {
                            return entity.pass.toCharArray()
                        }

                        override fun shouldRetry(resource: Resource<*>?): Boolean {
                            return ++retryCount < 3
                        }
                    }
                    log { "Authenticating with public key..." }
                    val keyProvider = loadKeys(extra.privateKey, null, passwordFinder)
                    log { "Loaded keys!" }
                    authPublickey(entity.user, keyProvider)
                    log { "Authenticated with public key!" }
                }
            }

            sftpClient = newSFTPClient()
        }
    }

    override fun disconnect() {
        withSSHClient { it.disconnect() }
        sshClient = null
        sftpClient = null
    }

    override fun mkdir(dst: String) {
        log { "mkdir: $dst" }
        withSFTPClient { it.mkdir(dst) }
    }

    override fun mkdirRecursively(dst: String) {
        withSFTPClient { it.mkdirs(dst) }
    }

    private fun openFile(src: String): RemoteFile {
        return withSFTPClient { it.open(src, setOf(OpenMode.READ, OpenMode.WRITE, OpenMode.CREAT)) }
    }

    override fun renameTo(src: String, dst: String) {
        log { "Rename $src to $dst" }
        withSFTPClient { it.rename(src, dst) }
    }

    override fun upload(src: String, dst: String, onUploading: (read: Long, total: Long) -> Unit) {
        val name = PathUtil.getFileName(src)
        val dstPath = "$dst/$name"
        log { "upload: $src to $dstPath" }
        val dstFile = openFile(dstPath)
        val dstStream = dstFile.RemoteFileOutputStream()
        val srcFile = File(src)
        val srcFileSize = srcFile.length()
        val srcInputStream = srcFile.inputStream()
        val countingStream = CountingOutputStreamImpl(dstStream, srcFileSize, onUploading)
        srcInputStream.copyTo(countingStream)
        srcInputStream.close()
        countingStream.close()
        dstFile.close()
        if (countingStream.byteCount == 0L) throw IOException("Failed to write remote file: 0 byte.")
        onUploading(countingStream.byteCount, countingStream.byteCount)
    }

    override fun download(src: String, dst: String, onDownloading: (written: Long, total: Long) -> Unit) {
        val name = PathUtil.getFileName(src)
        val dstPath = "${dst}/$name"
        log { "download: $src to $dstPath" }
        val dstFile = File(dstPath)
        val dstStream = FileOutputStream(dstFile)
        val srcFile = openFile(src)
        val srcFileSize = srcFile.length()
        val srcFileStream = srcFile.RemoteFileInputStream()
        val countingStream = CountingOutputStreamImpl(dstStream, srcFileSize, onDownloading)
        srcFileStream.copyTo(countingStream)
        srcFileStream.close()
        dstStream.close()
    }

    override fun deleteFile(src: String) {
        log { "deleteFile: $src" }
        withSFTPClient { it.rm(src) }
    }

    override fun removeDirectory(src: String) {
        log { "removeDirectory: $src" }
        withSFTPClient { it.rmdir(src) }
    }

    override fun clearEmptyDirectoriesRecursively(src: String) = withSFTPClient { client ->
        val emptyDirs = mutableListOf<String>()
        val paths = mutableListOf(src)

        while (paths.isNotEmpty()) {
            val dir = paths.removeFirst()
            val files = client.ls(dir)
            if (files.isEmpty()) {
                emptyDirs.add(dir)
            } else {
                for (file in files) {
                    val path = "${dir}/${file.name}"
                    if (file.isDirectory) {
                        paths.add(path)
                    }
                }
            }
        }

        // Remove reversed empty dirs.
        for (path in emptyDirs.reversed()) removeDirectory(path)
    }

    override fun deleteRecursively(src: String) {
        withSFTPClient {
            for (item in it.ls(src)) {
                if (item.isDirectory) {
                    deleteRecursively(item.path)
                } else {
                    deleteFile(item.path)
                }
            }

            removeDirectory(src)
        }
    }

    override fun listFiles(src: String): DirChildrenParcelable {
        log { "listFiles: $src" }
        val files = ArrayList<FileParcelable>()
        val directories = ArrayList<FileParcelable>()
        val dirInfo = withSFTPClient { it.ls(src) }
        for (item in dirInfo) {
            if (item.isDirectory) {
                directories.add(FileParcelable(item.name, item.attributes.atime))
            } else {
                files.add(FileParcelable(item.name, item.attributes.atime))
            }
        }
        files.sortBy { it.name }
        directories.sortBy { it.name }
        return DirChildrenParcelable(files = files, directories = directories)
    }

    override fun walkFileTree(src: String): List<PathParcelable> {
        val pathParcelableList = mutableListOf<PathParcelable>()
        val srcFile = withSFTPClient { it.stat(src) }
        if (srcFile.type != FileMode.Type.DIRECTORY) {
            pathParcelableList.add(PathParcelable(src))
        } else {
            val files = listFiles(src)
            for (i in files.files) {
                pathParcelableList.add(PathParcelable("${src}/${i.name}"))
            }
            for (i in files.directories) {
                pathParcelableList.addAll(walkFileTree("${src}/${i.name}"))
            }
        }
        return pathParcelableList
    }

    override fun exists(src: String): Boolean = runCatching { withSFTPClient { it.statExistence(src) } != null }.getOrElse { false }

    override fun size(src: String): Long = runCatching { withSFTPClient { it.size(src) } }.getOrElse { 0 }

    override suspend fun testConnection() {
        connect()
        disconnect()
    }

    private fun handleOriginalPath(path: String): String = run {
        val pathSplit = path.toPathList().toMutableList()
        // Remove “$Cloud:”
        pathSplit.removeFirst()
        // Add "."
        pathSplit.add(0, ".")
        pathSplit.toPathString()
    }

    override suspend fun setRemote(context: Context, onSet: suspend (remote: String, extra: String) -> Unit) {
        val extra = entity.getExtraEntity<SFTPExtra>()!!
        connect()
        PickYouLauncher.apply {
            val prefix = "${context.getString(R.string.cloud)}:"
            sTraverseBackend = { listFiles(it.replaceFirst(prefix, ".")) }
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
