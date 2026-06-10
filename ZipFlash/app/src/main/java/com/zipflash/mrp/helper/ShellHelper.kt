package com.zipflash.mrp.helper

import android.content.Context
import android.util.Log
import android.widget.TextView
import com.zipflash.mrp.SettingsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuRemoteProcess
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

object ShellHelper {
    interface OnScriptFinishedListener {
        fun onFinished()
        fun onError(error: String)
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun runShellScript(context: Context, scriptPath: String, outputView: TextView?, listener: OnScriptFinishedListener?) {
        val scriptFile = File(scriptPath)
        val settings = SettingsHelper(context)

        if (!scriptFile.exists()) {
            listener?.onError("Script not found: $scriptPath")
            return
        }

        if (!settings.isAnyFileMode && !scriptPath.endsWith(".sh")) {
            postLine(outputView, "[!] Only .sh files are allowed. Enable Any File Mode in Settings.\n")
            listener?.onError("Only .sh files allowed.")
            return
        }

        postClear(outputView, "[#] Running: ${scriptFile.name}\n")

        scope.launch {
            try {
                val command: Array<String> = if (scriptPath.endsWith(".sh")) {
                    arrayOf("sh", scriptFile.absolutePath)
                } else {
                    arrayOf(scriptFile.absolutePath)
                }

                if (CheckPermHelper.hasShizukuPermission()) {
                    runProcessWithShizuku(command, scriptFile.parent, outputView, listener)
                    return@launch
                }

                if (isRootAvailable()) {
                    runProcessWithRoot(command, scriptFile.parent, outputView, listener)
                    return@launch
                }

                listener?.onFinished()

            } catch (e: Exception) {
                listener?.onError(e.message ?: "Unknown error")
                postLine(outputView, "[!] Failed: ${e.message}\n")
            }
        }
    }

    fun runShellCommand(command: String, outputView: TextView?, listener: OnScriptFinishedListener?) {
        scope.launch {
            try {
                if (CheckPermHelper.hasShizukuPermission()) {
                    runProcessWithShizuku(arrayOf("sh", "-c", command), null, outputView, listener)
                    return@launch
                }

                if (isRootAvailable()) {
                    runProcessWithRoot(arrayOf("su", "-c", command), null, outputView, listener)
                    return@launch
                }

                listener?.onFinished()

            } catch (e: Exception) {
                listener?.onError(e.message ?: "Unknown error")
                postLine(outputView, "[!] Failed: ${e.message}\n")
            }
        }
    }

