package com.zipflash.mrp.helper

import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class FileHelperTest {

    @Test
    fun `getFileName extracts from file URI path`() {
        val uri = mockk<Uri>()
        every { uri.scheme } returns "file"
        every { uri.path } returns "/storage/emulated/0/Download/script.sh"

        val context = mockk<android.content.Context>()
        every { context.contentResolver } returns mockk()

        val result = FileHelper.getFileName(context, uri)
        assertEquals("script.sh", result)
    }

    @Test
    fun `getFileName returns default name when path is null`() {
        val uri = mockk<Uri>()
        every { uri.scheme } returns "unknown"
        every { uri.path } returns null

        val context = mockk<android.content.Context>()
        every { context.contentResolver } returns mockk()

        val result = FileHelper.getFileName(context, uri)
        assertEquals("script.sh", result)
    }

    @Test
    fun `getFileName extracts name after last slash`() {
        val uri = mockk<Uri>()
        every { uri.scheme } returns "unknown"
        every { uri.path } returns "some/path/to/my_script.zip"

        val context = mockk<android.content.Context>()
        every { context.contentResolver } returns mockk()

        val result = FileHelper.getFileName(context, uri)
        assertEquals("my_script.zip", result)
    }

    @Test
    fun `getMRPDir returns correct path`() {
        val dir = FileHelper.getMRPDir()
        assertEquals(File("/data/local/tmp/MRP"), dir)
    }
}
