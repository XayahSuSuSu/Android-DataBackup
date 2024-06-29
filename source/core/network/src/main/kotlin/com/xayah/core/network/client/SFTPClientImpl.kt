package com.xayah.core.network.client

import android.content.Context
import com.xayah.core.model.database.CloudEntity
import com.xayah.core.model.database.FTPExtra
import com.xayah.core.model.database.SFTPExtra
import com.xayah.core.network.R
import com.xayah.core.network.io.CountingOutputStreamImpl
import com.xayah.core.network.util.getExtraEntity
import com.xayah.core.rootservice.parcelables.PathParcelable
import com.xayah.core.util.GsonUtil
import com.xayah.core.util.LogUtil
import com.xayah.core.util.withMainContext
import com.xayah.libpickyou.parcelables.DirChildrenParcelable
import com.xayah.libpickyou.parcelables.FileParcelable
import com.xayah.libpickyou.ui.PickYouLauncher
import com.xayah.libpickyou.ui.model.PickerType
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.connection.channel.direct.Session
import net.schmizz.sshj.sftp.FileMode
import net.schmizz.sshj.sftp.OpenMode
import net.schmizz.sshj.sftp.RemoteFile
import net.schmizz.sshj.sftp.RemoteResourceInfo
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.userauth.password.PasswordFinder
import net.schmizz.sshj.userauth.password.Resource
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Paths
import kotlin.io.path.pathString


class SFTPClientImpl(private val entity: CloudEntity, private val extra: SFTPExtra) : CloudClient {
    private var sshClient: SSHClient? = null
    private var sshSession: Session? = null
    private var sftpClient: SFTPClient? = null

    private fun log(msg: () -> String): String = run {
        LogUtil.log { "SFTPClientImpl" to msg() }
        msg()
    }

    private fun <T> withSSHClient(block: (client: SSHClient) -> T) = run {
        if (sshClient == null) throw NullPointerException("Client is null.")
        block(sshClient!!)
    }

    private fun <T> withSSHSession(block: (client: Session) -> T) = run {
        if (sshSession == null) throw NullPointerException("Client is null.")
        block(sshSession!!)
    }

    private fun <T> withSFTPClient(block: (client: SFTPClient) -> T) = run {
        if (sftpClient == null) throw NullPointerException("Client is null.")
        block(sftpClient!!)
    }

    private fun pwd(): String = ByteArrayOutputStream().also { stream ->
        withSSHSession { session -> session.exec("pwd").inputStream.copyTo(stream) }
        stream.close()
    }.toString().replace("\n", "").trim()

    override fun connect() {
        sshClient = SSHClient().apply {
            addHostKeyVerifier(PromiscuousVerifier())
            connect(entity.host, extra.port)

            if (extra.authMode == 1) {
                val passwordFinder = object : PasswordFinder {
                    var retryCount = 0

                    override fun reqPassword(p0: Resource<*>?): CharArray {
                        return entity.pass.toCharArray()
                    }

                    override fun shouldRetry(p0: Resource<*>?): Boolean {
                        return ++retryCount < 3
                    }
                }

                log { "Authenticating with public key..." }
                val keyLoader = loadKeys(extra.privateKey, null, passwordFinder)
                log { "Loaded keys!" }
                authPublickey(entity.user, keyLoader)
                log { "Authenticated with public key!" }
            } else {
                authPassword(entity.user, entity.pass)
            }

            sftpClient = newSFTPClient()
            sshSession = startSession()
        }
    }

    override fun disconnect() {
        withSSHClient { it.disconnect() }
        sshClient = null
        sftpClient = null
        sshSession = null
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
        log { "list files: $src" }

        val files = ArrayList<FileParcelable>()
        val directories = ArrayList<FileParcelable>()

        val dirInfo = try {
            withSFTPClient { it.ls(src) }
        } catch (e: Throwable) {
            log { e.toString() }
            ArrayList<RemoteResourceInfo>()
        }

        for (item in dirInfo) {
            log { "Loaded ${item.path}" }

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
            val currentDir = pwd()
            sTraverseBackend = { listFiles(it.pathString) }
            sMkdirsBackend = { parent, child ->
                runCatching { mkdirRecursively("$parent/$child") }.isSuccess
            }
            sTitle = context.getString(R.string.select_target_directory)
            sPickerType = PickerType.DIRECTORY
            sLimitation = 1
            sRootPathList = listOf(currentDir)
            sDefaultPathList = listOf(currentDir)

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
