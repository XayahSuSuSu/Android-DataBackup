package com.xayah.dex;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;

import java.util.Locale;

final class PackageManagerUtil {
    private PackageManagerUtil() {
    }

    static PackageManagerWithLocale getPackageManager(Context context) {
        Locale locale = AppLocale.parse(System.getenv("APP_LABEL_LOCALE"));
        return new PackageManagerWithLocale(getPackageManager(context, locale), locale);
    }

    private static PackageManager getPackageManager(Context context, Locale locale) {
        if (locale == null) {
            return context.getPackageManager();
        }
        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        configuration.setLocale(locale);
        return context.createConfigurationContext(configuration).getPackageManager();
    }

    record PackageManagerWithLocale(PackageManager packageManager, Locale locale) {
    }
}
