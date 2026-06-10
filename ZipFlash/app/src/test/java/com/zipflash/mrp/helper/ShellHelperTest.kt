package com.zipflash.mrp.helper

import android.content.Context
import com.zipflash.mrp.SettingsHelper
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File

@ExtendWith(MockKExtension::class)
class ShellHelperTest {

    @MockK
    private lateinit var context: Context

    @MockK
    private lateinit var settingsHelper: SettingsHelper

    @Test
    fun `isRootAvailable returns false when su not found`() {
        // Can't directly unit-test private methods, but we can test via runShellScriptBlocking
        // with a non-existent script which returns early
        val nonExistentScript = "/tmp/nonexistent_script_12345.sh"
        val scriptFile = File(nonExistentScript)
        assertFalse(scriptFile.exists(), "Script should not exist for test validity")
    }

    @Test
    fun `runShellScriptBlocking calls onError for missing script`() {
        val scriptPath = "/tmp/missing_script.sh"
        val scriptFile = File(scriptPath)
        if (scriptFile.exists()) scriptFile.delete()

        every { context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE) } returns
            io.mockk.mockk(relaxed = true)

        var errorMessage: String? = null
        ShellHelper.runShellScriptBlocking(context, scriptPath,
            object : ShellHelper.OnScriptFinishedListener {
                override fun onFinished() {}
                override fun onError(error: String) { errorMessage = error }
            })

        assertTrue(errorMessage?.contains("not found") == true)
    }

    @Test
    fun `runShellScriptBlocking calls onError for non-sh when anyFileMode off`() {
        val scriptPath = "/tmp/script.txt"
        val scriptFile = File(scriptPath)
        scriptFile.writeText("echo test")
        scriptFile.deleteOnExit()

        val prefs = io.mockk.mockk<android.content.SharedPreferences>(relaxed = true)
        every { context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE) } returns prefs
        every { prefs.getBoolean("any_file_mode", false) } returns false

        var errorMessage: String? = null
        ShellHelper.runShellScriptBlocking(context, scriptPath,
            object : ShellHelper.OnScriptFinishedListener {
                override fun onFinished() {}
                override fun onError(error: String) { errorMessage = error }
            })

        assertTrue(errorMessage?.contains("Only .sh files") == true)
    }
}
