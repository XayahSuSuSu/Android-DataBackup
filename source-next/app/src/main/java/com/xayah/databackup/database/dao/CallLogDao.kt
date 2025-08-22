package com.xayah.databackup.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.xayah.databackup.database.entity.CallLog
import kotlinx.coroutines.flow.Flow

@Dao
interface CallLogDao {
    @Upsert(entity = CallLog::class)
    suspend fun upsert(callLogs: List<CallLog>)

    @Query("SELECT * from call_logs")
    fun loadFlowCallLogs(): Flow<List<CallLog>>

    @Query("UPDATE call_logs SET selected = :selected WHERE id = :id")
    suspend fun selectCallLog(id: Long, selected: Boolean)

    @Query("UPDATE call_logs SET selected = :selected WHERE (id) in (:ids)")
    suspend fun selectAllCallLogs(ids: List<Long>, selected: Boolean)
}
