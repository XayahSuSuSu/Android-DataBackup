package com.xayah.feature.main.tree

import android.content.Context
import com.xayah.core.util.command.Tree
import com.xayah.core.util.localBackupSaveDir
import com.xayah.core.util.localRestoreSaveDir
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class TreeRepository @Inject constructor(@ApplicationContext private val context: Context) {
    suspend fun tree(src: String, exclusionList: List<String>): List<String> = Tree.tree(src = src, exclusionList = exclusionList).out

    val typeList = listOf(context.getString(R.string.backup_dir), context.getString(R.string.restore_dir))
    val filterList = listOf(context.getString(R.string.simplify), context.getString(R.string.integral))

    fun getTargetPath(index: Int) = when (index) {
        1 -> context.localRestoreSaveDir()
        else -> context.localBackupSaveDir()
    }

    fun getExclusionList(index: Int) = when (index) {
        1 -> listOf()
        else -> listOf("tree", "configs", "log")
    }
}
