package com.xayah.core.service.util

import com.xayah.core.database.dao.PackageBackupOperationDao
import com.xayah.core.database.model.PackageBackupOperation
import com.xayah.core.model.OperationState

suspend fun PackageBackupOperationDao.upsertApk(op: PackageBackupOperation, opState: OperationState? = null, opLog: String? = null, opBytes: Long? = null) =
    run {
        op.apkOp.state = opState ?: op.apkOp.state
        op.apkOp.log = opLog ?: op.apkOp.log
        op.apkOp.bytes = opBytes ?: op.apkOp.bytes
        upsert(op)
    }

suspend fun PackageBackupOperationDao.upsertUser(op: PackageBackupOperation, opState: OperationState? = null, opLog: String? = null, opBytes: Long? = null) =
    run {
        op.userOp.state = opState ?: op.userOp.state
        op.userOp.log = opLog ?: op.userOp.log
        op.userOp.bytes = opBytes ?: op.userOp.bytes
        upsert(op)
    }

suspend fun PackageBackupOperationDao.upsertUserDe(op: PackageBackupOperation, opState: OperationState? = null, opLog: String? = null, opBytes: Long? = null) =
    run {
        op.userDeOp.state = opState ?: op.userDeOp.state
        op.userDeOp.log = opLog ?: op.userDeOp.log
        op.userDeOp.bytes = opBytes ?: op.userDeOp.bytes
        upsert(op)
    }

suspend fun PackageBackupOperationDao.upsertData(op: PackageBackupOperation, opState: OperationState? = null, opLog: String? = null, opBytes: Long? = null) =
    run {
        op.dataOp.state = opState ?: op.dataOp.state
        op.dataOp.log = opLog ?: op.dataOp.log
        op.dataOp.bytes = opBytes ?: op.dataOp.bytes
        upsert(op)
    }

suspend fun PackageBackupOperationDao.upsertObb(op: PackageBackupOperation, opState: OperationState? = null, opLog: String? = null, opBytes: Long? = null) =
    run {
        op.obbOp.state = opState ?: op.obbOp.state
        op.obbOp.log = opLog ?: op.obbOp.log
        op.obbOp.bytes = opBytes ?: op.obbOp.bytes
        upsert(op)
    }

suspend fun PackageBackupOperationDao.upsertMedia(op: PackageBackupOperation, opState: OperationState? = null, opLog: String? = null, opBytes: Long? = null) =
    run {
        op.mediaOp.state = opState ?: op.mediaOp.state
        op.mediaOp.log = opLog ?: op.mediaOp.log
        op.mediaOp.bytes = opBytes ?: op.mediaOp.bytes
        upsert(op)
    }
