package com.xayah.core.network.client

import android.content.Context
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2CreateOptions
import com.hierynomus.mssmb2.SMB2Dialect
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.common.SMBRuntimeException
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.Directory
import com.hierynomus.smbj.share.DiskShare
import com.hierynomus.smbj.share.Share
import com.rapid7.client.dcerpc.mssrvs.ServerService
import com.rapid7.client.dcerpc.transport.SMBTransportFactories
import com.xayah.core.common.util.toPathString
import com.xayah.core.model.SmbAuthMode
import com.xayah.core.model.SmbVersion
import com.xayah.core.model.database.CloudEntity
import com.xayah.core.model.database.SMBExtra
import com.xayah.core.network.R
import com.xayah.core.network.io.CountingOutputStreamImpl
import com.xayah.core.network.util.getExtraEntity
import com.xayah.core.rootservice.parcelables.PathParcelable
import com.xayah.core.util.GsonUtil
import com.xayah.core.util.LogUtil
import com.xayah.core.util.PathUtil
import com.xayah.core.util.SymbolUtil
import com.xayah.core.util.toPathList
import com.xayah.core.util.withLog
import com.xayah.core.util.withMainContext
import com.xayah.libpickyou.parcelables.DirChildrenParcelable
import com.xayah.libpickyou.parcelables.FileParcelable
import com.xayah.libpickyou.ui.PickYouLauncher
import com.xayah.libpickyou.ui.model.PickerType
import java.io.File
import java.io.IOException


class SMBClientImpl(private val entity: CloudEntity, private val extra: SMBExtra) : CloudClient {
    private var client: SMBClient? = null
    private var session: Session? = null
    private var share: Share? = null
    private var shareName: String = ""
    private var availableShares = listOf<String>()

    private fun log(msg: () -> String): String = run {
        LogUtil.log { "SMBClientImpl" to msg() }
        msg()
    }

    companion object {
        // Ref: https://learn.microsoft.com/en-us/openspecs/windows_protocols/ms-srvs/6069f8c0-c93f-43a0-a5b4-7ed447eb4b84
        private const val STYPE_SPECIAL: Int = (0x80000000).toInt()
    }

    private fun withClient(block: (client: SMBClient) -> Unit) = run {
        if (client == null) throw NullPointerException("Client is null.")
        block(client!!)
    }

    private fun withSession(block: (session: Session) -> Unit) = run {
        if (session == null) throw NullPointerException("Session is null.")
        block(session!!)
    }

    private fun <R> withDiskShare(block: (diskShare: DiskShare) -> R) = run {
        if (share == null) throw NullPointerException("Share is null.")
        block(share!! as DiskShare)
    }

    private fun setShare(share: String?) = withSession { session ->
        if (share == null) {
            this.share = null
            this.shareName = ""
        } else {
            this.share = session.connectShare(share)
            this.shareName = share
        }
    }

    private fun SmbVersion.toDialect() = when (this) {
        SmbVersion.SMB_2_0_2 -> SMB2Dialect.SMB_2_0_2
        SmbVersion.SMB_2_1 -> SMB2Dialect.SMB_2_1
        SmbVersion.SMB_3_0 -> SMB2Dialect.SMB_3_0
        SmbVersion.SMB_3_0_2 -> SMB2Dialect.SMB_3_0_2
        SmbVersion.SMB_3_1_1 -> SMB2Dialect.SMB_3_1_1
    }

    override fun connect() {
        val dialects = extra.version.map { it.toDialect() }
        val config = SmbConfig.builder()
            .withDialects(dialects)
            .build()
        client = SMBClient(config).apply {
            connect(entity.host, extra.port).also { connection ->
                log { "Dialect: ${connection.connectionContext.negotiatedProtocol.dialect.name}" }
                log { "Mode: ${extra.mode}" }

                val authentication = when (extra.mode) {
                    SmbAuthMode.PASSWORD -> AuthenticationContext(entity.user, entity.pass.toCharArray(), extra.domain)
                    SmbAuthMode.GUEST -> AuthenticationContext.guest()
                    SmbAuthMode.ANONYMOUS -> AuthenticationContext.anonymous()
                }

                session = connection.authenticate(authentication)
                if (extra.share.isNotEmpty()) setShare(extra.share)
                withSession { _ ->
                    val transport = SMBTransportFactories.SRVSVC.getTransport(session)
                    val serverService = ServerService(transport)
                    val shares = serverService.shares1
                    availableShares = shares.filter { (it.type and STYPE_SPECIAL == STYPE_SPECIAL).not() }.map { it.netName }
                }
            }
        }
    }