    fun runPrivilegedCommand(cmd: String): String {
        val output = StringBuilder()

        try {
            if (CheckPermHelper.hasShizukuPermission()) {
                val proc = Shizuku.newProcess(arrayOf("sh", "-c", cmd), null, null)
                val reader = BufferedReader(InputStreamReader(proc.inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    output.append(line).append("\n")
                }
                proc.waitFor()
                proc.destroy()
                return output.toString()
            }

            if (isRootAvailable()) {
                val p = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
                val reader = BufferedReader(InputStreamReader(p.inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    output.append(line).append("\n")
                }
                p.waitFor()
                return output.toString()
            }

            return ""
        } catch (t: Throwable) {
            Log.e("ZipFlash", "runPrivilegedCommand failed: ${t.message}")
            return ""
        }
    }

    private fun runProcessWithShizuku(command: Array<String>, workingDir: String?, outputView: TextView?, listener: OnScriptFinishedListener?) {
        val process = Shizuku.newProcess(command, null, workingDir)

        val input = BufferedReader(InputStreamReader(process.inputStream))
        val error = BufferedReader(InputStreamReader(process.errorStream))

        var line: String?
        while (input.readLine().also { line = it } != null) {
            postLine(outputView, "$line\n")
        }
        while (error.readLine().also { line = it } != null) {
            postLine(outputView, "[ERR] $line\n")
        }

        val exitCode = process.waitFor()
        process.destroy()

        if (exitCode == 0) {
            listener?.onFinished()
        } else {
            listener?.onError("Exit code: $exitCode")
        }
    }

    private fun runProcessWithRoot(command: Array<String>, workingDir: String?, outputView: TextView?, listener: OnScriptFinishedListener?) {
        val pb = ProcessBuilder(*command)
        if (workingDir != null) pb.directory(File(workingDir))
        val process = pb.start()

        val input = BufferedReader(InputStreamReader(process.inputStream))
        val error = BufferedReader(InputStreamReader(process.errorStream))

        var line: String?
        while (input.readLine().also { line = it } != null) {
            postLine(outputView, "$line\n")
        }
        while (error.readLine().also { line = it } != null) {
            postLine(outputView, "[ERR] $line\n")
        }

        val exitCode = process.waitFor()
        process.destroy()

        if (exitCode == 0) {
            listener?.onFinished()
        } else {
            listener?.onError("Exit code: $exitCode")
        }
    }

    private fun postLine(view: TextView?, line: String) {
        if (view == null) return
        view.post { view.append(line) }
    }

    private fun postClear(view: TextView?, line: String) {
        if (view == null) return
        view.post {
            view.text = ""
            view.append(line)
        }
    }

    fun runShellScriptBlocking(context: Context, scriptPath: String, listener: OnScriptFinishedListener?) {
        val scriptFile = File(scriptPath)
        val settings = SettingsHelper(context)

        if (!scriptFile.exists()) {
            listener?.onError("Script not found: $scriptPath")
            return
        }

        if (!settings.isAnyFileMode && !scriptPath.endsWith(".sh")) {
            listener?.onError("Only .sh files allowed.")
            return
        }

        try {
            val command: Array<String> = if (scriptPath.endsWith(".sh")) {
                arrayOf("sh", scriptFile.absolutePath)
            } else {
                arrayOf(scriptFile.absolutePath)
            }

            if (CheckPermHelper.hasShizukuPermission()) {
                runProcessWithShizukuBlocking(command, scriptFile.parent, listener)
                return
            }

            if (isRootAvailable()) {
                runProcessWithRootBlocking(command, scriptFile.parent, listener)
                return
            }

            listener?.onFinished()
        } catch (e: Exception) {
            listener?.onError(e.message ?: "Unknown error")
        }
    }

    fun runShellCommandBlocking(command: String): String {
        val output = StringBuilder()
        try {
            if (CheckPermHelper.hasShizukuPermission()) {
                val proc = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
                val reader = BufferedReader(InputStreamReader(proc.inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    output.append(line).append("\n")
                }
                proc.waitFor()
                proc.destroy()
                return output.toString()
            }
            if (isRootAvailable()) {
                val p = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
                val reader = BufferedReader(InputStreamReader(p.inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    output.append(line).append("\n")
                }
                p.waitFor()
                return output.toString()
            }
        } catch (t: Throwable) {
            Log.e("ZipFlash", "runShellCommandBlocking failed: ${t.message}")
        }
        return ""
    }

    private fun runProcessWithShizukuBlocking(command: Array<String>, workingDir: String?, listener: OnScriptFinishedListener?) {
        val process = Shizuku.newProcess(command, null, workingDir)

        val input = BufferedReader(InputStreamReader(process.inputStream))
        val error = BufferedReader(InputStreamReader(process.errorStream))

        var line: String?
        while (input.readLine().also { line = it } != null) { }
        while (error.readLine().also { line = it } != null) { }

        val exitCode = process.waitFor()
        process.destroy()

        if (exitCode == 0) {
            listener?.onFinished()
        } else {
            listener?.onError("Exit code: $exitCode")
        }
    }

    private fun runProcessWithRootBlocking(command: Array<String>, workingDir: String?, listener: OnScriptFinishedListener?) {
        val pb = ProcessBuilder(*command)
        if (workingDir != null) pb.directory(File(workingDir))
        val process = pb.start()

        val input = BufferedReader(InputStreamReader(process.inputStream))
        val error = BufferedReader(InputStreamReader(process.errorStream))

        var line: String?
        while (input.readLine().also { line = it } != null) { }
        while (error.readLine().also { line = it } != null) { }

        val exitCode = process.waitFor()
        process.destroy()

        if (exitCode == 0) {
            listener?.onFinished()
        } else {
            listener?.onError("Exit code: $exitCode")
        }
    }

    private var cachedRootAvailable: Boolean? = null

    private fun isRootAvailable(): Boolean {
        if (cachedRootAvailable != null) return cachedRootAvailable!!
        try {
            val p = Runtime.getRuntime().exec(arrayOf("which", "su"))
            val exit = p.waitFor()
            cachedRootAvailable = exit == 0
            return cachedRootAvailable!!
        } catch (e: Exception) {
            cachedRootAvailable = false
            return false
        }
    }

    fun resetRootCache() {
        cachedRootAvailable = null
    }
}
