package com.xayah.databackup.service.backup

import android.provider.Telephony
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.xayah.databackup.database.entity.FieldMap
import com.xayah.databackup.database.entity.Mms
import com.xayah.databackup.util.LogHelper
import kotlin.coroutines.cancellation.CancellationException

data class BackupMmsSources(val paths: List<String>, val failedRecordIds: List<Long>)

class BackupMmsSourceHelper {
    companion object {
        private const val TAG = "BackupMmsSourceHelper"
    }

    private val mAdapter = Moshi.Builder().build().adapter<List<FieldMap>>()

    fun getSources(messages: List<Mms>): BackupMmsSources {
        val paths = linkedSetOf<String>()
        val failedRecordIds = mutableListOf<Long>()
        for ((id, _, _, part) in messages) {
            runCatching {
                val parts = part?.let { mAdapter.fromJson(it) }.orEmpty()
                parts.mapNotNullTo(paths) { it[Telephony.Mms.Part._DATA]?.toString()?.takeIf(String::isNotBlank) }
            }.onFailure { error ->
                if (error is CancellationException || error !is Exception) {
                    throw error
                }
                failedRecordIds += id
                // Parser exceptions can contain message content. Never log their message or stack here.
                LogHelper.w(TAG, "getSources", "Cannot parse MMS attachment metadata: id=$id, error=${error.javaClass.simpleName}")
            }
        }
        return BackupMmsSources(paths.toList(), failedRecordIds)
    }
}
