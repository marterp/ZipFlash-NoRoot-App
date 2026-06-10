package com.zipflash.mrp.viewmodel

import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FlashViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: FlashViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = FlashViewModel()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun mockUri(scheme: String = "content"): Uri {
        val uri = mockk<Uri>()
        every { uri.scheme } returns scheme
        return uri
    }

    @Test
    fun `initial state is correct`() {
        val state = viewModel.uiState.value
        assertEquals("", state.outputText)
        assertEquals(false, state.isRunning)
        assertEquals(false, state.isFileSelected)
        assertEquals(false, state.showSuccess)
    }

    @Test
    fun `selectFile with zip marks as selected`() {
        viewModel.selectFile(mockUri(), "module.zip", false)
        val state = viewModel.uiState.value
        assertEquals(true, state.isFileSelected)
        assertEquals(true, state.outputText.contains("module.zip"))
        assertEquals(true, state.outputText.contains("ZIP selected"))
    }

    @Test
    fun `selectFile with sh marks as selected`() {
        viewModel.selectFile(mockUri(), "script.sh", false)
        val state = viewModel.uiState.value
        assertEquals(true, state.isFileSelected)
        assertEquals(true, state.outputText.contains("SH script selected"))
    }

    @Test
    fun `selectFile with unsupported type does not mark as selected`() {
        viewModel.selectFile(mockUri(), "file.txt", false)
        val state = viewModel.uiState.value
        assertEquals(false, state.isFileSelected)
        assertEquals(true, state.outputText.contains("Unsupported file type"))
    }

    @Test
    fun `selectFile with anyFileMode allows any extension`() {
        viewModel.selectFile(mockUri(), "file.txt", true)
        val state = viewModel.uiState.value
        assertEquals(true, state.isFileSelected)
    }

    @Test
    fun `clearFileSelection resets state`() {
        viewModel.selectFile(mockUri(), "module.zip", false)
        viewModel.clearFileSelection()
        val state = viewModel.uiState.value
        assertEquals(false, state.isFileSelected)
        assertEquals("", state.outputText)
    }

    @Test
    fun `appendOutput adds text`() {
        viewModel.appendOutput("line1\n")
        viewModel.appendOutput("line2\n")
        assertEquals("line1\nline2\n", viewModel.uiState.value.outputText)
    }

    @Test
    fun `dismissSuccess sets showSuccess to false`() {
        viewModel.dismissSuccess()
        assertEquals(false, viewModel.uiState.value.showSuccess)
    }

    @Test
    fun `selectFile with null fileName handles gracefully`() {
        viewModel.selectFile(mockUri(), null, false)
        val state = viewModel.uiState.value
        assertEquals(false, state.isFileSelected)
    }
}
