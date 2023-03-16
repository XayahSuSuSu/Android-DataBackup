package com.xayah.databackup.ui.activity.list.telephony.components

import android.Manifest
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.xayah.databackup.R
import com.xayah.databackup.data.LoadingState
import com.xayah.databackup.ui.activity.guide.components.GuideScaffold
import com.xayah.databackup.ui.activity.guide.components.card.CardEnvironment
import com.xayah.databackup.util.*

@OptIn(ExperimentalPermissionsApi::class)
@ExperimentalMaterial3Api
@Composable
fun TelephonyPermission(onFinish: () -> Unit) {
    // SMS permission
    val smsState = remember {
        mutableStateOf(LoadingState.Loading)
    }
    val smsPermissionState = rememberPermissionState(Manifest.permission.READ_SMS) {
        if (it)
            smsState.value = LoadingState.Success
        else
            smsState.value = LoadingState.Failed
    }

    // Contacts permissions
    val contactsState = remember {
        mutableStateOf(LoadingState.Loading)
    }
    val contactsPermissionsState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
        )
    ) {
        contactsState.value = LoadingState.Success
        for (i in it) {
            if (i.value.not()) {
                contactsState.value = LoadingState.Failed
            }
        }
    }

    // Call log permissions
    val callLogState = remember {
        mutableStateOf(LoadingState.Loading)
    }
    val callLogPermissionsState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.WRITE_CALL_LOG,
        )
    ) {
        callLogState.value = LoadingState.Success
        for (i in it) {
            if (i.value.not()) {
                callLogState.value = LoadingState.Failed
            }
        }
    }

    GuideScaffold(
        title = stringResource(R.string.telephony_permissions),
        icon = Icons.Rounded.CheckCircle,
        showBtnIcon = false,
        nextBtnIcon = Icons.Rounded.ArrowForward,
        onNextBtnClick = onFinish,
        items = {
            item {
                CardEnvironment(
                    item = stringResource(R.string.sms_permission),
                    cardState = smsState.value,
                    onCardClick = {
                        smsPermissionState.launchPermissionRequest()
                    }
                )
            }

            item {
                CardEnvironment(
                    item = stringResource(R.string.contacts_permission),
                    cardState = contactsState.value,
                    onCardClick = {
                        contactsPermissionsState.launchMultiplePermissionRequest()
                    }
                )
            }

            item {
                CardEnvironment(
                    item = stringResource(R.string.call_log_permission),
                    cardState = callLogState.value,
                    onCardClick = {
                        callLogPermissionsState.launchMultiplePermissionRequest()
                    }
                )
            }
        }
    )
}
