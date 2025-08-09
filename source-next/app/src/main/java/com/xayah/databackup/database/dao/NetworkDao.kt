package com.xayah.databackup.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.xayah.databackup.database.entity.Network
import kotlinx.coroutines.flow.Flow

@Dao
interface NetworkDao {
    @Upsert(entity = Network::class)
    suspend fun upsert(networks: List<Network>)

    @Query("SELECT * from networks")
    fun loadFlowNetworks(): Flow<List<Network>>

    @Query("UPDATE networks SET selected = :selected WHERE id = :id")
    suspend fun selectNetwork(id: Int, selected: Boolean)

    @Query("UPDATE networks SET selected = :selected WHERE (id) in (:ids)")
    suspend fun selectAllNetworks(ids: List<Int>, selected: Boolean)
}
