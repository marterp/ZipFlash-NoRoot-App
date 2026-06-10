package com.zipflash.mrp

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SettingsHelperTest {

    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var settings: SettingsHelper

    @BeforeEach
    fun setUp() {
        prefs = mockk(relaxed = true)
        editor = mockk(relaxUnitFun = true)

        every { prefs.getString(any(), any()) } answers { secondArg() }
        every { prefs.getInt(any(), any()) } answers { secondArg<Int>() }
        every { prefs.getBoolean(any(), any()) } answers { secondArg<Boolean>() }
        every { prefs.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.putInt(any(), any()) } returns editor
        every { editor.putBoolean(any(), any()) } returns editor

        val context = mockk<Context>()
        every { context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE) } returns prefs

        settings = SettingsHelper(context)
    }

    @Test
    fun `themeMode returns dark by default`() {
        every { prefs.getInt("theme_mode", 1) } returns 1
        assertEquals(SettingsHelper.ThemeMode.DARK, settings.themeMode)
    }

    @Test
    fun `themeMode returns light when set to 0`() {
        every { prefs.getInt("theme_mode", 1) } returns 0
        assertEquals(SettingsHelper.ThemeMode.LIGHT, settings.themeMode)
    }

    @Test
    fun `themeMode returns follow system when set to 2`() {
        every { prefs.getInt("theme_mode", 1) } returns 2
        assertEquals(SettingsHelper.ThemeMode.FOLLOW_SYSTEM, settings.themeMode)
    }

    @Test
    fun `language returns system by default`() {
        every { prefs.getString("language", "system") } returns "system"
        assertEquals("system", settings.language)
    }

    @Test
    fun `language returns custom value`() {
        every { prefs.getString("language", "system") } returns "es"
        assertEquals("es", settings.language)
    }

    @Test
    fun `isAnyFileMode returns false by default`() {
        every { prefs.getBoolean("any_file_mode", false) } returns false
        assertEquals(false, settings.isAnyFileMode)
    }

    @Test
    fun `isAnyFileMode returns true when set`() {
        every { prefs.getBoolean("any_file_mode", false) } returns true
        assertEquals(true, settings.isAnyFileMode)
    }

    @Test
    fun `showActivities returns false by default`() {
        every { prefs.getBoolean("show_activities", false) } returns false
        assertEquals(false, settings.showActivities)
    }

    @Test
    fun `showSystemApps returns default value`() {
        assertEquals(false, settings.showSystemApps)
    }

    @Test
    fun `setThemeMode stores theme value`() {
        settings.setThemeMode(SettingsHelper.ThemeMode.LIGHT)
        verify { editor.putInt("theme_mode", 0) }
        verify { editor.apply() }
    }

    @Test
    fun `setLanguage stores language code`() {
        settings.setLanguage("fr")
        verify { editor.putString("language", "fr") }
        verify { editor.apply() }
    }

    @Test
    fun `setLanguage with null resets to system`() {
        settings.setLanguage(null)
        verify { editor.putString("language", "system") }
        verify { editor.apply() }
    }

    @Test
    fun `updateMultipleSettings with all values`() {
        settings.updateMultipleSettings(true, true, true, true)
        verify { editor.putBoolean("any_file_mode", true) }
        verify { editor.putBoolean("show_activities", true) }
        verify { editor.putBoolean("show_system_apps", true) }
        verify { editor.putBoolean("optimize_script", true) }
        verify { editor.apply() }
    }

    @Test
    fun `updateMultipleSettings with null values skips those keys`() {
        settings.updateMultipleSettings(true, null, null, null)
        verify(exactly = 1) { editor.putBoolean(any(), any()) }
        verify { editor.putBoolean("any_file_mode", true) }
        verify { editor.apply() }
    }

    @Test
    fun `setShowActivities stores value`() {
        settings.setShowActivities(true)
        verify { editor.putBoolean("show_activities", true) }
        verify { editor.apply() }
    }

    @Test
    fun `setShowSystemApps stores value`() {
        settings.setShowSystemApps(true)
        verify { editor.putBoolean("show_system_apps", true) }
        verify { editor.apply() }
    }

    @Test
    fun `setOptimizeScriptEnabled stores value`() {
        settings.setOptimizeScriptEnabled(true)
        verify { editor.putBoolean("optimize_script", true) }
        verify { editor.apply() }
    }

    @Test
    fun `setModuleSettingsEnabled stores value`() {
        settings.setModuleSettingsEnabled(true)
        verify { editor.putBoolean("module_settings", true) }
        verify { editor.apply() }
    }

    @Test
    fun `setGameModeEnabled stores value`() {
        settings.setGameModeEnabled(true)
        verify { editor.putBoolean("game_mode", true) }
        verify { editor.apply() }
    }
}
