package com.xayah.databackup.ui.activity.main.page.tree

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.xayah.databackup.R
import com.xayah.databackup.util.PathUtil
import com.xayah.databackup.util.command.PreparationUtil
import com.xayah.databackup.util.readBackupSavePath
import com.xayah.librootservice.util.withIOContext
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

enum class TreeType {
    Simplify,
    Integral;

    fun ofString(context: Context) = when (this) {
        Simplify -> context.getString(R.string.simplify)
        Integral -> context.getString(R.string.integral)
    }
}

data class TreeUiState(
    val treeText: String,
    val typeList: List<TreeType>,
    val selectedIndex: Int,
)

@HiltViewModel
class TreeViewModel @Inject constructor() : ViewModel() {
    private val _uiState = mutableStateOf(
        TreeUiState(
            treeText = "",
            typeList = listOf(TreeType.Simplify, TreeType.Integral),
            selectedIndex = 0
        )
    )
    val uiState: State<TreeUiState>
        get() = _uiState

    private suspend fun loadTree(context: Context) = withIOContext {
        val uiState = uiState.value
        when (uiState.typeList[uiState.selectedIndex]) {
            TreeType.Simplify -> PreparationUtil.tree(path = context.readBackupSavePath(), exclude = PathUtil.getExcludeDirs())
            TreeType.Integral -> PreparationUtil.tree(path = context.readBackupSavePath())
        }
    }

    suspend fun setTreeText(context: Context) {
        _uiState.value = uiState.value.copy(treeText = loadTree(context))
    }

    suspend fun setTreeType(context: Context, type: TreeType) {
        withIOContext {
            val uiState = uiState.value
            val typeList = uiState.typeList
            _uiState.value = uiState.copy(selectedIndex = typeList.indexOf(type))
            setTreeText(context)
        }
    }
}
