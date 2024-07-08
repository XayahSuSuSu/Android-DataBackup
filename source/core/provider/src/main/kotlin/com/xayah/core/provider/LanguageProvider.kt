package com.xayah.core.provider

import android.content.Context
import android.content.res.Resources
import com.xayah.core.datastore.readAppLanguage
import kotlinx.coroutines.flow.first
import java.util.Locale

class LanguageProvider private constructor() {
    companion object {
        suspend fun getLocalizedContext(context: Context): Context {
            val readLang = context.readAppLanguage().first()

            val locale = if (readLang == "auto") {
                 Resources.getSystem().configuration.locales[0]
            } else {
                val splitLocale = readLang.split('_')
                assert(splitLocale.size < 3)

                if (splitLocale.size < 2) {
                    Locale(splitLocale.first())
                } else {
                    Locale(splitLocale.first(), splitLocale.last())
                }
            }

            val conf = context.resources.configuration.apply {
                setLocale(locale)
                setLayoutDirection(locale)
            }
            return context.createConfigurationContext(conf)
        }
    }
}
