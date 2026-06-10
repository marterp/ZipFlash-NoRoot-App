package com.zipflash.mrp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zipflash.mrp.api.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class Module(
    val title: String,
    val linkText: String,
    val url: String,
    val description: String,
    val index: Int
)

data class ModulesUiState(
    val modules: List<Module> = emptyList(),
    val filteredModules: List<Module> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
)

class ModulesViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ModulesUiState())
    val uiState: StateFlow<ModulesUiState> = _uiState.asStateFlow()

    private var cachedModules: List<Module>? = null

    fun loadModules(forceRefresh: Boolean = false) {
        if (!forceRefresh && cachedModules != null && cachedModules!!.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(
                modules = cachedModules!!,
                filteredModules = cachedModules!!,
                isLoading = false,
                isRefreshing = false,
                error = null
            )
            return
        }

        if (_uiState.value.isLoading) return

        _uiState.value = _uiState.value.copy(
            isLoading = _uiState.value.modules.isEmpty(),
            isRefreshing = _uiState.value.modules.isNotEmpty(),
            error = null
        )

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dtos = ApiClient.modulesApi.getModules()
                val moduleList = dtos.mapIndexed { index, dto ->
                    Module(
                        title = dto.title,
                        linkText = dto.linkText,
                        url = dto.url,
                        description = dto.description,
                        index = index
                    )
                }
                cachedModules = moduleList

                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        modules = moduleList,
                        filteredModules = moduleList,
                        isLoading = false,
                        isRefreshing = false,
                        error = if (moduleList.isEmpty()) "Failed to load modules. Please try again later." else null
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    val hasData = _uiState.value.modules.isNotEmpty()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = if (!hasData) {
                            if (e is java.net.UnknownHostException || e is java.net.ConnectException)
                                "No internet connection. Please check Wi-Fi or mobile data."
                            else
                                "Failed to load modules. Please try again later."
                        } else null
                    )
                }
            }
        }
    }

    fun filter(query: String?) {
        val filtered = if (query.isNullOrBlank()) {
            _uiState.value.modules
        } else {
            val q = query.lowercase().trim()
            _uiState.value.modules.filter {
                it.title.lowercase().contains(q) || it.description.lowercase().contains(q)
            }
        }
        _uiState.value = _uiState.value.copy(filteredModules = filtered)
    }

    fun sortBy(sortType: Int) {
        val sorted = when (sortType) {
            0 -> _uiState.value.filteredModules.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
            1 -> _uiState.value.filteredModules.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.title })
            2 -> _uiState.value.filteredModules.sortedByDescending { it.index }
            else -> _uiState.value.filteredModules
        }
        _uiState.value = _uiState.value.copy(filteredModules = sorted)
    }
}
