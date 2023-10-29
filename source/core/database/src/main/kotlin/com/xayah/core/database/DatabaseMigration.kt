package com.xayah.core.database

import androidx.room.RenameColumn
import androidx.room.migration.AutoMigrationSpec

object DatabaseMigrations {
    @RenameColumn(
        tableName = "DirectoryEntity",
        fromColumnName = "directoryType",
        toColumnName = "opType",
    )
    class Schema2to3 : AutoMigrationSpec
}
