package com.xayah.core.database.dao

import androidx.room.Dao
import androidx.room.Upsert
import com.xayah.core.model.database.MessageEntity

@Dao
interface MessageDao {
    @Upsert(entity = MessageEntity::class)
    suspend fun upsert(items: List<MessageEntity>)

    @Upsert(entity = MessageEntity::class)
    suspend fun upsert(item: MessageEntity)
}