package com.xayah.core.hfx

import top.weixiansen574.hybridfilexfer.core.ReadFileCall
import top.weixiansen574.hybridfilexfer.core.WriteFileCall
import top.weixiansen574.hybridfilexfer.core.bean.Directory
import top.weixiansen574.hybridfilexfer.core.bean.RemoteFile
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.LinkedBlockingDeque

class LocalReadFileCall(
    buffers: LinkedBlockingDeque<ByteBuffer>,
    files: List<RemoteFile>,
    localDir: Directory,
    remoteDir: Directory,
    operateThreadCount: Int,
) : ReadFileCall(buffers, files, localDir, remoteDir, operateThreadCount) {
    private var currentRaf: RandomAccessFile? = null
    private var currentChannel: FileChannel? = null

    override fun fileExists(path: String): Boolean = File(path).exists()

    override fun listFiles(path: String): List<RemoteFile>? {
        val list = File(path).listFiles() ?: return null
        return list.map { RemoteFile(it) }
    }

    override fun openFile(path: String): FileChannel {
        val raf = RandomAccessFile(path, "r")
        currentRaf = raf
        val ch = raf.channel
        currentChannel = ch
        return ch
    }

    override fun closeFile() {
        try {
            currentChannel?.close()
        } finally {
            currentChannel = null
            try {
                currentRaf?.close()
            } finally {
                currentRaf = null
            }
        }
    }
}

class LocalWriteFileCall(
    buffers: LinkedBlockingDeque<ByteBuffer>,
    dequeCount: Int,
) : WriteFileCall(buffers, dequeCount) {
    private var currentRaf: RandomAccessFile? = null
    private var currentChannel: FileChannel? = null

    override fun createParentDirIfNotExists(path: String) {
        val parent = File(path).parentFile
        if (parent != null && !parent.exists()) {
            parent.mkdirs()
        }
    }

    override fun tryMkdirs(path: String) {
        val dir = File(path)
        if (!dir.exists()) {
            dir.mkdirs()
        }
    }

    override fun createAndOpenFile(path: String, length: Long): FileChannel {
        val raf = RandomAccessFile(path, "rw")
        if (raf.length() != length) {
            raf.setLength(length)
        }
        currentRaf = raf
        val ch = raf.channel
        currentChannel = ch
        return ch
    }

    override fun closeFile() {
        try {
            currentChannel?.close()
        } finally {
            currentChannel = null
            try {
                currentRaf?.close()
            } finally {
                currentRaf = null
            }
        }
    }

    override fun setFileLastModified(path: String, time: Long): Boolean {
        val file = File(path)
        return if (file.exists() && time > 0) file.setLastModified(time) else true
    }
}

class ChunkReadFileCall(
    buffers: LinkedBlockingDeque<ByteBuffer>,
    private val chunkFile: File,
    private val streamId: String,
    private val chunkLength: Long,
    operateThreadCount: Int,
) : ReadFileCall(buffers, emptyList(), null, null, operateThreadCount) {
    private var currentRaf: RandomAccessFile? = null
    private var currentChannel: FileChannel? = null

    override fun produceBlocks() {
        fileIndex = 0
        val ch = openFile(chunkFile.absolutePath)
        try {
            readChannelToDeque(ch, streamId, 0L, chunkLength)
        } finally {
            closeFile()
        }
    }

    override fun fileExists(path: String): Boolean = chunkFile.exists()

    override fun listFiles(path: String): List<RemoteFile>? = null

    override fun openFile(path: String): FileChannel {
        val raf = RandomAccessFile(chunkFile, "r")
        currentRaf = raf
        val ch = raf.channel
        currentChannel = ch
        return ch
    }

    override fun closeFile() {
        try {
            currentChannel?.close()
        } finally {
            currentChannel = null
            try {
                currentRaf?.close()
            } finally {
                currentRaf = null
            }
        }
    }
}

