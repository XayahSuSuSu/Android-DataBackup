package com.xayah.core.data.repository

import android.content.Context
import androidx.annotation.StringRes
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ContextRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun getString(@StringRes resId: Int) = context.getString(resId)
}
