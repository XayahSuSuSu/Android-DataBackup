package com.xayah.databackup.ui.activity.list.telephony.components

import android.Manifest
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.xayah.databackup.R
import com.xayah.databackup.data.SmsItem
import com.xayah.databackup.ui.activity.list.telephony.TelephonyViewModel
import com.xayah.databackup.ui.components.IconButton
import com.xayah.databackup.ui.components.Scaffold
import com.xayah.databackup.ui.components.TopBarTitle

@ExperimentalPermissionsApi
@ExperimentalMaterial3Api
@Composable
fun SmsListScaffold(
    viewModel: TelephonyViewModel,
    itemContent: @Composable LazyItemScope.(item: SmsItem) -> Unit,
    onConfirm: () -> Unit,
    onFinish: () -> Unit
) {
    val mediumPadding = dimensionResource(R.dimen.padding_medium)
    val list = viewModel.smsList.collectAsState()

    val smsPermissionState = rememberPermissionState(
        Manifest.permission.READ_SMS
    )
    LaunchedEffect(null) {
        // Check permission
        if (smsPermissionState.status.isGranted.not()) {
            smsPermissionState.launchPermissionRequest()
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                modifier = Modifier.padding(mediumPadding),
                onClick = onConfirm,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null
                )
            }
        },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    TopBarTitle(text = stringResource(id = R.string.telephony))
                },
                scrollBehavior = this,
                navigationIcon = {
                    IconButton(icon = Icons.Rounded.ArrowBack, onClick = onFinish)
                },
            )
        },
        topPaddingRate = 1,
        content = {
            items(items = list.value, itemContent = itemContent)
        }
    )
}