    override fun disconnect() {
        runCatching {
            withDiskShare { diskShare ->
                diskShare.close()
            }
            withClient { client ->
                client.close()
            }
        }.withLog()
        share = null
        client = null
    }

    override fun exists(src: String): Boolean {
        var exists = false
        withDiskShare { diskShare ->
            if (diskShare.fileExists(src)) {
                exists = true
                return@withDiskShare
            }
            if (diskShare.folderExists(src)) {
                exists = true
                return@withDiskShare
            }
        }
        return exists
    }

    override fun mkdir(dst: String) = withDiskShare { diskShare ->
        log { "mkdir: $dst" }
        if (exists(dst).not()) diskShare.mkdir(dst)
    }

    override fun mkdirRecursively(dst: String) {
        val dirs = dst.split("/")
        var currentDir = ""
        for (i in dirs) {
            currentDir += "/$i"
            currentDir = currentDir.trimStart('/')
            if (exists(currentDir).not()) mkdir(currentDir)
        }
    }

    private fun openDirectory(src: String): Directory = withDiskShare { diskShare ->
        diskShare.openDirectory(
            src,
            setOf(AccessMask.GENERIC_ALL),
            null,
            SMB2ShareAccess.ALL,
            SMB2CreateDisposition.FILE_OPEN,
            null
        )
    }

    private fun openFile(src: String): com.hierynomus.smbj.share.File = withDiskShare { diskShare ->
        diskShare.openFile(
            src,
            setOf(AccessMask.GENERIC_ALL),
            setOf(FileAttributes.FILE_ATTRIBUTE_NORMAL),
            SMB2ShareAccess.ALL,
            SMB2CreateDisposition.FILE_OPEN_IF,
            setOf(SMB2CreateOptions.FILE_RANDOM_ACCESS)
        )
    }

    override fun renameTo(src: String, dst: String): Unit = withDiskShare { diskShare ->
        log { "renameTo: from $src to $dst" }
        if (diskShare.folderExists(src)) {
            val dir = openDirectory(src)
            dir.rename(dst.replace("/", SymbolUtil.BACKSLASH.toString()), false)
        } else if (diskShare.fileExists(src)) {
            val file = openFile(src)
            file.rename(dst, false)
        }
    }

