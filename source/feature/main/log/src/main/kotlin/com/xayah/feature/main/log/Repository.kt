package com.xayah.feature.main.log

import android.content.Context
import com.xayah.core.common.util.trim
import com.xayah.core.util.FileUtil
import com.xayah.core.util.LogUtil
import com.xayah.core.util.LogUtil.LOG_FILE_Prefix
import com.xayah.core.util.PathUtil
import com.xayah.core.util.SymbolUtil
import com.xayah.core.util.logDir
import com.xayah.core.util.withIOContext
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class LogListRepository @Inject constructor(@ApplicationContext private val context: Context) {
    suspend fun listLogFiles(): List<LogCardItem> = withIOContext {
        val items = mutableListOf<LogCardItem>()
        FileUtil.listFilePaths(context.logDir()).forEach { path ->
            val name = PathUtil.getFileName(path)
            val sizeBytes = FileUtil.calculateSize(path)
            val timestamp = name.replace(LOG_FILE_Prefix, "")
            items.add(LogCardItem(name = name, sizeBytes = sizeBytes.toDouble(), timestamp = timestamp.toLongOrNull() ?: 0, path = path))
        }
        items.sortedByDescending { it.timestamp }
    }

    fun shareLog(name: String) = run {
        LogUtil.shareLog(context = context, name = name)
    }
}

class LogDetailRepository @Inject constructor(@ApplicationContext private val context: Context) {
    suspend fun getFilePath(name: String): String = withIOContext {
        "${context.logDir()}/$name"
    }

    suspend fun getContentList(path: String): List<String> = withIOContext {
        FileUtil.readText(path = path).split(SymbolUtil.LF).trim()
    }
}
