package com.zipflash.mrp

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

class SettingsHelper(context: Context) {

    enum class FontType(val key: String, val typeface: Typeface) {
        SANS("sans", Typeface.SANS_SERIF),
        SERIF("serif", Typeface.SERIF),
        MONOSPACE("monospace", Typeface.MONOSPACE);

        companion object {
            fun fromKey(key: String?): FontType {
                return when (key) {
                    "sans" -> SANS
                    "serif" -> SERIF
                    else -> MONOSPACE
                }
            }
        }
    }

    enum class ThemeMode(val value: Int) {
        LIGHT(0),
        DARK(1),
        FOLLOW_SYSTEM(2);

        companion object {
            fun fromValue(value: Int): ThemeMode {
                return when (value) {
                    0 -> LIGHT
                    1 -> DARK
                    else -> FOLLOW_SYSTEM
                }
            }
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    val font: FontType
        get() {
            val fontKey = prefs.getString(KEY_FONT, FontType.MONOSPACE.key)!!
            return FontType.fromKey(fontKey)
        }

    val themeMode: ThemeMode
        get() {
            val mode = prefs.getInt(KEY_THEME_MODE, ThemeMode.DARK.value)
            return ThemeMode.fromValue(mode)
        }

    val language: String
        get() = prefs.getString(KEY_LANGUAGE, "system")!!

    val isAnyFileMode: Boolean
        get() = prefs.getBoolean(KEY_ANY_FILE_MODE, false)

    val showActivities: Boolean
        get() = prefs.getBoolean(KEY_SHOW_ACTIVITIES, false)

    val showSystemApps: Boolean
        get() = prefs.getBoolean(KEY_SHOW_SYSTEM_APPS, false)

    val isOptimizeScriptEnabled: Boolean
        get() = prefs.getBoolean(KEY_OPTIMIZE_SCRIPT, false)

    val isModuleSettingsEnabled: Boolean
        get() = prefs.getBoolean(KEY_MODULE_SETTINGS, false)

    val isGameModeEnabled: Boolean
        get() = prefs.getBoolean(KEY_GAME_MODE, false)

    fun setFont(font: FontType) {
        prefs.edit().putString(KEY_FONT, font.key).apply()
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putInt(KEY_THEME_MODE, mode.value).apply()
    }

    fun setLanguage(languageCode: String?) {
        prefs.edit().putString(KEY_LANGUAGE, languageCode ?: "system").apply()
    }

    fun setAnyFileMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ANY_FILE_MODE, enabled).apply()
    }

    fun setShowActivities(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_ACTIVITIES, enabled).apply()
    }

    fun setShowSystemApps(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_SYSTEM_APPS, enabled).apply()
    }

    fun setOptimizeScriptEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_OPTIMIZE_SCRIPT, enabled).apply()
    }

    fun setModuleSettingsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MODULE_SETTINGS, enabled).apply()
    }

    fun setGameModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GAME_MODE, enabled).apply()
    }

    fun updateMultipleSettings(
        anyFileMode: Boolean?,
        showActivities: Boolean?,
        showSystemApps: Boolean?,
        optimizeScript: Boolean?
    ) {
        val editor = prefs.edit()
        if (anyFileMode != null) editor.putBoolean(KEY_ANY_FILE_MODE, anyFileMode)
        if (showActivities != null) editor.putBoolean(KEY_SHOW_ACTIVITIES, showActivities)
        if (showSystemApps != null) editor.putBoolean(KEY_SHOW_SYSTEM_APPS, showSystemApps)
        if (optimizeScript != null) editor.putBoolean(KEY_OPTIMIZE_SCRIPT, optimizeScript)
        editor.apply()
    }

    fun resetDefaults() {
        prefs.edit()
            .putString(KEY_FONT, FontType.MONOSPACE.key)
            .putBoolean(KEY_ANY_FILE_MODE, false)
            .putBoolean(KEY_SHOW_ACTIVITIES, false)
            .putBoolean(KEY_SHOW_SYSTEM_APPS, false)
            .putBoolean(KEY_OPTIMIZE_SCRIPT, false)
            .putBoolean(KEY_MODULE_SETTINGS, false)
            .putBoolean(KEY_GAME_MODE, false)
            .putInt(KEY_THEME_MODE, ThemeMode.DARK.value)
            .putString(KEY_LANGUAGE, "system")
            .apply()
    }

    fun applyFontToView(view: View) {
        val fontType = font
        applyTypefaceToViewGroup(view, fontType.typeface)
    }

    private fun applyTypefaceToViewGroup(view: View, typeface: Typeface) {
        when (view) {
            is ViewGroup -> {
                for (i in 0 until view.childCount) {
                    applyTypefaceToViewGroup(view.getChildAt(i), typeface)
                }
            }
            is TextView -> view.typeface = typeface
        }
    }

    companion object {
        private const val PREF_NAME = "AppSettings"
        private const val KEY_FONT = "font"
        private const val KEY_ANY_FILE_MODE = "any_file_mode"
        private const val KEY_SHOW_ACTIVITIES = "show_activities"
        private const val KEY_SHOW_SYSTEM_APPS = "show_system_apps"
        private const val KEY_OPTIMIZE_SCRIPT = "optimize_script"
        private const val KEY_MODULE_SETTINGS = "module_settings"
        private const val KEY_GAME_MODE = "game_mode"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_LANGUAGE = "language"
    }
}
