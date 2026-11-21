package com.xayah.core.network.client

import java.io.InputStream

interface StreamingCloudClient : CloudClient {
    val maxBufferBytes: Long
    val chunkSizeBytes: Long
    val continuousStream: Boolean
    val inlineCrc32: Boolean
    fun isTransportConnected(): Boolean
    fun reconnectTransport()
    fun streamBegin(streamId: String, targetVirtualPath: String, lastModified: Long)
    fun streamResume(streamId: String, targetVirtualPath: String, lastModified: Long): Long
    fun streamAppendChunk(
        streamId: String,
        chunkFilePath: String,
        chunkOffset: Long,
        chunkCrc32: Long,
        onProgress: (sent: Long, total: Long) -> Unit,
        onSpeedBytesPerSec: (Long) -> Unit = {},
    )
    fun streamCommit(streamId: String, expectedTotalSize: Long)
    fun streamAbort(streamId: String)
    fun streamContinuous(
        inputStream: InputStream,
        targetVirtualPath: String,
        lastModified: Long,
        validatePackResult: () -> Pair<Boolean, String>,
        onProgress: (uploadedBytes: Long, speedBytesPerSec: Long) -> Unit = { _, _ -> },
    ): Long
}
