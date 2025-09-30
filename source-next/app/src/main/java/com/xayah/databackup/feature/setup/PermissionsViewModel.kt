package com.xayah.databackup.feature.setup

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.lifecycle.viewModelScope
import com.google.accompanist.permissions.MultiplePermissionsState
import com.topjohnwu.superuser.Shell
import com.xayah.databackup.App
import com.xayah.databackup.R
import com.xayah.databackup.feature.MainActivity
import com.xayah.databackup.ui.component.CardState
import com.xayah.databackup.util.BaseViewModel
import com.xayah.databackup.util.KeyFirstLaunch
import com.xayah.databackup.util.NotificationHelper
import com.xayah.databackup.util.ShellHelper
import com.xayah.databackup.util.saveBoolean
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

data class CardProp(
    val state: CardState,
    @DrawableRes val icon: Int,
    val title: String,
    val content: String,
    val continuation: CancellableContinuation<Unit>? = null,
)

data class UiState(
    val rootCardProp: CardProp = CardProp(
        state = CardState.Idle,
        icon = R.drawable.ic_hash,
        title = App.application.getString(R.string.root),
        content = App.application.getString(R.string.root_desc)
    ),
    val notificationProp: CardProp = CardProp(
        state = CardState.Idle,
        icon = R.drawable.ic_bell,
        title = App.application.getString(R.string.notification),
        content = App.application.getString(R.string.notification_desc)
    ),
    val contactProp: CardProp = CardProp(
        state = CardState.Idle,
        icon = R.drawable.ic_user_round,
        title = App.application.getString(R.string.contacts),
        content = App.application.getString(R.string.contact_permission_desc)
    ),
    val callLogProp: CardProp = CardProp(
        state = CardState.Idle,
        icon = R.drawable.ic_phone,
        title = App.application.getString(R.string.call_logs),
        content = App.application.getString(R.string.call_log_permission_desc)
    ),
    val messageProp: CardProp = CardProp(
        state = CardState.Idle,
        icon = R.drawable.ic_message_circle,
        title = App.application.getString(R.string.messages),
        content = App.application.getString(R.string.message_permission_desc)
    ),
)

