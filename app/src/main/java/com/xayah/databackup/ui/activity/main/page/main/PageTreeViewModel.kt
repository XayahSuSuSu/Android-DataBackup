package com.xayah.databackup.ui.activity.main.page.main

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.xayah.databackup.R
import com.xayah.databackup.util.PathUtil
import com.xayah.databackup.util.command.PreparationUtil
import com.xayah.databackup.util.readBackupSavePath
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class TreeType {
    Simplify,
    Integral;

    fun ofString(context: Context) = when (this) {
        Simplify -> context.getString(R.string.simplify)
        Integral -> context.getString(R.string.integral)
    }
}

data class PageTreeUiState(
    val treeText: String,
    val typeList: List<TreeType>,
    val selectedIndex: Int,
)

@HiltViewModel
class PageTreeViewModel @Inject constructor() : ViewModel() {
    private val _uiState = mutableStateOf(
        PageTreeUiState(
            treeText = "",
            typeList = listOf(TreeType.Simplify, TreeType.Integral),
            selectedIndex = 0
        )
    )
    val uiState: State<PageTreeUiState>
        get() = _uiState

    private suspend fun loadTree(context: Context) = when (uiState.value.typeList[uiState.value.selectedIndex]) {
        TreeType.Simplify -> PreparationUtil.tree(path = context.readBackupSavePath(), exclude = PathUtil.getExcludeDirs())
        TreeType.Integral -> PreparationUtil.tree(path = context.readBackupSavePath())
    }

    suspend fun setTreeText(context: Context) {
        _uiState.value = uiState.value.copy(treeText = loadTree(context))
    }

    suspend fun setTreeType(context: Context, type: TreeType) {
        withContext(Dispatchers.IO) {
            val list = uiState.value.typeList
            _uiState.value = uiState.value.copy(selectedIndex = list.indexOf(type))
            setTreeText(context)
        }
    }
}
