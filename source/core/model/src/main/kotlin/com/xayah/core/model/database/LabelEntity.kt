package com.xayah.core.model.database

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(indices = [Index(value = ["label"], unique = true)])
data class LabelEntity(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    var label: String,
)

@Entity(
    indices = [Index(value = ["labelId", "appId"], unique = true)],
    primaryKeys = ["labelId", "appId"],
    foreignKeys = [
        ForeignKey(
            entity = LabelEntity::class,
            parentColumns = ["id"],
            childColumns = ["labelId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PackageEntity::class,
            parentColumns = ["id"],
            childColumns = ["appId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class LabelAppCrossRefEntity(
    var labelId: Long,
    var appId: Long,
)

@Entity(
    indices = [Index(value = ["labelId", "fileId"], unique = true)],
    primaryKeys = ["labelId", "fileId"],
    foreignKeys = [
        ForeignKey(
            entity = LabelEntity::class,
            parentColumns = ["id"],
            childColumns = ["labelId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MediaEntity::class,
            parentColumns = ["id"],
            childColumns = ["fileId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class LabelFileCrossRefEntity(
    var labelId: Long,
    var fileId: Long,
)

data class AppWithLabels(
    @Embedded val app: PackageEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(LabelAppCrossRefEntity::class, parentColumn = "appId", entityColumn = "labelId")
    )
    val labels: List<LabelEntity>
)

data class LabelWithAppIds(
    @Embedded val label: LabelEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        entity = PackageEntity::class,
        associateBy = Junction(LabelAppCrossRefEntity::class, parentColumn = "labelId", entityColumn = "appId"),
        projection = ["id"]
    )
    val ids: List<Long>
)