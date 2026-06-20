package com.xayah.dex;

import java.util.Locale;

class AppLocale {
    static Locale parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        Locale locale = Locale.forLanguageTag(value.trim().replace('_', '-'));
        for (String language : Locale.getISOLanguages()) {
            if (language.equals(locale.getLanguage())) {
                return locale;
            }
        }
        return null;
    }
}
