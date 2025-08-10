package com.xayah.core.util

import android.content.Context
import androidx.core.app.LocaleManagerCompat
import com.xayah.core.datastore.ConstantUtil
import com.xayah.core.datastore.readLanguage
import com.xayah.core.util.LanguageUtil.toLocale
import kotlinx.coroutines.flow.map
import java.util.Locale

fun Context.readMappedLanguage() = readLanguage().map { it.toLocale(this) }

object LanguageUtil {
    fun getSystemLocale(context: Context) = LocaleManagerCompat.getSystemLocales(context).get(0)!!

    fun String.toLocale(context: Context): Locale = if (this == ConstantUtil.LANGUAGE_SYSTEM) {
        getSystemLocale(context)
    } else {
        Locale.forLanguageTag(this)
    }
}
