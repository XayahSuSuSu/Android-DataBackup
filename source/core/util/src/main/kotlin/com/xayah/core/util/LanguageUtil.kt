package com.xayah.core.util

import android.content.Context
import androidx.core.app.LocaleManagerCompat
import com.xayah.core.datastore.ConstantUtil
import java.util.Locale

object LanguageUtil {
    fun getSystemLocale(context: Context) = LocaleManagerCompat.getSystemLocales(context).get(0)!!

    fun String.toLocale(context: Context): Locale = if (this == ConstantUtil.LANGUAGE_SYSTEM) {
        getSystemLocale(context)
    } else {
        Locale.forLanguageTag(this)
    }
}
