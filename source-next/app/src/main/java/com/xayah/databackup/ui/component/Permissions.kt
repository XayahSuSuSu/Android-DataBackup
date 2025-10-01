package com.xayah.databackup.ui.component

import android.Manifest
import androidx.compose.runtime.Composable
import com.google.accompanist.permissions.rememberMultiplePermissionsState

val ContactPermissions = listOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)
val CallLogPermissions = listOf(Manifest.permission.READ_CALL_LOG, Manifest.permission.WRITE_CALL_LOG)
val MessagePermissions = listOf(Manifest.permission.READ_SMS)

@Composable
fun rememberContactPermissionsState(
    onPermissionsResult: (Map<String, Boolean>) -> Unit = {}
) = rememberMultiplePermissionsState(
    ContactPermissions,
    onPermissionsResult
)

@Composable
fun rememberCallLogPermissionsState(
    onPermissionsResult: (Map<String, Boolean>) -> Unit = {}
) = rememberMultiplePermissionsState(
    CallLogPermissions,
    onPermissionsResult
)

@Composable
fun rememberMessagePermissionsState(
    onPermissionsResult: (Map<String, Boolean>) -> Unit = {}
) = rememberMultiplePermissionsState(
    MessagePermissions,
    onPermissionsResult
)
