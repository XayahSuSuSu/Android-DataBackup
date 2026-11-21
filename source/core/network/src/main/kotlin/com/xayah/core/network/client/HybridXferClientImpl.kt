package com.xayah.core.network.client

import android.content.Context
import com.xayah.core.common.util.toPathString
import com.xayah.core.hfx.BufferAllocator
import com.xayah.core.hfx.HFXServerEngine
import com.xayah.core.hfx.JvmDirectBufferAllocator
import com.xayah.core.hfx.NetInterfaceDiscoverer
import com.xayah.core.model.database.CloudEntity
import com.xayah.core.model.database.HybridXferExtra
import com.xayah.core.network.R
import com.xayah.core.network.util.getExtraEntity
import com.xayah.core.rootservice.parcelables.PathParcelable
import com.xayah.core.util.GsonUtil
import com.xayah.core.util.LogUtil
import com.xayah.core.util.toPathList
import com.xayah.core.util.withMainContext
import com.xayah.libnative.NativeLib
import com.xayah.libpickyou.PickYouLauncher
import com.xayah.libpickyou.parcelables.DirChildrenParcelable
import com.xayah.libpickyou.parcelables.FileParcelable
import com.xayah.libpickyou.ui.model.PickerType
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer

private object NativeBufferAllocator : BufferAllocator {
    override fun allocate(size: Int): ByteBuffer? {
        return if (NativeLib.ensureLoaded()) {
            NativeLib.allocateLargeBuffer(size) ?: JvmDirectBufferAllocator.allocate(size)
        } else {
            JvmDirectBufferAllocator.allocate(size)
        }
    }

    override fun free(buffer: ByteBuffer) {
        if (NativeLib.ensureLoaded()) {
            NativeLib.freeBuffer(buffer)
        }
    }
}

