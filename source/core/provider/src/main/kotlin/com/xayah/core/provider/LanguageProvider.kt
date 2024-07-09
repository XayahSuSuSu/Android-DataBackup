package com.xayah.core.provider

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import com.xayah.core.datastore.readAppLanguage
import kotlinx.coroutines.flow.first
import java.util.Locale

class LanguageProvider private constructor() {
    companion object {
        fun convertStringToLocale(localeStr: String): Locale {
            val splitLocale = localeStr.split('-')

            return if (splitLocale.size < 2) {
                Locale(splitLocale.first())
            } else {
                Locale(splitLocale.first(), splitLocale[1])
            }
        }

        suspend fun getLocalizedConfiguration(context: Context): Configuration {
            val readLang = context.readAppLanguage().first()

            val locale = if (readLang == "auto") {
                Resources.getSystem().configuration.locales[0]
            } else {
                convertStringToLocale(readLang)
            }

            return context.resources.configuration.apply {
                setLocale(locale)
                setLayoutDirection(locale)
            }
        }

        suspend fun getLocalizedContext(context: Context): Context {
            return context.createConfigurationContext(getLocalizedConfiguration(context))
        }
    }
}
