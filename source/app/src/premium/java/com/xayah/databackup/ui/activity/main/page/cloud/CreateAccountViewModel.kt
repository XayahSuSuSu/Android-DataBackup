package com.xayah.databackup.ui.activity.main.page.cloud

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.lifecycle.ViewModel
import com.xayah.databackup.DataBackupApplication
import com.xayah.databackup.R
import com.xayah.databackup.ui.component.DialogState
import com.xayah.databackup.ui.component.openConfirmDialog
import com.xayah.databackup.util.LogUtil
import com.xayah.databackup.util.SymbolUtil
import com.xayah.databackup.util.command.CloudUtil
import com.xayah.databackup.util.command.toSpaceString
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class TextFieldConfig(
    val emphasizedState: MutableState<Boolean> = mutableStateOf(false),
    val key: String,
    val value: MutableState<String> = mutableStateOf(""),
    val placeholder: String,
    val leadingIcon: ImageVector,
)

data class TypeConfig(
    val typeDisplay: String,
    val type: String,
    val name: MutableState<String> = mutableStateOf(""),
    val nameEmphasizedState: MutableState<Boolean> = mutableStateOf(false),
    val fixedArgs: List<String> = listOf(),
    val textFields: List<TextFieldConfig>,
)

data class CreateAccountUiState(
    val logUtil: LogUtil,
    val typeList: List<TypeConfig>,
    val typeIndex: Int,
)

private class TextFieldConfigTokens(context: Context) {
    val tokenUrl = TextFieldConfig(
        key = "url",
        placeholder = context.getString(R.string.url),
        leadingIcon = ImageVector.vectorResource(context.theme, context.resources, R.drawable.ic_rounded_link),
    )
    val tokenUsername = TextFieldConfig(
        key = "user",
        placeholder = context.getString(R.string.username),
        leadingIcon = ImageVector.vectorResource(context.theme, context.resources, R.drawable.ic_rounded_person),
    )
    val tokenPassword = TextFieldConfig(
        key = "pass",
        placeholder = context.getString(R.string.password),
        leadingIcon = ImageVector.vectorResource(context.theme, context.resources, R.drawable.ic_rounded_key),
    )
    val tokenHost = TextFieldConfig(
        key = "host",
        placeholder = context.getString(R.string.url),
        leadingIcon = ImageVector.vectorResource(context.theme, context.resources, R.drawable.ic_rounded_link),
    )
    val tokenPort = TextFieldConfig(
        key = "port",
        value = mutableStateOf("21"),
        placeholder = context.getString(R.string.port),
        leadingIcon = ImageVector.vectorResource(context.theme, context.resources, R.drawable.ic_rounded_lan),
    )
}

private class TypeConfigTokens {
    val tokenFTP = TypeConfig(
        typeDisplay = "FTP",
        type = "ftp",
        textFields = run {
            val token = TextFieldConfigTokens(DataBackupApplication.application)
            listOf(
                token.tokenHost,
                token.tokenPort,
                token.tokenUsername,
                token.tokenPassword,
            )
        }
    )
    val tokenWebDAV = TypeConfig(
        typeDisplay = "WebDAV",
        type = "webdav",
        fixedArgs = listOf("vendor=other"),
        textFields = run {
            val token = TextFieldConfigTokens(DataBackupApplication.application)
            listOf(
                token.tokenUrl,
                token.tokenUsername,
                token.tokenPassword,
            )
        }
    )
    val tokenSMB = TypeConfig(
        typeDisplay = "SMB / CIFS",
        type = "smb",
        textFields = run {
            val token = TextFieldConfigTokens(DataBackupApplication.application)
            listOf(
                token.tokenHost,
                token.tokenUsername,
                token.tokenPassword,
            )
        }
    )
}

@HiltViewModel
class CreateAccountViewModel @Inject constructor(logUtil: LogUtil) : ViewModel() {
    private val _uiState = mutableStateOf(
        CreateAccountUiState(
            logUtil = logUtil,
            typeList = run {
                val token = TypeConfigTokens()
                listOf(
                    token.tokenFTP,
                    token.tokenWebDAV,
                    token.tokenSMB,
                )
            },
            typeIndex = 0,
        )
    )
    val uiState: State<CreateAccountUiState>
        get() = _uiState

    fun setTypeIndex(typeIndex: Int) {
        _uiState.value = uiState.value.copy(typeIndex = typeIndex)
    }

    private fun resetTypeList() {
        _uiState.value = uiState.value.copy(
            typeList = run {
                val token = TypeConfigTokens()
                listOf(
                    token.tokenFTP,
                    token.tokenWebDAV,
                    token.tokenSMB,
                )
            }
        )
    }

    suspend fun create(context: Context, dialogState: DialogState, onSuccess: suspend () -> Unit) {
        val uiState by uiState
        val current = uiState.typeList[uiState.typeIndex]
        val name by current.name
        val type = current.type
        val argList = current.textFields.map {
            "${it.key}=${SymbolUtil.QUOTE}${it.value.value}${SymbolUtil.QUOTE}"
        }.toMutableList()
        if (current.fixedArgs.isNotEmpty()) argList.addAll(current.fixedArgs)
        val (isSuccess, _) = CloudUtil.Config.create(uiState.logUtil, name, type, argList.toSpaceString())
        if (isSuccess) {
            resetTypeList()
            onSuccess.invoke()
        } else {
            dialogState.openConfirmDialog(context, context.getString(R.string.failed))
        }
    }
}