class HybridXferClientImpl(
    private val entity: CloudEntity,
    private val extra: HybridXferExtra,
) : StreamingCloudClient {

    private var engine: HFXServerEngine? = null

    private fun log(msg: () -> String): String = run {
        LogUtil.log { "HybridXferClientImpl" to msg() }
        msg()
    }

    private fun requireEngine(): HFXServerEngine {
        return engine ?: throw IOException("HybridXfer client is not connected.")
    }

    private fun normalizeVirtualPath(path: String): String {
        val cleaned = path.replace('\\', '/').trim()
        if (cleaned.isEmpty() || cleaned == "/") return "/"
        val noLeading = cleaned.trimStart('/')
        return "/$noLeading"
    }

    override val maxBufferBytes: Long
        get() = extra.maxBufferMb.coerceIn(64, 16384).toLong() * 1024L * 1024L

    override val chunkSizeBytes: Long
        get() = extra.chunkSizeMb.coerceIn(16, 256).toLong() * 1024L * 1024L

    override val continuousStream: Boolean
        get() = extra.continuousStream

    override val inlineCrc32: Boolean
        get() = extra.inlineCrc32

    override fun isTransportConnected(): Boolean = engine?.isConnected() == true

    override fun reconnectTransport() {
        log { "Reconnecting HybridXfer transport..." }
        runCatching { engine?.forceClose() }
        engine = null
        connect()
    }

    override fun connect() {
        val usbCount = extra.usbParallelChannels.coerceIn(1, 24)
        val localBuf = extra.localBufferCount.coerceIn(32, 1024)
        val remoteBuf = extra.remoteBufferCount.coerceIn(128, 1024)
        log {
            "Starting HybridXfer server on port ${extra.port}, interfaces=${extra.enabledInterfaces}, " +
                "usbParallel=$usbCount, continuous=${extra.continuousStream}, sockBuf=${extra.socketBufferMb}MB, " +
                "ramBuf=${localBuf}MB, maxDiskBuf=${extra.maxBufferMb}MB"
        }
        val resolvedInterfaces = NetInterfaceDiscoverer.resolveEnabled(
            enabledNames = extra.enabledInterfaces,
            usbParallelChannels = usbCount,
        )
        val newEngine = HFXServerEngine(NativeBufferAllocator)
        newEngine.startAndWaitClient(
            port = extra.port,
            interfaces = resolvedInterfaces,
            localBufferCount = localBuf,
            remoteBufferCount = remoteBuf,
            timeoutMs = extra.connectTimeoutSec.coerceAtLeast(5).toLong() * 1000L,
            socketBufferMb = extra.socketBufferMb.coerceIn(1, 16),
        )
        engine = newEngine
    }

    override fun disconnect() {
        log { "Detaching HybridXfer session." }
        runCatching { engine?.detach() }
        engine = null
    }

    override fun mkdir(dst: String) {
        val vPath = normalizeVirtualPath(dst)
        log { "mkdir: $vPath" }
        if (!requireEngine().mkdir(vPath, "")) {
            throw IOException("Failed to mkdir on PC: $vPath")
        }
    }

    override fun mkdirRecursively(dst: String) {
        mkdir(dst)
    }

    override fun renameTo(src: String, dst: String) {
        val vSrc = normalizeVirtualPath(src)
        val vDst = normalizeVirtualPath(dst)
        log { "renameTo: $vSrc -> $vDst" }
        if (!requireEngine().rename(vSrc, vDst)) {
            throw IOException("Failed to rename on PC: $vSrc -> $vDst")
        }
    }

    override fun upload(src: String, dst: String, onUploading: (read: Long, total: Long) -> Unit) {
        val srcFile = File(src)
        val vDstDir = normalizeVirtualPath(dst)
        log { "upload: $src -> $vDstDir" }
        requireEngine().uploadFile(
            localFile = srcFile,
            remoteVirtualDir = vDstDir,
            onProgress = onUploading,
        )
    }

    override fun download(src: String, dst: String, onDownloading: (written: Long, total: Long) -> Unit) {
        val vSrc = normalizeVirtualPath(src)
        val dstDir = File(dst)
        log { "download: $vSrc -> $dst" }
        requireEngine().downloadFile(
            remoteVirtualFile = vSrc,
            localDstDir = dstDir,
            onProgress = onDownloading,
        )
    }

    override fun deleteFile(src: String) {
        val vSrc = normalizeVirtualPath(src)
        log { "deleteFile: $vSrc" }
        requireEngine().delete(vSrc)
    }

    override fun removeDirectory(src: String) {
        deleteFile(src)
    }

    override fun clearEmptyDirectoriesRecursively(src: String) {
        // Optional cleanup
    }

    override fun deleteRecursively(src: String) {
        deleteFile(src)
    }

    override fun listFiles(src: String): DirChildrenParcelable {
        val vSrc = normalizeVirtualPath(src)
        val files = mutableListOf<FileParcelable>()
        val directories = mutableListOf<FileParcelable>()
        val list = requireEngine().listFiles(vSrc)
        for (item in list) {
            val fp = FileParcelable(item.name, item.lastModified())
            if (item.isDirectory) {
                directories.add(fp)
            } else {
                files.add(fp)
            }
        }
        files.sortBy { it.name }
        directories.sortBy { it.name }
        return DirChildrenParcelable(files = files, directories = directories)
    }

    override fun walkFileTree(src: String): List<PathParcelable> {
        val vSrc = normalizeVirtualPath(src)
        val result = mutableListOf<PathParcelable>()
        val stat = requireEngine().stat(vSrc)
        if (!stat.exists) return result
        if (!stat.isDirectory) {
            result.add(PathParcelable(vSrc))
            return result
        }
        val children = listFiles(vSrc)
        val base = if (vSrc == "/") "" else vSrc
        for (f in children.files) {
            result.add(PathParcelable("$base/${f.name}"))
        }
        for (d in children.directories) {
            result.addAll(walkFileTree("$base/${d.name}"))
        }
        return result
    }

    override fun exists(src: String): Boolean {
        val vSrc = normalizeVirtualPath(src)
        return requireEngine().stat(vSrc).exists
    }

    override fun size(src: String): Long {
        val vSrc = normalizeVirtualPath(src)
        val stat = requireEngine().stat(vSrc)
        if (!stat.exists) return 0L
        if (!stat.isDirectory) return stat.size
        var total = 0L
        val children = listFiles(vSrc)
        val base = if (vSrc == "/") "" else vSrc
        for (f in children.files) {
            total += requireEngine().stat("$base/${f.name}").size
        }
        for (d in children.directories) {
            total += size("$base/${d.name}")
        }
        return total
    }

    override suspend fun testConnection() {
        connect()
        try {
            exists("/")
        } finally {
            disconnect()
        }
    }

    private fun handleOriginalPath(path: String): String {
        val pathSplit = path.toPathList().toMutableList()
        pathSplit.removeFirstOrNull()
        val res = pathSplit.toPathString()
        return if (res.isEmpty()) "/" else res
    }

    override suspend fun setRemote(context: Context, onSet: suspend (remote: String, extra: String) -> Unit) {
        val currentExtra = entity.getExtraEntity<HybridXferExtra>() ?: extra
        connect()
        try {
            val prefix = "${context.getString(R.string.cloud)}:"
            val pickYou = PickYouLauncher(
                checkPermission = false,
                traverseBackend = { listFiles(it.replaceFirst(prefix, "").ifEmpty { "/" }) },
                mkdirsBackend = { parent, child ->
                    runCatching { mkdirRecursively(handleOriginalPath("$parent/$child")) }.isSuccess
                },
                title = context.getString(R.string.select_target_directory),
                pickerType = PickerType.DIRECTORY,
                rootPathList = listOf(prefix),
                defaultPathList = listOf(prefix),
            )
            withMainContext {
                val pathString = pickYou.awaitLaunch(context)
                onSet(handleOriginalPath(pathString), GsonUtil().toJson(currentExtra))
            }
        } finally {
            disconnect()
        }
    }

    override fun streamBegin(streamId: String, targetVirtualPath: String, lastModified: Long) {
        requireEngine().streamBegin(streamId, normalizeVirtualPath(targetVirtualPath), lastModified)
    }

    override fun streamResume(streamId: String, targetVirtualPath: String, lastModified: Long): Long {
        return requireEngine().streamResume(streamId, normalizeVirtualPath(targetVirtualPath), lastModified)
    }

    override fun streamAppendChunk(
        streamId: String,
        chunkFilePath: String,
        chunkOffset: Long,
        chunkCrc32: Long,
        onProgress: (sent: Long, total: Long) -> Unit,
        onSpeedBytesPerSec: (Long) -> Unit,
    ) {
        requireEngine().streamAppendChunk(
            streamId = streamId,
            chunkFile = File(chunkFilePath),
            chunkOffset = chunkOffset,
            chunkCrc32 = chunkCrc32,
            onProgress = onProgress,
            onSpeed = { list ->
                val sum = list.sumOf { it.uploadTraffic }
                onSpeedBytesPerSec(sum)
            },
        )
    }

    override fun streamCommit(streamId: String, expectedTotalSize: Long) {
        requireEngine().streamCommit(streamId, expectedTotalSize)
    }

    override fun streamAbort(streamId: String) {
        engine?.streamAbort(streamId)
    }

    override fun streamContinuous(
        inputStream: InputStream,
        targetVirtualPath: String,
        lastModified: Long,
        validatePackResult: () -> Pair<Boolean, String>,
        onProgress: (uploadedBytes: Long, speedBytesPerSec: Long) -> Unit,
    ): Long {
        return requireEngine().streamContinuousFromInputStream(
            inputStream = inputStream,
            targetVirtualPath = normalizeVirtualPath(targetVirtualPath),
            lastModified = lastModified,
            verifyInlineCrc32 = extra.inlineCrc32,
            validatePackResult = validatePackResult,
            onProgress = onProgress,
        )
    }
}

