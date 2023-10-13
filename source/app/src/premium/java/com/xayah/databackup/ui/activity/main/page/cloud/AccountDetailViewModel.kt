package com.xayah.databackup.ui.activity.main.page.cloud

import android.content.Context
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xayah.databackup.R
import com.xayah.databackup.data.CloudAccountEntity
import com.xayah.databackup.data.CloudDao
import com.xayah.databackup.ui.component.DialogState
import com.xayah.databackup.ui.component.openConfirmDialog
import com.xayah.databackup.util.LogUtil
import com.xayah.databackup.util.SymbolUtil
import com.xayah.databackup.util.command.CloudUtil
import com.xayah.databackup.util.command.toSpaceString
import com.xayah.librootservice.util.withIOContext
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TextFieldConfig(
    val emphasizedState: MutableState<Boolean> = mutableStateOf(false),
    val key: String,
    val value: MutableState<String> = mutableStateOf(""),
    val keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
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

private object TextFieldConfigTokens {
    fun getUrl(context: Context, value: String?) = TextFieldConfig(
        key = "url",
        value = mutableStateOf(value ?: ""),
        placeholder = context.getString(R.string.url),
        leadingIcon = ImageVector.vectorResource(context.theme, context.resources, R.drawable.ic_rounded_link),
    )

    fun getUsername(context: Context, value: String?) = TextFieldConfig(
        key = "user",
        value = mutableStateOf(value ?: ""),
        placeholder = context.getString(R.string.username),
        leadingIcon = ImageVector.vectorResource(context.theme, context.resources, R.drawable.ic_rounded_person),
    )

    fun getPassword(context: Context, value: String?) = TextFieldConfig(
        key = "pass",
        value = mutableStateOf(value ?: ""),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        placeholder = context.getString(R.string.password),
        leadingIcon = ImageVector.vectorResource(context.theme, context.resources, R.drawable.ic_rounded_key),
    )

    fun getHost(context: Context, value: String?) = TextFieldConfig(
        key = "host",
        value = mutableStateOf(value ?: ""),
        placeholder = context.getString(R.string.url),
        leadingIcon = ImageVector.vectorResource(context.theme, context.resources, R.drawable.ic_rounded_link),
    )

    fun getPort(context: Context, value: String?) = TextFieldConfig(
        key = "port",
        value = mutableStateOf(value ?: "21"),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        placeholder = context.getString(R.string.port),
        leadingIcon = ImageVector.vectorResource(context.theme, context.resources, R.drawable.ic_rounded_lan),
    )
}

private object TypeConfigTokens {
    fun getFTP(context: Context, account: CloudAccountEntity?) = TypeConfig(
        name = mutableStateOf(account?.name ?: ""),
        typeDisplay = "FTP",
        type = "ftp",
        textFields = run {
            listOf(
                TextFieldConfigTokens.getHost(context, account?.account?.host),
                TextFieldConfigTokens.getPort(context, account?.account?.port?.toString()),
                TextFieldConfigTokens.getUsername(context, account?.account?.user),
                TextFieldConfigTokens.getPassword(context, account?.account?.pass),
            )
        }
    )

    fun getWebDAV(context: Context, account: CloudAccountEntity?) = TypeConfig(
        name = mutableStateOf(account?.name ?: ""),
        typeDisplay = "WebDAV",
        type = "webdav",
        fixedArgs = listOf("vendor=other"),
        textFields = run {
            listOf(
                TextFieldConfigTokens.getUrl(context, account?.account?.url),
                TextFieldConfigTokens.getUsername(context, account?.account?.user),
                TextFieldConfigTokens.getPassword(context, account?.account?.pass),
            )
        }
    )

    fun getSMB(context: Context, account: CloudAccountEntity?) = TypeConfig(
        name = mutableStateOf(account?.name ?: ""),
        typeDisplay = "SMB / CIFS",
        type = "smb",
        textFields = run {
            listOf(
                TextFieldConfigTokens.getHost(context, account?.account?.host),
                TextFieldConfigTokens.getUsername(context, account?.account?.user),
                TextFieldConfigTokens.getPassword(context, account?.account?.pass),
            )
        }
    )
}

enum class AccountDetailMode {
    Create,
    Edit,
}

data class AccountDetailUiState(
    val logUtil: LogUtil,
    val cloudDao: CloudDao,
    val typeList: List<TypeConfig>,
    val typeIndex: Int,
    val mode: AccountDetailMode,
)

@HiltViewModel
class AccountDetailViewModel @Inject constructor(@ApplicationContext context: Context, logUtil: LogUtil, cloudDao: CloudDao) : ViewModel() {
    private val _uiState = mutableStateOf(
        AccountDetailUiState(
            logUtil = logUtil,
            cloudDao = cloudDao,
            typeList = run {
                listOf(
                    TypeConfigTokens.getFTP(context, null),
                    TypeConfigTokens.getWebDAV(context, null),
                    TypeConfigTokens.getSMB(context, null),
                )
            },
            typeIndex = 0,
            mode = AccountDetailMode.Create,
        )
    )
    val uiState: State<AccountDetailUiState>
        get() = _uiState

    fun setTypeIndex(typeIndex: Int) {
        _uiState.value = uiState.value.copy(typeIndex = typeIndex)
    }

    private fun resetTypeList(context: Context, account: CloudAccountEntity? = null) {
        _uiState.value = uiState.value.copy(
            typeList = run {
                listOf(
                    TypeConfigTokens.getFTP(context, account),
                    TypeConfigTokens.getWebDAV(context, account),
                    TypeConfigTokens.getSMB(context, account),
                )
            }
        )
    }

    private fun setEditMode() {
        _uiState.value = uiState.value.copy(mode = AccountDetailMode.Edit)
    }

    fun initialize(context: Context, entityName: String?) {
        val uiState by uiState

        viewModelScope.launch {
            withIOContext {
                if (entityName != null) {
                    val account = uiState.cloudDao.queryAccountByName(entityName)
                    if (account != null) {
                        val typeList = uiState.typeList.map { it.type }
                        val index = typeList.indexOf(account.account.type)
                        if (index != -1) {
                            setTypeIndex(index)
                            resetTypeList(context, account)
                            setEditMode()
                        }
                    }
                }
            }
        }
    }

    suspend fun update(context: Context, dialogState: DialogState, onSuccess: suspend () -> Unit) {
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
            onSuccess.invoke()
            resetTypeList(context)
        } else {
            dialogState.openConfirmDialog(context, context.getString(R.string.failed))
        }
    }
}
