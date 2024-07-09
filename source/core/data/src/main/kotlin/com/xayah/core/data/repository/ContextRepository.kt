package com.xayah.core.data.repository

import android.content.Context
import androidx.annotation.StringRes
import com.xayah.core.provider.LanguageProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

class ContextRepository @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val context = runBlocking { LanguageProvider.getLocalizedContext(context) }

    fun getString(@StringRes resId: Int) = context.getString(resId)

    suspend fun <T> withContext(block: suspend (context: Context) -> T) = run {
        block(context)
    }

    fun <T> withContextSync(block: (context: Context) -> T) = block(context)
}
