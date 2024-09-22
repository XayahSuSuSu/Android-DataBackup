package com.xayah.feature.main.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xayah.core.data.repository.ListData
import com.xayah.core.data.repository.ListDataRepo
import com.xayah.core.hiddenapi.castTo
import com.xayah.core.model.OpType
import com.xayah.core.model.Target
import com.xayah.core.model.UserInfo
import com.xayah.core.model.util.of
import com.xayah.core.ui.route.MainRoutes
import com.xayah.core.util.decodeURL
import com.xayah.core.util.launchOnDefault
import com.xayah.feature.main.list.ListTopBarUiState.Loading
import com.xayah.feature.main.list.ListTopBarUiState.Success
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ListTopBarViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val listDataRepo: ListDataRepo,
) : ViewModel() {
    private val target: Target = Target.valueOf(savedStateHandle.get<String>(MainRoutes.ARG_TARGET)!!.decodeURL().trim())
    private val opType: OpType = OpType.of(savedStateHandle.get<String>(MainRoutes.ARG_OP_TYPE)?.decodeURL()?.trim())

    val uiState: StateFlow<ListTopBarUiState> = when (target) {
        Target.Apps -> listDataRepo.getListData().map {
            val listData = it.castTo<ListData.Apps>()
            Success.Apps(
                opType = opType,
                selected = listData.selected,
                total = listData.total,
                isUpdating = listData.isUpdating,
                userIndex = listData.userIndex,
                userList = listData.userList,
                userMap = listData.userMap,
            )
        }

        Target.Files -> listDataRepo.getListData().map {
            val listData = it.castTo<ListData.Files>()
            Success.Files(
                opType = opType,
                selected = listData.selected,
                total = listData.total,
                isUpdating = listData.isUpdating,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        initialValue = Loading,
        started = SharingStarted.WhileSubscribed(5_000),
    )

    fun search(text: String) {
        viewModelScope.launchOnDefault {
            listDataRepo.setSearchQuery(text)
        }
    }

    fun setUser(index: Int) {
        viewModelScope.launchOnDefault {
            listDataRepo.setUserIndex(index)
        }
    }
}

sealed interface ListTopBarUiState {
    data object Loading : ListTopBarUiState
    sealed class Success(
        open val opType: OpType,
        open val selected: Long,
        open val total: Long,
        open val isUpdating: Boolean,
    ) : ListTopBarUiState {
        data class Apps(
            override val opType: OpType,
            override val selected: Long,
            override val total: Long,
            override val isUpdating: Boolean,
            val userIndex: Int,
            val userList: List<UserInfo>,
            val userMap: Map<Int, Long>,
        ) : Success(opType, selected, total, isUpdating)

        data class Files(
            override val opType: OpType,
            override val selected: Long,
            override val total: Long,
            override val isUpdating: Boolean,
        ) : Success(opType, selected, total, isUpdating)
    }
}
