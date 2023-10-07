package com.xayah.databackup.ui.activity.main.page.tree

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.xayah.databackup.R
import com.xayah.databackup.util.PathUtil
import com.xayah.databackup.util.command.PreparationUtil
import com.xayah.databackup.util.readBackupSavePath
import com.xayah.librootservice.util.withIOContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
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
    val treeTextList: List<String>,
    val typeList: List<TreeType>,
    val selectedIndex: Int,
)

@HiltViewModel
class TreeViewModel @Inject constructor() : ViewModel() {
    private val _uiState = mutableStateOf(
        TreeUiState(
            treeTextList = listOf(),
            typeList = listOf(TreeType.Simplify, TreeType.Integral),
            selectedIndex = 0
        )
    )
    val uiState: State<TreeUiState>
        get() = _uiState

    private suspend fun loadTree(context: Context): List<String> = withIOContext {
        val uiState by uiState
        when (uiState.typeList[uiState.selectedIndex]) {
            TreeType.Simplify -> PreparationUtil.tree(path = context.readBackupSavePath().first(), exclude = PathUtil.getExcludeDirs()).split("\n")
            TreeType.Integral -> PreparationUtil.tree(path = context.readBackupSavePath().first()).split("\n")
        }
    }

    suspend fun setTreeText(context: Context) {
        _uiState.value = uiState.value.copy(treeTextList = loadTree(context))
    }

    suspend fun setTreeType(context: Context, type: TreeType) {
        withIOContext {
            val uiState by uiState
            val typeList = uiState.typeList
            _uiState.value = uiState.copy(selectedIndex = typeList.indexOf(type))
            setTreeText(context)
        }
    }
}
