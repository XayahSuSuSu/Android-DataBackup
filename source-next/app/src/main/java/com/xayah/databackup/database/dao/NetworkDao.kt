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
}
