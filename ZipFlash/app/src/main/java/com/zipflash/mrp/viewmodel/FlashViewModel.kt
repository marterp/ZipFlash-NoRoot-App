package com.zipflash.mrp.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zipflash.mrp.helper.AppManager
import com.zipflash.mrp.helper.FileHelper
import com.zipflash.mrp.helper.ScriptRunner
import com.zipflash.mrp.helper.ShellHelper
import com.zipflash.mrp.helper.ZipExtractor
import com.zipflash.mrp.models.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FlashUiState(
    val outputText: String = "",
    val isRunning: Boolean = false,
    val isFileSelected: Boolean = false,
    val allApps: List<AppInfo> = emptyList(),
    val workDir: String? = null,
    val showSuccess: Boolean = false,
)

class FlashViewModel : ViewModel() {

    private var selectedZipUri: Uri? = null

    private val _uiState = MutableStateFlow(FlashUiState())
    val uiState: StateFlow<FlashUiState> = _uiState.asStateFlow()

    fun selectFile(uri: Uri, fileName: String?, anyFileMode: Boolean) {
        selectedZipUri = uri
        val sb = StringBuilder()
        sb.append("[✓] File selected: $fileName\n")

        val supported = when {
            fileName?.endsWith(".zip") == true -> {
                sb.append("[✓] ZIP selected. Ready to run.\n")
                true
            }
            fileName?.endsWith(".sh") == true -> {
                sb.append("[✓] SH script selected. Ready to run.\n")
                true
            }
            anyFileMode -> {
                sb.append("[✓] Ready to run\n")
                true
            }
            else -> {
                sb.append("[!] Unsupported file type.\n")
                false
            }
        }

        _uiState.value = _uiState.value.copy(
            outputText = sb.toString(),
            isFileSelected = supported
        )
    }

    fun clearFileSelection() {
        selectedZipUri = null
        _uiState.value = _uiState.value.copy(
            outputText = "",
            isFileSelected = false,
            showSuccess = false
        )
    }

    fun runScript(context: Context, revert: Boolean) {
        val uri = selectedZipUri ?: return
        _uiState.value = _uiState.value.copy(isRunning = true, showSuccess = false)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val fileName = FileHelper.getFileName(context, uri)

                if (fileName != null && fileName.endsWith(".zip")) {
                    val workDir = ZipExtractor.extractToModules(context, uri, null)
                    appendOutput("[✓] ZIP Extracted to: $workDir\n")

                    val scriptToRun = if (revert) "$workDir/revert.sh" else "$workDir/run.sh"
                    runShellScriptBlocking(context, scriptToRun)
                } else {
                    ScriptRunner.runSingleShBlocking(context, uri, object : ScriptRunner.OnScriptFinishedListener {
                        override fun onFinished() {
                            viewModelScope.launch {
                                _uiState.value = _uiState.value.copy(showSuccess = true, isRunning = false)
                            }
                        }

                        override fun onError(error: String) {
                            appendOutput("[!] Error: $error\n")
                            _uiState.value = _uiState.value.copy(isRunning = false)
                        }
                    })
                }
            } catch (e: Exception) {
                appendOutput("[!] Error: ${e.message}\n")
                _uiState.value = _uiState.value.copy(isRunning = false)
            }
        }
    }

    private suspend fun runShellScriptBlocking(context: Context, scriptPath: String) {
        ShellHelper.runShellScriptBlocking(context, scriptPath,
            object : ShellHelper.OnScriptFinishedListener {
                override fun onFinished() {
                    viewModelScope.launch {
                        _uiState.value = _uiState.value.copy(showSuccess = true, isRunning = false)
                    }
                }

                override fun onError(error: String) {
                    appendOutput("[!] Error: $error\n")
                    _uiState.value = _uiState.value.copy(isRunning = false)
                }
            })
    }

    fun appendOutput(text: String) {
        _uiState.value = _uiState.value.copy(
            outputText = _uiState.value.outputText + text
        )
    }

    fun preloadApps(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val apps = AppManager.loadInstalledApps(context)
            _uiState.value = _uiState.value.copy(allApps = apps)
        }
    }

    fun createMRPFolder(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            ShellHelper.runShellCommandBlocking("mkdir -p /data/local/tmp/MRP")
        }
    }

    fun dismissSuccess() {
        _uiState.value = _uiState.value.copy(showSuccess = false)
    }

    fun getSelectedUri(): Uri? = selectedZipUri
}
