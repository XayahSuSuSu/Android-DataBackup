package com.xayah.databackup.feature.setup

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.topjohnwu.superuser.Shell
import com.xayah.databackup.App
import com.xayah.databackup.R
import com.xayah.databackup.feature.MainActivity
import com.xayah.databackup.ui.component.CardState
import com.xayah.databackup.util.KeyFirstLaunch
import com.xayah.databackup.util.NotificationHelper
import com.xayah.databackup.util.ShellHelper
import com.xayah.databackup.util.saveBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class CardProp(
    val state: CardState,
    @DrawableRes val icon: Int,
    val title: String,
    val content: String,
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
)

class PermissionsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    var isGrantingNotificationPermission: Boolean = false
    val allGranted: Boolean
        get() = uiState.value.rootCardProp.state == CardState.Success && uiState.value.notificationProp.state == CardState.Success

    private val mutex = Mutex()

    fun validateRoot(context: Context) {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                mutex.withLock {
                    if (uiState.value.rootCardProp.state != CardState.Success) {
                        fun onWaiting() {
                            _uiState.update { currentState ->
                                currentState.copy(rootCardProp = currentState.rootCardProp.copy(state = CardState.Waiting))
                            }
                        }

                        fun onSuccess() {
                            _uiState.update { currentState ->
                                currentState.copy(rootCardProp = currentState.rootCardProp.copy(state = CardState.Success))
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
        }
    }

    fun checkNotification(context: Context) {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                mutex.withLock {
                    fun onSuccess() {
                        _uiState.update { currentState ->
                            currentState.copy(notificationProp = currentState.notificationProp.copy(state = CardState.Success))
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
                }
            }
        }
    }

    fun validateNotification(context: Context) {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                mutex.withLock {
                    if (uiState.value.notificationProp.state != CardState.Success) {
                        fun onWaiting() {
                            _uiState.update { currentState ->
                                currentState.copy(notificationProp = currentState.notificationProp.copy(state = CardState.Waiting))
                            }
                        }

                        fun onSuccess() {
                            _uiState.update { currentState ->
                                currentState.copy(notificationProp = currentState.notificationProp.copy(state = CardState.Success))
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
                            isGrantingNotificationPermission = true
                            if (msg != null) {
                                onFailure(msg)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun validateAll(context: Context) {
        validateRoot(context)
        validateNotification(context)
    }

    fun onNextButtonClick(context: Context) {
        if (allGranted) {
            viewModelScope.launch {
                withContext(Dispatchers.Default) {
                    context.saveBoolean(KeyFirstLaunch, false)
                    context.startActivity(Intent(context, MainActivity::class.java))
                    (context as Activity).finish()
                }
            }
        } else {
            validateAll(context)
        }
    }
}
