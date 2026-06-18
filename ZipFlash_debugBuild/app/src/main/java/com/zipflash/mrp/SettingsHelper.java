package com.zipflash.mrp;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Centralized app settings helper (Java 7 compatible).
 * - Strongly-typed FontType enum
 * - Strongly-typed ThemeMode enum (defaults to DARK)
 * - Batch updates with single apply()
 * - Recursive font application utility
 */
public class SettingsHelper {

    // Preference file and keys
    private static final String PREF_NAME = "AppSettings";
    private static final String KEY_FONT = "font";
    private static final String KEY_ANY_FILE_MODE = "any_file_mode";
    private static final String KEY_SHOW_ACTIVITIES = "show_activities";
    private static final String KEY_SHOW_SYSTEM_APPS = "show_system_apps";
    private static final String KEY_OPTIMIZE_SCRIPT = "optimize_script";
    private static final String KEY_MODULE_SETTINGS = "module_settings";
    private static final String KEY_GAME_MODE = "game_mode";
    private static final String KEY_THEME_MODE = "theme_mode"; // 0=LIGHT, 1=DARK, 2=FOLLOW_SYSTEM
    private static final String KEY_LANGUAGE = "language";

    private final SharedPreferences prefs;

    public SettingsHelper(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // -------------------------
    // Font enum and accessors
    // -------------------------
    public enum FontType {
        SANS("sans", Typeface.SANS_SERIF),
        SERIF("serif", Typeface.SERIF),
        MONOSPACE("monospace", Typeface.MONOSPACE);

        private final String key;
        private final Typeface typeface;

        FontType(String key, Typeface typeface) {
            this.key = key;
            this.typeface = typeface;
        }

        public String getKey() {
            return key;
        }

        public Typeface getTypeface() {
            return typeface;
        }

        public static FontType fromKey(String key) {
            if (key == null) return MONOSPACE;
            if ("sans".equals(key)) return SANS;
            if ("serif".equals(key)) return SERIF;
            return MONOSPACE;
        }
    }

    public FontType getFont() {
        String fontKey = prefs.getString(KEY_FONT, FontType.MONOSPACE.getKey());
        return FontType.fromKey(fontKey);
    }

    public void setFont(FontType font) {
        if (font == null) font = FontType.MONOSPACE;
        prefs.edit().putString(KEY_FONT, font.getKey()).apply();
    }

    // -------------------------
    // Theme enum and accessors
    // -------------------------
    public enum ThemeMode {
        LIGHT(0),
        DARK(1),
        FOLLOW_SYSTEM(2);

        private final int value;

        ThemeMode(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static ThemeMode fromValue(int value) {
            switch (value) {
                case 0: return LIGHT;
                case 1: return DARK;
                case 2:
                default: return FOLLOW_SYSTEM;
            }
        }
    }

    // Default to DARK if nothing stored
    public ThemeMode getThemeMode() {
        int mode = prefs.getInt(KEY_THEME_MODE, ThemeMode.DARK.getValue());
        return ThemeMode.fromValue(mode);
    }

    public void setThemeMode(ThemeMode mode) {
        if (mode == null) mode = ThemeMode.DARK;
        prefs.edit().putInt(KEY_THEME_MODE, mode.getValue()).apply();
    }

    // -------------------------
    // Language accessors
    // -------------------------
    public String getLanguage() {
        return prefs.getString(KEY_LANGUAGE, "system");
    }

    public void setLanguage(String languageCode) {
        if (languageCode == null) languageCode = "system";
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply();
    }

    // -------------------------
    // Boolean toggles
    // -------------------------
    public boolean isAnyFileMode() {
        return prefs.getBoolean(KEY_ANY_FILE_MODE, false);
    }

    public void setAnyFileMode(boolean enabled) {
        prefs.edit().putBoolean(KEY_ANY_FILE_MODE, enabled).apply();
    }

    public boolean getShowActivities() {
        return prefs.getBoolean(KEY_SHOW_ACTIVITIES, false);
    }

    public void setShowActivities(boolean enabled) {
        prefs.edit().putBoolean(KEY_SHOW_ACTIVITIES, enabled).apply();
    }

    public boolean getShowSystemApps() {
        return prefs.getBoolean(KEY_SHOW_SYSTEM_APPS, false);
    }

    public void setShowSystemApps(boolean enabled) {
        prefs.edit().putBoolean(KEY_SHOW_SYSTEM_APPS, enabled).apply();
    }

    public boolean isOptimizeScriptEnabled() {
        return prefs.getBoolean(KEY_OPTIMIZE_SCRIPT, false);
    }

    public void setOptimizeScriptEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_OPTIMIZE_SCRIPT, enabled).apply();
    }

    public boolean isModuleSettingsEnabled() {
        return prefs.getBoolean(KEY_MODULE_SETTINGS, false);
    }

    public void setModuleSettingsEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_MODULE_SETTINGS, enabled).apply();
    }

    public boolean isGameModeEnabled() {
        return prefs.getBoolean(KEY_GAME_MODE, false);
    }

    public void setGameModeEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_GAME_MODE, enabled).apply();
    }
	

    // -------------------------
    // Batch update helper
    // -------------------------
    public void updateMultipleSettings(Boolean anyFileMode,
                                       Boolean showActivities,
                                       Boolean showSystemApps,
                                       Boolean optimizeScript) {
        SharedPreferences.Editor editor = prefs.edit();
        if (anyFileMode != null) editor.putBoolean(KEY_ANY_FILE_MODE, anyFileMode.booleanValue());
        if (showActivities != null) editor.putBoolean(KEY_SHOW_ACTIVITIES, showActivities.booleanValue());
        if (showSystemApps != null) editor.putBoolean(KEY_SHOW_SYSTEM_APPS, showSystemApps.booleanValue());
        if (optimizeScript != null) editor.putBoolean(KEY_OPTIMIZE_SCRIPT, optimizeScript.booleanValue());
        editor.apply();
    }

    // -------------------------
    // Reset to defaults
    // -------------------------
    public void resetDefaults() {
        prefs.edit()
			.putString(KEY_FONT, FontType.MONOSPACE.getKey())
			.putBoolean(KEY_ANY_FILE_MODE, false)
			.putBoolean(KEY_SHOW_ACTIVITIES, false)
			.putBoolean(KEY_SHOW_SYSTEM_APPS, false)
			.putBoolean(KEY_OPTIMIZE_SCRIPT, false)
			.putBoolean(KEY_MODULE_SETTINGS, false)
			.putBoolean(KEY_GAME_MODE, false)
			.putInt(KEY_THEME_MODE, ThemeMode.DARK.getValue()) // default DARK
			.putString(KEY_LANGUAGE, "system")
			.apply();
    }

    // -------------------------
    // UI font applicator
    // -------------------------
    public void applyFontToView(View view) {
        FontType fontType = getFont();
        applyTypefaceToViewGroup(view, fontType.getTypeface());
    }

    private void applyTypefaceToViewGroup(View view, Typeface typeface) {
        if (view == null) return;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyTypefaceToViewGroup(group.getChildAt(i), typeface);
            }
        } else if (view instanceof TextView) {
            ((TextView) view).setTypeface(typeface);
        }
    }
}
