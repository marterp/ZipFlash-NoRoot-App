package com.zipflash.mrp.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zipflash.mrp.helper.CheckPermHelper
import com.zipflash.mrp.manager.ModuleManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.File

data class ModuleManagerUiState(
    val modules: List<File> = emptyList(),
    val isLoading: Boolean = true,
)

class ModuleManagerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ModuleManagerUiState())
    val uiState: StateFlow<ModuleManagerUiState> = _uiState.asStateFlow()

    fun loadModules(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val modules = if (CheckPermHelper.isSkipShizuku(context) || !CheckPermHelper.hasShizukuPermission()) {
                loadLocalModules(context)
            } else {
                loadShizukuModules()
            }

            withContext(Dispatchers.Main) {
                _uiState.value = ModuleManagerUiState(modules = modules, isLoading = false)
            }
        }
    }

    private fun loadShizukuModules(): List<File> {
        val modules = mutableListOf<File>()
        val modulesDir = File("/data/local/tmp/modules")

        try {
            if (!modulesDir.exists()) {
                val mkdir = Shizuku.newProcess(arrayOf("mkdir", "-p", modulesDir.absolutePath), null, null)
                mkdir.waitFor()
                mkdir.destroy()
            }

            if (modulesDir.isDirectory) {
                val files = modulesDir.listFiles()
                if (files != null) {
                    for (f in files) {
                        if (f.isDirectory) modules.add(f)
                    }
                }
            }
        } catch (e: Exception) {
            // Error handled by returning empty list
        }

        return modules
    }

    private fun loadLocalModules(context: Context): List<File> {
        val mm = ModuleManager(context)
        val dir = mm.getModulesDir()
        val modules = mutableListOf<File>()
        val files = dir.listFiles()
        if (files != null) {
            for (f in files) {
                if (f.isFile && f.name.endsWith(".zip")) {
                    modules.add(f)
                }
            }
        }
        return modules
    }
}
