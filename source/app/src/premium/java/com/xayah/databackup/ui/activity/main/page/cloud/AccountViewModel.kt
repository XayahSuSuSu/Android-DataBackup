package com.xayah.databackup.ui.activity.main.page.cloud

import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.gson.reflect.TypeToken
import com.xayah.databackup.DataBackupApplication
import com.xayah.databackup.R
import com.xayah.databackup.data.formatSize
import com.xayah.databackup.util.GsonUtil
import com.xayah.databackup.util.LogUtil
import com.xayah.databackup.util.command.CloudUtil
import com.xayah.databackup.util.saveCloudAccountNum
import com.xayah.librootservice.util.ExceptionUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

data class AccountUiState(
    val mutex: Mutex,
    val logUtil: LogUtil,
    val gsonUtil: GsonUtil,
    val isLoading: Boolean,
    val accounts: List<Account>,
)

enum class AccountConfigSizeBytes(val value: Long) {
    Loading(-255L),
    FetchFailed(-1L)
}

data class AccountConfig(
    val url: String = "",
    val host: String = "",
    val type: String = "",
    val user: String = "",
    val pass: String = "",
    val vendor: String = "",
    val sizeBytes: Long = AccountConfigSizeBytes.Loading.value,
) {
    val sizeDisplay: String
        get() = when (sizeBytes) {
            AccountConfigSizeBytes.Loading.value -> DataBackupApplication.application.getString(R.string.loading)
            AccountConfigSizeBytes.FetchFailed.value -> DataBackupApplication.application.getString(R.string.fetch_failed)
            else -> formatSize(sizeBytes.toDouble())
        }
}

typealias AccountMap = HashMap<String, AccountConfig>

data class Account(
    val name: String,
    val config: AccountConfig,
)

@HiltViewModel
class AccountViewModel @Inject constructor(logUtil: LogUtil, gsonUtil: GsonUtil) : ViewModel() {
    private val _uiState = mutableStateOf(
        AccountUiState(
            mutex = Mutex(),
            logUtil = logUtil,
            gsonUtil = gsonUtil,
            isLoading = true,
            accounts = listOf(),
        )
    )
    val uiState: State<AccountUiState>
        get() = _uiState

    suspend fun initialize() {
        val uiState by uiState
        if (uiState.mutex.isLocked.not()) {
            uiState.mutex.withLock {
                val (_, json) = CloudUtil.Config.dump(uiState.logUtil)
                val type = object : TypeToken<AccountMap>() {}.type
                val map = ExceptionUtil.tryOn(block = { uiState.gsonUtil.fromJson(json, type) }, onException = { AccountMap() })
                DataBackupApplication.application.saveCloudAccountNum(map.size)

                _uiState.value = uiState.copy(
                    isLoading = false,
                    accounts = map.map { (key, value) -> Account(name = key, config = value) }
                )

                val newAccounts = uiState.accounts.toMutableList()
                uiState.accounts.forEachIndexed { index, account ->
                    val (_, info) = CloudUtil.size(logUtil = uiState.logUtil, gsonUtil = uiState.gsonUtil, name = account.name)
                    newAccounts[index] = account.copy(config = account.config.copy(sizeBytes = info.bytes))
                    _uiState.value = uiState.copy(accounts = newAccounts.toList())
                }
            }
        }
    }

    suspend fun delete(account: Account) {
        val uiState by uiState
        CloudUtil.Config.delete(logUtil = uiState.logUtil, name = account.name)
        initialize()
    }
}
