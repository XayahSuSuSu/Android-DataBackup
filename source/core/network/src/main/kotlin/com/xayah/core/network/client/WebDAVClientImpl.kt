package com.xayah.core.network.client

import android.content.Context
import com.xayah.core.common.util.toPathString
import com.xayah.core.model.database.CloudEntity
import com.xayah.core.network.R
import com.xayah.core.rootservice.parcelables.PathParcelable
import com.xayah.core.util.LogUtil
import com.xayah.core.util.PathUtil
import com.xayah.core.util.toPathList
import com.xayah.core.util.withMainContext
import com.xayah.libpickyou.parcelables.DirChildrenParcelable
import com.xayah.libpickyou.parcelables.FileParcelable
import com.xayah.libpickyou.ui.PickYouLauncher
import com.xayah.libpickyou.ui.model.PickerType
import com.xayah.libsardine.DavResource
import com.xayah.libsardine.impl.OkHttpSardine
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit


class WebDAVClientImpl(private val entity: CloudEntity) : CloudClient {
    private var client: OkHttpSardine? = null

    private fun log(msg: () -> String): String = run {
        LogUtil.log { "WebDAVClientImpl" to msg() }
        msg()
    }

    private fun getPath(path: String) = "${entity.host.trimEnd('/')}/${path.trimStart('/')}"

    private fun withClient(block: (client: OkHttpSardine) -> Unit) = run {
        if (client == null) throw NullPointerException("Client is null.")
        block(client!!)
    }

    override fun connect() {
        client = OkHttpSardine(
            OkHttpClient.Builder()
                .connectTimeout(0, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS)
                .writeTimeout(0, TimeUnit.SECONDS)
                .build()
        ).apply {
            setCredentials(entity.user, entity.pass)
            list(entity.host)
        }
    }

    override fun disconnect() {
        client = null
    }

    override fun mkdir(dst: String) = withClient { client ->
        log { "mkdir: ${getPath(dst)}" }
        client.createDirectory(getPath(dst))
    }

    override fun mkdirRecursively(dst: String) = withClient { client ->
        val dirs = dst.split("/")
        var currentDir = ""
        for (i in dirs) {
            currentDir += "/$i"
            currentDir = currentDir.trimStart('/')
            if (client.exists(getPath(currentDir)).not()) mkdir(currentDir)
        }
    }

    override fun renameTo(src: String, dst: String) = withClient { client ->
        log { "renameTo: from ${getPath(src)} to ${getPath(dst)}" }
        client.move(getPath(src), getPath(dst), false)
    }

    override fun upload(src: String, dst: String, onUploading: (read: Long, total: Long) -> Unit) = withClient { client ->
        val name = PathUtil.getFileName(src)
        val dstPath = "${getPath(dst)}/$name"
        log { "upload: $src to $dstPath" }
        val srcFile = File(src)
        client.put(dstPath, srcFile, null)
    }

    override fun download(src: String, dst: String, onDownloading: (written: Long, total: Long) -> Unit) = withClient { client ->
        val name = PathUtil.getFileName(src)
        val dstPath = "${dst}/$name"
        log { "download: ${getPath(src)} to $dstPath" }
        val dstOutputStream = File(dstPath).outputStream()
        val srcInputStream = client.get(getPath(src))
        srcInputStream.copyTo(dstOutputStream)
        srcInputStream.close()
        dstOutputStream.close()
    }

    override fun deleteFile(src: String) = withClient { client ->
        log { "deleteFile: ${getPath(src)}" }
        client.delete(getPath(src))
    }

    override fun removeDirectory(src: String) = withClient { client ->
        log { "removeDirectory: ${getPath(src)}" }
        client.delete(getPath(src))
    }

    private fun clearEmptyDirectoriesRecursivelyInternal(src: String): Boolean {
        var isEmpty = true
        withClient { client ->
            val resources: List<DavResource> = client.list(src)

            for (res in resources) {
                if (!res.isDirectory) {
                    isEmpty = false
                } else {
                    if (clearEmptyDirectoriesRecursivelyInternal(res.path).not()) {
                        isEmpty = false
                    }
                }
            }

            if (isEmpty) {
                client.delete(src)
            }

        }
        return isEmpty
    }


    override fun clearEmptyDirectoriesRecursively(src: String) {
        clearEmptyDirectoriesRecursivelyInternal(getPath(src))
    }

    override fun deleteRecursively(src: String) = removeDirectory(src)

    override fun listFiles(src: String): DirChildrenParcelable {
        val files = mutableListOf<FileParcelable>()
        val directories = mutableListOf<FileParcelable>()
        withClient { client ->
            val resources = client.list(getPath(src))
            for ((index, resource) in resources.withIndex()) {
                if (index == 0) continue
                val creationTime = runCatching { resource.creation.time }.getOrDefault(0)
                val fileParcelable = FileParcelable(resource.name, creationTime)
                if (resource.isDirectory) directories.add(fileParcelable)
                else files.add(fileParcelable)
            }
        }
        files.sortBy { it.name }
        directories.sortBy { it.name }
        return DirChildrenParcelable(files = files, directories = directories)
    }

    private fun walkFileTreeRecursively(src: String): List<PathParcelable> {
        val pathParcelableList = mutableListOf<PathParcelable>()
        val files = listFiles(src)
        for (i in files.files) {
            pathParcelableList.add(PathParcelable("${src}/${i.name}"))
        }
        for (i in files.directories) {
            pathParcelableList.addAll(walkFileTreeRecursively("${src}/${i.name}"))
        }
        return pathParcelableList
    }

    override fun walkFileTree(src: String): List<PathParcelable> {
        val pathParcelableList = mutableListOf<PathParcelable>()
        withClient { client ->
            val srcFile = client.list(getPath(src))[0]
            if (srcFile.isDirectory) {
                pathParcelableList.addAll(walkFileTreeRecursively(src))
            } else {
                pathParcelableList.add(PathParcelable(src))
            }
        }
        return pathParcelableList
    }

    override fun exists(src: String): Boolean = runCatching { withClient { client -> client.list(getPath(src)) } }.isSuccess

    private fun sizeRecursively(src: String): Long {
        var size = 0L
        withClient { client ->
            val files = listFiles(src)
            for (i in files.files) {
                size += client.list(getPath("${src}/${i.name}"))[0].contentLength
            }
            for (i in files.directories) {
                size("${src}/${i.name}")
            }
        }
        return size
    }

    override fun size(src: String): Long {
        var size = 0L
        withClient { client ->
            val srcFile = client.list(getPath(src))[0]
            size += if (srcFile.isDirectory) {
                sizeRecursively(src)
            } else {
                srcFile.contentLength
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
        connect()
        PickYouLauncher.apply {
            val prefix = "${context.getString(R.string.cloud)}:"
            sTraverseBackend = { listFiles(it.replaceFirst(prefix, "")) }
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
                onSet(handleOriginalPath(pathString), "")
            }
        }
        disconnect()
    }
}