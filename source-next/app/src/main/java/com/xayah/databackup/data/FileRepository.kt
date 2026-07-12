package com.xayah.databackup.data

import com.xayah.databackup.database.entity.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class FileRepository {
    companion object {
        private const val TAG = "FileRepository"
    }

    val isBackupFilesSelected: Flow<Boolean> = flowOf(false)

    val files: Flow<List<File>> = flowOf(listOf())
    val filesSelected: Flow<List<File>> = files.map { files -> files.filter { it.selected } }
}
