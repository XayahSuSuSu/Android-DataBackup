package com.xayah.feature.main.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xayah.core.data.repository.AppsRepo
import com.xayah.core.data.repository.ListData
import com.xayah.core.data.repository.ListDataRepo
import com.xayah.core.hiddenapi.castTo
import com.xayah.core.model.App
import com.xayah.core.model.File
import com.xayah.core.model.OpType
import com.xayah.core.model.SortType
import com.xayah.core.model.Target
import com.xayah.core.model.database.PackageDataStates
import com.xayah.core.model.util.of
import com.xayah.core.ui.route.MainRoutes
import com.xayah.core.util.decodeURL
import com.xayah.feature.main.list.ListBottomSheetUiState.Loading
import com.xayah.feature.main.list.ListBottomSheetUiState.Success
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ListBottomSheetViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val listDataRepo: ListDataRepo,
    private val appsRepo: AppsRepo
) : ViewModel() {
    private val target: Target = Target.valueOf(savedStateHandle.get<String>(MainRoutes.ARG_TARGET)!!.decodeURL().trim())
    private val opType: OpType = OpType.of(savedStateHandle.get<String>(MainRoutes.ARG_OP_TYPE)?.decodeURL()?.trim())

    val uiState: StateFlow<ListBottomSheetUiState> = when (target) {
        Target.Apps -> combine(
            listDataRepo.getListData(),
            listDataRepo.getAppList()
        ) { lData, aList ->
            val listData = lData.castTo<ListData.Apps>()
            Success.Apps(
                opType = opType,
                showFilterSheet = listData.showFilterSheet,
                showDataItemsSheet = listData.showDataItemsSheet,
                showSystemApps = listData.showSystemApps,
                sortIndex = listData.sortIndex,
                sortType = listData.sortType,
                appList = aList,
            )
        }

        Target.Files -> combine(
            listDataRepo.getListData(),
            listDataRepo.getFileList()
        ) { lData, fList ->
            val listData = lData.castTo<ListData.Files>()
            Success.Files(
                opType = opType,
                showFilterSheet = listData.showFilterSheet,
                sortIndex = listData.sortIndex,
                sortType = listData.sortType,
                fileList = fList,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        initialValue = Loading,
        started = SharingStarted.WhileSubscribed(5_000),
    )

    fun setShowFilterSheet(value: Boolean) {
        viewModelScope.launch {
            listDataRepo.setShowFilterSheet(value)
        }
    }

    fun setShowDataItemsSheet(value: Boolean) {
        viewModelScope.launch {
            listDataRepo.setShowDataItemsSheet(value)
        }
    }

    fun setShowSystemApps() {
        viewModelScope.launch {
            if (uiState.value is Success.Apps) {
                var isShow = false
                listDataRepo.setShowSystemApps {
                    isShow = it.not()
                    it.not()
                }
                val state = uiState.value.castTo<Success.Apps>()
                if (isShow.not()) {
                    appsRepo.unselectAll(state.appList.filter { it.isSystemApp }.map { it.id })
                }
            }
        }
    }

    fun setSortByType() {
        viewModelScope.launch {
            listDataRepo.setSortType { if (it == SortType.ASCENDING) SortType.DESCENDING else SortType.ASCENDING }
        }
    }

    fun setSortByIndex(index: Int) {
        viewModelScope.launch {
            listDataRepo.setSortIndex { index }
        }
    }

    fun setDataItems(selections: PackageDataStates) {
        viewModelScope.launch {
            if (uiState.value is Success.Apps) {
                val state = uiState.value.castTo<Success.Apps>()
                appsRepo.setDataItems(state.appList.filter { it.selected }.map { it.id }, selections)
            }
        }
    }
}

sealed interface ListBottomSheetUiState {
    data object Loading : ListBottomSheetUiState
    sealed class Success(
        open val opType: OpType,
        open val showFilterSheet: Boolean,
    ) : ListBottomSheetUiState {
        data class Apps(
            override val opType: OpType,
            override val showFilterSheet: Boolean,
            val showDataItemsSheet: Boolean,
            val showSystemApps: Boolean,
            val sortIndex: Int,
            val sortType: SortType,
            val appList: List<App>,
        ) : Success(opType, showFilterSheet)

        data class Files(
            override val opType: OpType,
            override val showFilterSheet: Boolean,
            val sortIndex: Int,
            val sortType: SortType,
            val fileList: List<File>,
        ) : Success(opType, showFilterSheet)
    }
}
