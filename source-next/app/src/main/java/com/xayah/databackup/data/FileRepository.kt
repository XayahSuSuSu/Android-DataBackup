package com.xayah.databackup.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class FileRepository {
    companion object {
        private const val TAG = "FileRepository"
    }

    val isBackupFilesSelected: Flow<Boolean> = flowOf(false)

    val files: Flow<List<Any>> = flowOf(listOf())
    val filesSelected: Flow<List<Any>> = files.map { files -> files.filter { false } }
}