class PermissionsViewModel : BaseViewModel() {
    companion object {
        private const val TIMEOUT = 30000L // 30s
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    var mIsGrantingNotificationPermission: Boolean = false
    val misRequiredGranted: Boolean
        get() = uiState.value.rootCardProp.state == CardState.Success && uiState.value.notificationProp.state == CardState.Success
    val misAllGranted: Boolean
        get() = uiState.value.rootCardProp.state == CardState.Success && uiState.value.notificationProp.state == CardState.Success &&
                uiState.value.contactProp.state == CardState.Success && uiState.value.callLogProp.state == CardState.Success &&
                uiState.value.messageProp.state == CardState.Success


    suspend fun validateRoot(context: Context) {
        withContext(Dispatchers.Default) {
            if (uiState.value.rootCardProp.state != CardState.Success) {
                fun onWaiting() {
                    _uiState.update { currentState ->
                        currentState.copy(rootCardProp = currentState.rootCardProp.copy(state = CardState.Waiting))
                    }
                }

                fun onSuccess() {
                    _uiState.update { currentState ->
                        currentState.copy(
                            rootCardProp = currentState.rootCardProp.copy(
                                state = CardState.Success,
                                content = context.getString(R.string.granted)
                            )
                        )
                    }
                }

                fun onFailure(msg: String) {
                    _uiState.update { currentState ->
                        currentState.copy(rootCardProp = currentState.rootCardProp.copy(state = CardState.Error, content = msg))
                    }
                }
                onWaiting()
                var errMsg: String?
                val errDesc = context.getString(R.string.root_denied_msg)
                runCatching {
                    ShellHelper.initMainShell(context = context)
                }.onFailure {
                    errMsg = it.localizedMessage
                }
                runCatching {
                    if (Shell.getShell().isRoot) {
                        onSuccess()
                    } else {
                        onFailure(errDesc)
                    }
                }.onFailure {
                    errMsg = it.localizedMessage
                    errMsg?.also { msg -> onFailure("$msg\n$errDesc") }
                }
            }
        }
    }

    suspend fun checkNotification(context: Context) {
        withContext(Dispatchers.Default) {
            fun onSuccess() {
                _uiState.update { currentState ->
                    currentState.copy(
                        notificationProp = currentState.notificationProp.copy(
                            state = CardState.Success,
                            content = context.getString(R.string.granted)
                        )
                    )
                }
            }

            fun onFailure(msg: String) {
                _uiState.update { currentState ->
                    currentState.copy(notificationProp = currentState.notificationProp.copy(state = CardState.Error, content = msg))
                }
            }

            val result = NotificationHelper.checkPermission(context)
            if (result) {
                onSuccess()
            } else {
                onFailure(context.getString(R.string.permission_is_denied))
            }
            uiState.value.notificationProp.continuation?.resume(Unit) { _, _, _ -> }
            _uiState.update { currentState ->
                currentState.copy(notificationProp = currentState.notificationProp.copy(continuation = null))
            }
        }
    }

    suspend fun validateNotification(context: Context) {
        withContext(Dispatchers.Default) {
            if (uiState.value.notificationProp.state != CardState.Success) {
                fun onWaiting() {
                    _uiState.update { currentState ->
                        currentState.copy(notificationProp = currentState.notificationProp.copy(state = CardState.Waiting))
                    }
                }

                fun onSuccess() {
                    _uiState.update { currentState ->
                        currentState.copy(
                            notificationProp = currentState.notificationProp.copy(
                                state = CardState.Success,
                                content = context.getString(R.string.granted)
                            )
                        )
                    }
                }

                fun onFailure(msg: String) {
                    _uiState.update { currentState ->
                        currentState.copy(notificationProp = currentState.notificationProp.copy(state = CardState.Error, content = msg))
                    }
                }
                onWaiting()
                if (NotificationHelper.checkPermission(context)) {
                    onSuccess()
                } else {
                    val msg = withContext(Dispatchers.Main) {
                        NotificationHelper.requestPermission(context)
                    }
                    mIsGrantingNotificationPermission = true
                    if (msg != null) {
                        onFailure(msg)
                    } else {
                        runCatching {
                            withTimeout(TIMEOUT) {
                                suspendCancellableCoroutine {
                                    // Suspend here util receiving the result
                                    _uiState.update { currentState ->
                                        currentState.copy(notificationProp = currentState.notificationProp.copy(continuation = it))
                                    }
                                }
                            }
                        }.onFailure {
                            // Timeout
                        }
                    }
                }
            }
        }
    }

    suspend fun checkContact(context: Context, result: Map<String, Boolean>) {
        withContext(Dispatchers.Default) {
            fun onSuccess() {
                _uiState.update { currentState ->
                    currentState.copy(
                        contactProp = currentState.contactProp.copy(
                            state = CardState.Success,
                            content = context.getString(R.string.granted)
                        )
                    )
                }
            }

            fun onFailure(msg: String) {
                _uiState.update { currentState ->
                    currentState.copy(contactProp = currentState.contactProp.copy(state = CardState.Error, content = msg))
                }
            }

            var validated = true
            result.forEach {
                if (it.value.not()) {
                    validated = false
                }
            }
            if (validated) {
                onSuccess()
            } else {
                onFailure(context.getString(R.string.permission_is_denied))
            }
            uiState.value.contactProp.continuation?.resume(Unit) { _, _, _ -> }
            _uiState.update { currentState ->
                currentState.copy(contactProp = currentState.contactProp.copy(continuation = null))
            }
        }
    }

    suspend fun validateContact(state: MultiplePermissionsState) {
        withContext(Dispatchers.Default) {
            if (uiState.value.contactProp.state != CardState.Success) {
                fun onWaiting() {
                    _uiState.update { currentState ->
                        currentState.copy(contactProp = currentState.contactProp.copy(state = CardState.Waiting))
                    }
                }
                onWaiting()
                withContext(Dispatchers.Main) {
                    state.launchMultiplePermissionRequest()
                }
                runCatching {
                    withTimeout(TIMEOUT) {
                        suspendCancellableCoroutine {
                            // Suspend here util receiving the result
                            _uiState.update { currentState ->
                                currentState.copy(contactProp = currentState.contactProp.copy(continuation = it))
                            }
                        }
                    }
                }.onFailure {
                    // Timeout
                }
            }
        }
    }

    suspend fun checkCallLog(context: Context, result: Map<String, Boolean>) {
        withContext(Dispatchers.Default) {
            fun onSuccess() {
                _uiState.update { currentState ->
                    currentState.copy(
                        callLogProp = currentState.callLogProp.copy(
                            state = CardState.Success,
                            content = context.getString(R.string.granted)
                        )
                    )
                }
            }

            fun onFailure(msg: String) {
                _uiState.update { currentState ->
                    currentState.copy(callLogProp = currentState.callLogProp.copy(state = CardState.Error, content = msg))
                }
            }

            var validated = true
            result.forEach {
                if (it.value.not()) {
                    validated = false
                }
            }
            if (validated) {
                onSuccess()
            } else {
                onFailure(context.getString(R.string.permission_is_denied))
            }
            uiState.value.callLogProp.continuation?.resume(Unit) { _, _, _ -> }
            _uiState.update { currentState ->
                currentState.copy(callLogProp = currentState.callLogProp.copy(continuation = null))
            }
        }
    }

    suspend fun validateCallLog(state: MultiplePermissionsState) {
        withContext(Dispatchers.Default) {
            if (uiState.value.callLogProp.state != CardState.Success) {
                fun onWaiting() {
                    _uiState.update { currentState ->
                        currentState.copy(callLogProp = currentState.callLogProp.copy(state = CardState.Waiting))
                    }
                }
                onWaiting()
                withContext(Dispatchers.Main) {
                    state.launchMultiplePermissionRequest()
                }
                runCatching {
                    withTimeout(TIMEOUT) {
                        suspendCancellableCoroutine {
                            // Suspend here util receiving the result
                            _uiState.update { currentState ->
                                currentState.copy(callLogProp = currentState.callLogProp.copy(continuation = it))
                            }
                        }
                    }
                }.onFailure {
                    // Timeout
                }
            }
        }
    }

    suspend fun checkMessage(context: Context, result: Map<String, Boolean>) {
        withContext(Dispatchers.Default) {
            fun onSuccess() {
                _uiState.update { currentState ->
                    currentState.copy(
                        messageProp = currentState.messageProp.copy(
                            state = CardState.Success,
                            content = context.getString(R.string.granted)
                        )
                    )
                }
            }

            fun onFailure(msg: String) {
                _uiState.update { currentState ->
                    currentState.copy(messageProp = currentState.messageProp.copy(state = CardState.Error, content = msg))
                }
            }

            var validated = true
            result.forEach {
                if (it.value.not()) {
                    validated = false
                }
            }
            if (validated) {
                onSuccess()
            } else {
                onFailure(context.getString(R.string.permission_is_denied))
            }
            uiState.value.messageProp.continuation?.resume(Unit) { _, _, _ -> }
            _uiState.update { currentState ->
                currentState.copy(messageProp = currentState.messageProp.copy(continuation = null))
            }
        }
    }

    suspend fun validateMessage(state: MultiplePermissionsState) {
        withContext(Dispatchers.Default) {
            if (uiState.value.messageProp.state != CardState.Success) {
                fun onWaiting() {
                    _uiState.update { currentState ->
                        currentState.copy(messageProp = currentState.messageProp.copy(state = CardState.Waiting))
                    }
                }
                onWaiting()
                withContext(Dispatchers.Main) {
                    state.launchMultiplePermissionRequest()
                }
                runCatching {
                    withTimeout(TIMEOUT) {
                        suspendCancellableCoroutine {
                            // Suspend here util receiving the result
                            _uiState.update { currentState ->
                                currentState.copy(messageProp = currentState.messageProp.copy(continuation = it))
                            }
                        }
                    }
                }.onFailure {
                    // Timeout
                }
            }
        }
    }

    private fun validateAll(
        context: Context,
        contactsPermissionState: MultiplePermissionsState,
        callLogsPermissionState: MultiplePermissionsState,
        messagesPermissionState: MultiplePermissionsState
    ) {
        withLock {
            validateRoot(context)
            validateNotification(context)
            validateContact(contactsPermissionState)
            validateCallLog(callLogsPermissionState)
            validateMessage(messagesPermissionState)
        }
    }

    fun onSkipButtonClick(context: Context) {
        if (misRequiredGranted) {
            viewModelScope.launch {
                withContext(Dispatchers.Default) {
                    context.saveBoolean(KeyFirstLaunch, false)
                    context.startActivity(Intent(context, MainActivity::class.java))
                    (context as Activity).finish()
                }
            }
        }
    }

    fun onNextButtonClick(
        context: Context,
        contactsPermissionState: MultiplePermissionsState,
        callLogsPermissionState: MultiplePermissionsState,
        messagesPermissionState: MultiplePermissionsState
    ) {
        if (misAllGranted) {
            viewModelScope.launch {
                withContext(Dispatchers.Default) {
                    context.saveBoolean(KeyFirstLaunch, false)
                    context.startActivity(Intent(context, MainActivity::class.java))
                    (context as Activity).finish()
                }
            }
        } else {
            validateAll(context, contactsPermissionState, callLogsPermissionState, messagesPermissionState)
        }
    }
}
