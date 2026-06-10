package com.zipflash.mrp.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ModulesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: ModulesViewModel
    private lateinit var mutableState: MutableStateFlow<ModulesUiState>

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ModulesViewModel()
        val field = ModulesViewModel::class.java.getDeclaredField("_uiState")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        mutableState = field.get(viewModel) as MutableStateFlow<ModulesUiState>
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is empty`() {
        val state = viewModel.uiState.value
        assertEquals(true, state.modules.isEmpty())
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `filter with null shows all`() {
        viewModel.filter(null)
        assertEquals(0, viewModel.uiState.value.filteredModules.size)
    }

    @Test
    fun `filter with blank shows all`() {
        viewModel.filter("")
        assertEquals(0, viewModel.uiState.value.filteredModules.size)
    }

    @Test
    fun `filter matches title case-insensitively`() {
        val modules = listOf(
            Module("Alpha Module", "link", "url", "Some description", 0),
            Module("Beta Module", "link", "url", "Another description", 1),
            Module("Gamma", "link", "url", "Alpha related", 2)
        )
        mutableState.value = ModulesUiState(modules = modules, filteredModules = modules)

        viewModel.filter("alpha")
        val filtered = viewModel.uiState.value.filteredModules
        assertEquals(2, filtered.size)
        assertEquals("Alpha Module", filtered[0].title)
        assertEquals("Gamma", filtered[1].title)
    }

    @Test
    fun `sortBy 0 sorts ascending case-insensitive`() {
        val modules = listOf(
            Module("beta", "", "", "", 0),
            Module("Alpha", "", "", "", 1),
            Module("CHarlie", "", "", "", 2)
        )
        mutableState.value = ModulesUiState(modules = modules, filteredModules = modules)

        viewModel.sortBy(0)
        val sorted = viewModel.uiState.value.filteredModules
        assertEquals("Alpha", sorted[0].title)
        assertEquals("beta", sorted[1].title)
        assertEquals("CHarlie", sorted[2].title)
    }

    @Test
    fun `sortBy 1 sorts descending case-insensitive`() {
        val modules = listOf(
            Module("Alpha", "", "", "", 0),
            Module("beta", "", "", "", 1),
            Module("CHarlie", "", "", "", 2)
        )
        mutableState.value = ModulesUiState(modules = modules, filteredModules = modules)

        viewModel.sortBy(1)
        val sorted = viewModel.uiState.value.filteredModules
        assertEquals("CHarlie", sorted[0].title)
        assertEquals("beta", sorted[1].title)
        assertEquals("Alpha", sorted[2].title)
    }

    @Test
    fun `sortBy 2 sorts by index descending`() {
        val modules = listOf(
            Module("a", "", "", "", 0),
            Module("b", "", "", "", 1),
            Module("c", "", "", "", 2)
        )
        mutableState.value = ModulesUiState(modules = modules, filteredModules = modules)

        viewModel.sortBy(2)
        val sorted = viewModel.uiState.value.filteredModules
        assertEquals("c", sorted[0].title)
        assertEquals("b", sorted[1].title)
        assertEquals("a", sorted[2].title)
    }

    @Test
    fun `loadModules without force uses cache`() {
        val modules = listOf(Module("Cached", "", "", "", 0))
        mutableState.value = ModulesUiState(modules = modules, filteredModules = modules)

        viewModel.loadModules(forceRefresh = false)
        assertEquals(1, viewModel.uiState.value.modules.size)
        assertEquals("Cached", viewModel.uiState.value.modules[0].title)
    }
}
