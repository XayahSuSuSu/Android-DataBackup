package com.xayah.feature.main.tree

import android.content.Context
import com.xayah.core.util.command.Tree
import com.xayah.core.util.localBackupSaveDir
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class TreeRepository @Inject constructor(@ApplicationContext private val context: Context) {
    suspend fun tree(src: String, exclusionList: List<String>): List<String> = Tree.tree(src = src, exclusionList = exclusionList).out

    val filterList = listOf(context.getString(R.string.simplify), context.getString(R.string.integral))

    fun getTargetPath() = context.localBackupSaveDir()

    fun getExclusionList(index: Int) = when (index) {
        1 -> listOf()
        else -> listOf("tree", "configs", "log")
    }
}
