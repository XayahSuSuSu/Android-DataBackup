package com.xayah.databackup.data

import com.xayah.databackup.database.entity.Contact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FileRepository {
    companion object {
        private const val TAG = "FileRepository"
    }

    val isBackupFilesSelected: Flow<Boolean> = flowOf(false)

    val files: Flow<List<Contact>> = flowOf(listOf())
}
