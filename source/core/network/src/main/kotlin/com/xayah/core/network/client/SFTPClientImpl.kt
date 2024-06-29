package com.xayah.core.network.client

import android.content.Context
import com.hierynomus.smbj.io.InputStreamByteChunkProvider
import com.xayah.core.common.util.toPathString
import com.xayah.core.model.database.CloudEntity
import com.xayah.core.model.database.FTPExtra
import com.xayah.core.network.R
import com.xayah.core.network.io.CountingInputStreamImpl
import com.xayah.core.network.io.CountingOutputStreamImpl
import com.xayah.core.network.util.getExtraEntity
import com.xayah.core.rootservice.parcelables.PathParcelable
import com.xayah.core.util.GsonUtil
import com.xayah.core.util.LogUtil
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
import net.schmizz.sshj.sftp.RemoteFile.RemoteFileOutputStream
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Paths
import kotlin.io.path.pathString


class SFTPClientImpl(private val entity: CloudEntity, private val extra: FTPExtra) : CloudClient {
    private var sshClient: SSHClient? = null
    private var sftpClient: SFTPClient? = null

    private fun log(msg: () -> String): String = run {
        LogUtil.log { "FTPClientImpl" to msg() }
        msg()
    }

    private fun <T> withSSHClient(block: (client: SSHClient) -> T) = run {
        if (sshClient == null) throw NullPointerException("Client is null.")
        block(sshClient!!)
    }

    private fun <T> withSFTPClient(block: (client: SFTPClient) -> T) = run {
        if (sftpClient == null) throw NullPointerException("Client is null.")
        block(sftpClient!!)
    }

    override fun connect() {
        sshClient = SSHClient().apply {
            addHostKeyVerifier(PromiscuousVerifier())
            connect(entity.host, extra.port)
            authPassword(entity.user, entity.pass)
            sftpClient = newSFTPClient()
        }
    }

    override fun disconnect() {
        withSSHClient { it.disconnect() }
    }

    override fun mkdir(dst: String) {
        withSFTPClient { it.mkdir(dst) }
    }

    override fun mkdirRecursively(dst: String) {
        withSFTPClient { it.mkdirs(dst) }
    }

    private fun openFile(src: String): RemoteFile {
        return withSFTPClient {
            it.open(
                src,
                setOf(
                    OpenMode.READ,
                    OpenMode.WRITE,
                    OpenMode.CREAT,
                )
            )
        }
    }

    override fun renameTo(src: String, dst: String) {
        log { "Rename $src to $dst" }
        withSFTPClient { it.rename(src, dst) }
    }

    override fun upload(src: String, dst: String, onUploading: (read: Long, total: Long) -> Unit) {
        val name = Paths.get(src).fileName
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
        onUploading(countingStream.byteCount, countingStream.byteCount)
    }

    override fun download(
        src: String,
        dst: String,
        onDownloading: (written: Long, total: Long) -> Unit
    ) {
        val name = Paths.get(src).fileName
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
        withSFTPClient { it.rm(src) }
    }

    override fun removeDirectory(src: String) {
        withSFTPClient { it.rmdir(src) }
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
        val files = ArrayList<FileParcelable>()
        val directories = ArrayList<FileParcelable>()

        for (item in withSFTPClient { it.ls(src) }) {
            if (item.isDirectory) {
                directories.add(
                    FileParcelable(
                        item.name,
                        item.attributes.atime,
                    )
                )
            } else {
                files.add(
                    FileParcelable(
                        item.name,
                        item.attributes.atime,
                    )
                )
            }
        }

        files.sortBy { it.name }
        directories.sortBy { it.name }
        return DirChildrenParcelable(files, directories)
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

    override fun exists(src: String): Boolean {
        return try {
            withSFTPClient { it.statExistence(src) } != null
        } catch (e: Throwable) {
            log { e.toString() }
            false
        }
    }

    override fun size(src: String): Long {
        return try {
            withSFTPClient { it.size(src) }
        } catch (e: Throwable) {
            log { e.toString() }
            -1
        }
    }

    override suspend fun testConnection() {
        connect()
        disconnect()
    }

    override suspend fun setRemote(
        context: Context,
        onSet: suspend (remote: String, extra: String) -> Unit
    ) {
        val extra = entity.getExtraEntity<FTPExtra>()!!
        connect()
        PickYouLauncher.apply {
            val prefix = "/home/${entity.user}"
            sTraverseBackend = { listFiles(it.pathString) }
            sMkdirsBackend = { parent, child ->
                runCatching { mkdirRecursively("$parent/$child") }.isSuccess
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
                onSet(pathString, GsonUtil().toJson(extra))
            }
        }
        disconnect()
    }
}
