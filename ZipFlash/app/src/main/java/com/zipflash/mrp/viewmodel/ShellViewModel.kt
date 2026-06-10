package com.zipflash.mrp.viewmodel

import android.content.Context
import android.graphics.Color
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zipflash.mrp.helper.ShellHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

data class ShellUiState(
    val outputText: SpannableStringBuilder = SpannableStringBuilder(),
    val isRunning: Boolean = false,
    val currentDir: String = "/",
    val commandHistory: List<String> = emptyList(),
    val historyIndex: Int = 0,
)

class ShellViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ShellUiState())
    val uiState: StateFlow<ShellUiState> = _uiState.asStateFlow()

    private val savedHistory = mutableListOf<String>()

    fun loadHistory(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val joined = prefs.getString(HISTORY_KEY, "") ?: ""
        savedHistory.clear()
        if (joined.isNotEmpty()) {
            for (part in joined.split("\n")) {
                if (part.trim().isNotEmpty()) savedHistory.add(part)
            }
        }
        _uiState.value = _uiState.value.copy(
            commandHistory = savedHistory.toList(),
            historyIndex = savedHistory.size
        )
    }

    fun saveHistory(context: Context) {
        val sb = StringBuilder()
        for (cmd in savedHistory) {
            sb.append(cmd).append("\n")
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(HISTORY_KEY, sb.toString()).apply()
    }

    fun recordCommand(cmd: String) {
        if (cmd.isNotEmpty() && (savedHistory.isEmpty() || cmd != savedHistory.last())) {
            savedHistory.add(cmd)
        }
        _uiState.value = _uiState.value.copy(
            commandHistory = savedHistory.toList(),
            historyIndex = savedHistory.size
        )
    }

    fun navigateHistory(direction: Int): String? {
        val state = _uiState.value
        var index = state.historyIndex
        var result: String? = null

        when (direction) {
            KEY_UP -> {
                if (index > 0) {
                    index--
                    result = savedHistory[index]
                }
            }
            KEY_DOWN -> {
                if (index < savedHistory.size - 1) {
                    index++
                    result = savedHistory[index]
                } else if (index == savedHistory.size - 1) {
                    index++
                    result = ""
                }
            }
        }

        _uiState.value = _uiState.value.copy(historyIndex = index)
        return result
    }

    fun handleCommand(command: String) {
        val sp = SpannableStringBuilder("\nuser@device:${_uiState.value.currentDir} $ $command\n")
        _uiState.value = _uiState.value.copy(
            outputText = SpannableStringBuilder(_uiState.value.outputText).append(sp),
            isRunning = true
        )

        if (command.startsWith("cd ")) {
            handleCdCommand(command)
        } else {
            runCommand(command)
        }
    }

    private fun handleCdCommand(command: String) {
        val currentDir = _uiState.value.currentDir
        val newDir = command.substring(3).trim()
        val targetDir = when {
            newDir == ".." -> {
                val lastSlash = currentDir.lastIndexOf('/')
                if (lastSlash > 0) currentDir.substring(0, lastSlash) else "/"
            }
            newDir.startsWith("/") -> newDir
            else -> if (currentDir == "/") "/$newDir" else "$currentDir/$newDir"
        }

        val dir = java.io.File(targetDir)
        if (dir.exists() && dir.isDirectory) {
            _uiState.value = _uiState.value.copy(currentDir = dir.absolutePath)
            appendColoredLine("[#] Changed directory to: ${dir.absolutePath}", Color.parseColor("#FFA726"))
        } else {
            appendColoredLine("[!] No such directory: $targetDir", Color.RED)
        }
        _uiState.value = _uiState.value.copy(isRunning = false)
    }

    private fun runCommand(command: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val process = Shizuku.newProcess(
                    arrayOf("sh", "-c", command), null, _uiState.value.currentDir
                )

                val input = BufferedReader(InputStreamReader(process.inputStream))
                val error = BufferedReader(InputStreamReader(process.errorStream))

                val output = SpannableStringBuilder()
                var line: String?
                while (input.readLine().also { line = it } != null) output.append(line).append("\n")
                while (error.readLine().also { line = it } != null) {
                    val start = output.length
                    output.append("[ERR] $line\n")
                    output.setSpan(ForegroundColorSpan(Color.RED), start, output.length, 0)
                }

                process.waitFor()
                process.destroy()

                withContext(Dispatchers.Main) {
                    val current = _uiState.value.outputText
                    _uiState.value = _uiState.value.copy(
                        outputText = SpannableStringBuilder(current).append(output),
                        isRunning = false
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    appendColoredLine("Exception: ${e.message}", Color.RED)
                    _uiState.value = _uiState.value.copy(isRunning = false)
                }
            }
        }
    }

    fun appendColoredLine(text: String, color: Int) {
        val sp = SpannableStringBuilder("$text\n")
        sp.setSpan(ForegroundColorSpan(color), 0, sp.length, 0)
        _uiState.value = _uiState.value.copy(
            outputText = SpannableStringBuilder(_uiState.value.outputText).append(sp)
        )
    }

    fun clearOutput() {
        _uiState.value = _uiState.value.copy(outputText = SpannableStringBuilder())
    }

    companion object {
        const val KEY_UP = -1
        const val KEY_DOWN = 1
        private const val PREFS_NAME = "shell_history"
        private const val HISTORY_KEY = "cmds"
    }
}