    override fun upload(src: String, dst: String, onUploading: (read: Long, total: Long) -> Unit) = run {
        val name = PathUtil.getFileName(src)
        val dstPath = "$dst/$name"
        log { "upload: $src to $dstPath" }
        val dstFile = openFile(dstPath)
        val dstStream = dstFile.outputStream
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

    override fun download(src: String, dst: String, onDownloading: (written: Long, total: Long) -> Unit) = run {
        val name = PathUtil.getFileName(src)
        val dstPath = "$dst/$name"
        log { "download: $src to $dstPath" }
        val dstOutputStream = File(dstPath).outputStream()
        val srcFile = openFile(src)
        val countingStream = CountingOutputStreamImpl(dstOutputStream, -1) { written, total -> onDownloading(written, total) }
        srcFile.read(countingStream)
        srcFile.close()
        dstOutputStream.close()
        countingStream.close()
        onDownloading(countingStream.byteCount, countingStream.byteCount)
    }

    override fun deleteFile(src: String) = withDiskShare { diskShare ->
        log { "deleteFile: $src" }
        diskShare.rm(src)
    }

    override fun removeDirectory(src: String) = withDiskShare { diskShare ->
        log { "removeDirectory: $src" }
        diskShare.rmdir(src, true)
    }

    private fun clearEmptyDirectoriesRecursivelyInternal(src: String): Boolean {
        var isEmpty = true

        withDiskShare { diskShare ->
            if (diskShare.folderExists(src)) {
                val files = listFiles("/${shareName}/$src")
                if (files.files.isNotEmpty()) {
                    isEmpty = false
                }
                for (i in files.directories) {
                    if (clearEmptyDirectoriesRecursivelyInternal("${src}/${i.name}").not()) {
                        isEmpty = false
                    }
                }
                if (isEmpty) {
                    removeDirectory(src)
                }
            }
        }
        return isEmpty
    }


    override fun clearEmptyDirectoriesRecursively(src: String) {
        clearEmptyDirectoriesRecursivelyInternal(src)
    }

    override fun deleteRecursively(src: String) = withDiskShare { diskShare ->
        if (diskShare.fileExists(src)) deleteFile(src)
        else if (diskShare.folderExists(src)) removeDirectory(src)
        else throw IOException("$src not found.")
    }

    /**
     * @param src "$shareName/path"
     */
    override fun listFiles(src: String): DirChildrenParcelable {
        if (src.isEmpty()) {
            setShare(null)
        } else {
            val sharePrefix = src.split("/").getOrNull(1) ?: ""
            withSession {
                setShare(sharePrefix)
            }
        }

        val files = mutableListOf<FileParcelable>()
        val directories = mutableListOf<FileParcelable>()
        if (share != null) {
            withDiskShare { diskShare ->
                // Remove share name
                val srcPath = src.replaceFirst("/${shareName}", "")
                val clientFiles = diskShare.list(srcPath)
                for (file in clientFiles) {
                    if (file.fileName == "." || file.fileName == "..") continue
                    val path = if (srcPath.isEmpty()) file.fileName else "${srcPath.trimEnd('/')}/${file.fileName}"
                    val creationTime = file.creationTime.toEpochMillis()
                    val fileParcelable = FileParcelable(file.fileName, creationTime)
                    if (diskShare.folderExists(path)) directories.add(fileParcelable)
                    else files.add(fileParcelable)
                }
            }
        } else if (availableShares.isEmpty()) {
            throw SMBRuntimeException("Share is null and there are no other available shares.")
        } else {
            directories.addAll(availableShares.map { FileParcelable(it, 0L) })
        }
        files.sortBy { it.name }
        directories.sortBy { it.name }
        return DirChildrenParcelable(files = files, directories = directories)
    }

    override fun walkFileTree(src: String): List<PathParcelable> {
        val pathParcelableList = mutableListOf<PathParcelable>()
        withDiskShare { diskShare ->
            if (diskShare.folderExists(src)) {
                val files = listFiles("/${shareName}/$src")
                for (i in files.files) {
                    pathParcelableList.add(PathParcelable("${src}/${i.name}"))
                }
                for (i in files.directories) {
                    pathParcelableList.addAll(walkFileTree("${src}/${i.name}"))
                }
            } else if (diskShare.fileExists(src)) {
                pathParcelableList.add(PathParcelable(src))
            }
        }
        return pathParcelableList
    }

    /**
     * @param src "path" without "$shareName"
     */
    override fun size(src: String): Long {
        var size = 0L
        withDiskShare { diskShare ->
            if (diskShare.folderExists(src)) {
                val files = listFiles("/${shareName}/$src")
                for (i in files.files) {
                    size += diskShare.getFileInformation("${src}/${i.name}").standardInformation.endOfFile
                }
                for (i in files.directories) {
                    size("${src}/${i.name}")
                }
            } else if (diskShare.fileExists(src)) {
                size += diskShare.getFileInformation(src).standardInformation.endOfFile
            }
        }
        log { "size: $size, $src" }
        return size
    }

    override suspend fun testConnection() {
        connect()
        disconnect()
    }

    private fun handleOriginalPath(path: String): Pair<String, String> = run {
        val pathSplit = path.toPathList().toMutableList()
        // Remove “$Cloud:/$share”
        pathSplit.removeFirst()
        val share = pathSplit.removeFirst()
        val target = pathSplit.toPathString()
        share to target
    }

    override suspend fun setRemote(context: Context, onSet: suspend (remote: String, extra: String) -> Unit) {
        val extra = entity.getExtraEntity<SMBExtra>()!!
        connect()
        PickYouLauncher.apply {
            val prefix = "${context.getString(R.string.cloud)}:"
            sTraverseBackend = { listFiles(it.replaceFirst(prefix, "")) }
            sMkdirsBackend = { parent, child ->
                val (_, target) = handleOriginalPath("$parent/$child")
                runCatching { mkdirRecursively(target) }.isSuccess
            }
            sTitle = context.getString(R.string.select_target_directory)
            sPickerType = PickerType.DIRECTORY
            sLimitation = 1
            sRootPathList = listOf(prefix)
            sDefaultPathList = if (extra.share.isNotEmpty()) listOf(prefix, extra.share) else listOf(prefix)

        }
        withMainContext {
            val pathList = PickYouLauncher.awaitPickerOnce(context)
            pathList.firstOrNull()?.also { pathString ->
                val (share, remote) = handleOriginalPath(pathString)
                onSet(remote, GsonUtil().toJson(extra.copy(share = share)))
            }
        }
        disconnect()
    }
}
