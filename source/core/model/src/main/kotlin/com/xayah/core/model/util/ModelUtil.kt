package com.xayah.core.model.util

import com.xayah.core.model.CompressionType
import com.xayah.core.model.OpType
import com.xayah.core.model.SortType

fun CompressionType.Companion.of(name: String?): CompressionType =
    runCatching { CompressionType.valueOf(name!!.uppercase()) }.getOrDefault(CompressionType.ZSTD)

fun OpType.Companion.of(name: String?): OpType =
    runCatching { OpType.valueOf(name!!.uppercase()) }.getOrDefault(OpType.BACKUP)

fun SortType.Companion.of(name: String?): SortType =
    runCatching { SortType.valueOf(name!!.uppercase()) }.getOrDefault(SortType.ASCENDING)
