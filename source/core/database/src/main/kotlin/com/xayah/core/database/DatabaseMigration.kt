package com.xayah.core.database

import androidx.room.RenameColumn
import androidx.room.migration.AutoMigrationSpec

object DatabaseMigrations {
    @RenameColumn(
        tableName = "DirectoryEntity",
        fromColumnName = "directoryType",
        toColumnName = "opType",
    )
    @RenameColumn(
        tableName = "PackageBackupOperation",
        fromColumnName = "apkLog",
        toColumnName = "apk_log",
    )
    @RenameColumn(
        tableName = "PackageBackupOperation",
        fromColumnName = "userLog",
        toColumnName = "user_log",
    )
    @RenameColumn(
        tableName = "PackageBackupOperation",
        fromColumnName = "userDeLog",
        toColumnName = "userDe_log",
    )
    @RenameColumn(
        tableName = "PackageBackupOperation",
        fromColumnName = "dataLog",
        toColumnName = "data_log",
    )
    @RenameColumn(
        tableName = "PackageBackupOperation",
        fromColumnName = "obbLog",
        toColumnName = "obb_log",
    )
    @RenameColumn(
        tableName = "PackageBackupOperation",
        fromColumnName = "mediaLog",
        toColumnName = "media_log",
    )
    @RenameColumn(
        tableName = "PackageBackupOperation",
        fromColumnName = "apkState",
        toColumnName = "apk_state",
    )
    @RenameColumn(
        tableName = "PackageBackupOperation",
        fromColumnName = "userState",
        toColumnName = "user_state",
    )
    @RenameColumn(
        tableName = "PackageBackupOperation",
        fromColumnName = "userDeState",
        toColumnName = "userDe_state",
    )
    @RenameColumn(
        tableName = "PackageBackupOperation",
        fromColumnName = "dataState",
        toColumnName = "data_state",
    )
    @RenameColumn(
        tableName = "PackageBackupOperation",
        fromColumnName = "obbState",
        toColumnName = "obb_state",
    )
    @RenameColumn(
        tableName = "PackageBackupOperation",
        fromColumnName = "mediaState",
        toColumnName = "media_state",
    )
    @RenameColumn(
        tableName = "PackageRestoreOperation",
        fromColumnName = "apkLog",
        toColumnName = "apk_log",
    )
    @RenameColumn(
        tableName = "PackageRestoreOperation",
        fromColumnName = "userLog",
        toColumnName = "user_log",
    )
    @RenameColumn(
        tableName = "PackageRestoreOperation",
        fromColumnName = "userDeLog",
        toColumnName = "userDe_log",
    )
    @RenameColumn(
        tableName = "PackageRestoreOperation",
        fromColumnName = "dataLog",
        toColumnName = "data_log",
    )
    @RenameColumn(
        tableName = "PackageRestoreOperation",
        fromColumnName = "obbLog",
        toColumnName = "obb_log",
    )
    @RenameColumn(
        tableName = "PackageRestoreOperation",
        fromColumnName = "mediaLog",
        toColumnName = "media_log",
    )
    @RenameColumn(
        tableName = "PackageRestoreOperation",
        fromColumnName = "apkState",
        toColumnName = "apk_state",
    )
    @RenameColumn(
        tableName = "PackageRestoreOperation",
        fromColumnName = "userState",
        toColumnName = "user_state",
    )
    @RenameColumn(
        tableName = "PackageRestoreOperation",
        fromColumnName = "userDeState",
        toColumnName = "userDe_state",
    )
    @RenameColumn(
        tableName = "PackageRestoreOperation",
        fromColumnName = "dataState",
        toColumnName = "data_state",
    )
    @RenameColumn(
        tableName = "PackageRestoreOperation",
        fromColumnName = "obbState",
        toColumnName = "obb_state",
    )
    @RenameColumn(
        tableName = "PackageRestoreOperation",
        fromColumnName = "mediaState",
        toColumnName = "media_state",
    )
    @RenameColumn(
        tableName = "MediaBackupOperationEntity",
        fromColumnName = "opLog",
        toColumnName = "data_log",
    )
    @RenameColumn(
        tableName = "MediaBackupOperationEntity",
        fromColumnName = "opState",
        toColumnName = "data_state",
    )
    @RenameColumn(
        tableName = "MediaBackupOperationEntity",
        fromColumnName = "state",
        toColumnName = "mediaState",
    )
    @RenameColumn(
        tableName = "MediaRestoreOperationEntity",
        fromColumnName = "opLog",
        toColumnName = "data_log",
    )
    @RenameColumn(
        tableName = "MediaRestoreOperationEntity",
        fromColumnName = "opState",
        toColumnName = "data_state",
    )
    @RenameColumn(
        tableName = "MediaRestoreOperationEntity",
        fromColumnName = "state",
        toColumnName = "mediaState",
    )

    class Schema2to3 : AutoMigrationSpec
}
