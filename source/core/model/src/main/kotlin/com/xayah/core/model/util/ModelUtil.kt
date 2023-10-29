package com.xayah.core.model.util

import com.xayah.core.model.CompressionType
import com.xayah.core.model.OpType

fun CompressionType.Companion.of(name: String?): CompressionType =
    runCatching { CompressionType.valueOf(name!!.uppercase()) }.getOrDefault(CompressionType.ZSTD)

fun OpType.Companion.of(name: String?): OpType =
    runCatching { OpType.valueOf(name!!.uppercase()) }.getOrDefault(OpType.BACKUP)
