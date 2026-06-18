// app/src/main/java/com/zipflash/mrp/util/LocaleUtils.java
package com.zipflash.mrp.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

import java.util.Locale;

public final class LocaleUtils {

    // Must match SettingsHelper
    private static final String PREF_NAME = "AppSettings"; // same as SettingsHelper [file:2]
    private static final String KEY_LANGUAGE = "language";  // same as SettingsHelper [file:2]

    private LocaleUtils() {}

    public static String readSavedLanguage(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return p.getString(KEY_LANGUAGE, "system"); // default to system [file:2]
    }

    public static Context wrap(Context base, String languageCode) {
        if (languageCode == null || "system".equals(languageCode)) {
            return base; // use device locale [file:2]
        }
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Resources res = base.getResources();
        Configuration config = new Configuration(res.getConfiguration());
        if (Build.VERSION.SDK_INT >= 24) {
            config.setLocale(locale);
            return base.createConfigurationContext(config);
        } else {
            config.locale = locale;
            res.updateConfiguration(config, res.getDisplayMetrics());
            return base;
        }
    }
}
